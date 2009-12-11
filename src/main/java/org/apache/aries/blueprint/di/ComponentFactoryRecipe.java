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
package org.apache.aries.blueprint.di;

import java.util.List;

import org.apache.aries.blueprint.ExtendedBlueprintContainer;
import org.apache.aries.blueprint.ext.ComponentFactoryMetadata;
import org.osgi.service.blueprint.container.ComponentDefinitionException;

/**
 * Pass-through recipe that allows custom bean manager (represented by a ComponentFactoryMetadata instance)
 * to fit into the container lifecycle.
 * 
 * @param <T>
 */
public class ComponentFactoryRecipe<T extends ComponentFactoryMetadata> extends AbstractRecipe {
    private T metadata;
    private List<Recipe> dependencies;
    
    public ComponentFactoryRecipe(String name, T metadata, 
            ExtendedBlueprintContainer container, List<Recipe> dependencies) {
        super(name);
        this.metadata = metadata;
        this.dependencies = dependencies;
        metadata.init(container);
    }

    @Override
    protected Object internalCreate() throws ComponentDefinitionException {
        return metadata.create();
    }

    public List<Recipe> getDependencies() {
        return dependencies;
    }

    @Override 
    public void destroy(Object instance) {
        metadata.destroy(instance);
    }
    
    protected T getMetadata() {
        return metadata;
    }

}
