/**
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
package org.apache.aries.jpa.blueprint.aries.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContextType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.PassThroughMetadata;
import org.apache.aries.blueprint.parser.Parser;
import org.apache.aries.blueprint.reflect.BeanMetadataImpl;
import org.apache.aries.blueprint.reflect.RefMetadataImpl;
import org.apache.aries.blueprint.reflect.ReferenceMetadataImpl;
import org.apache.aries.jpa.blueprint.aries.impl.NSHandler;
import org.apache.aries.jpa.container.context.PersistenceContextProvider;
import org.apache.aries.unittest.mocks.MethodCall;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.blueprint.reflect.BeanArgument;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class NSHandlerTest {
  private Element root;
  private Element root_110;
  private NSHandler sut;
  private PersistenceContextProvider manager;
  private ParserContext parserCtx;
  private Bundle clientBundle;
  private List<ComponentMetadata> registeredComponents = new ArrayList<ComponentMetadata>();
  
  @Before
  public void setup() throws Exception {
    registeredComponents.clear();
    
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(getClass().getClassLoader().getResourceAsStream("jpa.xml"));
    root = doc.getDocumentElement();
    
    builder = factory.newDocumentBuilder();
    doc = builder.parse(getClass().getClassLoader().getResourceAsStream("jpa_110.xml"));
    root_110 = doc.getDocumentElement();
    
    sut = new NSHandler();
    manager = Skeleton.newMock(PersistenceContextProvider.class);
    sut.setManager(manager);
    sut.contextAvailable(null);
    
    clientBundle = Skeleton.newMock(Bundle.class);
        
    ComponentDefinitionRegistry registry = Skeleton.newMock(new ComponentDefinitionRegistryMock(), 
        ComponentDefinitionRegistry.class);
    
    parserCtx = Skeleton.newMock(new ParserContextMock(), ParserContext.class);
    Skeleton.getSkeleton(parserCtx).setReturnValue(
        new MethodCall(ParserContext.class,"getComponentDefinitionRegistry"), registry);
    Skeleton.getSkeleton(parserCtx).setReturnValue(
        new MethodCall(ParserContext.class, "getDefaultActivation"), "eager");
    Skeleton.getSkeleton(parserCtx).setReturnValue(
        new MethodCall(ParserContext.class, "getDefaultTimeout"), "5000");
  }
  
  private static class ParserContextMock {
    public<T> T parseElement(Class<T> type, ComponentMetadata enclosingComponent, Element element) {
      return new Parser().parseElement(type, enclosingComponent, element);
    }
    
    public<T extends Metadata> T createMetadata(Class<T> clazz) {
      if (ReferenceMetadata.class.isAssignableFrom(clazz))
        return clazz.cast(new ReferenceMetadataImpl());
      else if (RefMetadata.class.isAssignableFrom(clazz))
        return clazz.cast(new RefMetadataImpl());
      else
        return clazz.cast(new BeanMetadataImpl());
    }
  }
  
  private class ComponentDefinitionRegistryMock {
    public ComponentMetadata getComponentDefinition(String id) {
      PassThroughMetadata bundleMeta =  Skeleton.newMock(PassThroughMetadata.class);
      Skeleton.getSkeleton(bundleMeta).setReturnValue(
          new MethodCall(PassThroughMetadata.class, "getObject"), clientBundle);
      return bundleMeta;
    }
    
    public void registerComponentDefinition(ComponentMetadata component) {
      registeredComponents.add(component);
    }
  }
  
  @Test
  public void testUnit() {
    Element e = getTestElement("unit", root);
    BeanMetadata bean = 
      (BeanMetadata) sut.decorate(e, new BeanMetadataImpl(), parserCtx);
    BeanProperty property = (BeanProperty) bean.getProperties().get(0);
    ReferenceMetadata reference = (ReferenceMetadata) property.getValue();
    
    assertEquals("emf", property.getName());
    assertEquals(EntityManagerFactory.class.getName(), reference.getInterface());
    assertEquals("(&(!(org.apache.aries.jpa.proxy.factory=*))(osgi.unit.name=myUnit))", reference.getFilter());
    
    Skeleton.getSkeleton(manager).assertSkeletonNotCalled();
    
    assertTrue(registeredComponents.isEmpty());
  }
  
  @Test
  public void testUnit_110() {
    Element e = getTestElement("unit", root_110);
    BeanMetadata bean = 
      (BeanMetadata) sut.decorate(e, new BeanMetadataImpl(), parserCtx);
    BeanProperty property = (BeanProperty) bean.getProperties().get(0);
    ReferenceMetadata reference = (ReferenceMetadata) property.getValue();
    
    assertEquals("emf", property.getName());
    assertEquals(EntityManagerFactory.class.getName(), reference.getInterface());
    assertEquals("(&(!(org.apache.aries.jpa.proxy.factory=*))(osgi.unit.name=myUnit))", reference.getFilter());
    
    Skeleton.getSkeleton(manager).assertSkeletonNotCalled();
    
    assertTrue(registeredComponents.isEmpty());
  }
  
  @Test
  public void testUnitNoName() {
    Element e = getTestElement("unitNoName", root);
    BeanMetadata bean = 
      (BeanMetadata) sut.decorate(e, new BeanMetadataImpl(), parserCtx);
    BeanProperty property = (BeanProperty) bean.getProperties().get(0);
    ReferenceMetadata reference = (ReferenceMetadata) property.getValue();
    
    assertEquals("emf2", property.getName());
    assertEquals("(&(!(org.apache.aries.jpa.proxy.factory=*))"+NSHandler.EMPTY_UNIT_NAME_FILTER+")", 
        reference.getFilter());

    assertTrue(registeredComponents.isEmpty());
  }
  
  @Test
  public void testUnitNoName_110() {
    Element e = getTestElement("unitNoName", root_110);
    BeanMetadata bean = 
      (BeanMetadata) sut.decorate(e, new BeanMetadataImpl(), parserCtx);
    BeanProperty property = (BeanProperty) bean.getProperties().get(0);
    ReferenceMetadata reference = (ReferenceMetadata) property.getValue();
    
    assertEquals("emf2", property.getName());
    assertEquals("(&(!(org.apache.aries.jpa.proxy.factory=*))"+NSHandler.EMPTY_UNIT_NAME_FILTER+")", 
        reference.getFilter());

    assertTrue(registeredComponents.isEmpty());
  }
  
  @Test
  public void testEmptyUnitName() {
    Element e = getTestElement("emptyUnitName", root);
    BeanMetadata bean = 
      (BeanMetadata) sut.decorate(e, new BeanMetadataImpl(), parserCtx);
    BeanProperty property = (BeanProperty) bean.getProperties().get(0);
    ReferenceMetadata reference = (ReferenceMetadata) property.getValue();
    
    assertEquals("emf3", property.getName());
    assertEquals("(&(!(org.apache.aries.jpa.proxy.factory=*))"+NSHandler.EMPTY_UNIT_NAME_FILTER+")",
        reference.getFilter());
    
    assertTrue(registeredComponents.isEmpty());
  }
  
  @Test
  public void testEmptyUnitName_110() {
    Element e = getTestElement("emptyUnitName", root_110);
    BeanMetadata bean = 
      (BeanMetadata) sut.decorate(e, new BeanMetadataImpl(), parserCtx);
    BeanProperty property = (BeanProperty) bean.getProperties().get(0);
    ReferenceMetadata reference = (ReferenceMetadata) property.getValue();
    
    assertEquals("emf3", property.getName());
    assertEquals("(&(!(org.apache.aries.jpa.proxy.factory=*))"+NSHandler.EMPTY_UNIT_NAME_FILTER+")",
        reference.getFilter());
    
    assertTrue(registeredComponents.isEmpty());
  }
  
  @Test 
  public void testBeanMetadataOverwrite() {
    Element e = getTestElement("unit", root);
    BeanMetadataImpl oldBean = new BeanMetadataImpl();
    oldBean.setId("myid");
    oldBean.setProperties(Arrays.asList(Skeleton.newMock(BeanProperty.class)));
    
    BeanMetadata bean = (BeanMetadata) sut.decorate(e, oldBean, parserCtx);

    assertEquals("myid", bean.getId());
    assertEquals(2, bean.getProperties().size());
    
    assertTrue(registeredComponents.isEmpty());
  }
  
  @Test 
  public void testBeanMetadataOverwrite_110() {
    Element e = getTestElement("unit", root);
    BeanMetadataImpl oldBean = new BeanMetadataImpl();
    oldBean.setId("myid");
    oldBean.setProperties(Arrays.asList(Skeleton.newMock(BeanProperty.class)));
    
    BeanMetadata bean = (BeanMetadata) sut.decorate(e, oldBean, parserCtx);

    assertEquals("myid", bean.getId());
    assertEquals(2, bean.getProperties().size());
    
    assertTrue(registeredComponents.isEmpty());
  }

  @Test
  public void testDefaultContext() {
    Element e = getTestElement("context", root);
    BeanMetadata bean = 
      (BeanMetadata) sut.decorate(e, new BeanMetadataImpl(), parserCtx);
    BeanMetadata innerBean = (BeanMetadata) ((BeanProperty) bean.getProperties().get(0)).getValue();

    assertEquals("createEntityManager", innerBean.getFactoryMethod());
    assertEquals("internalClose", innerBean.getDestroyMethod());

    assertEquals(1, registeredComponents.size());
    ReferenceMetadata reference = (ReferenceMetadata) registeredComponents.get(0);
    
    assertEquals(EntityManagerFactory.class.getName(), reference.getInterface());
    assertEquals("(&(org.apache.aries.jpa.proxy.factory=true)(osgi.unit.name=myUnit))", reference.getFilter());
    
    Map<String,Object> props = new HashMap<String, Object>();
    props.put(PersistenceContextProvider.PERSISTENCE_CONTEXT_TYPE, PersistenceContextType.TRANSACTION);
    Skeleton.getSkeleton(manager).assertCalled(
        new MethodCall(PersistenceContextProvider.class, "registerContext", "myUnit", clientBundle, props));
  }
  
  @Test
  public void testDefaultContext_110() {
    Element e = getTestElement("context", root_110);
    BeanMetadata bean = 
      (BeanMetadata) sut.decorate(e, new BeanMetadataImpl(), parserCtx);
    BeanMetadata innerBean = (BeanMetadata) ((BeanProperty) bean.getProperties().get(0)).getValue();

    assertEquals("createEntityManager", innerBean.getFactoryMethod());
    assertEquals("internalClose", innerBean.getDestroyMethod());

    assertEquals(1, registeredComponents.size());
    ReferenceMetadata reference = (ReferenceMetadata) registeredComponents.get(0);
    
    assertEquals(EntityManagerFactory.class.getName(), reference.getInterface());
    assertEquals("(&(org.apache.aries.jpa.proxy.factory=true)(osgi.unit.name=myUnit))", reference.getFilter());
    
    Map<String,Object> props = new HashMap<String, Object>();
    props.put(PersistenceContextProvider.PERSISTENCE_CONTEXT_TYPE, PersistenceContextType.TRANSACTION);
    Skeleton.getSkeleton(manager).assertCalled(
        new MethodCall(PersistenceContextProvider.class, "registerContext", "myUnit", clientBundle, props));
  }
  
  @Test
  public void testContextNoPersistenceContextProvider() {
    
    sut.contextUnavailable(null);
    Element e = getTestElement("context", root);
    BeanMetadata bean = 
      (BeanMetadata) sut.decorate(e, new BeanMetadataImpl(), parserCtx);
    BeanMetadata innerBean = (BeanMetadata) ((BeanProperty) bean.getProperties().get(0)).getValue();

    assertEquals("createEntityManager", innerBean.getFactoryMethod());
    assertEquals("internalClose", innerBean.getDestroyMethod());

    assertEquals(1, registeredComponents.size());
    ReferenceMetadata reference = (ReferenceMetadata) registeredComponents.get(0);
    
    assertEquals(EntityManagerFactory.class.getName(), reference.getInterface());
    assertEquals("(&(org.apache.aries.jpa.proxy.factory=true)(osgi.unit.name=myUnit))", reference.getFilter());
    
    Map<String,Object> props = new HashMap<String, Object>();
    props.put(PersistenceContextProvider.PERSISTENCE_CONTEXT_TYPE, PersistenceContextType.TRANSACTION);
    Skeleton.getSkeleton(manager).assertNotCalled(
        new MethodCall(PersistenceContextProvider.class, "registerContext", String.class, Bundle.class, Map.class));
  }
  
  @Test
  public void testContextNoPersistenceContextProvider_110() {
    
    sut.contextUnavailable(null);
    Element e = getTestElement("context", root_110);
    BeanMetadata bean = 
      (BeanMetadata) sut.decorate(e, new BeanMetadataImpl(), parserCtx);
    BeanMetadata innerBean = (BeanMetadata) ((BeanProperty) bean.getProperties().get(0)).getValue();

    assertEquals("createEntityManager", innerBean.getFactoryMethod());
    assertEquals("internalClose", innerBean.getDestroyMethod());

    assertEquals(1, registeredComponents.size());
    ReferenceMetadata reference = (ReferenceMetadata) registeredComponents.get(0);
    
    assertEquals(EntityManagerFactory.class.getName(), reference.getInterface());
    assertEquals("(&(org.apache.aries.jpa.proxy.factory=true)(osgi.unit.name=myUnit))", reference.getFilter());
    
    Map<String,Object> props = new HashMap<String, Object>();
    props.put(PersistenceContextProvider.PERSISTENCE_CONTEXT_TYPE, PersistenceContextType.TRANSACTION);
    Skeleton.getSkeleton(manager).assertNotCalled(
        new MethodCall(PersistenceContextProvider.class, "registerContext", String.class, Bundle.class, Map.class));
  }
  
  @Test
  public void testContextWithProps() {
    Element e = getTestElement("contextWithProps", root);
    BeanMetadata bean = 
      (BeanMetadata) sut.decorate(e, new BeanMetadataImpl(), parserCtx);
    BeanMetadata innerBean = (BeanMetadata) ((BeanProperty) bean.getProperties().get(0)).getValue();
    
    assertEquals("createEntityManager", innerBean.getFactoryMethod());
    
    assertEquals(1, registeredComponents.size());
    ReferenceMetadata reference = (ReferenceMetadata) registeredComponents.get(0);
    
    assertEquals(EntityManagerFactory.class.getName(), reference.getInterface());
    assertEquals("(&(org.apache.aries.jpa.proxy.factory=true)"+NSHandler.EMPTY_UNIT_NAME_FILTER+")", 
        reference.getFilter());
    
    Map<String,Object> props = new HashMap<String, Object>();
    props.put(PersistenceContextProvider.PERSISTENCE_CONTEXT_TYPE, PersistenceContextType.EXTENDED);
    props.put("one", "eins");
    props.put("two", "zwo");
    Skeleton.getSkeleton(manager).assertCalled(
        new MethodCall(PersistenceContextProvider.class, "registerContext", "", clientBundle, props));    
  }
  
  @Test
  public void testContextWithProps_110() {
    Element e = getTestElement("contextWithProps", root_110);
    BeanMetadata bean = 
      (BeanMetadata) sut.decorate(e, new BeanMetadataImpl(), parserCtx);
    BeanMetadata innerBean = (BeanMetadata) ((BeanProperty) bean.getProperties().get(0)).getValue();
    
    assertEquals("createEntityManager", innerBean.getFactoryMethod());
    
    assertEquals(1, registeredComponents.size());
    ReferenceMetadata reference = (ReferenceMetadata) registeredComponents.get(0);
    
    assertEquals(EntityManagerFactory.class.getName(), reference.getInterface());
    assertEquals("(&(org.apache.aries.jpa.proxy.factory=true)"+NSHandler.EMPTY_UNIT_NAME_FILTER+")", 
        reference.getFilter());
    
    Map<String,Object> props = new HashMap<String, Object>();
    props.put(PersistenceContextProvider.PERSISTENCE_CONTEXT_TYPE, PersistenceContextType.EXTENDED);
    props.put("one", "eins");
    props.put("two", "zwo");
    Skeleton.getSkeleton(manager).assertCalled(
        new MethodCall(PersistenceContextProvider.class, "registerContext", "", clientBundle, props));    
  }
  
  @Test
  public void testNoMoreProxying() {
      Element e = getTestElement("contextWithProps", root);
      BeanMetadata input = new BeanMetadataImpl();
      Object output = sut.decorate(e, input, parserCtx);
      assertTrue(input == output);
  }
  
  @Test
  public void testNoMoreProxying_110() {
      Element e = getTestElement("contextWithProps", root_110);
      BeanMetadata input = new BeanMetadataImpl();
      Object output = sut.decorate(e, input, parserCtx);
      assertTrue(input == output);
  }
  
  @Test
  public void testNonIndexedArgs_110() {
      Element e = getTestElement("withUnitArg", root_110);
      BeanMetadata input = new BeanMetadataImpl();
      Object output = sut.decorate(e, input, parserCtx);
      assertEquals("Wrong number of arguments",
          1 ,input.getArguments().size());
      assertEquals("Wrong class type", "javax.persistence.EntityManagerFactory",
          ((BeanArgument)input.getArguments().get(0)).getValueType());
      assertEquals("Wrong index", -1,
          ((BeanArgument)input.getArguments().get(0)).getIndex());
      
      ReferenceMetadata reference = (ReferenceMetadata) ((BeanArgument)input.getArguments().get(0)).getValue();
      assertEquals(EntityManagerFactory.class.getName(), reference.getInterface());
      assertEquals("(&(!(org.apache.aries.jpa.proxy.factory=*))(osgi.unit.name=myUnit))", reference.getFilter());
      
      Skeleton.getSkeleton(manager).assertSkeletonNotCalled();
      assertTrue(registeredComponents.isEmpty());
      
      e = getTestElement("withContextArg", root_110);
      input = new BeanMetadataImpl();
      output = sut.decorate(e, input, parserCtx);
      
      assertEquals("Wrong number of arguments",
          1 ,input.getArguments().size());
      assertEquals("Wrong type", "javax.persistence.EntityManager",
          ((BeanArgument)input.getArguments().get(0)).getValueType());
      assertEquals("Wrong index", -1,
          ((BeanArgument)input.getArguments().get(0)).getIndex());
      
      BeanMetadata innerBean = (BeanMetadata) ((BeanArgument)input.getArguments().get(0)).getValue();

      assertEquals("createEntityManager", innerBean.getFactoryMethod());
      assertEquals("internalClose", innerBean.getDestroyMethod());

      assertEquals(1, registeredComponents.size());
      reference = (ReferenceMetadata) registeredComponents.get(0);
      
      assertEquals(EntityManagerFactory.class.getName(), reference.getInterface());
      assertEquals("(&(org.apache.aries.jpa.proxy.factory=true)(osgi.unit.name=myUnit))", reference.getFilter());
      
      Map<String,Object> props = new HashMap<String, Object>();
      props.put(PersistenceContextProvider.PERSISTENCE_CONTEXT_TYPE, PersistenceContextType.TRANSACTION);
      Skeleton.getSkeleton(manager).assertCalled(
          new MethodCall(PersistenceContextProvider.class, "registerContext", "myUnit", clientBundle, Map.class));
  }
  
  @Test
  public void testIndexedArgs_110() {
      Element e = getTestElement("withIndexedUnitArg", root_110);
      BeanMetadata input = new BeanMetadataImpl();
      Object output = sut.decorate(e, input, parserCtx);
      assertEquals("Wrong number of arguments",
          1 ,input.getArguments().size());
      assertEquals("Wrong class type", "javax.persistence.EntityManagerFactory",
          ((BeanArgument)input.getArguments().get(0)).getValueType());
      assertEquals("Wrong index", 0,
          ((BeanArgument)input.getArguments().get(0)).getIndex());
      
      ReferenceMetadata reference = (ReferenceMetadata) ((BeanArgument)input.getArguments().get(0)).getValue();
      assertEquals(EntityManagerFactory.class.getName(), reference.getInterface());
      assertEquals("(&(!(org.apache.aries.jpa.proxy.factory=*))(osgi.unit.name=myUnit))", reference.getFilter());
      
      Skeleton.getSkeleton(manager).assertSkeletonNotCalled();
      assertTrue(registeredComponents.isEmpty());
      
      e = getTestElement("withIndexedContextArg", root_110);
      input = new BeanMetadataImpl();
      output = sut.decorate(e, input, parserCtx);
      
      assertEquals("Wrong number of arguments",
          1 ,input.getArguments().size());
      assertEquals("Wrong type", "javax.persistence.EntityManager",
          ((BeanArgument)input.getArguments().get(0)).getValueType());
      assertEquals("Wrong index", 1,
          ((BeanArgument)input.getArguments().get(0)).getIndex());
      
      BeanMetadata innerBean = (BeanMetadata) ((BeanArgument)input.getArguments().get(0)).getValue();

      assertEquals("createEntityManager", innerBean.getFactoryMethod());
      assertEquals("internalClose", innerBean.getDestroyMethod());

      assertEquals(1, registeredComponents.size());
      reference = (ReferenceMetadata) registeredComponents.get(0);
      
      assertEquals(EntityManagerFactory.class.getName(), reference.getInterface());
      assertEquals("(&(org.apache.aries.jpa.proxy.factory=true)(osgi.unit.name=myUnit))", reference.getFilter());
      
      Map<String,Object> props = new HashMap<String, Object>();
      props.put(PersistenceContextProvider.PERSISTENCE_CONTEXT_TYPE, PersistenceContextType.TRANSACTION);
      Skeleton.getSkeleton(manager).assertCalled(
          new MethodCall(PersistenceContextProvider.class, "registerContext", "myUnit", clientBundle, Map.class));
  }
  
  @Test
  public void testInvalidIndex_110() {
      Element e = getTestElement("withInvalidIndexArg", root_110);
      BeanMetadata input = new BeanMetadataImpl();
      try {
          Object output = sut.decorate(e, input, parserCtx);
          fail("Should throw an exception");
      } catch (IllegalArgumentException iae) {
          assertTrue("Wrong cause type", iae.getCause() instanceof NumberFormatException);
      }
  }
  
  private Element getTestElement(String beanName, Element root) {
    NodeList ns = root.getElementsByTagName("bean");
    
    Element bean = null;
    for (int i=0; i<ns.getLength(); i++) {
      bean = (Element) ns.item(i);
      if (beanName.equals(bean.getAttribute("id")))
        break;
    }
    
    ns = bean.getChildNodes();
    for (int i=0; i<ns.getLength(); i++) {
      if (ns.item(i).getNodeType() == Node.ELEMENT_NODE) {
        Element e = (Element) ns.item(i);
        if (NSHandler.NS_URI_100.equals(e.getNamespaceURI())
            || NSHandler.NS_URI_110.equals(e.getNamespaceURI()))
          return e;
      }
    }
    
    return null;
  }
  
  @Test
  public void testgetSchemaLocation()
  {
    assertNotNull("No schema found", sut.getSchemaLocation(NSHandler.NS_URI_100));
    assertNotNull("No schema found", sut.getSchemaLocation(NSHandler.NS_URI_110));
    assertFalse("Should not be the same schema", sut.getSchemaLocation(NSHandler.NS_URI_100)
                  .equals(sut.getSchemaLocation(NSHandler.NS_URI_110)));
    assertNull("No schema expected", sut.getSchemaLocation("http://maven.apache.org/POM/4.0.0"));
  }
}
