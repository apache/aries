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
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.lang.reflect.Type;

public abstract class AbstractRecipe implements Recipe {
    private static final AtomicLong ID = new AtomicLong(1);
    private long id;
    private String name;
    protected Boolean allowPartial;

    protected AbstractRecipe() {
        id = ID.getAndIncrement();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null) throw new NullPointerException("name is null");
        this.name = name;
    }

    public Object create() throws ConstructionException {
        return create(null);
    }

    public final Object create(ClassLoader classLoader) throws ConstructionException {
        // if classloader was passed in, set it on the thread
        ClassLoader oldClassLoader = null;
        if (classLoader != null) {
            oldClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(classLoader);
        }

        try {
            return create(Object.class, false);
        } finally {
            // if we set a thread context class loader, reset it
            if (classLoader != null) {
                Thread.currentThread().setContextClassLoader(oldClassLoader);
            }
        }
    }

    public final Object create(Type expectedType, boolean lazyRefAllowed) throws ConstructionException {
        if (expectedType == null) throw new NullPointerException("expectedType is null");

        // assure there is a valid thread context class loader
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        if (oldClassLoader == null) {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        }

        // if there is no execution context, create one
        boolean createNewContext = !ExecutionContext.isContextSet();
        if (createNewContext) {
            ExecutionContext.setContext(new DefaultExecutionContext());
        }

        try {
            ExecutionContext context = ExecutionContext.getContext();

            // if this recipe has already been executed in this context, return the currently registered value
            String name = getName();
            if (name != null && context.containsCreatedObject(name)) {
                return context.getCreatedObject(name);
            }

            // execute the recipe
            context.push(this);
            try {
                return internalCreate(expectedType, lazyRefAllowed);
            } finally {
                Recipe popped = context.pop();
                if (popped != this) {
                    //noinspection ThrowFromFinallyBlock
                    throw new IllegalStateException("Internal Error: recipe stack is corrupt:" +
                            " Expected " + this + " to be popped of the stack but " + popped + " was");
                }
            }
        } finally {
            // if we set a new execution context, remove it from the thread
            if (createNewContext) {
                ExecutionContext context = ExecutionContext.getContext();
                ExecutionContext.setContext(null);

                Map<String,List<Reference>> unresolvedRefs = context.getUnresolvedRefs();
                if (!unresolvedRefs.isEmpty()) {
                    throw new UnresolvedReferencesException(unresolvedRefs);
                }
            }

            // if we set a thread context class loader, clear it
            if (oldClassLoader == null) {
                Thread.currentThread().setContextClassLoader(null);
            }
        }
    }

    protected abstract Object internalCreate(Type expectedType, boolean lazyRefAllowed) throws ConstructionException;
    
    public void setAllowPartial(Boolean allowPartial) {
        this.allowPartial = allowPartial;
    }
    
    public Boolean getAllowPartial() {
        return allowPartial;
    }
    
    private boolean isAllowPartial() {
        return (allowPartial == null) ? true : allowPartial.booleanValue();
    }
    
    protected void addObject(Object obj, boolean partial) {
        if (partial && !isAllowPartial()) {
            return;
        }
        String name = getName();
        if (name != null) {
            ExecutionContext.getContext().addObject(name, obj, partial);
        }
    }
    
    public List<Recipe> getNestedRecipes() {
        return new ArrayList<Recipe>();
    }

    public List<Recipe> getConstructorRecipes() {
        return Collections.emptyList();
    }

    public String toString() {
        if (name != null) {
            return name;
        }

        String string = getClass().getSimpleName();
        if (string.endsWith("Recipe")) {
            string = string.substring(0, string.length() - "Recipe".length());
        }
        return string + "@" + id;
    }
}
