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
import java.util.Set;

import javax.transaction.TransactionManager;

import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.parser.ComponentDefinitionRegistryImpl;
import org.apache.aries.blueprint.parser.NamespaceHandlerSet;
import org.apache.aries.blueprint.parser.Parser;
import org.apache.aries.blueprint.reflect.PassThroughMetadataImpl;
import org.apache.aries.transaction.parsing.TxNamespaceHandler;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.osgi.framework.Bundle;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.coordinator.Coordinator;

public class BaseNameSpaceHandlerSetup {
    protected Bundle b;
    protected DummyNamespaceHandlerRegistry nhri;
    protected TxNamespaceHandler namespaceHandler;
    protected IMocksControl control;

    @Before
    public void setUp() throws InvalidSyntaxException {
        control = EasyMock.createControl();
        b = control.createMock(Bundle.class);
        TransactionManager tm = control.createMock(TransactionManager.class);
        Coordinator coordinator = control.createMock(Coordinator.class);
        control.replay();

        namespaceHandler = new TxNamespaceHandler();
        namespaceHandler.setTm(tm);
        namespaceHandler.setCoordinator(coordinator);

        String[] namespaces = new String[] { "http://aries.apache.org/xmlns/transactions/v2.0.0" };
        nhri = new DummyNamespaceHandlerRegistry();
        nhri.addNamespaceHandlers(namespaces, namespaceHandler);
    }

    @After
    public void tearDown() throws Exception{
        control.verify();
        b = null;
        nhri = null;
    }

    protected ComponentDefinitionRegistry parseCDR(String name) throws Exception {
        Parser p = new Parser();

        URL bpxml = this.getClass().getResource(name);
        p.parse(Arrays.asList(bpxml));

        Set<URI> nsuris = p.getNamespaces();
        NamespaceHandlerSet nshandlers = nhri.getNamespaceHandlers(nsuris, b);

        ComponentDefinitionRegistry cdr = new ComponentDefinitionRegistryImpl();
        cdr.registerComponentDefinition(new PassThroughMetadataImpl("blueprintBundle", b));
        p.populate(nshandlers, cdr);

        return cdr;
    }
}
