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
package org.apache.aries.jpa.blueprint.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.aries.blueprint.NamespaceHandler;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        JpaNsHandler handler = new JpaNsHandler();
        context.registerService(NamespaceHandler.class, handler, createProps(JpaNsHandler.NAMESPACE_JPAN_10));
        context.registerService(NamespaceHandler.class, handler, createProps(JpaNsHandler.NAMESPACE_JPA_20));
    }

    private static Dictionary<String, String> createProps(String namespace) {
        Dictionary<String, String> props = new Hashtable<String, String>(); // NOSONAR
        props.put("osgi.service.blueprint.namespace", namespace);
        return props;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        // Unregistering happens automatically
    }

}
