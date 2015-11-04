/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.jpa.container.weaving.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.spi.ClassTransformer;

import org.osgi.framework.Bundle;
import org.osgi.framework.hooks.weaving.WeavingException;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This weaving hook delegates to any registered {@link ClassTransformer} instances for a given bundle
 */
public class JPAWeavingHook implements WeavingHook, TransformerRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(JPAWeavingHook.class);

    /**
     * With luck we will only have one persistence unit per bundle, but if we don't we'll need to call them
     * until one of them does a transform or we run out.
     */
    private final Map<Bundle, LinkedHashSet<ClassTransformer>> registeredTransformers = new HashMap<Bundle, LinkedHashSet<ClassTransformer>>();

    /**
     * This constructor should not be called directly, the {@link JPAWeavingHookFactory} should be used to
     * ensure that Weaving support is available.
     */
    JPAWeavingHook() {
    }

    public void weave(WovenClass wovenClass) {
        BundleWiring wiring = wovenClass.getBundleWiring();
        Bundle bundle = wiring.getBundle();
        ClassLoader cl = wiring.getClassLoader();
        Collection<ClassTransformer> transformersToTry = getTransformers(bundle);
        for (ClassTransformer transformer : transformersToTry) {
            if (transformClass(wovenClass, cl, transformer)) {
                LOGGER.info("Weaving " + wovenClass.getClassName() + " using " + transformer.getClass().getName());
                break;
            };
        }
        Class<?> dClass = wovenClass.getDefinedClass();
        if (transformersToTry.size() == 0 && dClass != null && dClass.getAnnotation(Entity.class) != null) {
            LOGGER.warn("Loading " + wovenClass.getClassName() + " before transformer is present");
        }
    }

    @SuppressWarnings("unchecked")
    private synchronized Collection<ClassTransformer> getTransformers(Bundle bundle) {
        LinkedHashSet<ClassTransformer> transformers = registeredTransformers.get(bundle);
        return transformers != null ? new ArrayList<ClassTransformer>(transformers) : Collections.EMPTY_LIST;
    }

    private boolean transformClass(WovenClass wovenClass, ClassLoader cl, ClassTransformer transformer)
        throws ThreadDeath, OutOfMemoryError {
        try {
            byte[] result = transformer
                .transform(cl, 
                           wovenClass.getClassName(),
                           wovenClass.getDefinedClass(), 
                           wovenClass.getProtectionDomain(),
                           wovenClass.getBytes());
            if (result != null) {
                wovenClass.setBytes(result);
                wovenClass.getDynamicImports().add("org.eclipse.persistence.*");
                wovenClass.getDynamicImports().add("org.apache.openjpa.*");
                return true;
            }
        } catch (Throwable t) {
            if (t instanceof ThreadDeath)
                throw (ThreadDeath)t;
            else if (t instanceof OutOfMemoryError)
                throw (OutOfMemoryError)t;
            else {
                Bundle b = wovenClass.getBundleWiring().getBundle();
                String msg = String.format("Weaving failure", wovenClass.getClassName(),
                                           b.getSymbolicName(), b.getVersion(), transformer);
                throw new WeavingException(msg, t);
            }
        }
        return false;
    }

    public synchronized void addTransformer(Bundle pBundle, ClassTransformer transformer) {
        LOGGER.info("Adding transformer " + transformer.getClass().getName());
        LinkedHashSet<ClassTransformer> transformers = registeredTransformers.get(pBundle);
        if (transformers == null) {
            transformers = new LinkedHashSet<ClassTransformer>();
            registeredTransformers.put(pBundle, transformers);
        }
        transformers.add(transformer);
    }

    public synchronized void removeTransformer(Bundle pBundle, ClassTransformer transformer) {
        LinkedHashSet<ClassTransformer> set = registeredTransformers.get(pBundle);
        if (set == null || !set.remove(transformer)) {
            throw new IllegalStateException("Transformer " + transformer + " not registered");
        }
        if (set.isEmpty()) {
            registeredTransformers.remove(pBundle);
        }
    }

}
