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

import java.net.URI;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.transaction.TransactionManager;

import org.apache.aries.mocks.BundleMock;

import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.container.NamespaceHandlerRegistry;
import org.apache.aries.blueprint.container.Parser;
import org.apache.aries.blueprint.container.NamespaceHandlerRegistry.NamespaceHandlerSet;
import org.apache.aries.blueprint.namespace.ComponentDefinitionRegistryImpl;
import org.apache.aries.blueprint.namespace.NamespaceHandlerRegistryImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.BeanProperty;

import org.apache.aries.transaction.TxComponentMetaDataHelperImpl;
import org.apache.aries.transaction.TxInterceptorImpl;
import org.apache.aries.transaction.parsing.TxElementHandler;
import org.apache.aries.unittest.mocks.Skeleton;

public class NameSpaceHandlerTest {
    
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
      
      URL bpxml = this.getClass().getResource("aries.xml");
      List<URL> bpxmlList = new LinkedList<URL>();
      bpxmlList.add(bpxml); 
      
      p.parse(bpxmlList);
      Set<URI> nsuris = p.getNamespaces();
      NamespaceHandlerSet nshandlers = nhri.getNamespaceHandlers(nsuris, b);
      p.validate(nshandlers.getSchema());
      
      ComponentDefinitionRegistry cdr = new ComponentDefinitionRegistryImpl();
      p.populate(nshandlers, cdr);
      
      BeanMetadata comp = (BeanMetadata) cdr.getComponentDefinition("top");
      
      BeanMetadata anon = (BeanMetadata) ((BeanProperty) comp.getProperties().get(0)).getValue();
      BeanMetadata anonToo = (BeanMetadata) ((BeanProperty) comp.getProperties().get(1)).getValue();

      assertEquals("Required", txenhancer.getComponentMethodTxAttribute(anon, "doSomething"));
      assertEquals("Never", txenhancer.getComponentMethodTxAttribute(anonToo, "doSomething"));
        
    }
    
    @Test
    public void testOptionalMethodAttribute() throws Exception
    {
      Parser p = new Parser();
      
      URL bpxml = this.getClass().getResource("aries2.xml");
      List<URL> bpxmlList = new LinkedList<URL>();
      bpxmlList.add(bpxml); 
      
      p.parse(bpxmlList);
      Set<URI> nsuris = p.getNamespaces();
      NamespaceHandlerSet nshandlers = nhri.getNamespaceHandlers(nsuris, b);
      p.validate(nshandlers.getSchema());
      
      ComponentDefinitionRegistry cdr = new ComponentDefinitionRegistryImpl();
      p.populate(nshandlers, cdr);
      
      BeanMetadata comp = (BeanMetadata) cdr.getComponentDefinition("top");
      
      BeanMetadata anon = (BeanMetadata) ((BeanProperty) comp.getProperties().get(0)).getValue();
      BeanMetadata anonToo = (BeanMetadata) ((BeanProperty) comp.getProperties().get(1)).getValue();

      assertEquals("Required", txenhancer.getComponentMethodTxAttribute(anon, "doSomething"));
      assertEquals("Never", txenhancer.getComponentMethodTxAttribute(anonToo, "doSomething"));
        
    }
    
    @Test
    public void testBundleWideAndBeanLevelTx() throws Exception
    {
      Parser p = new Parser();
      
      URL bpxml = this.getClass().getResource("mixed-aries.xml");
      List<URL> bpxmlList = new LinkedList<URL>();
      bpxmlList.add(bpxml); 
      
      p.parse(bpxmlList);
      Set<URI> nsuris = p.getNamespaces();
      NamespaceHandlerSet nshandlers = nhri.getNamespaceHandlers(nsuris, b);
      p.validate(nshandlers.getSchema());
      
      ComponentDefinitionRegistry cdr = new ComponentDefinitionRegistryImpl();
      p.populate(nshandlers, cdr);
      
      BeanMetadata compRequiresNew = (BeanMetadata) cdr.getComponentDefinition("requiresNew");
      BeanMetadata compNoTx = (BeanMetadata) cdr.getComponentDefinition("noTx");
      BeanMetadata compSomeTx = (BeanMetadata) cdr.getComponentDefinition("someTx");
      BeanMetadata compAnotherBean = (BeanMetadata) cdr.getComponentDefinition("anotherBean");

      assertEquals("RequiresNew", txenhancer.getComponentMethodTxAttribute(compRequiresNew, "doSomething"));
      assertEquals("Never", txenhancer.getComponentMethodTxAttribute(compNoTx, "doSomething"));
      assertEquals("Required", txenhancer.getComponentMethodTxAttribute(compSomeTx, "doSomething"));
      assertEquals("Mandatory", txenhancer.getComponentMethodTxAttribute(compSomeTx, "getRows"));
      assertEquals("Required", txenhancer.getComponentMethodTxAttribute(compAnotherBean, "doSomething"));
      assertEquals("Supports", txenhancer.getComponentMethodTxAttribute(compAnotherBean, "getWhatever"));
        
    }
}
