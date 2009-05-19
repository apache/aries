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
package org.apache.geronimo.blueprint.di;

import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.geronimo.blueprint.utils.TypeUtils;

/**
 * @version $Rev: 6687 $ $Date: 2005-12-28T21:08:56.733437Z $
 */
public class MapRecipe extends AbstractRecipe {
    private final List<Recipe[]> entries;
    private Class typeClass;
    private final EnumSet<Option> options = EnumSet.noneOf(Option.class);

    public MapRecipe(Class type) {
        if (type == null) throw new NullPointerException("type is null");
        this.typeClass = type;
        this.entries = new ArrayList<Recipe[]>();
    }

    public void allow(Option option){
        options.add(option);
    }

    public void disallow(Option option){
        options.remove(option);
    }

    public List<Recipe> getNestedRecipes() {
        List<Recipe> nestedRecipes = new ArrayList<Recipe>(entries.size() * 2);
        for (Object[] entry : entries) {
            Object key = entry[0];
            if (key instanceof Recipe) {
                Recipe recipe = (Recipe) key;
                nestedRecipes.add(recipe);
            }

            Object value = entry[1];
            if (value instanceof Recipe) {
                Recipe recipe = (Recipe) value;
                nestedRecipes.add(recipe);
            }
        }
        return nestedRecipes;
    }

    public List<Recipe> getConstructorRecipes() {
        if (!options.contains(Option.LAZY_ASSIGNMENT)) {
            return getNestedRecipes();
        }
        return Collections.emptyList();
    }

    protected Object internalCreate(boolean lazyRefAllowed) throws ConstructionException {
        Class mapType = getType(Object.class);

        if (!TypeUtils.hasDefaultConstructor(mapType)) {
            throw new ConstructionException("Type does not have a default constructor " + mapType.getName());
        }

        Object o;
        try {
            o = mapType.newInstance();
        } catch (Exception e) {
            throw new ConstructionException("Error while creating set instance: " + mapType.getName());
        }

        Map instance;
        if (o instanceof Map) {
            instance = (Map) o;
        } else if (o instanceof Dictionary) {
            instance = new DummyDictionaryAsMap((Dictionary) o);
        } else {
            throw new ConstructionException("Specified map type does not implement the Map interface: " + mapType.getName());
        }

        // add to execution context if name is specified
        if (getName() != null) {
            ExecutionContext.getContext().addObject(getName(), instance);
        }

        // add map entries
        boolean refAllowed = options.contains(Option.LAZY_ASSIGNMENT);
        for (Recipe[] entry : entries) {
            Object key = entry[0].create(refAllowed);
            Object value = entry[1] != null ? entry[1].create(refAllowed) : null;

            if (key instanceof Reference) {
                // when the key reference and optional value reference are both resolved
                // the key/value pair will be added to the map
                Reference.Action action = new UpdateMap(instance, key, value);
                ((Reference) key).setAction(action);
                if (value instanceof Reference) {
                    ((Reference) value).setAction(action);
                }
            } else if (value instanceof Reference) {
                // add a null place holder assigned to the key
                //noinspection unchecked
                instance.put(key, null);
                // when value is resolved we will replace the null value with they real value
                Reference.Action action = new UpdateValue(instance, key);
                ((Reference) value).setAction(action);
            } else {
                //noinspection unchecked
                instance.put(key, value);
            }
        }
        return instance;
    }

    private Class getType(Type expectedType) {
        Class expectedClass = TypeUtils.toClass(expectedType);
        if (typeClass != null) {
            Class type = typeClass;
            // if expectedType is a subclass of the assigned type,
            // we use it assuming it has a default constructor
            if (type.isAssignableFrom(expectedClass)) {
                return getMap(expectedClass);                
            } else {
                return getMap(type);
            }
        }

        // no type explicitly set
        return getMap(expectedClass);
    }
    
    private Class getMap(Class type) {
        if (TypeUtils.hasDefaultConstructor(type)) {
            return type;
        } else if (SortedMap.class.isAssignableFrom(type)) {
            return TreeMap.class;
        } else if (ConcurrentMap.class.isAssignableFrom(type)) {
            return ConcurrentHashMap.class;
        } else {
            return LinkedHashMap.class;
        }
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

    private static class UpdateValue implements Reference.Action {
        private final Map map;
        private final Object key;

        public UpdateValue(Map map, Object key) {
            this.map = map;
            this.key = key;
        }

        @SuppressWarnings({"unchecked"})
        public void onSet(Reference ref) {
            map.put(key, ref.get());
        }
    }


    private static class UpdateMap implements Reference.Action {
        private final Map map;
        private final Object key;
        private final Object value;

        public UpdateMap(Map map, Object key, Object value) {
            this.map = map;
            this.key = key;
            this.value = value;
        }

        @SuppressWarnings({"unchecked"})
        public void onSet(Reference ignored) {
            Object key = this.key;
            if (key instanceof Reference) {
                Reference reference = (Reference) key;
                if (!reference.isResolved()) {
                    return;
                }
                key = reference.get();
            }
            Object value = this.value;
            if (value instanceof Reference) {
                Reference reference = (Reference) value;
                if (!reference.isResolved()) {
                    return;
                }
                value = reference.get();
            }
            map.put(key, value);
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
