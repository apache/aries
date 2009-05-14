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

import org.apache.geronimo.blueprint.Destroyable;
import org.apache.xbean.recipe.ConstructionException;
import org.apache.xbean.recipe.Recipe;
import org.apache.xbean.recipe.Repository;

/**
 * 
 */
public class BlueprintObjectRepository implements Repository {

    private SortedMap<String, Object> instances;
    private List<DestroyCallback> destroyList;

    public BlueprintObjectRepository() {
        instances = new TreeMap<String, Object>();
        destroyList = new ArrayList<DestroyCallback>();
    }
    
    public BlueprintObjectRepository(BlueprintObjectRepository source) {
        instances = new TreeMap<String, Object>(source.instances);       
        destroyList = new ArrayList<DestroyCallback>();
    }
    
    public void set(String name, Object instance) {
        instances.put(name, instance);
    }
    
    public boolean contains(String name) {
        return instances.containsKey(name);
    }

    public Object get(String name) {
        return instances.get(name);
    }

    public void add(String name, Object instance) {
        Object existingObj = instances.get(name);
        if (existingObj != null) {
            if (existingObj instanceof BlueprintObjectRecipe) {    
                BlueprintObjectRecipe recipe = (BlueprintObjectRecipe) existingObj;
                Method method = recipe.getDestroyMethod(instance);
                if (method != null) {
                    destroyList.add(new DestroyCallback(method, instance));
                }
            } else if (!(existingObj instanceof Recipe)) {
                throw new ConstructionException("Name " + name + " is already registered to instance " + instance);
            }
        }

        instances.put(name, instance);
    }

    public void updateRecipe(String name, Recipe recipe) {
        if (instances.get(name) instanceof Recipe) {
            instances.put(name, recipe);
        }
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
