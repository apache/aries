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

import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

import junit.framework.Assert;

import org.apache.aries.blueprint.CallbackTracker.Callback;
import org.apache.aries.blueprint.container.BlueprintRepository;
import org.apache.aries.blueprint.container.ServiceRecipe;
import org.apache.aries.blueprint.di.CircularDependencyException;
import org.apache.aries.blueprint.di.ExecutionContext;
import org.apache.aries.blueprint.di.MapRecipe;
import org.apache.aries.blueprint.di.Recipe;
import org.apache.aries.blueprint.di.Repository;
import org.apache.aries.blueprint.parser.ComponentDefinitionRegistryImpl;
import org.apache.aries.blueprint.pojos.*;
import org.apache.aries.blueprint.proxy.ProxyUtils;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.container.ComponentDefinitionException;

import static org.junit.Assert.assertArrayEquals;

public class WiringTest extends AbstractBlueprintTest {

    public void testWiring() throws Exception {
        ComponentDefinitionRegistryImpl registry = parse("/test-wiring.xml");
        Repository repository = new TestBlueprintContainer(registry).getRepository();
        
        Object obj1 = repository.create("pojoA");
        assertNotNull(obj1);
        assertTrue(obj1 instanceof PojoA);
        PojoA pojoa = (PojoA) obj1;
        // test singleton scope
        assertTrue(obj1 == repository.create("pojoA"));
        
        Object obj2 = repository.create("pojoB");
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
        assertTrue(pojoa.getSet().contains(pojob.getUri()));
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
        Object obj3 = repository.create("service1");
        assertNotNull(obj3);
        assertTrue(obj3 instanceof ServiceRegistration);    

        ExecutionContext.Holder.setContext((ExecutionContext) repository);
        for(Recipe r : ((ServiceRecipe)repository.getRecipe("service1")).getDependencies()) {
        	if(r instanceof MapRecipe) {
        		Map m = (Map) r.create();
        		assertEquals("value1", m.get("key1"));
        		assertEquals("value2", m.get("key2"));
        		assertTrue(m.get("key3") instanceof List);
        	}
        }
        ExecutionContext.Holder.setContext(null);
        
        // tests 'prototype' scope
        Object obj4 = repository.create("pojoC");
        assertNotNull(obj4);
        
        assertTrue(obj4 != repository.create("pojoC"));
        
        repository.destroy();
        
        // test destroy-method
        assertEquals(true, pojob.getDestroyCalled());
    }
    
    public void testSetterDisambiguation() throws Exception {
        ComponentDefinitionRegistryImpl registry = parse("/test-wiring.xml");
        Repository repository = new TestBlueprintContainer(registry).getRepository();

        AmbiguousPojo pojo = (AmbiguousPojo) repository.create("ambiguousViaInt");
        assertEquals(5, pojo.getSum());
        
        pojo = (AmbiguousPojo) repository.create("ambiguousViaList");
        assertEquals(7, pojo.getSum());
        
        
    }
    
    public void testFieldInjection() throws Exception {
      ComponentDefinitionRegistryImpl registry = parse("/test-wiring.xml");
      Repository repository = new TestBlueprintContainer(registry).getRepository();
      
      Object fiTestBean = repository.create("FITestBean");
      assertNotNull(fiTestBean);
      assertTrue(fiTestBean instanceof FITestBean);
      
      FITestBean bean = (FITestBean) fiTestBean;
      // single field injection
      assertEquals("value", bean.getAttr());
      // prefer setter injection to field injection
      assertEquals("IS_LOWER", bean.getUpperCaseAttr());
      // support cascaded injection 'bean.name' via fields
      assertEquals("aName", bean.getBeanName());
      
      // fail if field-injection is not specified
      try {
          repository.create("FIFailureTestBean");
          Assert.fail("Expected exception");
      } catch (ComponentDefinitionException cde) {}
      
      // fail if field-injection is false
      try {
          repository.create("FIFailureTest2Bean");
          Assert.fail("Expected exception");
      } catch (ComponentDefinitionException cde) {}
    }
    
