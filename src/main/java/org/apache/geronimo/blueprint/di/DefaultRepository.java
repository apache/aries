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
package org.apache.geronimo.blueprint.di;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default repository implementation
 */
public class DefaultRepository implements Repository {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRepository.class);

    private Map<String, Object> defaults;
    private Map<String, Recipe> recipes;
    private Map<String, Object> instances;
    private List<Destroyable> destroyList;

    public DefaultRepository() {
        recipes = new TreeMap<String, Recipe>();
        defaults = new TreeMap<String, Object>();
        instances = new TreeMap<String, Object>();
        destroyList = new ArrayList<Destroyable>();
    }
    
    public DefaultRepository(DefaultRepository source) {
        recipes = new TreeMap<String, Recipe>(source.recipes);
        defaults = new TreeMap<String, Object>();
        instances = new TreeMap<String, Object>(source.instances);
        destroyList = new ArrayList<Destroyable>();
    }

    public void set(String name, Object instance) {
        if (instance instanceof Recipe) {
            recipes.put(name, (Recipe) instance);
        } else {
            instances.put(name, instance);
        }
    }
    
    public boolean contains(String name) {
        return recipes.containsKey(name) || instances.containsKey(name) || defaults.containsKey(name);
    }

    public Object get(String name) {
        return instances.get(name) != null ? instances.get(name) : (recipes.get(name) != null ? recipes.get(name) : defaults.get(name));
    }

    public Object getInstance(String name) {
        return instances.get(name);
    }

    public Recipe getRecipe(String name) {
        return recipes.get(name);
    }

    public Object getDefault(String name) {
        return defaults.get(name);
    }

    public Set<String> getNames() {
        Set<String> names = new HashSet<String>();
        names.addAll(recipes.keySet());
        names.addAll(instances.keySet());
        names.addAll(defaults.keySet());
        return names;
    }

    public void putDefault(String name, Object instance) {
        defaults.put(name, instance);
    }

    public void putRecipe(String name, Recipe recipe) {
        if (instances.get(name) != null) {
            throw new ComponentDefinitionException("Name " + name + " is already registered to instance " + instances.get(name));
        }
        recipes.put(name, recipe);
    }

    public void putInstance(String name, Object instance) {
        if (instances.get(name) != null) {
            throw new ComponentDefinitionException("Name " + name + " is already registered to instance " + instances.get(name));
        }
        Recipe recipe = recipes.get(name);
        if (recipe != null) {
            Destroyable destroy = recipe.getDestroyable(instance);
            if (destroy != null) {
                destroyList.add(destroy);
            }
        }
        instances.put(name, instance);
    }

    public void destroy() {
        // destroy objects in reverse creation order
        ListIterator<Destroyable> reverse = destroyList.listIterator(destroyList.size());
        while (reverse.hasPrevious()) {
            Destroyable d = reverse.previous();
            try {
                d.destroy();
            } catch (Exception e) {
                LOGGER.info("Error destroying bean " + d, e);
            }
        }
        destroyList.clear();
        instances.clear();
    }
    
}
