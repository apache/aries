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
package org.apache.geronimo.blueprint;

import java.math.BigInteger;
import java.net.URI;

import org.apache.geronimo.blueprint.context.Instanciator;
import org.apache.geronimo.blueprint.convert.ConversionServiceImpl;
import org.apache.geronimo.blueprint.namespace.ComponentDefinitionRegistryImpl;
import org.apache.geronimo.blueprint.pojos.PojoA;
import org.apache.geronimo.blueprint.pojos.PojoB;
import org.apache.xbean.recipe.ObjectGraph;
import org.apache.xbean.recipe.Repository;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.convert.ConversionService;
import org.osgi.service.blueprint.namespace.ComponentDefinitionRegistry;

public class WiringTest extends AbstractBlueprintTest {

    public void testWiring() throws Exception {
        ComponentDefinitionRegistryImpl registry = parse("/test-wiring.xml");
        Instanciator i = new TestInstanciator(registry);
        Repository repository = i.createRepository();
        ObjectGraph graph = new ObjectGraph(repository);
        
        Object obj1 = graph.create("pojoA");
        assertNotNull(obj1);
        assertTrue(obj1 instanceof PojoA);
        PojoA pojoa = (PojoA) obj1;
        // test singleton scope
        assertTrue(obj1 == graph.create("pojoA"));
        
        Object obj2 = graph.create("pojoB");
        assertNotNull(obj2);
        assertTrue(obj2 instanceof PojoB);
        PojoB pojob = (PojoB) obj2;
        
        assertNotNull(pojoa.getPojob());
        assertNotNull(pojoa.getPojob().getUri());
        
        assertNotNull(pojoa.getList());
        assertEquals("list value", pojoa.getList().get(0));
        assertEquals(new Integer(55), pojoa.getList().get(2));
        assertEquals(URI.create("http://geronimo.apache.org"), pojoa.getList().get(3));
        Object c0 = pojoa.getList().get(1);
        Object c1 = pojoa.getList().get(4);
        assertNotNull(c0);
        assertNotNull(c1);
        assertEquals(PojoB.class, c0.getClass());
        assertEquals(PojoB.class, c1.getClass());
        assertNotSame(c0, c1);

        assertNotNull(pojoa.getArray());
        assertEquals("list value", pojoa.getArray()[0]);
        assertEquals(pojob, pojoa.getArray()[1]);
        assertEquals(new Integer(55), pojoa.getArray()[2]);
        assertEquals(URI.create("http://geronimo.apache.org"), pojoa.getArray()[3]);
        
        assertNotNull(pojoa.getSet());
        assertTrue(pojoa.getSet().contains("set value"));
        assertTrue(pojoa.getSet().contains(pojob));
        
        assertNotNull(pojoa.getMap());
        assertEquals("val", pojoa.getMap().get("key"));
        assertEquals(pojob, pojoa.getMap().get(pojob));      
        
        assertNotNull(pojoa.getNumber());
        assertEquals(new BigInteger("10"), pojoa.getNumber());
        
        assertNotNull(pojoa.getIntArray());
        assertEquals(3, pojoa.getIntArray().length);
        assertEquals(1, pojoa.getIntArray()[0]);
        assertEquals(50, pojoa.getIntArray()[1]);
        assertEquals(100, pojoa.getIntArray()[2]);
        
        assertNotNull(pojoa.getNumberArray());
        assertEquals(3, pojoa.getNumberArray().length);
        assertEquals(new Integer(1), pojoa.getNumberArray()[0]);
        assertEquals(new BigInteger("50"), pojoa.getNumberArray()[1]);
        assertEquals(new Long(100), pojoa.getNumberArray()[2]);
        
        // test init-method
        assertEquals(true, pojob.getInitCalled());
        
        // test service
        Object obj3 = graph.create("service1");
        assertNotNull(obj3);
        assertTrue(obj3 instanceof ServiceRegistration);    
        
        // tests 'prototype' scope
        Object obj4 = graph.create("pojoC");
        assertNotNull(obj4);
        assertTrue(obj4 != graph.create("pojoC"));
    }

    private static class TestInstanciator extends Instanciator {
        ConversionServiceImpl conversionService = new ConversionServiceImpl();
        ComponentDefinitionRegistryImpl registry;
        
        public TestInstanciator(ComponentDefinitionRegistryImpl registry) {
            super(null);
            this.registry = registry;
        }
        
        @Override
        public ConversionService getConversionService() {
            return conversionService;
        }
        
        @Override
        public ComponentDefinitionRegistryImpl getComponentDefinitionRegistry() {
            return registry;
        }
        
    }
}
