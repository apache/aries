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
package org.apache.aries.blueprint.container;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.aries.blueprint.ExtendedBlueprintContainer;
import org.apache.aries.blueprint.di.CircularDependencyException;
import org.apache.aries.blueprint.di.ExecutionContext;
import org.apache.aries.blueprint.di.IdRefRecipe;
import org.apache.aries.blueprint.di.Recipe;
import org.apache.aries.blueprint.di.RefRecipe;
import org.apache.aries.blueprint.di.Repository;
import org.apache.aries.blueprint.di.CollectionRecipe;
import org.osgi.service.blueprint.container.ReifiedType;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.container.NoSuchComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default repository implementation
 */
public class BlueprintRepository implements Repository, ExecutionContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintRepository.class);

    /**
     * The blueprint container
     */
    private final ExtendedBlueprintContainer blueprintContainer;

    /**
     * Contains object recipes
     */
    private final Map<String, Recipe> recipes = new ConcurrentHashMap<String, Recipe>();

    /**
     * Contains object instances
     */
    private final Map<String, Object> instances = new ConcurrentHashMap<String, Object>();

    /**
     * Keep track of creation order
     */
    private final List<String> creationOrder = new CopyOnWriteArrayList<String>();

    /**
     * Lock for object instance creation
     */
    private final Object instanceLock = new Object();

    /**
     * Contains partial objects.
     */
    private final Map<String, Object> partialObjects = new ConcurrentHashMap<String, Object>();

    /**
     * Before each recipe is executed it is pushed on the stack.  The
     * stack is used to detect circular dependencies.
     */
    private final LinkedList<Recipe> stack = new LinkedList<Recipe>();
    
    public BlueprintRepository(ExtendedBlueprintContainer container) {
        blueprintContainer = container;
    }
    
    public Object getInstance(String name) {
        return instances.get(name);
    }

    public Recipe getRecipe(String name) {
        return recipes.get(name);
    }

    public Set<String> getNames() {
        Set<String> names = new HashSet<String>();
        names.addAll(recipes.keySet());
        names.addAll(instances.keySet());
        return names;
    }

    public void putRecipe(String name, Recipe recipe) {
        if (instances.get(name) != null) {
            throw new ComponentDefinitionException("Name " + name + " is already registered to instance " + instances.get(name));
        }
        recipes.put(name, recipe);
    }
    
    public void removeRecipe(String name) {
        if (instances.get(name) != null)
            throw new ComponentDefinitionException("Name " + name + " is already instanciated as " + instances.get(name) + " and cannot be removed.");

        recipes.remove(name);
    }

    private Object convert(String name, Object instance) throws ComponentDefinitionException {
        try {
            // Make sure to go through the conversion step in case we have a Convertible object
            return convert(instance, new ReifiedType(Object.class));
        } catch (Exception e) {
            throw new ComponentDefinitionException("Unable to convert instance " + name, e);
        }
    }
        
    public Object create(String name) throws ComponentDefinitionException {
        ExecutionContext oldContext = ExecutionContext.Holder.setContext(this);
        try {
            Object instance = createInstance(name);                       
            return convert(name, instance);
        } finally {
            ExecutionContext.Holder.setContext(oldContext);
        }
    }
    
    public Map<String, Object> createAll(Collection<String> names) throws ComponentDefinitionException {
        ExecutionContext oldContext = ExecutionContext.Holder.setContext(this);
        try {
            Map<String, Object> instances = createInstances(names);
            for (String name : instances.keySet()) {
                Object obj = instances.get(name);
                instances.put(name, convert(name, obj));
            }
            return instances;
        } finally {
            ExecutionContext.Holder.setContext(oldContext);
        }
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
        ExecutionContext oldContext = ExecutionContext.Holder.setContext(this);
        try {
            Set<Recipe> allRecipes = new HashSet<Recipe>();
            Collection<String> topLevel = names != null && names.length > 0 ? Arrays.asList(names) : recipes.keySet();
            for (String name : topLevel) {
                internalGetAllRecipes(allRecipes, getRecipe(name));
            }
            return allRecipes;
        } finally {
            ExecutionContext.Holder.setContext(oldContext);
        }
    }

    /*
     * This method should not be called directly, only from one of the getAllRecipes() methods.
     */
    private void internalGetAllRecipes(Set<Recipe> allRecipes, Recipe r) {
        if (r != null) {
            if (allRecipes.add(r)) {
                for (Recipe c : r.getDependencies()) {
                    internalGetAllRecipes(allRecipes, c);
                }
            }
        }
    }

    private Object createInstance(String name) {
        Object instance = getInstance(name);
        if (instance == null) {
            Map <String, Object> instances = createInstances(Arrays.asList(name));
            instance = instances.get(name); 
            if (instance == null) {
                throw new NoSuchComponentException(name);
            }
        }
        return instance;
    }

    private Map<String, Object> createInstances(Collection<String> names) {
        // We need to synchronize recipe creation on the repository
        // so that we don't end up with multiple threads creating the
        // same instance at the same time.
        synchronized (instanceLock) {
            DependencyGraph graph = new DependencyGraph(this);
            HashMap<String, Object> objects = new LinkedHashMap<String, Object>();
            for (Map.Entry<String, Recipe> entry : graph.getSortedRecipes(names).entrySet()) {
                String name = entry.getKey();
                Object object = instances.get(name);
                if (object == null) {
                    Recipe recipe = entry.getValue();
                    object = recipe.create();
                }
                objects.put(name, object);
            }
            return objects;
        }
    }
        
    public void validate() {
        for (Recipe recipe : getAllRecipes()) {
            // Check that references are satisfied
            String ref = null;
            if (recipe instanceof RefRecipe) {
                ref = ((RefRecipe) recipe).getIdRef();
            } else if (recipe instanceof IdRefRecipe) {
                ref = ((IdRefRecipe) recipe).getIdRef();
            }
            if (ref != null && getRecipe(ref) == null) {
                throw new ComponentDefinitionException("Unresolved ref/idref to component: " + ref);
            }
            // Check service
            if (recipe instanceof ServiceRecipe) {
                Recipe r = ((ServiceRecipe) recipe).getServiceRecipe();
                if (r instanceof RefRecipe) {
                    r = getRecipe(((RefRecipe) r).getIdRef());
                }
                if (r instanceof ServiceRecipe) {
                    throw new ComponentDefinitionException("The target for a <service> element must not be <service> element");
                }
                if (r instanceof ReferenceListRecipe) {
                    throw new ComponentDefinitionException("The target for a <service> element must not be <reference-list> element");
                }
                CollectionRecipe listeners = ((ServiceRecipe) recipe).getListenersRecipe();
                for (Recipe lr : listeners.getDependencies()) {
                    // The listener recipe is a bean recipe with the listener being set in a property
                    for (Recipe l : lr.getDependencies()) {
                        if (l instanceof RefRecipe) {
                            l = getRecipe(((RefRecipe) l).getIdRef());
                        }
                        if (l instanceof ServiceRecipe) {
                            throw new ComponentDefinitionException("The target for a <registration-listener> element must not be <service> element");
                        }
                        if (l instanceof ReferenceListRecipe) {
                            throw new ComponentDefinitionException("The target for a <registration-listener> element must not be <reference-list> element");
                        }
                    }
                }
            }
            // Check references
            if (recipe instanceof AbstractServiceReferenceRecipe) {
                CollectionRecipe listeners = ((AbstractServiceReferenceRecipe) recipe).getListenersRecipe();
                for (Recipe lr : listeners.getDependencies()) {
                    // The listener recipe is a bean recipe with the listener being set in a property
                    for (Recipe l : lr.getDependencies()) {
                        if (l instanceof RefRecipe) {
                            l = getRecipe(((RefRecipe) l).getIdRef());
                        }
                        if (l instanceof ServiceRecipe) {
                            throw new ComponentDefinitionException("The target for a <reference-listener> element must not be <service> element");
                        }
                        if (l instanceof ReferenceListRecipe) {
                            throw new ComponentDefinitionException("The target for a <reference-listener> element must not be <reference-list> element");
                        }
                    }
                }
            }
        }
    }

    public void destroy() {
        // destroy objects in reverse creation order
        List<String> order = new ArrayList<String>(creationOrder);
        Collections.reverse(order);
        for (String name : order) {
            Recipe recipe = recipes.get(name);
            if (recipe != null) {
                recipe.destroy(instances.get(name));
            }
        }
        instances.clear();
        creationOrder.clear();
    }

    public Object getInstanceLock() {
        return instanceLock;
    }

    public void push(Recipe recipe) {
        if (stack.contains(recipe)) {
            ArrayList<Recipe> circularity = new ArrayList<Recipe>(stack.subList(stack.indexOf(recipe), stack.size()));

            // remove anonymous nodes from circularity list
            for (Iterator<Recipe> iterator = circularity.iterator(); iterator.hasNext();) {
                Recipe item = iterator.next();
                if (item != recipe && item.getName() == null) {
                    iterator.remove();
                }
            }

            // add ending node to list so a full circuit is shown
            circularity.add(recipe);

            throw new CircularDependencyException(circularity);
        }
        stack.add(recipe);
    }

    public Recipe pop() {
        return stack.removeLast();
    }

    public LinkedList<Recipe> getStack() {
        return new LinkedList<Recipe>(stack);
    }

    public boolean containsObject(String name) {
        return getInstance(name) != null
                || getRecipe(name) != null;
    }

    public Object getObject(String name) {
        Object object = getInstance(name);
        if (object == null) {
            object = getRecipe(name);
        }
        return object;
    }

    public void addFullObject(String name, Object object) {
        if (instances.get(name) != null) {
            throw new ComponentDefinitionException("Name " + name + " is already registered to instance " + instances.get(name));
        }
        instances.put(name, object);
        creationOrder.add(name); 
        partialObjects.remove(name);
    }
    
    public void addPartialObject(String name, Object object) {
        partialObjects.put(name, object);
    }
    
    public Object removePartialObject(String name) {
        return partialObjects.remove(name);
    }
    
    public Object getPartialObject(String name) {
        Object obj = partialObjects.get(name);
        if (obj == null) {
            obj = getInstance(name);
        }
        return obj;
    }

    public Object convert(Object value, ReifiedType type) throws Exception {
        return blueprintContainer.getConverter().convert(value, type);
    }
    
    public boolean canConvert(Object value, ReifiedType type) {
        return blueprintContainer.getConverter().canConvert(value, type);
    }

    public Class loadClass(String typeName) throws ClassNotFoundException {
        return blueprintContainer.loadClass(typeName);
    }
}
