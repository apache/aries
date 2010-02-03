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
package org.apache.aries.jpa.container.context.namespace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
import org.apache.aries.blueprint.container.Parser;
import org.apache.aries.blueprint.reflect.BeanMetadataImpl;
import org.apache.aries.blueprint.reflect.RefMetadataImpl;
import org.apache.aries.blueprint.reflect.ReferenceMetadataImpl;
import org.apache.aries.jpa.container.context.PersistenceManager;
import org.apache.aries.unittest.mocks.MethodCall;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
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
  private NSHandler sut;
  private PersistenceManager manager;
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
    
    sut = new NSHandler();
    manager = Skeleton.newMock(PersistenceManager.class);
    sut.setManager(manager);
    
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
      if (clazz.isAssignableFrom(ReferenceMetadata.class))
        return clazz.cast(new ReferenceMetadataImpl());
      else if (clazz.isAssignableFrom(RefMetadata.class))
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
    Element e = getTestElement("unit");
    BeanMetadata bean = 
      (BeanMetadata) sut.decorate(e, Skeleton.newMock(BeanMetadata.class), parserCtx);
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
    Element e = getTestElement("unitNoName");
    BeanMetadata bean = 
      (BeanMetadata) sut.decorate(e, Skeleton.newMock(BeanMetadata.class), parserCtx);
    BeanProperty property = (BeanProperty) bean.getProperties().get(0);
    ReferenceMetadata reference = (ReferenceMetadata) property.getValue();
    
    assertEquals("emf2", property.getName());
    assertEquals("(&(!(org.apache.aries.jpa.proxy.factory=*))"+NSHandler.EMPTY_UNIT_NAME_FILTER+")", 
        reference.getFilter());

    assertTrue(registeredComponents.isEmpty());
  }
  
  @Test
  public void testEmptyUnitName() {
    Element e = getTestElement("emptyUnitName");
    BeanMetadata bean = 
      (BeanMetadata) sut.decorate(e, Skeleton.newMock(BeanMetadata.class), parserCtx);
    BeanProperty property = (BeanProperty) bean.getProperties().get(0);
    ReferenceMetadata reference = (ReferenceMetadata) property.getValue();
    
    assertEquals("emf3", property.getName());
    assertEquals("(&(!(org.apache.aries.jpa.proxy.factory=*))"+NSHandler.EMPTY_UNIT_NAME_FILTER+")",
        reference.getFilter());
    
    assertTrue(registeredComponents.isEmpty());
  }
  
  @Test 
  public void testBeanMetadataOverwrite() {
    Element e = getTestElement("unit");
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
    Element e = getTestElement("context");
    BeanMetadata bean = 
      (BeanMetadata) sut.decorate(e, Skeleton.newMock(BeanMetadata.class), parserCtx);
    BeanMetadata innerBean = (BeanMetadata) ((BeanProperty) bean.getProperties().get(0)).getValue();

    assertEquals("createEntityManager", innerBean.getFactoryMethod());

    assertEquals(1, registeredComponents.size());
    ReferenceMetadata reference = (ReferenceMetadata) registeredComponents.get(0);
    
    assertEquals(EntityManagerFactory.class.getName(), reference.getInterface());
    assertEquals("(&(org.apache.aries.jpa.proxy.factory=*)(osgi.unit.name=myUnit))", reference.getFilter());
    
    Map<String,Object> props = new HashMap<String, Object>();
    props.put("type", PersistenceContextType.TRANSACTION);
    Skeleton.getSkeleton(manager).assertCalled(
        new MethodCall(PersistenceManager.class, "registerContext", "myUnit", clientBundle, props));
  }
  
  @Test
  public void testContextWithProps() {
    Element e = getTestElement("contextWithProps");
    BeanMetadata bean = 
      (BeanMetadata) sut.decorate(e, Skeleton.newMock(BeanMetadata.class), parserCtx);
    BeanMetadata innerBean = (BeanMetadata) ((BeanProperty) bean.getProperties().get(0)).getValue();
    
    assertEquals("createEntityManager", innerBean.getFactoryMethod());
    
    assertEquals(1, registeredComponents.size());
    ReferenceMetadata reference = (ReferenceMetadata) registeredComponents.get(0);
    
    assertEquals(EntityManagerFactory.class.getName(), reference.getInterface());
    assertEquals("(&(org.apache.aries.jpa.proxy.factory=*)"+NSHandler.EMPTY_UNIT_NAME_FILTER+")", 
        reference.getFilter());
    
    Map<String,Object> props = new HashMap<String, Object>();
    props.put("type", PersistenceContextType.EXTENDED);
    props.put("one", "eins");
    props.put("two", "zwo");
    Skeleton.getSkeleton(manager).assertCalled(
        new MethodCall(PersistenceManager.class, "registerContext", "", clientBundle, props));    
  }
  
  private Element getTestElement(String beanName) {
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
        if (NSHandler.NS_URI.equals(e.getNamespaceURI()))
          return e;
      }
    }
    
    return null;
  }
}
