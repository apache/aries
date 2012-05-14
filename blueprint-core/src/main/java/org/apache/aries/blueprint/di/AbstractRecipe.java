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
package org.apache.aries.blueprint.di;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import org.apache.aries.blueprint.container.GenericType;
import org.osgi.service.blueprint.container.ReifiedType;
import org.osgi.service.blueprint.container.ComponentDefinitionException;

public abstract class AbstractRecipe implements Recipe {

    protected final String name;
    protected boolean prototype = true;

    protected AbstractRecipe(String name) {
        if (name == null) throw new NullPointerException("name is null");
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isPrototype() {
        return prototype;
    }

    public void setPrototype(boolean prototype) {
        this.prototype = prototype;
    }

    public final Object create() throws ComponentDefinitionException {
        // Ensure a container has been set
        ExecutionContext context = ExecutionContext.Holder.getContext();

        synchronized (context.getInstanceLock()) {
            // if this recipe has already been executed in this container, return the currently registered value
            Object obj = context.getPartialObject(name);
            if (obj != null) {
                return obj;
            }

            // execute the recipe
            context.push(this);
            try {
                obj = internalCreate();
                if (!prototype) {
                    context.addFullObject(name, obj);
                }
                return obj;
            } finally {
                Recipe popped = context.pop();
                if (popped != this) {
                    //noinspection ThrowFromFinallyBlock
                    throw new IllegalStateException("Internal Error: recipe stack is corrupt:" +
                            " Expected " + this + " to be popped of the stack but was " + popped);
                }
            }
        }
    }

    protected abstract Object internalCreate() throws ComponentDefinitionException;

    protected void addPartialObject(Object obj) {
        if (!prototype) {                 
            ExecutionContext.Holder.getContext().addPartialObject(name, obj);
        }
    }
    
    protected Object convert(Object obj, ReifiedType type) throws Exception {
        return ExecutionContext.Holder.getContext().convert(obj, type);
    }

    protected Object convert(Object obj, Type type) throws Exception {
        return ExecutionContext.Holder.getContext().convert(obj, new GenericType(type));
    }

    protected Class loadClass(String className) {
        ReifiedType t = loadType(className, null);
        return t != null ? t.getRawClass() : null;
    }

    protected ReifiedType loadType(String typeName) {
        return loadType(typeName, null);
    }

    protected ReifiedType loadType(String typeName, ClassLoader fromClassLoader) {
        if (typeName == null) {
            return null;
        }
        try {
            return GenericType.parse(typeName, fromClassLoader != null ? fromClassLoader : ExecutionContext.Holder.getContext());
        } catch (ClassNotFoundException e) {
            throw new ComponentDefinitionException("Unable to load class " + typeName + " from recipe " + this, e);
        }
    }

    public void destroy(Object instance) {
    }

    public List<Recipe> getConstructorDependencies() {
        return Collections.emptyList();
    }
    
    public String toString() {
        return getClass().getSimpleName() + "[" +
                "name='" + name + '\'' +
                ']';

    }

}
