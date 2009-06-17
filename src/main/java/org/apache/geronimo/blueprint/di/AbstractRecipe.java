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
import java.util.Map;
import java.util.HashMap;

import org.osgi.service.blueprint.container.ComponentDefinitionException;
import static org.apache.geronimo.blueprint.utils.TypeUtils.toClass;
import org.apache.geronimo.blueprint.utils.TypeUtils;

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
                addObject(obj, false);
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

    protected void addObject(Object obj, boolean partial) {
        if (prototype) {
            return;
        }
        ExecutionContext.Holder.getContext().addObject(name, obj, partial);
    }
    
    protected Object convert(Object obj, Type type) throws Exception {
        return ExecutionContext.Holder.getContext().convert(obj, type);
    }

    protected Class loadClass(String className) {
        return toClass(loadType(className, null));
    }

    protected Type loadType(String typeName) {
        return loadType(typeName, null);
    }

    protected Type loadType(String typeName, ClassLoader fromClassLoader) {
        if (typeName == null) {
            return null;
        }
        try {
            return TypeUtils.parseJavaType(typeName, fromClassLoader != null ? fromClassLoader : ExecutionContext.Holder.getContext());
        } catch (ClassNotFoundException e) {
            throw new ComponentDefinitionException("Unable to load class " + typeName + " from recipe " + this, e);
        }
    }

    public void destroy(Object instance) {
    }

    public String toString() {
        return getClass().getSimpleName() + "[" +
                "name='" + name + '\'' +
                ']';

    }

}
