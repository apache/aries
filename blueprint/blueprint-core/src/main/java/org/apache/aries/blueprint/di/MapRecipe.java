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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.aries.blueprint.utils.ReflectionUtils;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.container.ReifiedType;

/**
 * @version $Rev$ $Date$
 */
public class MapRecipe extends AbstractRecipe {

    private final List<Recipe[]> entries;
    private final Class<?> typeClass;
    private final Object keyType;
    private final Object valueType;
    
    
    public MapRecipe(String name, Class<?> type, Object keyType, Object valueType) {
        super(name);
        if (type == null) throw new NullPointerException("type is null");
        this.typeClass = type;
        this.entries = new ArrayList<Recipe[]>();
        this.keyType = keyType;
        this.valueType = valueType;
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
    
    private ReifiedType getType(Object o) {
        ReifiedType type;
        if (o instanceof Class) {
            type = new ReifiedType((Class) o);
        } else if (o instanceof String) {
            type = loadType((String) o);
        } else {
            type = new ReifiedType(Object.class);
        }
        return type;
    }

    protected Object internalCreate() throws ComponentDefinitionException {
        Class<?> mapType = getMap(typeClass);

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

        ReifiedType defaultConvertKeyType = getType(keyType);
        ReifiedType defaultConvertValueType = getType(valueType);
                
        // add map entries
        try {
            for (Recipe[] entry : entries) {
                ReifiedType convertKeyType = workOutConversionType(entry[0], defaultConvertKeyType);
                Object key = convert(entry[0].create(), convertKeyType);
                // Each entry may have its own types
                ReifiedType convertValueType = workOutConversionType(entry[1], defaultConvertValueType);
                Object value = entry[1] != null ? convert(entry[1].create(), convertValueType) : null;
                instance.put(key, value);
            }
        } catch (Exception e) {
            throw new ComponentDefinitionException(e);
        }
        return instance;
    }

    protected ReifiedType workOutConversionType(Recipe entry, ReifiedType defaultType) {
        if (entry instanceof ValueRecipe) {
            return getType(((ValueRecipe) entry).getValueType());
        } else {
            return defaultType;
        }
    }

    public void put(Recipe key, Recipe value) {
        if (key == null) throw new NullPointerException("key is null");
        entries.add(new Recipe[]{key, value});
    }

    public void putAll(Map<Recipe, Recipe> map) {
        if (map == null) throw new NullPointerException("map is null");
        for (Map.Entry<Recipe, Recipe> entry : map.entrySet()) {
            Recipe key = entry.getKey();
            Recipe value = entry.getValue();
            put(key, value);
        }
    }

    public static Class<?> getMap(Class<?> type) {
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
