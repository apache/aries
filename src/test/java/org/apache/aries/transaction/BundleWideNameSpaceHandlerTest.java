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
import java.util.Properties;
import java.util.Set;

import javax.transaction.TransactionManager;

import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.container.NamespaceHandlerRegistry;
import org.apache.aries.blueprint.container.Parser;
import org.apache.aries.blueprint.container.NamespaceHandlerRegistry.NamespaceHandlerSet;
import org.apache.aries.blueprint.namespace.ComponentDefinitionRegistryImpl;
import org.apache.aries.blueprint.namespace.NamespaceHandlerRegistryImpl;
import org.apache.aries.mocks.BundleMock;
import org.apache.aries.transaction.parsing.TxElementHandler;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.reflect.BeanMetadata;

public class BundleWideNameSpaceHandlerTest {
    
	Bundle b;
	private NamespaceHandlerRegistry nhri;
	private TxComponentMetaDataHelperImpl txenhancer;
	
    @Before
    public void setUp() {
        b = Skeleton.newMock(new BundleMock("org.apache.aries.tx", new Properties()), Bundle.class);
        BundleContext ctx = b.getBundleContext();
        nhri = new NamespaceHandlerRegistryImpl(ctx);
        
        TransactionManager tm = Skeleton.newMock(TransactionManager.class);
        
        txenhancer = new TxComponentMetaDataHelperImpl();
        
        TxInterceptorImpl txinterceptor = new TxInterceptorImpl();
        txinterceptor.setTransactionManager(tm);
        txinterceptor.setTxMetaDataHelper(txenhancer);
        
        TxElementHandler namespaceHandler = new TxElementHandler();
        namespaceHandler.setTransactionInterceptor(txinterceptor);
        namespaceHandler.setTxMetaDataHelper(txenhancer);
            
        Properties props = new Properties();
        props.put("osgi.service.blueprint.namespace", new String[]{"http://aries.apache.org/xmlns/transactions/v1.0.0", "http://aries.apache.org/xmlns/transactions/v1.1.0"});
        ctx.registerService(NamespaceHandler.class.getName(), namespaceHandler, props);
    }
    
    @After
    public void tearDown() throws Exception{
    	b = null;
        nhri = null;
        txenhancer = null;
    }
    
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
