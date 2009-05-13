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

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * @version $Rev$ $Date$
 */
public class ArrayRecipe extends AbstractRecipe {
    private final List<Object> list;
    private String typeName;
    private Class typeClass;
    private final EnumSet<Option> options = EnumSet.noneOf(Option.class);

    public ArrayRecipe() {
        list = new ArrayList<Object>();
    }

    public ArrayRecipe(String type) {
        list = new ArrayList<Object>();
        this.typeName = type;
    }

    public ArrayRecipe(Class type) {
        if (type == null) throw new NullPointerException("type is null");

        this.list = new ArrayList<Object>();
        this.typeClass = type;
    }

    public ArrayRecipe(ArrayRecipe collectionRecipe) {
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
        Class expectedClass = RecipeHelper.toClass(expectedType);
        Class myType = getType(expectedType);
        return expectedClass.isArray() && RecipeHelper.isAssignable(expectedClass.getComponentType(), myType);
    }

    protected Object internalCreate(Type expectedType, boolean lazyRefAllowed) throws ConstructionException {
        Class type = getType(expectedType);

        // create array instance
        Object array;
        try {
            array = Array.newInstance(type, list.size());
        } catch (Exception e) {
            throw new ConstructionException("Error while creating array instance: " + type.getName());
        }

        // add to execution context if name is specified
        if (getName() != null) {
            ExecutionContext.getContext().addObject(getName(), array);
        }

        boolean refAllowed = options.contains(Option.LAZY_ASSIGNMENT);

        int index = 0;
        for (Object value : list) {
            value = RecipeHelper.convert(type, value, refAllowed);
            
            if (value instanceof Reference) {
                Reference reference = (Reference) value;
                reference.setAction(new UpdateArray(array, index));
            } else {
                //noinspection unchecked
                Array.set(array, index, value);
            }
            index++;
        }
        
        return array;
    }

    private Class getType(Type expectedType) {       
        Class expectedClass = RecipeHelper.toClass(expectedType);
        if (expectedClass.isArray()) {
            expectedClass = expectedClass.getComponentType();
        }
        Class type = expectedClass;
        if (typeClass != null || typeName != null) {
            type = typeClass;
            if (type == null) {
                try {
                    type = RecipeHelper.loadClass(typeName);
                } catch (ClassNotFoundException e) {
                    throw new ConstructionException("Type class could not be found: " + typeName);
                }
            }
            
            // in case where expectedType is a subclass of the assigned type
            if (type.isAssignableFrom(expectedClass)) {
                type = expectedClass;
            }
        }

        return type;
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

    private static class UpdateArray implements Reference.Action {
        private final Object array;
        private final int index;

        public UpdateArray(Object array, int index) {
            this.array = array;
            this.index = index;
        }

        @SuppressWarnings({"unchecked"})
        public void onSet(Reference ref) {
            Array.set(array, index, ref.get());
        }
    }
}
