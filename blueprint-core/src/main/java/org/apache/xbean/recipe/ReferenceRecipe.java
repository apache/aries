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
package org.apache.xbean.recipe;

import java.util.Collections;
import java.util.List;
import java.lang.reflect.Type;

public class ReferenceRecipe extends AbstractRecipe {
    private String referenceName;

    public ReferenceRecipe() {
    }

    public ReferenceRecipe(String referenceName) {
        this.referenceName = referenceName;
    }

    public String getReferenceName() {
        return referenceName;
    }

    public void setReferenceName(String name) {
        this.referenceName = name;
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
        } else {
            return RecipeHelper.isInstance(type, object);
        }
    }

    protected Object internalCreate(Type expectedType, boolean lazyRefAllowed) throws ConstructionException {
        if (referenceName == null) {
            throw new ConstructionException("Reference name has not been set");
        }

        ExecutionContext context = ExecutionContext.getContext();

        Object object;
        if (!context.containsObject(referenceName)) {
            if (!lazyRefAllowed) {
                throw new ConstructionException("Currently no object registered with name " + referenceName +
                        " and a lazy reference not allowed");
            }

            Reference reference = new Reference(referenceName);
            context.addReference(reference);
            object = reference;
        } else {
            object = context.getObject(referenceName);
            if (object instanceof Recipe) {
                if (lazyRefAllowed) {
                    Reference reference = new Reference(referenceName);
                    context.addReference(reference);
                    object = reference;
                } else {
                    Recipe recipe = (Recipe) object;
                    object = recipe.create(expectedType, false);
                }

            }
        }

        // add to execution context if name is specified
        if (getName() != null) {
            if (object instanceof Reference) {
                object = new WrapperReference(getName(), (Reference) object);
            } else {
                ExecutionContext.getContext().addObject(getName(), object);
            }
        }

        return object;
    }

    private static class WrapperReference extends Reference {
        private final Reference delegate;

        private WrapperReference(String name, Reference delegate) {
            super(name);
            this.delegate = delegate;
        }

        public boolean isResolved() {
            return delegate.isResolved();
        }

        public Object get() {
            return delegate.get();
        }

        public void set(Object object) {
            if (isResolved()) {
                throw new ConstructionException("Reference has already been resolved");
            }

            // add to execution context
            ExecutionContext.getContext().addObject(getName(), object);

            delegate.set(object);
        }

        public void setAction(Action action) {
            delegate.setAction(action);
        }
    }
}
