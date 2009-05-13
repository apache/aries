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

/**
 * Reference is a named (lazy) reference from one object to another. This data class is updated when the reference
 * is resolved which can be immedately when the ref is created, or later when an instance with the referenced
 * name is created.
 * <p/>
 * When the reference is resolved, an optional Action will be invoked which is commonly used to update a
 * property on the source object of the reference.
 */
public class Reference {
    private final String name;
    private boolean resolved;
    private Object instance;
    private Action action;

    /**
     * Create a reference to the specified name.
     * @param name the name of the referenced object
     */
    public Reference(String name) {
        this.name = name;
    }

    /**
     * Gets the name of the referenced object.
     * @return name the name of the referenced object
     */
    public String getName() {
        return name;
    }

    /**
     * Has this reference been resolved?
     * @return true if the reference has been resolved; false otherwise
     */
    public boolean isResolved() {
        return resolved;
    }

    /**
     * Gets the referenced object instance or null if the reference has not been resolved yet;
     *
     * @return the referenced object instance or null
     */
    public Object get() {
        return instance;
    }

    /**
     * Sets the referenced object instance.  If an action is registered the onSet method is invoked.
     *
     * @param object the reference instance
     */
    public void set(Object object) {
        if (resolved) {
            throw new ConstructionException("Reference has already been resolved");
        }
        resolved = true;
        this.instance = object;
        if (action != null) {
            action.onSet(this);
        }
    }

    /**
     * Registers an action to invoke when the instance is set.  If the instance, has already been set, the
     * onSet method will immedately be invoked.
     *
     * @return the action to invoke when this refernce is resolved; not null
     */
    public void setAction(Action action) {
        if (action == null) {
            throw new NullPointerException("action is null");
        }
        this.action = action;
        if (resolved) {
            action.onSet(this);
        }
    }

    public static interface Action {
        void onSet(Reference ref);
    }
}