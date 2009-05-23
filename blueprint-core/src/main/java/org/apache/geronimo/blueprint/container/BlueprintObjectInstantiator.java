/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.geronimo.blueprint.container;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.geronimo.blueprint.ExtendedBlueprintContainer;
import org.apache.geronimo.blueprint.di.DefaultExecutionContext;
import org.apache.geronimo.blueprint.di.ExecutionContext;
import org.apache.geronimo.blueprint.di.Recipe;
import org.apache.geronimo.blueprint.di.Repository;
import org.apache.geronimo.blueprint.utils.ConversionUtils;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.container.NoSuchComponentException;

/**
 */
public class BlueprintObjectInstantiator  {

    private ExtendedBlueprintContainer blueprintContainer;
    private Repository repository;

    public BlueprintObjectInstantiator(ExtendedBlueprintContainer blueprintContainer, Repository repository) {
        this.blueprintContainer = blueprintContainer;
        this.repository = repository;
    }
    
    public  Repository getRepository() {
        return repository;
    }
    
    public Object create(String name) throws ComponentDefinitionException {
        Map<String, Object> instances = createAll(Arrays.asList(name));
        return instances.get(name);
    }
    
    public Map<String,Object> createAll(String... names) throws ComponentDefinitionException {
        return createAll(Arrays.asList(names));
    }
        
    public Map<String, Object> createAll(Collection<String> names) throws ComponentDefinitionException {
        Map<String, Object> instances = new LinkedHashMap<String, Object>();
        for (String name : names) {
            
            boolean createNewContext = !ExecutionContext.isContextSet();
            if (createNewContext) {
                ExecutionContext.setContext(new DefaultExecutionContext(blueprintContainer, repository));
            }
            
            try {
                Object obj = createInstance(name);
                try {
                    // Make sure to go through the conversion step in case we have a Convertible object
                    obj = ConversionUtils.convert(obj, Object.class, blueprintContainer.getConverter());
                } catch (Exception e) {
                    throw new ComponentDefinitionException("Unable to convert instance " + name, e);
                }
                instances.put(name, obj);
                Set<Recipe> processed = new HashSet<Recipe>();
                boolean modified;
                do {
                    modified = false;
                    List<Recipe> recipes = new ArrayList<Recipe>(ExecutionContext.getContext().getCreatedRecipes());
                    for (Recipe recipe : recipes) {
                        if (processed.add(recipe)) {
                            recipe.postCreate();
                            modified = true;
                        }
                    }
                } while (modified);
            } finally {
                if (createNewContext) {
                    ExecutionContext.setContext(null);
                }
            }
        }
        return instances;
    }
    
    private Object createInstance(String name) {
        Object recipe = repository.get(name);
        if (recipe == null) {
            throw new NoSuchComponentException(name);
        }
        Object obj = recipe;
        if (recipe instanceof Recipe) {
            obj = ((Recipe) recipe).create();
        }
        return obj;
    }
        
}
