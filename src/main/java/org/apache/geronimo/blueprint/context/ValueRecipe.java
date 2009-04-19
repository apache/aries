/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.geronimo.blueprint.context;

import java.lang.reflect.Type;

import org.apache.xbean.recipe.AbstractRecipe;
import org.apache.xbean.recipe.ConstructionException;
import org.apache.xbean.recipe.RecipeHelper;
import org.osgi.service.blueprint.convert.ConversionService;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev$, $Date$
 */
public class ValueRecipe extends AbstractRecipe {

    private ConversionService conversionService;
    private String value;
    private Class type;
    private Class groupingType;

    public ValueRecipe(ConversionService conversionService, String value, Class type, Class groupingType) {
        this.conversionService = conversionService;
        this.value = value;
        this.type = type;
        this.groupingType = groupingType;
    }

    private static Class determineType(Class type, Class groupingType, Class defaultType) throws RuntimeException {
        if (type != null) {
            if (groupingType == null || groupingType.isAssignableFrom(type)) {
                return type;
            } else {
                throw new RuntimeException(type.getName() + " cannot be assigned to " + groupingType.getName());
            }
        } else if (groupingType != null) {
            return groupingType;
        } else {
            return defaultType;
        }
    }

    @Override
    protected Object internalCreate(Type expectedType, boolean lazyRefAllowed) throws ConstructionException {
        Class myType = determineType(type, groupingType, RecipeHelper.toClass(expectedType));

        try {
            return conversionService.convert(value, myType);
        } catch (Exception e) {
            throw new ConstructionException(e);
        }
    }

    public boolean canCreate(Type expectedType) {
        return true;
    }

}
