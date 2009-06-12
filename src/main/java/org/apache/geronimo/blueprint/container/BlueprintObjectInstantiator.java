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
import org.apache.geronimo.blueprint.di.IdRefRecipe;
import org.apache.geronimo.blueprint.di.Recipe;
import org.apache.geronimo.blueprint.di.RefRecipe;
import org.apache.geronimo.blueprint.di.Repository;
import org.apache.geronimo.blueprint.utils.ConversionUtils;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.container.NoSuchComponentException;

/**
 * TODO: consider deleting this class and merging it into the Repository as they are tied together
 * this would allow making the repository thread safe
 *
 */
public class BlueprintObjectInstantiator  {

    private ExtendedBlueprintContainer blueprintContainer;
    private Repository repository;

    public BlueprintObjectInstantiator(ExtendedBlueprintContainer blueprintContainer, Repository repository) {
        this.blueprintContainer = blueprintContainer;
        this.repository = repository;
        checkReferences();
    }
    
    public Repository getRepository() {
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
            
            ExecutionContext oldContext = ExecutionContext.setContext(new DefaultExecutionContext(blueprintContainer, repository));

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
                ExecutionContext.setContext(oldContext);
            }
        }
        return instances;
    }
    
    public <T> List<T> getAllRecipes(Class<T> clazz, String... names) {
        List<T> recipes = new ArrayList<T>();
        for (Recipe r : getAllRecipes(names)) {
            if (clazz.isInstance(r)) {
                recipes.add(clazz.cast(r));
            }
        }
        return recipes;
    }

    public Set<Recipe> getAllRecipes(String... names) {
        ExecutionContext oldContext = ExecutionContext.setContext(new DefaultExecutionContext(blueprintContainer, repository));
        try {
            Set<Recipe> recipes = new HashSet<Recipe>();
            synchronized (repository) {
                Set<String> topLevel = names != null && names.length > 0 ? new HashSet<String>(Arrays.asList(names)) : repository.getNames();
                for (String name : topLevel) {
                    internalGetAllRecipes(recipes, repository.getRecipe(name));
                }
            }
            return recipes;
        } finally {
            ExecutionContext.setContext(oldContext);
        }
    }

    /*
     * This method should not be called directly, only from one of the getAllRecipes() methods.
     */
    private void internalGetAllRecipes(Set<Recipe> recipes, Recipe r) {
        if (r != null) {
            if (recipes.add(r)) {
                for (Recipe c : r.getNestedRecipes()) {
                    internalGetAllRecipes(recipes, c);
                }
            }
        }
    }

    private Object createInstance(String name) {
        // We need to synchronize recipe creation on the repository
        // so that we don't end up with multiple threads creating the
        // same instance at the same time.
        Object instance = repository.getInstance(name);
        if (instance == null) {
            synchronized (repository) {
                instance = repository.getInstance(name);
                if (instance == null) {
                    Recipe recipe = repository.getRecipe(name);
                    if (recipe != null) {
                        instance = recipe.create();
                    }
                }
            }
        }
        if (instance == null) {
            instance = repository.getDefault(name);
        }
        if (instance == null) {
            throw new NoSuchComponentException(name);
        }
        return instance;
    }
        
    private void checkReferences() {
        for (Recipe recipe : getAllRecipes()) {
            String ref = null;
            if (recipe instanceof RefRecipe) {
                ref = ((RefRecipe) recipe).getIdRef();
            } else if (recipe instanceof IdRefRecipe) {
                ref = ((IdRefRecipe) recipe).getIdRef();
            }
            if (ref != null && repository.getRecipe(ref) == null && repository.getDefault(ref) == null) {
                throw new ComponentDefinitionException("Unresolved ref/idref to component: " + ref);
            }
        }
    }

}
