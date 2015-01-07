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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.web.converter.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.aries.web.converter.WarToWabConverter;
import org.apache.aries.web.url.WAR_URLServiceHandler;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.url.URLStreamHandlerService;

public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("url.handler.protocol", new String[]{"webbundle"});
        context.registerService(URLStreamHandlerService.class, new WAR_URLServiceHandler(), props);
        context.registerService(WarToWabConverter.class, new WarToWabConverterService(), null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        // Services will be unregistered by framework
    }

}
