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

public interface Repository {
    /**
     * Does this repository contain a object with the specified name.
     *
     * @param name the unique name of the object instance
     * @return true if this repository contain a object with the specified name
     */
    boolean contains(String name);

    /**
     * Gets the object or recipe with the specified name from the repository.
     *
     * @param name the unique name of the object instance
     * @return the object instance, a recipe to build the object or null
     */
    Object get(String name);

    /**
     * Add an object to the repository.
     *
     * @param name the unique name of the object instance
     * @param object the object instance
     * @throws ConstructionException if another object instance is already registered with the name
     */
    void add(String name, Object object);
}
