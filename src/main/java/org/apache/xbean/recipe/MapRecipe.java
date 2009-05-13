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
package org.apache.xbean.recipe;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Dictionary;
import java.util.AbstractMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @version $Rev: 6687 $ $Date: 2005-12-28T21:08:56.733437Z $
 */
public class MapRecipe extends AbstractRecipe {
    private final List<Object[]> entries;
    private String typeName;
    private Class typeClass;
    private final EnumSet<Option> options = EnumSet.noneOf(Option.class);

    public MapRecipe() {
        entries = new ArrayList<Object[]>();
    }

    public MapRecipe(String type) {
        this.typeName = type;
        entries = new ArrayList<Object[]>();
    }

    public MapRecipe(Class type) {
        if (type == null) throw new NullPointerException("type is null");
        this.typeClass = type;
        entries = new ArrayList<Object[]>();
    }

    public MapRecipe(Map<?,?> map) {
        if (map == null) throw new NullPointerException("map is null");

        entries = new ArrayList<Object[]>(map.size());

        // If the specified set has a default constructor we will recreate the set, otherwise we use a LinkedHashMap or TreeMap
        if (RecipeHelper.hasDefaultConstructor(map.getClass())) {
            this.typeClass = map.getClass();
        } else if (map instanceof SortedMap) {
            this.typeClass = TreeMap.class;
        } else if (map instanceof ConcurrentMap) {
            this.typeClass = ConcurrentHashMap.class;
        } else {
            this.typeClass = LinkedHashMap.class;
        }
        putAll(map);
    }

    public MapRecipe(MapRecipe mapRecipe) {
        if (mapRecipe == null) throw new NullPointerException("mapRecipe is null");
        this.typeName = mapRecipe.typeName;
        this.typeClass = mapRecipe.typeClass;
        entries = new ArrayList<Object[]>(mapRecipe.entries);
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

    public boolean canCreate(Type type) {
        Class myType = getType(type);
        return RecipeHelper.isAssignable(type, myType);
    }

    protected Object internalCreate(Type expectedType, boolean lazyRefAllowed) throws ConstructionException {
        Class mapType = getType(expectedType);

        if (!RecipeHelper.hasDefaultConstructor(mapType)) {
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

        // get component type
        Type keyType = Object.class;
        Type valueType = Object.class;
        Type[] typeParameters = RecipeHelper.getTypeParameters(Map.class, expectedType);
        if (typeParameters != null && typeParameters.length == 2) {
            if (typeParameters[0] instanceof Class) {
                keyType = typeParameters[0];
            }
            if (typeParameters[1] instanceof Class) {
                valueType = typeParameters[1];
            }
        }

        // add to execution context if name is specified
        if (getName() != null) {
            ExecutionContext.getContext().addObject(getName(), instance);
        }

        // add map entries
        boolean refAllowed = options.contains(Option.LAZY_ASSIGNMENT);
        for (Object[] entry : entries) {
            Object key = RecipeHelper.convert(keyType, entry[0], refAllowed);
            Object value = RecipeHelper.convert(valueType, entry[1], refAllowed);

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
        Class expectedClass = RecipeHelper.toClass(expectedType);
        if (typeClass != null || typeName != null) {
            Class type = typeClass;
            if (type == null) {
                try {
                    type = RecipeHelper.loadClass(typeName);
                } catch (ClassNotFoundException e) {
                    throw new ConstructionException("Type class could not be found: " + typeName);
                }
            }

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
        if (RecipeHelper.hasDefaultConstructor(type)) {
            return type;
        } else if (SortedMap.class.isAssignableFrom(type)) {
            return TreeMap.class;
        } else if (ConcurrentMap.class.isAssignableFrom(type)) {
            return ConcurrentHashMap.class;
        } else {
            return LinkedHashMap.class;
        }
    }

    public void put(Object key, Object value) {
        if (key == null) throw new NullPointerException("key is null");
        entries.add(new Object[] { key, value});
    }

    public void putAll(Map<?,?> map) {
        if (map == null) throw new NullPointerException("map is null");
        for (Map.Entry<?,?> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
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
