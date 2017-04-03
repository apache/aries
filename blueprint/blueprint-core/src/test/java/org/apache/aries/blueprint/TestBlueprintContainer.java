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

import java.util.List;

import org.apache.aries.blueprint.container.BlueprintContainerImpl;
import org.apache.aries.blueprint.parser.ComponentDefinitionRegistryImpl;
import org.apache.aries.proxy.ProxyManager;
import org.apache.aries.proxy.impl.JdkProxyManager;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Target;

public class TestBlueprintContainer extends BlueprintContainerImpl {

    public TestBlueprintContainer(ComponentDefinitionRegistryImpl registry) throws Exception {
        this(registry, new JdkProxyManager());
    }

    public TestBlueprintContainer(ComponentDefinitionRegistryImpl registry, ProxyManager proxyManager) throws Exception {
        super(null, new TestBundleContext(), null, null, null, null, null, null, proxyManager, null);
        resetComponentDefinitionRegistry();

        if (registry == null) {
            return;
        }

        List<Target> converters = registry.getTypeConverters();
        for (Target converter : converters) {
            getComponentDefinitionRegistry().registerTypeConverter(converter);
        }
        for (String name : registry.getComponentDefinitionNames()) {
            ComponentMetadata comp = registry.getComponentDefinition(name);
            if (!converters.contains(comp)) {
                getComponentDefinitionRegistry().registerComponentDefinition(comp);
                for (Interceptor interceptor : registry.getInterceptors(comp)) {
                    getComponentDefinitionRegistry().registerInterceptorWithComponent(comp, interceptor);
                }
            }
        }

        processTypeConverters();
        processProcessors();
    }
    
    @Override
    public Class loadClass(String name) throws ClassNotFoundException {
        return Thread.currentThread().getContextClassLoader().loadClass(name);
    }

}
