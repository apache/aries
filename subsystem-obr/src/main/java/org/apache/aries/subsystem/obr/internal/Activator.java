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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.repository.Repository;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {
	private final Map<ServiceReference<RepositoryAdmin>, ServiceRegistration<Repository>> registrations = Collections.synchronizedMap(new HashMap<ServiceReference<RepositoryAdmin>, ServiceRegistration<Repository>>());
    
    private ServiceTracker<RepositoryAdmin, RepositoryAdmin> tracker;

    @Override
    public void start(final BundleContext context) {
        tracker = new ServiceTracker<RepositoryAdmin, RepositoryAdmin>(context, RepositoryAdmin.class.getName(), null) {
            @Override
        	public RepositoryAdmin addingService(ServiceReference<RepositoryAdmin> reference) {
                registrations.put(reference, context.registerService(
                		Repository.class,
                		new RepositoryAdminRepository(context.getService(reference)),
                		null));
                return super.addingService(reference);
            }
            
            @Override
            public void removedService(ServiceReference<RepositoryAdmin> reference, RepositoryAdmin service) {
            	ServiceRegistration<Repository> registration = registrations.get(reference);
            	if (registration == null)
            		return;
            	registration.unregister();
            	super.removedService(reference, service);
            }
        };
        tracker.open();
    }

    @Override
    public void stop(BundleContext context) {
        tracker.close();
    }
}
