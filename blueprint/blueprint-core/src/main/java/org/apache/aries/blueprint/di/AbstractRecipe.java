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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.apache.aries.blueprint.container.GenericType;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.container.Converter;
import org.osgi.service.blueprint.container.ReifiedType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRecipe implements Recipe {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(AbstractRecipe.class);

    protected final String name;
    protected boolean prototype = true;

    protected AbstractRecipe(String name) {
        if (name == null)
            throw new NullPointerException("name is null");
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

        // if this recipe has already been executed in this context, return the
        // currently registered value
        Object result = context.getPartialObject(name);
        if (result != null) {
            return result;
        }

        // execute the recipe
        context.push(this);
        boolean didCreate = false;
        try {
            if (!prototype) {
                FutureTask<Object> objectCreation = new FutureTask<Object>(
                        new Callable<Object>() {
                            public Object call()
                                    throws ComponentDefinitionException {
                                return internalCreate();
                            }
                        });
                Future<Object> resultFuture = context.addFullObject(name,
                        objectCreation);

                // are we the first to try to create it
                if (resultFuture == null) {
                    didCreate = true;
                    objectCreation.run();
                    resultFuture = objectCreation;
                }

                try {
                    result = resultFuture.get();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ee) {
                    if (ee.getCause() instanceof ComponentDefinitionException)
                        throw (ComponentDefinitionException) ee.getCause();
                    else if (ee.getCause() instanceof RuntimeException)
                        throw (RuntimeException) ee.getCause();
                    else
                        throw (Error) ee.getCause();
                }

            } else {
                result = internalCreate();
            }
        } finally {
            if (didCreate)
                context.removePartialObject(name);

            Recipe popped = context.pop();
            if (popped != this) {
                // noinspection ThrowFromFinallyBlock
                throw new IllegalStateException(
                        "Internal Error: recipe stack is corrupt:"
                                + " Expected " + this
                                + " to be popped of the stack but was "
                                + popped);
            }
        }

        return result;
    }

    protected abstract Object internalCreate()
            throws ComponentDefinitionException;

    protected void addPartialObject(Object obj) {
        if (!prototype) {
            ExecutionContext.Holder.getContext().addPartialObject(name, obj);
        }
    }

    protected boolean canConvert(Object obj, ReifiedType type) {
        return ExecutionContext.Holder.getContext().canConvert(obj, type);
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
        return doLoadType(typeName, fromClassLoader, true, false);
    }

    private ReifiedType doLoadType(String typeName,
            ClassLoader fromClassLoader, boolean checkNestedIfFailed,
            boolean retry) {
        try {
            return GenericType.parse(typeName,
                    fromClassLoader != null ? fromClassLoader
                            : ExecutionContext.Holder.getContext());
        } catch (ClassNotFoundException e) {
            String errorMessage = "Unable to load class " + typeName
                    + " from recipe " + this;
            if (checkNestedIfFailed) {
                int lastDot = typeName.lastIndexOf('.');
                if (lastDot > 0 && lastDot < typeName.length()) {
                    String nestedTypeName = typeName.substring(0, lastDot)
                            + "$" + typeName.substring(lastDot + 1);
                    LOGGER.debug(errorMessage
                            + ", trying to load a nested class "
                            + nestedTypeName);
                    try {
                        return doLoadType(nestedTypeName, fromClassLoader,
                                false, true);
                    } catch (ComponentDefinitionException e2) {
                        // ignore, the recursive call will throw this exception,
                        // but ultimately the exception referencing the original
                        // typeName has to be thrown
                    }
                }
            }
            if (!retry) {
                LOGGER.error(errorMessage);
            }
            throw new ComponentDefinitionException(errorMessage, e);
        }
    }

    public void destroy(Object instance) {
    }

    public List<Recipe> getConstructorDependencies() {
        return Collections.emptyList();
    }

    public String toString() {
        return getClass().getSimpleName() + "[" + "name='" + name + '\'' + ']';

    }

}
