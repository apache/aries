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
package org.apache.geronimo.blueprint;

import org.apache.geronimo.blueprint.container.BlueprintContainerImpl;
import org.apache.geronimo.blueprint.namespace.ComponentDefinitionRegistryImpl;
import org.apache.geronimo.blueprint.reflect.EnvironmentMetadataImpl;

public class TestBlueprintContainer extends BlueprintContainerImpl {

    private ComponentDefinitionRegistryImpl registry;
    
    public TestBlueprintContainer(ComponentDefinitionRegistryImpl registry) {
        super(new TestBundleContext(), null, null, null, null, null);
        this.registry = registry;
        if (registry != null) {
            registry.registerComponentDefinition(new EnvironmentMetadataImpl("blueprintContainer", this));
            registry.registerComponentDefinition(new EnvironmentMetadataImpl("blueprintBundle", getBundleContext().getBundle()));
            registry.registerComponentDefinition(new EnvironmentMetadataImpl("blueprintBundleContext", getBundleContext()));
            registry.registerComponentDefinition(new EnvironmentMetadataImpl("blueprintConverter", getConverter()));
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
