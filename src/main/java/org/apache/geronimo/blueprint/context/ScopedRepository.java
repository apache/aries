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

import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.xbean.recipe.ConstructionException;
import org.apache.xbean.recipe.Recipe;
import org.apache.xbean.recipe.Repository;

/**
 * By default in object repository Recipes are replaced with objects that were creating using the given Recipe.
 * That essentially implements the 'singleton' scope. For 'prototype' scope we do not allow certain Recipes to
 * be replaced with resulting objects in the repository. That ensures that a new instance of a object is created
 * for such Recipes during graph instantiation.
 */
public class ScopedRepository implements Repository {

    private SortedMap<String, Object> instances;

    public ScopedRepository() {
        instances = new TreeMap<String, Object>();
    }
    
    public ScopedRepository(ScopedRepository source) {
        instances = new TreeMap<String, Object>(source.instances);
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
                if ( ((BlueprintObjectRecipe) existingObj ).getKeepRecipe()) {
                    return;
                }
            } else if (!(existingObj instanceof Recipe)) {
                throw new ConstructionException("Name " + name + " is already registered to instance " + instance);
            }
        }

        instances.put(name, instance);
    }
}
