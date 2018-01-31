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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.aries.blueprint.utils.ReflectionUtils;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.container.ReifiedType;

/**
 * @version $Rev$ $Date$
 */
public class CollectionRecipe extends AbstractRecipe {

    private final List<Recipe> list;
    private final Class<?> collectionTypeClass;
    private final String defaultValueType;

    public CollectionRecipe(String name, Class<?> collectionType, String valueType) {
        super(name);
        if (collectionType == null) throw new NullPointerException("type is null");
        this.collectionTypeClass = collectionType;
        this.defaultValueType = (valueType != null) ? valueType : Object.class.getName();
        this.list = new ArrayList<Recipe>();
    }

    public List<Recipe> getDependencies() {
        List<Recipe> nestedRecipes = new ArrayList<Recipe>(list.size());
        for (Recipe recipe : list) {
            if (recipe != null) {
                nestedRecipes.add(recipe);
            }
        }
        return nestedRecipes;
    }

    protected Object internalCreate() throws ComponentDefinitionException {
        Class type = getCollection(collectionTypeClass);

        if (!ReflectionUtils.hasDefaultConstructor(type)) {
            throw new ComponentDefinitionException("Type does not have a default constructor " + type.getName());
        }

        // create collection instance
        Object o;
        try {
            o = type.newInstance();
        } catch (Exception e) {
            throw new ComponentDefinitionException("Error while creating collection instance: " + type.getName());
        }
        if (!(o instanceof Collection)) {
            throw new ComponentDefinitionException("Specified collection type does not implement the Collection interface: " + type.getName());
        }
        Collection instance = (Collection) o;

        ReifiedType defaultConversionType = loadType(defaultValueType);
        Type conversionType = null;
        for (Recipe recipe : list) {
            Object value;
            if (recipe != null) {
                try {
                    conversionType = defaultConversionType.getRawClass();
                    if (recipe instanceof ValueRecipe) {
                        conversionType = ((ValueRecipe) recipe).getValueType();
                    }
                    value = convert(recipe.create(), conversionType);
                } catch (Exception e) {
                    throw new ComponentDefinitionException("Unable to convert value " + recipe + " to type " + conversionType, e);
                }
            } else {
                value = null;
            }
            instance.add(value);
        }
        return instance;
    }

    public void add(Recipe value) {
        list.add(value);
    }

    public static Class getCollection(Class type) {
        if (ReflectionUtils.hasDefaultConstructor(type)) {
            return type;
        } else if (SortedSet.class.isAssignableFrom(type)) {
            return TreeSet.class;
        } else if (Set.class.isAssignableFrom(type)) {
            return LinkedHashSet.class;
        } else if (List.class.isAssignableFrom(type)) {
            return ArrayList.class;
        } else if (Queue.class.isAssignableFrom(type)) {
            return LinkedList.class;
        } else {
            return ArrayList.class;
        }
    }
}
