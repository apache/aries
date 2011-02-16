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
package org.apache.aries.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URI;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.container.Parser;
import org.apache.aries.blueprint.container.NamespaceHandlerRegistry.NamespaceHandlerSet;
import org.apache.aries.blueprint.namespace.ComponentDefinitionRegistryImpl;
import org.junit.Test;
import org.osgi.service.blueprint.reflect.BeanMetadata;

public class BundleWideNameSpaceHandlerTest extends BaseNameSpaceHandlerSetup {
    
    @Test
    public void testMultipleElements() throws Exception
    {

      Parser p = new Parser();
      
      URL bpxml = this.getClass().getResource("bundlewide-aries.xml");
      List<URL> bpxmlList = new LinkedList<URL>();
      bpxmlList.add(bpxml); 
      
      p.parse(bpxmlList);
      Set<URI> nsuris = p.getNamespaces();
      NamespaceHandlerSet nshandlers = nhri.getNamespaceHandlers(nsuris, b);
      p.validate(nshandlers.getSchema());
      
      ComponentDefinitionRegistry cdr = new ComponentDefinitionRegistryImpl();
      p.populate(nshandlers, cdr);
            
      BeanMetadata compTop = (BeanMetadata) cdr.getComponentDefinition("top1");
      BeanMetadata compDown = (BeanMetadata) cdr.getComponentDefinition("down1");
      
      assertNotNull(compTop);
      assertNotNull(compDown);
      
      assertEquals("Required", txenhancer.getComponentMethodTxAttribute(compTop, "doSomething"));
      assertEquals("Never", txenhancer.getComponentMethodTxAttribute(compDown, "doSomething"));
        
    }
    
    @Test
    public void testMultipleElements2() throws Exception
    {
      Parser p = new Parser();
      
      URL bpxml = this.getClass().getResource("bundlewide-aries2.xml");
      List<URL> bpxmlList = new LinkedList<URL>();
      bpxmlList.add(bpxml); 
      
      p.parse(bpxmlList);
      Set<URI> nsuris = p.getNamespaces();
      NamespaceHandlerSet nshandlers = nhri.getNamespaceHandlers(nsuris, b);
      p.validate(nshandlers.getSchema());
      
      ComponentDefinitionRegistry cdr = new ComponentDefinitionRegistryImpl();
      p.populate(nshandlers, cdr);
      
      BeanMetadata compTop = (BeanMetadata) cdr.getComponentDefinition("top2");
      BeanMetadata compDown = (BeanMetadata) cdr.getComponentDefinition("down2");
      BeanMetadata compMiddle = (BeanMetadata) cdr.getComponentDefinition("middle2");
      
      assertNotNull(compTop);
      assertNotNull(compDown);
      assertNotNull(compMiddle);
      
      assertEquals("RequiresNew", txenhancer.getComponentMethodTxAttribute(compTop, "update1234"));
      assertEquals("Required", txenhancer.getComponentMethodTxAttribute(compTop, "update"));
      assertEquals("NotSupported", txenhancer.getComponentMethodTxAttribute(compTop, "doSomething"));
      
      assertEquals("Required", txenhancer.getComponentMethodTxAttribute(compDown, "doSomething"));
      assertEquals("NotSupported", txenhancer.getComponentMethodTxAttribute(compDown, "update1234"));
      
      assertEquals(null, txenhancer.getComponentMethodTxAttribute(compMiddle, "doSomething"));
        
    }
    
    @Test
    public void testMultipleElements3() throws Exception
    {
      Parser p = new Parser();
      
      URL bpxml = this.getClass().getResource("bundlewide-aries3.xml");
      List<URL> bpxmlList = new LinkedList<URL>();
      bpxmlList.add(bpxml); 
      
      p.parse(bpxmlList);
      Set<URI> nsuris = p.getNamespaces();
      NamespaceHandlerSet nshandlers = nhri.getNamespaceHandlers(nsuris, b);
      p.validate(nshandlers.getSchema());
      
      ComponentDefinitionRegistry cdr = new ComponentDefinitionRegistryImpl();
      p.populate(nshandlers, cdr);
      
      BeanMetadata compTop = (BeanMetadata) cdr.getComponentDefinition("top3");
      BeanMetadata compDown = (BeanMetadata) cdr.getComponentDefinition("down3");
      BeanMetadata compMiddle = (BeanMetadata) cdr.getComponentDefinition("middle3");
      
      assertNotNull(compTop);
      assertNotNull(compDown);
      assertNotNull(compMiddle);
      
      assertEquals("RequiresNew", txenhancer.getComponentMethodTxAttribute(compTop, "update1234"));
      assertEquals("Required", txenhancer.getComponentMethodTxAttribute(compTop, "update"));
      assertEquals("NotSupported", txenhancer.getComponentMethodTxAttribute(compTop, "doSomething"));
      
      assertEquals("Required", txenhancer.getComponentMethodTxAttribute(compDown, "doSomething"));
      assertEquals("NotSupported", txenhancer.getComponentMethodTxAttribute(compDown, "update1234"));
      
      assertEquals("Required", txenhancer.getComponentMethodTxAttribute(compMiddle, "doSomething"));
        
    }
}
