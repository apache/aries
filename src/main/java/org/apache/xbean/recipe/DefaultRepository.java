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

import java.util.SortedMap;
import java.util.TreeMap;

public class DefaultRepository implements Repository {
    /**
     * The unmarshaled object instances by name.
     */
    private final SortedMap<String, Object> instances = new TreeMap<String, Object>();

    /**
     * Does this repository contain a object with the specified name.
     *
     * @param name the unique name of the object instance
     * @return true if this repository contain a object with the specified name
     */
    public boolean contains(String name) {
        return instances.containsKey(name);
    }

    /**
     * Gets the object or recipe with the specified name from this repository.
     *
     * @param name the unique name of the object instance
     * @return the object instance, a recipe to build the object or null
     */
    public Object get(String name) {
        return instances.get(name);
    }

    /**
     * Add an object instance to this repository.
     *
     * @param name the unique name of the instance
     * @param instance the instance
     * @throws ConstructionException if another object instance is already registered with the name
     */
    public void add(String name, Object instance) {
        if (instances.containsKey(name) && !(instances.get(name) instanceof Recipe)) {
            throw new ConstructionException("Name " + name + " is already registered to instance " + instance);
        }
        instances.put(name, instance);
    }
}
