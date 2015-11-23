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
package org.apache.aries.blueprint.spring.support;

import java.util.Collections;

import org.apache.aries.blueprint.container.BlueprintContainerImpl;
import org.apache.aries.blueprint.parser.NamespaceHandlerSet;
import org.apache.aries.blueprint.parser.Parser;
import org.apache.aries.proxy.ProxyManager;
import org.apache.aries.proxy.impl.JdkProxyManager;

public class TestBlueprintContainer extends BlueprintContainerImpl {

    public TestBlueprintContainer() throws Exception {
        this(new JdkProxyManager());
    }

    public TestBlueprintContainer(ProxyManager proxyManager) throws Exception {
        super(null, new TestBundleContext(), null, null, null, null, null, null, proxyManager);
        resetComponentDefinitionRegistry();
    }

    @Override
    public Class loadClass(String name) throws ClassNotFoundException {
        return getClassLoader().loadClass(name);
    }

    @Override
    public ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    public void parse(String xml, NamespaceHandlerSet handlers) throws Exception {
        Parser parser = new Parser();
        parser.parse(Collections.singletonList(getClass().getResource(xml)));
        parser.populate(handlers, getComponentDefinitionRegistry());
        getRepository();
        processTypeConverters();
        processProcessors();
    }
}
