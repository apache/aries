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

import org.apache.aries.blueprint.di.Repository;
import org.apache.aries.blueprint.namespace.ComponentDefinitionRegistryImpl;
import org.apache.aries.blueprint.pojos.SimpleBean;

public class BeanLoadingTest extends AbstractBlueprintTest {

    public void testLoadSimpleBean() throws Exception {
        ComponentDefinitionRegistryImpl registry = parse("/test-bean-classes.xml");
        Repository repository = new TestBlueprintContainer(registry)
                .getRepository();

        Object obj = repository.create("simpleBean");
        assertNotNull(obj);
        assertTrue(obj instanceof SimpleBean);
    }

    public void testLoadSimpleBeanNested() throws Exception {
        ComponentDefinitionRegistryImpl registry = parse("/test-bean-classes.xml");
        Repository repository = new TestBlueprintContainer(registry)
                .getRepository();

        Object obj = repository.create("simpleBeanNested");
        assertNotNull(obj);
        assertTrue(obj instanceof SimpleBean.Nested);
    }
}
