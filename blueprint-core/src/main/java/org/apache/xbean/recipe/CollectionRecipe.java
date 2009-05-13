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
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @version $Rev: 6685 $ $Date: 2005-12-28T00:29:37.967210Z $
 */
public class CollectionRecipe extends AbstractRecipe {
    private final List<Object> list;
    private String typeName;
    private Class typeClass;
    private final EnumSet<Option> options = EnumSet.noneOf(Option.class);

    public CollectionRecipe() {
        list = new ArrayList<Object>();
    }

    public CollectionRecipe(String type) {
        list = new ArrayList<Object>();
        this.typeName = type;
    }

    public CollectionRecipe(Class type) {
        if (type == null) throw new NullPointerException("type is null");
        this.list = new ArrayList<Object>();
        this.typeClass = type;
    }

    public CollectionRecipe(Collection<?> collection) {
        if (collection == null) throw new NullPointerException("collection is null");

        this.list = new ArrayList<Object>(collection);

        // If the specified collection has a default constructor we will recreate the collection, otherwise we use a the default
        if (RecipeHelper.hasDefaultConstructor(collection.getClass())) {
            this.typeClass = collection.getClass();
        } else if (collection instanceof SortedSet) {
            this.typeClass = SortedSet.class;
        } else if (collection instanceof Set) {
            this.typeClass = Set.class;
        } else if (collection instanceof List) {
            this.typeClass = List.class;
        } else {
            this.typeClass = Collection.class;
        }
    }

    public CollectionRecipe(CollectionRecipe collectionRecipe) {
        if (collectionRecipe == null) throw new NullPointerException("setRecipe is null");
        this.typeName = collectionRecipe.typeName;
        this.typeClass = collectionRecipe.typeClass;
        list = new ArrayList<Object>(collectionRecipe.list);
    }

    public void allow(Option option) {
        options.add(option);
    }

    public void disallow(Option option) {
        options.remove(option);
    }

    public List<Recipe> getNestedRecipes() {
        List<Recipe> nestedRecipes = new ArrayList<Recipe>(list.size());
        for (Object o : list) {
            if (o instanceof Recipe) {
                Recipe recipe = (Recipe) o;
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

    public boolean canCreate(Type expectedType) {
        Class myType = getType(expectedType);
        return RecipeHelper.isAssignable(expectedType, myType);
    }

    protected Object internalCreate(Type expectedType, boolean lazyRefAllowed) throws ConstructionException {
        Class type = getType(expectedType);

        if (!RecipeHelper.hasDefaultConstructor(type)) {
            throw new ConstructionException("Type does not have a default constructor " + type.getName());
        }

        // create collection instance
        Object o;
        try {
            o = type.newInstance();
        } catch (Exception e) {
            throw new ConstructionException("Error while creating collection instance: " + type.getName());
        }
        if (!(o instanceof Collection)) {
            throw new ConstructionException("Specified collection type does not implement the Collection interface: " + type.getName());
        }
        Collection instance = (Collection) o;

        // add to execution context if name is specified
        if (getName() != null) {
            ExecutionContext.getContext().addObject(getName(), instance);
        }

        // get component type
        Type[] typeParameters = RecipeHelper.getTypeParameters(Collection.class, expectedType);
        Type componentType = Object.class;
        if (typeParameters != null && typeParameters.length == 1 && typeParameters[0] instanceof Class) {
            componentType = typeParameters[0];
        }

        boolean refAllowed = options.contains(Option.LAZY_ASSIGNMENT);

        int index = 0;
        for (Object value : list) {
            value = RecipeHelper.convert(componentType, value, refAllowed);

            if (value instanceof Reference) {
                Reference reference = (Reference) value;
                if (instance instanceof List) {
                    // add a null place holder in the list that will be updated later
                    //noinspection unchecked
                    instance.add(null);
                    reference.setAction(new UpdateList((List) instance, index));
                } else {
                    reference.setAction(new UpdateCollection(instance));
                }
            } else {
                //noinspection unchecked
                instance.add(value);
            }
            index++;
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
                return getCollection(expectedClass);                
            } else {
                return getCollection(type);
            }
        }
        
        // no type explicitly set
        return getCollection(expectedClass);
    }

    private Class getCollection(Class type) {
        if (RecipeHelper.hasDefaultConstructor(type)) {
            return type;
        } else if (SortedSet.class.isAssignableFrom(type)) {
            return TreeSet.class;
        } else if (Set.class.isAssignableFrom(type)) {
            return LinkedHashSet.class;
        } else if (List.class.isAssignableFrom(type)) {
            return ArrayList.class;
        } else {
            return ArrayList.class;
        }
    }
    
    public void add(Object value) {
        list.add(value);
    }

    public void addAll(Collection<?> value) {
        list.addAll(value);
    }

    public void remove(Object value) {
        list.remove(value);
    }

    public void removeAll(Object value) {
        list.remove(value);
    }

    public List<Object> getAll() {
        return Collections.unmodifiableList(list);
    }

    private static class UpdateCollection implements Reference.Action {
        private final Collection collection;

        public UpdateCollection(Collection collection) {
            this.collection = collection;
        }

        @SuppressWarnings({"unchecked"})
        public void onSet(Reference ref) {
            collection.add(ref.get());
        }
    }

    private static class UpdateList implements Reference.Action {
        private final List list;
        private final int index;

        public UpdateList(List list, int index) {
            this.list = list;
            this.index = index;
        }

        @SuppressWarnings({"unchecked"})
        public void onSet(Reference ref) {
            list.set(index, ref.get());
        }
    }
}
