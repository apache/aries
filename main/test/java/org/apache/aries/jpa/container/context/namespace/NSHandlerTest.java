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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContextType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.PassThroughMetadata;
import org.apache.aries.blueprint.container.Parser;
import org.apache.aries.blueprint.reflect.BeanMetadataImpl;
import org.apache.aries.jpa.container.context.PersistenceManager;
import org.apache.aries.unittest.mocks.MethodCall;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
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
  
  @Before
  public void setup() throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(getClass().getClassLoader().getResourceAsStream("jpa.xml"));
    root = doc.getDocumentElement();
    
    sut = new NSHandler();
    manager = Skeleton.newMock(PersistenceManager.class);
    sut.setManager(manager);
    
    clientBundle = Skeleton.newMock(Bundle.class);
    
    PassThroughMetadata bundleMeta =  Skeleton.newMock(PassThroughMetadata.class);
    Skeleton.getSkeleton(bundleMeta).setReturnValue(
        new MethodCall(PassThroughMetadata.class, "getObject"), clientBundle);
    
    ComponentDefinitionRegistry registry = Skeleton.newMock(ComponentDefinitionRegistry.class);
    Skeleton.getSkeleton(registry).setReturnValue(
        new MethodCall(ComponentDefinitionRegistry.class, "getComponentDefinition", "blueprintBundle"), 
        bundleMeta);
    
    parserCtx = Skeleton.newMock(new ParserContextMock(), ParserContext.class);
    Skeleton.getSkeleton(parserCtx).setReturnValue(
        new MethodCall(ParserContext.class,"getComponentDefinitionRegistry"), registry);
  }
  
  private static class ParserContextMock {
    public<T> T parseElement(Class<T> type, ComponentMetadata enclosingComponent, Element element) {
      return new Parser().parseElement(type, enclosingComponent, element);
    }
  }
  
  @Test
  public void testUnit() {
    Element e = getTestElement("unit");
    BeanMetadata bean = 
      (BeanMetadata) sut.decorate(e, Skeleton.newMock(BeanMetadata.class), null);
    BeanProperty property = (BeanProperty) bean.getProperties().get(0);
    ReferenceMetadata reference = (ReferenceMetadata) property.getValue();
    
    assertEquals("emf", property.getName());
    assertEquals(EntityManagerFactory.class.getName(), reference.getInterface());
    assertEquals("(osgi.unit.name=myUnit)", reference.getFilter());
    
    Skeleton.getSkeleton(manager).assertSkeletonNotCalled();
  }
  
  @Test
  public void testUnitNoName() {
    Element e = getTestElement("unitNoName");
    BeanMetadata bean = 
      (BeanMetadata) sut.decorate(e, Skeleton.newMock(BeanMetadata.class), null);
    BeanProperty property = (BeanProperty) bean.getProperties().get(0);
    ReferenceMetadata reference = (ReferenceMetadata) property.getValue();
    
    assertEquals("emf2", property.getName());
    assertEquals("(!(osgi.unit.name=*))", reference.getFilter());
  }
  
  @Test 
  public void testBeanMetadataOverwrite() {
    Element e = getTestElement("unit");
    BeanMetadataImpl oldBean = new BeanMetadataImpl();
    oldBean.setId("myid");
    oldBean.setProperties(Arrays.asList(Skeleton.newMock(BeanProperty.class)));
    
    BeanMetadata bean = (BeanMetadata) sut.decorate(e, oldBean, null);

    assertEquals("myid", bean.getId());
    assertEquals(2, bean.getProperties().size());
  }

  @Test
  public void testDefaultContext() {
    Element e = getTestElement("context");
    BeanMetadata bean = 
      (BeanMetadata) sut.decorate(e, Skeleton.newMock(BeanMetadata.class), parserCtx);
    ReferenceMetadata reference = (ReferenceMetadata) ((BeanProperty) bean.getProperties().get(0)).getValue();
    
    assertEquals(EntityManager.class.getName(), reference.getInterface());
    assertEquals("(osgi.unit.name=myUnit)", reference.getFilter());
    
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
    ReferenceMetadata reference = (ReferenceMetadata) ((BeanProperty) bean.getProperties().get(0)).getValue();
    
    assertEquals(EntityManager.class.getName(), reference.getInterface());
    assertEquals("(!(osgi.unit.name=*))", reference.getFilter());
    
    Map<String,Object> props = new HashMap<String, Object>();
    props.put("type", PersistenceContextType.EXTENDED);
    props.put("one", "eins");
    props.put("two", "zwo");
    Skeleton.getSkeleton(manager).assertCalled(
        new MethodCall(PersistenceManager.class, "registerContext", null, clientBundle, props));    
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
