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
package org.apache.aries.spifly.aop.sample;

import java.util.Properties;

import org.apache.aries.spifly.aop.sample.interf.TestInterface;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

    public void start(final BundleContext context) throws Exception {
        System.out
                .println("org.apache.aries.spifly.aop.sample.Activator started");
        Properties props = new Properties();
        props.put("myversion", context.getBundle().getVersion().toString());
        context.registerService(TestInterface.class.getName(), new TestImpl(),
                props);
    }

    public void stop(final BundleContext context) throws Exception {
        System.out
                .println("org.apache.aries.spifly.aop.sample.Activator stopped");
    }
}
