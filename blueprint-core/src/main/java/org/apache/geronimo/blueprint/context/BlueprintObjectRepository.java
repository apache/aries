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
package org.apache.geronimo.blueprint.context;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import org.apache.geronimo.blueprint.Destroyable;
import org.apache.xbean.recipe.ConstructionException;
import org.apache.xbean.recipe.Recipe;
import org.apache.xbean.recipe.Repository;

/**
 * 
 */
public class BlueprintObjectRepository implements Repository {

    private Map<String, Recipe> recipes;
    private Map<String, Object> instances;
    private List<DestroyCallback> destroyList;

    public BlueprintObjectRepository() {
        recipes = new TreeMap<String, Recipe>();
        instances = new TreeMap<String, Object>();
        destroyList = new ArrayList<DestroyCallback>();
    }
    
    public BlueprintObjectRepository(BlueprintObjectRepository source) {
        recipes = new TreeMap<String, Recipe>(source.recipes);
        instances = new TreeMap<String, Object>(source.instances);
        destroyList = new ArrayList<DestroyCallback>();
    }
    
    public void set(String name, Object instance) {
        if (instance instanceof Recipe) {
            recipes.put(name, (Recipe) instance);
        } else {
            instances.put(name, instance);
        }
    }
    
    public boolean contains(String name) {
        return recipes.containsKey(name) || instances.containsKey(name);
    }

    public Object get(String name) {
        return instances.get(name) != null ? instances.get(name) : recipes.get(name);
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

    public void add(String name, Object instance) {
        if (instances.get(name) != null) {
            throw new ConstructionException("Name " + name + " is already registered to instance " + instances.get(name));
        }
        if (instance instanceof Recipe) {
            recipes.put(name, (Recipe) instance);
        } else {
            if (recipes.get(name) instanceof BlueprintObjectRecipe) {
                BlueprintObjectRecipe recipe = (BlueprintObjectRecipe) recipes.get(name);
                Method method = recipe.getDestroyMethod(instance);
                if (method != null) {
                    destroyList.add(new DestroyCallback(method, instance));
                }
            }
            instances.put(name, instance);
        }
    }

    public void putRecipe(String name, Recipe recipe) {
        recipes.put(name, recipe);
    }

    public void putInstance(String name, Object instance) {
        instances.put(name, instance);
    }

    public void destroy() {
        // destroy objects in reverse creation order
        ListIterator<DestroyCallback> reverse = destroyList.listIterator(destroyList.size());
        while(reverse.hasPrevious()) {
            reverse.previous().destroy();
        }

        destroyList.clear();
        instances.clear();
    }
    
    private static class DestroyCallback implements Destroyable {

        private Method method;
        private Object instance;
        
        public DestroyCallback(Method method, Object instance) {
            this.method = method;
            this.instance = instance;
        }
        
        public void destroy() {
            try {
                method.invoke(instance);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
    }
}
