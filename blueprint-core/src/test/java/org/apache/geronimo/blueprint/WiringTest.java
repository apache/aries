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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.geronimo.blueprint.CallbackTracker.Callback;
import org.apache.geronimo.blueprint.context.BlueprintObjectInstantiator;
import org.apache.geronimo.blueprint.context.BlueprintObjectRepository;
import org.apache.geronimo.blueprint.context.RecipeBuilder;
import org.apache.geronimo.blueprint.namespace.ComponentDefinitionRegistryImpl;
import org.apache.geronimo.blueprint.pojos.BeanD;
import org.apache.geronimo.blueprint.pojos.Multiple;
import org.apache.geronimo.blueprint.pojos.PojoA;
import org.apache.geronimo.blueprint.pojos.PojoB;
import org.apache.geronimo.blueprint.pojos.PojoGenerics;
import org.apache.xbean.recipe.Repository;
import org.osgi.framework.ServiceRegistration;

public class WiringTest extends AbstractBlueprintTest {

    public void testWiring() throws Exception {
        ComponentDefinitionRegistryImpl registry = parse("/test-wiring.xml");
        RecipeBuilder i = new RecipeBuilder(new TestBlueprintContext(registry));
        BlueprintObjectRepository repository = i.createRepository();
        BlueprintObjectInstantiator graph = new BlueprintObjectInstantiator(repository);
        
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
        assertTrue(pojoa.getSet().contains(URI.create("http://geronimo.apache.org")));
        
        assertNotNull(pojoa.getMap());
        assertEquals("val", pojoa.getMap().get("key"));
        assertEquals(pojob, pojoa.getMap().get(pojob));      
        assertEquals(URI.create("http://geronimo.apache.org"), pojoa.getMap().get(new Integer(5)));
        
        assertNotNull(pojoa.getProps());
        assertEquals("value1", pojoa.getProps().get("key1"));
        assertEquals("value2", pojoa.getProps().get("2"));
        assertEquals("bar", pojoa.getProps().get("foo"));
        
        assertNotNull(pojoa.getNumber());
        assertEquals(new BigInteger("10"), pojoa.getNumber());
        
        assertNotNull(pojoa.getIntArray());
        assertEquals(3, pojoa.getIntArray().length);
        assertEquals(1, pojoa.getIntArray()[0]);
        assertEquals(50, pojoa.getIntArray()[1]);
        assertEquals(100, pojoa.getIntArray()[2]);
        
        assertNotNull(pojoa.getNumberArray());
        assertEquals(4, pojoa.getNumberArray().length);
        assertEquals(new Integer(1), pojoa.getNumberArray()[0]);
        assertEquals(new BigInteger("50"), pojoa.getNumberArray()[1]);
        assertEquals(new Long(100), pojoa.getNumberArray()[2]);
        assertEquals(new Integer(200), pojoa.getNumberArray()[3]);
        
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
        
        repository.destroy();       
        
        // test destroy-method
        assertEquals(true, pojob.getDestroyCalled());
    }
    
    public void testCompoundProperties() throws Exception {
        ComponentDefinitionRegistryImpl registry = parse("/test-wiring.xml");
        RecipeBuilder i = new RecipeBuilder(new TestBlueprintContext(registry));
        BlueprintObjectRepository repository = i.createRepository();
        BlueprintObjectInstantiator graph = new BlueprintObjectInstantiator(repository);
        
        Object obj5 = graph.create("compound");
        assertNotNull(obj5);
        assertTrue(obj5 instanceof PojoB);
        PojoB pojob = (PojoB) obj5;
    
        assertEquals("hello bean property", pojob.getBean().getName());
    }

    public void testIdRefs() throws Exception {
        ComponentDefinitionRegistryImpl registry = parse("/test-wiring.xml");
        RecipeBuilder i = new RecipeBuilder(new TestBlueprintContext(registry));
        BlueprintObjectRepository repository = i.createRepository();
        BlueprintObjectInstantiator graph = new BlueprintObjectInstantiator(repository);
        
        try {
            graph.create("badIdRef");
            fail("Did not throw exception");
        } catch (RuntimeException e) {
            // we expect exception
            // TODO: check error string?
        }
        
        Object obj = graph.create("goodIdRef");
        assertNotNull(obj);
        assertTrue(obj instanceof BeanD);
        BeanD bean = (BeanD) obj;
    
        assertEquals("pojoA", bean.getName());
    }
    
    public void testDependencies() throws Exception {
        CallbackTracker.clear();

        ComponentDefinitionRegistryImpl registry = parse("/test-depends-on.xml");
        RecipeBuilder i = new RecipeBuilder(new TestBlueprintContext(registry));
        BlueprintObjectRepository repository = i.createRepository();
        BlueprintObjectInstantiator graph = new BlueprintObjectInstantiator(repository);
        Map instances = graph.createAll("c", "d", "e");
        
        List<Callback> callback = CallbackTracker.getCallbacks();
        assertEquals(3, callback.size());
        checkInitCallback(instances.get("d"), callback.get(0));
        checkInitCallback(instances.get("c"), callback.get(1));
        checkInitCallback(instances.get("e"), callback.get(2));
                
        repository.destroy();
        
        assertEquals(6, callback.size());
        checkDestroyCallback(instances.get("e"), callback.get(3));
        checkDestroyCallback(instances.get("c"), callback.get(4));
        checkDestroyCallback(instances.get("d"), callback.get(5));
    }

    private void checkInitCallback(Object obj, Callback callback) { 
        assertEquals(Callback.INIT, callback.getType());
        assertEquals(obj, callback.getObject());
    }
    
