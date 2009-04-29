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
import org.osgi.service.blueprint.reflect.ValueMetadata;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev$, $Date$
 */
public class ValueRecipe extends AbstractRecipe {

    private ConversionService conversionService;
    private ValueMetadata value;
    private Class type;

    public ValueRecipe(ConversionService conversionService, ValueMetadata value, Class type) {
        this.conversionService = conversionService;
        this.value = value;
        this.type = type;
    }

    private static Class determineType(Class type, Type defaultType) {
        // TODO: check if type is assignable from defaultType?
        return (type != null) ? type : RecipeHelper.toClass(defaultType);
    }

    @Override
    protected Object internalCreate(Type expectedType, boolean lazyRefAllowed) throws ConstructionException {
        Class myType = determineType(type, expectedType);

        try {
            return conversionService.convert(value.getStringValue(), myType);
        } catch (Exception e) {            
            throw new ConstructionException(e);
        }
    }

    public boolean canCreate(Type expectedType) {
        // XXX: this is expensive but that's what spec wants
        Class myType = determineType(type, expectedType);
        
        try {
            conversionService.convert(value.getStringValue(), myType);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public String toString() {
        return "ValueRecipe: " + type + " " + value.getStringValue();
    }

}
