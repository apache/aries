/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.geronimo.blueprint.container;

import java.lang.reflect.Type;

import junit.framework.TestCase;
import org.apache.geronimo.blueprint.utils.TypeUtils;

public class GenericTypeTest extends TestCase {

    public void testParseTypes() throws ClassNotFoundException {
        GenericType type = GenericType.parse("java.util.List<java.lang.String[]>", getClass().getClassLoader());
        System.out.println(type);

        type = GenericType.parse("java.util.Map<int, java.util.List<java.lang.Integer>[]>", getClass().getClassLoader());
        System.out.println(type);

        Type t = TypeUtils.parseJavaType("java.util.List<java.lang.Integer>[]", getClass().getClassLoader());
        type = new GenericType(t);
        System.out.println(type.toString());
    }

}
