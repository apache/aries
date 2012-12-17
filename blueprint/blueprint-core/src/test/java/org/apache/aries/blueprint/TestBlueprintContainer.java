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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.aries.blueprint.container.AggregateConverter;
import org.apache.aries.blueprint.container.BlueprintContainerImpl;
import org.apache.aries.blueprint.parser.ComponentDefinitionRegistryImpl;
import org.apache.aries.blueprint.proxy.ProxyUtils;
import org.apache.aries.blueprint.reflect.PassThroughMetadataImpl;
import org.apache.aries.proxy.ProxyManager;
import org.apache.aries.proxy.impl.JdkProxyManager;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.container.Converter;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.Target;

public class TestBlueprintContainer extends BlueprintContainerImpl {

    private ComponentDefinitionRegistryImpl registry;
    
    public TestBlueprintContainer(ComponentDefinitionRegistryImpl registry) throws Exception {
        this(registry, new JdkProxyManager());
    }

    public TestBlueprintContainer(ComponentDefinitionRegistryImpl registry, ProxyManager proxyManager) throws Exception {
        super(null, new TestBundleContext(), null, null, null, null, null, null, proxyManager);
        this.registry = registry;
        if (registry != null) {
            registry.registerComponentDefinition(new PassThroughMetadataImpl("blueprintContainer", this));
            registry.registerComponentDefinition(new PassThroughMetadataImpl("blueprintBundle", getBundleContext().getBundle()));
            registry.registerComponentDefinition(new PassThroughMetadataImpl("blueprintBundleContext", getBundleContext()));
            registry.registerComponentDefinition(new PassThroughMetadataImpl("blueprintConverter", getConverter()));
            processTypeConverters();
        }
    }
    
    private void processTypeConverters() throws Exception {
        List<String> typeConverters = new ArrayList<String>();
        for (Target target : registry.getTypeConverters()) {
            if (target instanceof ComponentMetadata) {
                typeConverters.add(((ComponentMetadata) target).getId());
            } else if (target instanceof RefMetadata) {
                typeConverters.add(((RefMetadata) target).getComponentId());
            } else {
                throw new ComponentDefinitionException("Unexpected metadata for type converter: " + target);
            }
        }

        Map<String, Object> objects = getRepository().createAll(typeConverters, ProxyUtils.asList(Converter.class));
        for (String name : typeConverters) {
            Object obj = objects.get(name);
            if (obj instanceof Converter) {
                ((AggregateConverter)getConverter()).registerConverter((Converter) obj);
            } else {
                throw new ComponentDefinitionException("Type converter " + obj + " does not implement the " + Converter.class.getName() + " interface");
            }
        }
    }

    @Override
    public Class loadClass(String name) throws ClassNotFoundException {
        return Thread.currentThread().getContextClassLoader().loadClass(name);
    }

    @Override
    public ComponentDefinitionRegistryImpl getComponentDefinitionRegistry() {
        return registry;
    }

}
