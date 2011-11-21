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

import java.lang.reflect.InvocationHandler;
import java.util.Collection;
import java.util.concurrent.Callable;

import org.apache.aries.blueprint.di.Repository;
import org.apache.aries.blueprint.parser.ComponentDefinitionRegistryImpl;
import org.apache.aries.proxy.InvocationListener;
import org.apache.aries.proxy.ProxyManager;
import org.apache.aries.proxy.UnableToProxyException;
import org.apache.aries.proxy.impl.AbstractProxyManager;
import org.osgi.framework.Bundle;
import org.osgi.service.blueprint.container.ComponentDefinitionException;

public class ReferencesTest extends AbstractBlueprintTest {



    public void testWiring() throws Exception {
        ComponentDefinitionRegistryImpl registry = parse("/test-references.xml");
        ProxyManager proxyManager = new AbstractProxyManager() {
            @Override
            protected Object createNewProxy(Bundle bundle, Collection<Class<?>> classes, Callable<Object> objectCallable, InvocationListener invocationListener) throws UnableToProxyException {
                return new Object();
            }

            @Override
            protected InvocationHandler getInvocationHandler(Object o) {
                return null;
            }

            @Override
            protected boolean isProxyClass(Class<?> aClass) {
                return false;
            }
        };
        Repository repository = new TestBlueprintContainer(registry, proxyManager).getRepository();
        
        repository.create("refItf");

        try {
            repository.create("refClsErr");
            fail("Should have failed");
        } catch (ComponentDefinitionException e) {

        }

        repository.create("refClsOk");
    }

    static class ProxyGenerationException extends RuntimeException {
    }
    
}
