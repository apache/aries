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

import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Properties;
import java.util.Set;

import javax.transaction.TransactionManager;

import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.container.NamespaceHandlerRegistry;
import org.apache.aries.blueprint.namespace.NamespaceHandlerRegistryImpl;
import org.apache.aries.blueprint.parser.ComponentDefinitionRegistryImpl;
import org.apache.aries.blueprint.parser.NamespaceHandlerSet;
import org.apache.aries.blueprint.parser.Parser;
import org.apache.aries.blueprint.reflect.PassThroughMetadataImpl;
import org.apache.aries.mocks.BundleContextMock;
import org.apache.aries.mocks.BundleMock;
import org.apache.aries.transaction.parsing.TxNamespaceHandler;
import org.apache.aries.unittest.mocks.MethodCall;
import org.apache.aries.unittest.mocks.Skeleton;
import org.junit.After;
import org.junit.Before;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.container.BlueprintContainer;

public class BaseNameSpaceHandlerSetup {
    protected Bundle b;
    protected NamespaceHandlerRegistry nhri;
    protected TxComponentMetaDataHelperImpl txenhancer;
    protected TxNamespaceHandler namespaceHandler;
    
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
        
        namespaceHandler = new TxNamespaceHandler();
        
        BlueprintContainer container = Skeleton.newMock(BlueprintContainer.class);
        Skeleton.getSkeleton(container).setReturnValue(
                new MethodCall(BlueprintContainer.class, "getComponentInstance", TxNamespaceHandler.DEFAULT_INTERCEPTOR_ID),
                txinterceptor);
        namespaceHandler.setBlueprintContainer(container);
        namespaceHandler.setTxMetaDataHelper(txenhancer);
            
        Properties props = new Properties();
        props.put("osgi.service.blueprint.namespace", new String[]{"http://aries.apache.org/xmlns/transactions/v1.0.0", "http://aries.apache.org/xmlns/transactions/v1.1.0", "http://aries.apache.org/xmlns/transactions/v1.2.0"});
        ctx.registerService(NamespaceHandler.class.getName(), namespaceHandler, (Dictionary) props);
    }
      
    @After
    public void tearDown() throws Exception{
      b = null;
      nhri = null;
      txenhancer = null;
      
      BundleContextMock.clear();
    }
    
    protected ComponentDefinitionRegistry parseCDR(String name) throws Exception {
        Parser p = new Parser();
        
        URL bpxml = this.getClass().getResource(name);
        p.parse(Arrays.asList(bpxml));
        
        Set<URI> nsuris = p.getNamespaces();
        NamespaceHandlerSet nshandlers = nhri.getNamespaceHandlers(nsuris, b);
        p.validate(nshandlers.getSchema());
        
        ComponentDefinitionRegistry cdr = new ComponentDefinitionRegistryImpl();
        cdr.registerComponentDefinition(new PassThroughMetadataImpl("blueprintBundle", b));
        p.populate(nshandlers, cdr);
        
        return cdr;
    }
}
