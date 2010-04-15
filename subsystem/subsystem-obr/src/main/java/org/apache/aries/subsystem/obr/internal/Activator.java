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
package org.apache.aries.subsystem.obr.internal;

import org.apache.aries.subsystem.spi.ResourceResolver;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

    private BundleContext bundleContext;
    private ServiceRegistration registration;
    private ServiceTracker repositoryAdminTracker;

    public void start(BundleContext context) throws Exception {
        bundleContext = context;
        repositoryAdminTracker = new ServiceTracker(bundleContext,
                                                    RepositoryAdmin.class.getName(),
                                                    null) {
            public Object addingService(ServiceReference reference) {
                registration = bundleContext.registerService(ResourceResolver.class.getName(),
                                                             new ObrResourceResolver(repositoryAdminTracker),
                                                             null);
                return super.addingService(reference);
            }
        };
        repositoryAdminTracker.open();
    }

    public void stop(BundleContext context) throws Exception {
        registration.unregister();
        repositoryAdminTracker.close();
    }
}
