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
package org.apache.aries.blueprint.container;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.net.URI;

import junit.framework.TestCase;

public class GenericTypeTest extends TestCase {

    private GenericType parse(String expression) throws Exception {
        GenericType type = GenericType.parse(expression, getClass().getClassLoader());
        assertEquals(expression, type.toString());
        return type;
    }
    
    public void testArrays() {
        assertTrue(AggregateConverter.isAssignable(new Object[0], new GenericType(Object[].class)));
        assertFalse(AggregateConverter.isAssignable(new Object[0], new GenericType(String[].class)));
        assertTrue(AggregateConverter.isAssignable(new String[0], new GenericType(String[].class)));
        assertFalse(AggregateConverter.isAssignable(new String[0], new GenericType(URI[].class)));
        assertTrue(AggregateConverter.isAssignable(new String[0], new GenericType(Object[].class)));
    }

    public void testParseTypes() throws Exception {
        
        GenericType type = parse("java.util.List<java.lang.String[]>");
        assertEquals(List.class, type.getRawClass());
        assertEquals(1, type.size());
        assertEquals(String[].class, type.getActualTypeArgument(0).getRawClass());
        assertEquals(1, type.getActualTypeArgument(0).size());
        assertEquals(String.class, type.getActualTypeArgument(0).getActualTypeArgument(0).getRawClass());

        type = parse("java.util.Map<int,java.util.List<java.lang.Integer>[]>");
        assertEquals(Map.class, type.getRawClass());
        assertEquals(2, type.size());
        assertEquals(int.class, type.getActualTypeArgument(0).getRawClass());
        assertEquals(List[].class, type.getActualTypeArgument(1).getRawClass());
        assertEquals(1, type.getActualTypeArgument(1).size());
        assertEquals(Integer.class, type.getActualTypeArgument(1).getActualTypeArgument(0).getActualTypeArgument(0).getRawClass());

        type = parse("java.util.List<java.lang.Integer>[]");
        assertEquals(List[].class, type.getRawClass());
        assertEquals(1, type.size());
        assertEquals(Integer.class, type.getActualTypeArgument(0).getActualTypeArgument(0).getRawClass());

        type = parse("java.util.List<? extends java.lang.Number>");
        assertEquals(List.class, type.getRawClass());
        assertEquals(1, type.size());
        assertEquals(Number.class, type.getActualTypeArgument(0).getRawClass());
    }

    public void testBasic() throws Exception {        
        GenericType type = new GenericType(int[].class);
        assertEquals("int[]", type.toString());
        assertEquals(int[].class, type.getRawClass());
        assertEquals(0, type.getActualTypeArgument(0).size());
    }
}
