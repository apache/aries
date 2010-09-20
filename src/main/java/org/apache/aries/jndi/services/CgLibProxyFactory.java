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
package org.apache.aries.jndi.services;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import net.sf.cglib.proxy.Dispatcher;
import net.sf.cglib.proxy.Enhancer;

import org.apache.aries.util.BundleToClassLoaderAdapter;
import org.osgi.framework.Bundle;

public class CgLibProxyFactory implements ProxyFactory {

    public Object createProxy(final Bundle bundle,
                              final List<Class<?>> classes,
                              final Callable<Object> dispatcher) {
        Enhancer e = new Enhancer();
        e.setClassLoader(new CgLibClassLoader(bundle));
        e.setSuperclass(getTargetClass(classes));
        e.setInterfaces(getInterfaces(classes));
        e.setInterceptDuringConstruction(false);
        e.setCallback(new Dispatcher() {
            public Object loadObject() throws Exception {
                return dispatcher.call();
            }
        });
        e.setUseFactory(false);
        return e.create();
    }

    private static Class<?>[] getInterfaces(List<Class<?>> classes) {
        Set<Class<?>> interfaces = new HashSet<Class<?>>();
        for (Class<?> clazz : classes) {
            if (clazz.isInterface()) {
                interfaces.add(clazz);
            }
        }
        return interfaces.toArray(new Class[interfaces.size()]);
    }

    protected Class<?> getTargetClass(List<Class<?>> interfaceNames) {
        // Only allow class proxying if specifically asked to
        Class<?> root = Object.class;
        for (Class<?> clazz : interfaceNames) {
            if (!clazz.isInterface()) {
                if (root.isAssignableFrom(clazz)) {
                    root = clazz;
                } else if (clazz.isAssignableFrom(root)) {
                    // nothing to do, root is correct
                } else {
                    throw new IllegalArgumentException("Classes " + root.getClass().getName()
                                                       + " and " + clazz.getName()
                                                       + " are not in the same hierarchy");
                }
            }
        }
        return root;
    }

    private static class CgLibClassLoader extends BundleToClassLoaderAdapter {

        private ClassLoader cgLibClassLoader;

        public CgLibClassLoader(Bundle bundle) {
            super(bundle);
            this.cgLibClassLoader = Enhancer.class.getClassLoader();
        }

        @Override
        public Class<?> loadClass(final String name) throws ClassNotFoundException {
            if (name.startsWith("net.sf.cglib")) {
                return cgLibClassLoader.loadClass(name);
            } else {
                return super.loadClass(name);
            }
        }
    }
}
