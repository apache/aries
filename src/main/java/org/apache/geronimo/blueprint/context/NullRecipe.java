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

/**
 * Recipe that always returns null but only for a specified type.
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev$, $Date$
 */
public class NullRecipe extends AbstractRecipe {

    private Class type;

    public NullRecipe(Class type) {
        this.type = (type == null) ? Object.class : type;
    }

    @Override
    protected Object internalCreate(Type expectedType, boolean lazyRefAllowed) throws ConstructionException {
        Class expectedClass = RecipeHelper.toClass(expectedType);
        if (expectedClass.isAssignableFrom(type)) {
            return null;
        } else {
            throw new ConstructionException("");
        }
    }

    public boolean canCreate(Type expectedType) {
        Class expectedClass = RecipeHelper.toClass(expectedType);
        return expectedClass.isAssignableFrom(type);
    }

}