    private void checkDestroyCallback(Object obj, Callback callback) { 
        assertEquals(Callback.DESTROY, callback.getType());
        assertEquals(obj, callback.getObject());
    }
    
    public void testConstructor() throws Exception {
        ComponentDefinitionRegistryImpl registry = parse("/test-constructor.xml");
        RecipeBuilder i = new RecipeBuilder(new TestBlueprintContext(registry));
        Repository repository = i.createRepository();
        BlueprintObjectInstantiator graph = new BlueprintObjectInstantiator(repository);
        
        Object obj1 = graph.create("pojoA");
        assertNotNull(obj1);
        assertTrue(obj1 instanceof PojoA);
        PojoA pojoa = (PojoA) obj1;
        
        Object obj2 = graph.create("pojoB");
        testPojoB(obj2, URI.create("urn:myuri"), 10);
        
        assertEquals(obj2, pojoa.getPojob());
        assertEquals(new BigInteger("10"), pojoa.getNumber());
        
        Object obj3 = graph.create("pojoC");
        testPojoB(obj3, URI.create("urn:myuri-static"), 15);
        
        Object obj4 = graph.create("pojoD");
        testPojoB(obj4, URI.create("urn:myuri-static"), 15);
        
        Object obj5 = graph.create("pojoE");
        testPojoB(obj5, URI.create("urn:myuri-dynamic"), 20);
        
        Object obj6 = graph.create("multipleInt");
        testMultiple(obj6, null, 123, null);
        
        Object obj7 = graph.create("multipleInteger");
        testMultiple(obj7, null, -1, new Integer(123));
        
        Object obj8 = graph.create("multipleString");
        testMultiple(obj8, "123", -1, null);
        
        Object obj9 = graph.create("multipleStringConvertable");
        testMultiple(obj9, "hello", -1, null);
        
        Object obj10 = graph.create("multipleFactory1");
        testMultiple(obj10, null, 1234, null);

        Object obj11 = graph.create("multipleFactory2");
        testMultiple(obj11, "helloCreate-boolean", -1, null);        
        
        try {
            graph.create("multipleFactoryNull");
            fail("Did not throw exception");
        } catch (RuntimeException e) {
            // we expect exception 
            // TODO: check the exception string?
        }
        
        Object obj12 = graph.create("multipleFactoryTypedNull");
        testMultiple(obj12, "hello-boolean", -1, null);

        Object obj13 = graph.create("mapConstruction");
        Object obj14 = graph.create("propsConstruction");
    }
    
    private void testPojoB(Object obj, URI uri, int intValue) {
        assertNotNull(obj);
        assertTrue(obj instanceof PojoB);
        PojoB pojob = (PojoB) obj;
        assertEquals(uri, pojob.getUri());
        assertEquals(intValue, pojob.getNumber());
    }
    
    private void testMultiple(Object obj, String stringValue, int intValue, Integer integerValue) {
        assertNotNull(obj);
        assertTrue(obj instanceof Multiple);
        assertEquals(intValue, ((Multiple)obj).getInt());
        assertEquals(stringValue, ((Multiple)obj).getString());
        assertEquals(integerValue, ((Multiple)obj).getInteger());        
    }

    public void testGenerics() throws Exception {
        ComponentDefinitionRegistryImpl registry = parse("/test-generics.xml");
        RecipeBuilder i = new RecipeBuilder(new TestBlueprintContext(registry));
        Repository repository = i.createRepository();
        BlueprintObjectInstantiator graph = new BlueprintObjectInstantiator(repository);
        
        List<Integer> expectedList = new ArrayList<Integer>();
        expectedList.add(new Integer(10));
        expectedList.add(new Integer(20));
        expectedList.add(new Integer(50));
        
        Set<Long> expectedSet = new HashSet<Long>();
        expectedSet.add(new Long(1000));
        expectedSet.add(new Long(2000));
        expectedSet.add(new Long(5000));
        
        Map<Short, Boolean> expectedMap = new HashMap<Short, Boolean>();
        expectedMap.put(new Short((short)1), Boolean.TRUE);
        expectedMap.put(new Short((short)2), Boolean.FALSE);
        expectedMap.put(new Short((short)5), Boolean.TRUE);
        
        Object obj;
        PojoGenerics pojo;
        
        obj = graph.create("method");
        assertTrue(obj instanceof PojoGenerics);
        pojo = (PojoGenerics) obj;
        
        assertEquals(expectedList, pojo.getList());
        assertEquals(expectedSet, pojo.getSet());
        assertEquals(expectedMap, pojo.getMap());
        
        obj = graph.create("constructorList");
        assertTrue(obj instanceof PojoGenerics);
        pojo = (PojoGenerics) obj;
        
        assertEquals(expectedList, pojo.getList());
        
        obj = graph.create("constructorSet");
        assertTrue(obj instanceof PojoGenerics);
        pojo = (PojoGenerics) obj;
        
        assertEquals(expectedSet, pojo.getSet());
        
        obj = graph.create("constructorMap");
        assertTrue(obj instanceof PojoGenerics);
        pojo = (PojoGenerics) obj;
        
        assertEquals(expectedMap, pojo.getMap());
    }
    
    public void testCircular() throws Exception {
        ComponentDefinitionRegistryImpl registry = parse("/test-circular.xml");
        RecipeBuilder i = new RecipeBuilder(new TestBlueprintContext(registry));
        Repository repository = i.createRepository();
        BlueprintObjectInstantiator graph = new BlueprintObjectInstantiator(repository);

        try {
            Object obj1 = graph.create("a");
            fail("Test should have thrown an exception caused by the circular reference");
        } catch (Exception e) {
            // ok
        }
    }
     
}
