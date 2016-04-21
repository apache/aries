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

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.HashSet;

import javax.persistence.spi.ClassTransformer;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;

class WrappingTransformer implements ClassTransformer {
    private final ClassTransformer delegate;
    private final Collection<String> packageImportsToAdd = new HashSet<String>();
    
    public WrappingTransformer(ClassTransformer transformer) {
        delegate = transformer;
    }

    public WrappingTransformer(ClassTransformer delegate, ServiceReference<?> persistenceProvider) {
        validate(delegate, persistenceProvider);
        this.delegate = delegate;

        Object packages = persistenceProvider.getProperty("org.apache.aries.jpa.container.weaving.packages");
        if (packages instanceof String[]) {
            for (String s : (String[])packages) {
                packageImportsToAdd.add(s);
            }
        } else {
            Bundle provider = persistenceProvider.getBundle();
            String suffix = ";" + Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE + "=" + provider.getSymbolicName()
                            + ";" + Constants.BUNDLE_VERSION_ATTRIBUTE + "=" + provider.getVersion();

            BundleRevision br = provider.adapt(BundleWiring.class).getRevision();
            for (BundleCapability bc : br.getDeclaredCapabilities(BundleRevision.PACKAGE_NAMESPACE)) {
                packageImportsToAdd.add(bc.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE) + suffix);
            }
        }
    }

    private static void validate(ClassTransformer delegate, ServiceReference<?> persistenceProvider) {
        if (delegate == null) {
            throw new NullPointerException("Transformer delegate may not be null");
        }
        if (persistenceProvider == null) {
            throw new NullPointerException("PersistenceProvider may not be null");
        }
    }

 
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
        throws IllegalClassFormatException {
        return delegate.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
    }

    public Collection<String> getPackagesToAdd() {
        return packageImportsToAdd;
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof WrappingTransformer) {
            return delegate == ((WrappingTransformer)o).delegate;
        }
        return false;
    }

    @Override
    public String toString() {
        return "Transformer: " + delegate.toString() + " Packages to add: " + packageImportsToAdd;
    }
}
