/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.blueprint.di;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.aries.blueprint.utils.ReflectionUtils;
import org.osgi.service.blueprint.container.ComponentDefinitionException;

/**
 * @version $Rev$ $Date$
 */
public class MapRecipe extends AbstractRecipe {

    private final List<Recipe[]> entries;
    private final Class typeClass;

    public MapRecipe(String name, Class type) {
        super(name);
        if (type == null) throw new NullPointerException("type is null");
        this.typeClass = type;
        this.entries = new ArrayList<Recipe[]>();
    }

    public List<Recipe> getDependencies() {
        List<Recipe> nestedRecipes = new ArrayList<Recipe>(entries.size() * 2);
        for (Recipe[] entry : entries) {
            nestedRecipes.add(entry[0]);
            if (entry[1] != null) {
                nestedRecipes.add(entry[1]);
            }
        }
        return nestedRecipes;
    }

    protected Object internalCreate() throws ComponentDefinitionException {
        Class mapType = getMap(typeClass);

        if (!ReflectionUtils.hasDefaultConstructor(mapType)) {
            throw new ComponentDefinitionException("Type does not have a default constructor " + mapType.getName());
        }

        Object o;
        try {
            o = mapType.newInstance();
        } catch (Exception e) {
            throw new ComponentDefinitionException("Error while creating set instance: " + mapType.getName());
        }

        Map instance;
        if (o instanceof Map) {
            instance = (Map) o;
        } else if (o instanceof Dictionary) {
            instance = new DummyDictionaryAsMap((Dictionary) o);
        } else {
            throw new ComponentDefinitionException("Specified map type does not implement the Map interface: " + mapType.getName());
        }

        // add map entries
        for (Recipe[] entry : entries) {
            Object key = entry[0].create();
            Object value = entry[1] != null ? entry[1].create() : null;
            instance.put(key, value);
        }
        return instance;
    }

    public void put(Recipe key, Recipe value) {
        if (key == null) throw new NullPointerException("key is null");
        entries.add(new Recipe[] { key, value});
    }

    public void putAll(Map<Recipe,Recipe> map) {
        if (map == null) throw new NullPointerException("map is null");
        for (Map.Entry<Recipe,Recipe> entry : map.entrySet()) {
            Recipe key = entry.getKey();
            Recipe value = entry.getValue();
            put(key, value);
        }
    }

    public static Class getMap(Class type) {
        if (ReflectionUtils.hasDefaultConstructor(type)) {
            return type;
        } else if (SortedMap.class.isAssignableFrom(type)) {
            return TreeMap.class;
        } else if (ConcurrentMap.class.isAssignableFrom(type)) {
            return ConcurrentHashMap.class;
        } else {
            return LinkedHashMap.class;
        }
    }

    public static class DummyDictionaryAsMap extends AbstractMap {

        private final Dictionary dictionary;

        public DummyDictionaryAsMap(Dictionary dictionary) {
            this.dictionary = dictionary;
        }

        @Override
        public Object put(Object key, Object value) {
            return dictionary.put(key, value);
        }

        public Set entrySet() {
            throw new UnsupportedOperationException();
        }
    }

}
