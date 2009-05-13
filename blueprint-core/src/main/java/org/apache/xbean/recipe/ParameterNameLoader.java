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

import java.util.List;
import java.util.Map;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

/**
 * Determines the parameter names of Constructors or Methods.
 */
public interface ParameterNameLoader {
    /**
     * Gets the parameter names of the specified method or null if the class was compiled without debug symbols on.
     * @param method the method for which the parameter names should be retrieved
     * @return the parameter names or null if the class was compilesd without debug symbols on
     */
    List<String> get(Method method);

    /**
     * Gets the parameter names of the specified constructor or null if the class was compiled without debug symbols on.
     * @param constructor the constructor for which the parameters should be retrieved
     * @return the parameter names or null if the class was compiled without debug symbols on
     */
    List<String> get(Constructor constructor);
}
