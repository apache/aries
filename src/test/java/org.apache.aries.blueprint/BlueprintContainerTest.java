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
package org.apache.aries.blueprint;

import org.apache.aries.blueprint.container.BlueprintContainerImpl;
import org.apache.aries.blueprint.sample.Foo;
import org.junit.Test;

import java.net.URL;
import java.util.Arrays;

import static org.junit.Assert.*;

public class BlueprintContainerTest {

    @Test
    public void testSimple() throws Exception {
        URL url = getClass().getClassLoader().getResource("test.xml");
        BlueprintContainerImpl container = new BlueprintContainerImpl(getClass().getClassLoader(), Arrays.asList(url));

        Foo foo = (Foo) container.getComponentInstance("foo");
        System.out.println(foo);
        assertNotNull(foo);
        assertEquals(5, foo.getA());
        assertEquals(1, foo.getB());

        container.destroy();
    }

    public static void main(String[] args) throws Exception {
        URL url = BlueprintContainerTest.class.getClassLoader().getResource("test.xml");
        BlueprintContainerImpl container = new BlueprintContainerImpl(BlueprintContainerTest.class.getClassLoader(), Arrays.asList(url));
        System.out.println(container.getComponentInstance("foo"));
        container.destroy();
    }
}
