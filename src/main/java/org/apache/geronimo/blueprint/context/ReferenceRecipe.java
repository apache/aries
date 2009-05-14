/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.geronimo.blueprint.context;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import org.apache.xbean.recipe.AbstractRecipe;
import org.apache.xbean.recipe.ConstructionException;
import org.apache.xbean.recipe.ExecutionContext;
import org.apache.xbean.recipe.NoSuchObjectException;
import org.apache.xbean.recipe.Recipe;
import org.apache.xbean.recipe.RecipeHelper;
import org.osgi.framework.ServiceReference;

/**
 * Special reference recipe implementation that can recognize service objects
 * and either inject the service object or a ServiceReference object for it based
 * on the expected injection type.
 */
public class ReferenceRecipe extends AbstractRecipe {
    
    private String referenceName;

    public ReferenceRecipe(String referenceName) {
        this.referenceName = referenceName;
    }

    public String getReferenceName() {
        return referenceName;
    }

    public List<Recipe> getNestedRecipes() {
        ExecutionContext context = ExecutionContext.getContext();
        if (!context.containsObject(referenceName)) {
            throw new NoSuchObjectException(referenceName);
        }

        Object object = ExecutionContext.getContext().getObject(referenceName);
        if (object instanceof Recipe) {
            Recipe recipe = (Recipe) object;
            return Collections.singletonList(recipe);
        }
        return Collections.emptyList();
    }

    public List<Recipe> getConstructorRecipes() {
        return getNestedRecipes();
    }

    public boolean canCreate(Type type) {
        if (referenceName == null) {
            throw new ConstructionException("Reference name has not been set");
        }

        ExecutionContext context = ExecutionContext.getContext();

        Object object = context.getObject(referenceName);
        if (object instanceof Recipe) {
            Recipe recipe = (Recipe) object;
            return recipe.canCreate(type);
        } else if (isServiceReferenceInjection(object, type)) {
            return true;
        } else {
            return RecipeHelper.isInstance(type, object);
        }
    }

    public Type[] getTypes() {
        if (referenceName == null) {
            throw new ConstructionException("Reference name has not been set");
        }
        
        ExecutionContext context = ExecutionContext.getContext();
        
        Object object = context.getObject(referenceName);
        if (object instanceof Recipe) {
            Recipe recipe = (Recipe) object;
            return recipe.getTypes();
        } else if (object instanceof ServiceReferenceAccessor) {
            return new Type[] { object.getClass(), ServiceReference.class };
        } else {
            return new Type[] { object != null ? object.getClass() : Object.class };
        }
    }
    
    protected Object internalCreate(Type expectedType, boolean lazyRefAllowed) throws ConstructionException {
        if (referenceName == null) {
            throw new ConstructionException("Reference name has not been set");
        }

        ExecutionContext context = ExecutionContext.getContext();

        Object object;
        if (!context.containsObject(referenceName)) {
            throw new ConstructionException("There is no object registered with name " + referenceName);
        } else {
            object = context.getObject(referenceName);
            if (object instanceof Recipe) {
                Recipe recipe = (Recipe) object;
                object = recipe.create(expectedType, false);
            } else if (isServiceReferenceInjection(object, expectedType)) {
                object = ((ServiceReferenceAccessor) object).getServiceReference();
            }
        }

        return object;
    }

    private boolean isServiceReferenceInjection(Object object, Type expectedType) {
        if (object instanceof ServiceReferenceAccessor) {
            Class expectedClass = RecipeHelper.toClass(expectedType);
            return ServiceReference.class.equals(expectedClass);
        } else {
            return false;
        }
    }
}
