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
package org.apache.aries.jndi.priv;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

    private BundleActivator[] activators;

    public Activator() {
        this.activators = new BundleActivator[]{
                new org.apache.aries.jndi.startup.Activator(),
                new org.apache.aries.jndi.url.Activator()
		//                new org.apache.aries.jndi.rmi.Activator()
        };
    }

    public void start(BundleContext bundleContext) throws Exception {
        for (BundleActivator activator : activators) {
            activator.start(bundleContext);
        }
    }

    public void stop(BundleContext bundleContext) throws Exception {
        for (BundleActivator activator : activators) {
            activator.stop(bundleContext);
        }
    }

}
