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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class ExecutionContext {
    private static final ThreadLocal<ExecutionContext> context = new ThreadLocal<ExecutionContext>();

    public static boolean isContextSet() {
        return context.get() != null;
    }

    public static ExecutionContext getContext() {
        ExecutionContext executionContext = context.get();
        if (executionContext == null) {
            throw new IllegalStateException("Execution context has not been set");
        }
        return executionContext;
    }

    public static ExecutionContext setContext(ExecutionContext newContext) {
        ExecutionContext oldContext = context.get();
        context.set(newContext);
        return oldContext;
    }

    /**
     * Adds a recipe to the top of the execution stack.  If the recipe is already on
     * the stack, a CircularDependencyException is thrown.
     * @param recipe the recipe to add to the stack
     * @throws CircularDependencyException if the recipe is already on the stack
     */
    public abstract void push(Recipe recipe) throws CircularDependencyException;

    /**
     * Removes the top recipe from the execution stack.
     * @return the top recipe on the stack
     */
    public abstract Recipe pop();

    /**
     * Gets a snapshot of the current execution stack.  The returned list is
     * a snapshot so any modification of the returned list does not modify
     * the stack contained in this object.
     * @return a snapshot of the current execution stack
     */
    public abstract LinkedList<Recipe> getStack();

    /**
     * Does this context contain a object with the specified name.
     *
     * @param name the unique name of the object instance
     * @return true if this context contain a object with the specified name
     */
    public abstract boolean containsObject(String name);

    /**
     * Gets the object or recipe with the specified name from the repository.
     *
     * @param name the unique name of the object instance
     * @return the object instance, a recipe to build the object or null
     */
    public abstract Object getObject(String name);

    /**
     * Add an object to the repository.
     *
     * @param name the unique name of the object instance
     * @param object the object instance
     * @throws ConstructionException if another object instance is already registered with the name
     */
    public abstract void addObject(String name, Object object);

    /**
     * Adds a reference to an object to this context.  If an object is already registered under
     * the referenced name, the reference will immedately be set.  Otherwise, the reference will be set
     * when an object is added with the referenced name.
     *
     * @param reference the reference to set
     */
    public abstract void addReference(Reference reference);

    /**
     * Gets the unresolved references by name.
     *
     * @return the unresolved references by name
     */
    public abstract Map<String, List<Reference>> getUnresolvedRefs();

    /**
     * Gets the class loader used for loading of all classes during the
     * life of this execution context
     * @return the class loader for loading classes in this context
     */
    public abstract ClassLoader getClassLoader();
}
