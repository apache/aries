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
package org.apache.xbean.recipe;

import java.io.Serializable;
import java.util.List;
import java.lang.reflect.Type;

/**
 * @version $Rev: 6680 $ $Date: 2005-12-24T04:38:27.427468Z $
 */
public interface Recipe extends Serializable {
    String getName();

    Type[] getTypes();

    Object create() throws ConstructionException;
    Object create(ClassLoader classLoader) throws ConstructionException;
    Object create(Type expectedType, boolean lazyRefAllowed) throws ConstructionException;

    List<Recipe> getNestedRecipes();

    List<Recipe> getConstructorRecipes();
}
