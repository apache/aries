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
package org.apache.geronimo.blueprint.di;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.geronimo.blueprint.Destroyable;

public abstract class AbstractRecipe implements Recipe {

    private static final AtomicLong ID = new AtomicLong(1);

    private final long id;
    private String name;
    protected Boolean allowPartial;

    protected AbstractRecipe() {
        this.id = ID.getAndIncrement();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null) throw new NullPointerException("name is null");
        this.name = name;
    }

    public Object create() throws ConstructionException {
        return create(false);
    }

    public final Object create(boolean lazyRefAllowed) throws ConstructionException {
        // Ensure a container has been set
        ExecutionContext context = ExecutionContext.getContext();

        // if this recipe has already been executed in this container, return the currently registered value
        String name = getName();
        if (name != null && context.containsCreatedObject(name)) {
            return context.getCreatedObject(name);
        }

        // execute the recipe
        context.push(this);
        try {
            return internalCreate(lazyRefAllowed);
        } finally {
            Recipe popped = context.pop();
            if (popped != this) {
                //noinspection ThrowFromFinallyBlock
                throw new IllegalStateException("Internal Error: recipe stack is corrupt:" +
                        " Expected " + this + " to be popped of the stack but " + popped + " was");
            }
        }
    }

    protected abstract Object internalCreate(boolean lazyRefAllowed) throws ConstructionException;
    
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

    protected Object convert(Object obj, Type type) throws Exception {
        return ExecutionContext.getContext().convert(obj, type);
    }

    public Destroyable getDestroyable(Object instance) {
        return null;
    }
}
