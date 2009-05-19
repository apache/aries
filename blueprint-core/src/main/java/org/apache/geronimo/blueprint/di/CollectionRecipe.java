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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.geronimo.blueprint.utils.TypeUtils;

/**
 * @version $Rev: 6685 $ $Date: 2005-12-28T00:29:37.967210Z $
 */
public class CollectionRecipe extends AbstractRecipe {
    private final List<Recipe> list;
    private Class typeClass;
    private final EnumSet<Option> options = EnumSet.noneOf(Option.class);

    public CollectionRecipe(Class type) {
        this.list = new ArrayList<Recipe>();
        this.typeClass = type;
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

    protected Object internalCreate(boolean lazyRefAllowed) throws ConstructionException {
        Class type = getType(Object.class);

        if (!TypeUtils.hasDefaultConstructor(type)) {
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

        // add to execution container if name is specified
        if (getName() != null) {
            ExecutionContext.getContext().addObject(getName(), instance);
        }

        boolean refAllowed = options.contains(Option.LAZY_ASSIGNMENT);

        int index = 0;
        for (Recipe recipe : list) {
            Object value;
            if (recipe != null) {
                try {
                    value = recipe.create(refAllowed);
                } catch (Exception e) {
                    throw new ConstructionException("Unable to convert value " + recipe + " to type " + type, e);
                }
            } else {
                value = null;
            }

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
        Class expectedClass = TypeUtils.toClass(expectedType);
        if (typeClass != null) {
            Class type = typeClass;
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
        if (TypeUtils.hasDefaultConstructor(type)) {
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
    
    public void add(Recipe value) {
        list.add(value);
    }

    public void addAll(Collection<Recipe> value) {
        list.addAll(value);
    }

    public void remove(Recipe value) {
        list.remove(value);
    }

    public void removeAll(Recipe value) {
        list.remove(value);
    }

    public List<Recipe> getAll() {
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
