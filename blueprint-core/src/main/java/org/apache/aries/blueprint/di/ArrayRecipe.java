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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.container.ReifiedType;

/**
 * @version $Rev$ $Date$
 */
public class ArrayRecipe extends AbstractRecipe {

    private final List<Recipe> list;
    private final Object type;

    public ArrayRecipe(String name, Object type) {
        super(name);
        this.type = type;
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
        ReifiedType type;
        if (this.type instanceof Class) {
            type = new ReifiedType((Class) this.type);
        } else if (this.type instanceof String) {
            type = loadType((String) this.type);
        } else {
            type = new ReifiedType(Object.class);
        }

        // create array instance
        Object array;
        try {
            array = Array.newInstance(type.getRawClass(), list.size());
        } catch (Exception e) {
            throw new ComponentDefinitionException("Error while creating array instance: " + type);
        }

        int index = 0;
        for (Recipe recipe : list) {
            Object value;
            if (recipe != null) {
                try {
                    value = convert(recipe.create(), type);
                } catch (Exception e) {
                    throw new ComponentDefinitionException("Unable to convert value " + recipe + " to type " + type, e);
                }
            } else {
                value = null;
            }
            
            Array.set(array, index, value);
            index++;
        }
        
        return array;
    }

    public void add(Recipe value) {
        list.add(value);
    }

}
