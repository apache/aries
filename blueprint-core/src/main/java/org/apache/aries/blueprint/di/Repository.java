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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.service.blueprint.container.ComponentDefinitionException;

public interface Repository {

    /**
     * Returns the set of all known object names (recipes, instances or default objects)
     * @return
     */
    Set<String> getNames();

    /**
     * Return the singleton instance for the given name.
     * This method will not create the object if it has been created yet.
     *
     * @param name
     * @return the instance or <code>null</code>
     */
    Object getInstance(String name);

    /**
     * Return the recipe for the given name.
     *
     * @param name
     * @return the recipe or <code>null</code>
     */
    Recipe getRecipe(String name);

    void putRecipe(String name, Recipe recipe);
    
    /**
     * Remove an uninstantiated recipe
     * @param name
     * @throws ComponentDefinitionException if the recipe is already instantiated
     */
    void removeRecipe(String name);

    Object create(String name) throws ComponentDefinitionException;

    Map<String, Object> createAll(Collection<String> names) throws ComponentDefinitionException;

    <T> List<T> getAllRecipes(Class<T> clazz, String... names);

    Set<Recipe> getAllRecipes(String... names);

    void destroy();

    /**
     * Lock that should be used to synchronized creation of singletons
     *
     * @return
     */
    public Object getInstanceLock();
}