    public void testCompoundProperties() throws Exception {
        ComponentDefinitionRegistryImpl registry = parse("/test-wiring.xml");
        Repository repository = new TestBlueprintContainer(registry).getRepository();
        
        Object obj5 = repository.create("compound");
        assertNotNull(obj5);
        assertTrue(obj5 instanceof PojoB);
        PojoB pojob = (PojoB) obj5;
    
        assertEquals("hello bean property", pojob.getBean().getName());

        Object obj = repository.create("goodIdRef");
        assertNotNull(obj);
        assertTrue(obj instanceof BeanD);
        BeanD bean = (BeanD) obj;

        assertEquals("pojoA", bean.getName());
    }

    public void testIdRefs() throws Exception {
        ComponentDefinitionRegistryImpl registry = parse("/test-bad-id-ref.xml");

        try {
            new TestBlueprintContainer(registry).getRepository();
            fail("Did not throw exception");
        } catch (RuntimeException e) {
            // we expect exception
            // TODO: check error string?
        }
    }
    
    public void testDependencies() throws Exception {
        CallbackTracker.clear();

        ComponentDefinitionRegistryImpl registry = parse("/test-depends-on.xml");
        Repository repository = new TestBlueprintContainer(registry).getRepository();
        Map instances = repository.createAll(Arrays.asList("c", "d", "e"), ProxyUtils.asList(Object.class));
        
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
        Repository repository = new TestBlueprintContainer(registry).getRepository();

        Object obj1 = repository.create("pojoA");
        assertNotNull(obj1);
        assertTrue(obj1 instanceof PojoA);
        PojoA pojoa = (PojoA) obj1;
        
        Object obj2 = repository.create("pojoB");
        testPojoB(obj2, URI.create("urn:myuri"), 10);
        
        assertEquals(obj2, pojoa.getPojob());
        assertEquals(new BigInteger("10"), pojoa.getNumber());
        
        Object obj3 = repository.create("pojoC");
        testPojoB(obj3, URI.create("urn:myuri-static"), 15);
        
        Object obj4 = repository.create("pojoD");
        testPojoB(obj4, URI.create("urn:myuri-static"), 15);
        
        Object obj5 = repository.create("pojoE");
        testPojoB(obj5, URI.create("urn:myuri-dynamic"), 20);
        
        Object obj6 = repository.create("multipleInt");
        testMultiple(obj6, null, 123, null);
        
        Object obj7 = repository.create("multipleInteger");
        testMultiple(obj7, null, -1, new Integer(123));
        
        Object obj8 = repository.create("multipleString");
        testMultiple(obj8, "123", -1, null);

        // TODO: check the below tests when the incoherence between TCK / spec is solved
//        try {
//            graph.create("multipleStringConvertable");
//            fail("Did not throw exception");
//        } catch (RuntimeException e) {
//            // we expect exception
//        }
        
        Object obj10 = repository.create("multipleFactory1");
        testMultiple(obj10, null, 1234, null);

        Object obj11 = repository.create("multipleFactory2");
        testMultiple(obj11, "helloCreate-boolean", -1, null);        
        
        try {
            repository.create("multipleFactoryNull");
            fail("Did not throw exception");
        } catch (RuntimeException e) {
            // we expect exception 
            // TODO: check the exception string?
        }
        
        Object obj12 = repository.create("multipleFactoryTypedNull");
        testMultiple(obj12, "hello-boolean", -1, null);

        Object obj13 = repository.create("mapConstruction");
        Map<String, String> constructionMap = new HashMap<String, String>();
        constructionMap.put("a", "b");
        testMultiple(obj13, constructionMap);
        Object obj14 = repository.create("propsConstruction");
        Properties constructionProperties = new Properties();
        constructionProperties.put("a", "b");
        testMultiple(obj14,  constructionProperties);

        Object obja = repository.create("mapConstructionWithDefaultType");
        Map<String, Date> mapa = new HashMap<String, Date>();
        // Months are 0-indexed
        Calendar calendar = new GregorianCalendar(2012, 0, 6);
        calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
        mapa.put("date", new Date(calendar.getTimeInMillis()));
        testMultiple(obja, mapa);

        Object objc = repository.create("mapConstructionWithTypedEntries");
        Map mapc = new HashMap();
        mapc.put("boolean", Boolean.TRUE);
        mapc.put("double", 1.23);
        mapc.put("date", new Date(calendar.getTimeInMillis()));
        testMultiple(objc, mapc);

        Object objb = repository.create("mapConstructionWithNonDefaultTypedEntries");
        Map mapb = new HashMap();
        mapb.put("boolean", Boolean.TRUE);
        mapb.put("double", 3.45);
        mapb.put("otherdouble", 10.2);
        testMultiple(objb, mapb);
  
        Object objd = repository.create("mapConstructionWithNonDefaultTypedKeys");
        Map mapd = new HashMap();
        mapd.put(Boolean.TRUE, "boolean");
        mapd.put(42.42, "double");
        testMultiple(objd, mapd);

        BeanF obj15 = (BeanF) repository.create("booleanWrapped");
        assertNotNull(obj15.getWrapped());
        assertEquals(false, (boolean) obj15.getWrapped());
        assertNull(obj15.getPrim());

        // TODO: check the below tests when the incoherence between TCK / spec is solved
//        BeanF obj16 = (BeanF) graph.create("booleanPrim");
//        assertNotNull(obj16.getPrim());
//        assertEquals(false, (boolean) obj16.getPrim());
//        assertNull(obj16.getWrapped());
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

    private void testMultiple(Object obj, Map map) {
       assertNotNull(obj);
       assertTrue(obj instanceof Multiple);
       assertEquals(map, ((Multiple)obj).getMap());
   }

    private void testMultiple(Object obj, Properties map) {
       assertNotNull(obj);
       assertTrue(obj instanceof Multiple);
       assertEquals(map, ((Multiple)obj).getProperties());
   }

    public void testGenerics2() throws Exception {
        ComponentDefinitionRegistryImpl registry = parse("/test-generics.xml");
        Repository repository = new TestBlueprintContainer(registry).getRepository();
        repository.create("gen2");
    }

    public void testGenerics() throws Exception {
        ComponentDefinitionRegistryImpl registry = parse("/test-generics.xml");
        Repository repository = new TestBlueprintContainer(registry).getRepository();

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
        
        obj = repository.create("method");
        assertTrue(obj instanceof PojoGenerics);
        pojo = (PojoGenerics) obj;
        
        assertEquals(expectedList, pojo.getList());
        assertEquals(expectedSet, pojo.getSet());
        assertEquals(expectedMap, pojo.getMap());
        
        obj = repository.create("constructorList");
        assertTrue(obj instanceof PojoGenerics);
        pojo = (PojoGenerics) obj;
        
        assertEquals(expectedList, pojo.getList());
        
        obj = repository.create("constructorSet");
        assertTrue(obj instanceof PojoGenerics);
        pojo = (PojoGenerics) obj;
        
        assertEquals(expectedSet, pojo.getSet());
        
        obj = repository.create("constructorMap");
        assertTrue(obj instanceof PojoGenerics);
        pojo = (PojoGenerics) obj;
        
        assertEquals(expectedMap, pojo.getMap());
        
        obj = repository.create("genericPojo");
        assertTrue(obj instanceof Primavera);
        assertEquals("string", ((Primavera) obj).prop);
        
        obj = repository.create("doubleGenericPojo");
        assertTrue(obj instanceof Primavera);
        assertEquals("stringToo", ((Primavera) obj).prop);
    }

    public void testMixedGenericsTracker() throws Exception {
        ComponentDefinitionRegistryImpl registry = parse("/test-generics-mix.xml");
        Repository repository = new TestBlueprintContainer(registry).getRepository();
        repository.create("tracker");
    }

    public void testMixedGenericsTypedTracker() throws Exception {
        ComponentDefinitionRegistryImpl registry = parse("/test-generics-mix.xml");
        Repository repository = new TestBlueprintContainer(registry).getRepository();
        try {
            repository.create("typedTracker");
            fail("Should have thrown an exception");
        } catch (ComponentDefinitionException e) {
            // expected
        }
    }

    public void testMixedGenericsTypedTrackerRaw() throws Exception {
        ComponentDefinitionRegistryImpl registry = parse("/test-generics-mix.xml");
        Repository repository = new TestBlueprintContainer(registry).getRepository();
        repository.create("typedTrackerRaw");
    }

    public void testMixedGenericsTypedClassTracker() throws Exception {
        ComponentDefinitionRegistryImpl registry = parse("/test-generics-mix.xml");
        Repository repository = new TestBlueprintContainer(registry).getRepository();
        try {
            repository.create("typedClassTracker");
            fail("Should have thrown an exception");
        } catch (ComponentDefinitionException e) {
            // expected
        }
    }

    public void testMixedGenericsTypedClassTrackerRaw() throws Exception {
        ComponentDefinitionRegistryImpl registry = parse("/test-generics-mix.xml");
        Repository repository = new TestBlueprintContainer(registry).getRepository();
        repository.create("typedClassTrackerRaw");
    }

    public void testMixedGenericsTypedGeneric() throws Exception {
        ComponentDefinitionRegistryImpl registry = parse("/test-generics-mix.xml");
        Repository repository = new TestBlueprintContainer(registry).getRepository();
        repository.create("typedGenericTracker");
    }

    public void testMixedGenericsTypedGenericClass() throws Exception {
        ComponentDefinitionRegistryImpl registry = parse("/test-generics-mix.xml");
        Repository repository = new TestBlueprintContainer(registry).getRepository();
        repository.create("typedClassGenericTracker");
    }

    public void testThreadPoolCreation() throws Exception {
        ComponentDefinitionRegistryImpl registry = parse("/test-threadpool.xml");
        Repository repository = new TestBlueprintContainer(registry).getRepository();
        repository.create("executorService");
    }

    public void testCachePojo() throws Exception {
        ComponentDefinitionRegistryImpl registry = parse("/test-cache.xml");
        Repository repository = new TestBlueprintContainer(registry).getRepository();
        Thread.currentThread().setContextClassLoader(CachePojos.CacheContainer.class.getClassLoader());
        repository.create("queueCountCache");
    }

    public void testVarArgPojo() throws Exception {
        ComponentDefinitionRegistryImpl registry = parse("/test-vararg.xml");
        Repository repository = new TestBlueprintContainer(registry).getRepository();
        VarArg va = (VarArg) repository.create("vararg");
        assertArrayEquals(new String[] { "-web" }, va.args);
    }

    public void testCircular() throws Exception {
        BlueprintRepository repository = createBlueprintContainer().getRepository();

        // this should pass (we allow circular dependencies for components without init method)
        Object obj1 = repository.create("a");
                
        // test service and listener circular dependencies
        Object obj2 = repository.create("service");
        assertNotNull(obj2);
        assertTrue(obj2 instanceof ServiceRegistration);
        
        Object obj3 = repository.create("listener");
        assertNotNull(obj3);
        assertTrue(obj3 instanceof PojoListener);
        
        assertEquals(obj2, ((PojoListener) obj3).getService() );        
    }
     
    public void testCircularPrototype() throws Exception {
        BlueprintRepository repository = createBlueprintContainer().getRepository();
        
        try {
            repository.create("circularPrototypeDriver");
            fail("Did not throw exception");  
        } catch (CircularDependencyException e) {
            // that's what we expect
        }

        try {
            repository.create("circularPrototype");
            fail("Did not throw exception");  
        } catch (CircularDependencyException e) {
            // that's what we expect
        }
    }
    
    public void testRecursive() throws Exception {
        BlueprintRepository repository = createBlueprintContainer().getRepository();
        
        try {
            repository.create("recursiveConstructor");
            fail("Did not throw exception");           
        } catch (ComponentDefinitionException e) {
            if (e.getCause() instanceof CircularDependencyException) {                          
                // that's what we expect
            } else {
                fail("Did not throw expected exception");
                throw e;
            }
        }
        
        PojoRecursive pojo;
        
        pojo = (PojoRecursive) repository.create("recursiveSetter");
        assertNotNull(pojo);
                           
        pojo = (PojoRecursive) repository.create("recursiveInitMethod");
        assertNotNull(pojo);
    }
    
    public void testCircularBreaking() throws Exception {
        BlueprintRepository repository;
        
        repository = createBlueprintContainer().getRepository();        
        assertNotNull(repository.create("c1"));
        
        repository = createBlueprintContainer().getRepository();        
        assertNotNull(repository.create("c2"));
        
        repository = createBlueprintContainer().getRepository();        
        assertNotNull(repository.create("c3"));
    }
    
    private TestBlueprintContainer createBlueprintContainer() throws Exception {
        ComponentDefinitionRegistryImpl registry = parse("/test-circular.xml");
        return new TestBlueprintContainer(registry);
    }
    
}
