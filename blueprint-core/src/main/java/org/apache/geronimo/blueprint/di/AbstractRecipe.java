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
import java.util.List;

import org.apache.geronimo.blueprint.Destroyable;
import org.osgi.service.blueprint.container.ComponentDefinitionException;

public abstract class AbstractRecipe implements Recipe {

    private String name;
    protected Boolean allowPartial;

    protected AbstractRecipe() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null) throw new NullPointerException("name is null");
        this.name = name;
    }

    public final Object create() throws ComponentDefinitionException {
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
            return internalCreate();
        } finally {
            Recipe popped = context.pop();
            if (popped != this) {
                //noinspection ThrowFromFinallyBlock
                throw new IllegalStateException("Internal Error: recipe stack is corrupt:" +
                        " Expected " + this + " to be popped of the stack but " + popped + " was");
            }
        }
    }

    protected abstract Object internalCreate() throws ComponentDefinitionException;

    public void postCreate() {
    }

    public void setAllowPartial(Boolean allowPartial) {
        this.allowPartial = allowPartial;
    }
    
    public Boolean getAllowPartial() {
        return allowPartial;
    }
    
    private boolean isAllowPartial() {
        return (allowPartial == null) || allowPartial;
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

    public String toString() {
        if (name != null) {
            return name;
        }
        String string = getClass().getSimpleName();
        if (string.endsWith("Recipe")) {
            string = string.substring(0, string.length() - "Recipe".length());
        }
        return string;
    }

    protected Object convert(Object obj, Type type) throws Exception {
        return ExecutionContext.getContext().convert(obj, type);
    }

    protected Class loadClass(String className) {
        try {
            return ExecutionContext.getContext().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new ComponentDefinitionException(e);
        }
    }

    public Destroyable getDestroyable(Object instance) {
        return null;
    }
}
