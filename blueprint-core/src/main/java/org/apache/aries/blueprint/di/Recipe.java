/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.blueprint.di;

import java.util.List;

import org.osgi.service.blueprint.container.ComponentDefinitionException;

/**
 * The <code>Recipe</code> interface abstracts the creation of objects
 *
 * @version $Rev$ $Date$
 */
public interface Recipe {

    /**
     * Get the unique name for this recipe.
     *
     * @return the unique name for this recipe.
     */
    String getName();
    
    /**
     * Get the list of constructor dependencies, i.e. explicit and
     * argument dependencies. These dependencies must be satisfied
     * before the an object can be created.
     * 
     * @return a list of constructor dependencies
     */
    List<Recipe> getConstructorDependencies();

    /**
     * Get the list of nested recipes, i.e. all dependencies including 
     * constructor dependencies.
     *
     * @return a list of dependencies
     */
    List<Recipe> getDependencies();

    /**
     * Create an instance for this recipe.
     *
     * @return a new instance for this recipe
     * @throws ComponentDefinitionException
     */
    Object create() throws ComponentDefinitionException;

    /**
     * Destroy an instance created by this recipe
     *
     * @param instance the instance to be destroyed
     */
    void destroy(Object instance);

}
