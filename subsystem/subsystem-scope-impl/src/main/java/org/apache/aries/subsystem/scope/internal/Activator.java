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
package org.apache.aries.subsystem.scope.internal;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.apache.aries.subsystem.scope.ScopeAdmin;
import org.apache.aries.subsystem.scope.impl.ScopeAdminServiceFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    private static BundleContext context;
    private List<ServiceRegistration> registrations = new ArrayList<ServiceRegistration>();
    ScopeAdminServiceFactory scopeAdminFactory;

    static BundleContext getContext() {
        return context;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext
     * )
     */
    public void start(BundleContext bundleContext) throws Exception {
        Activator.context = bundleContext;
        scopeAdminFactory = new ScopeAdminServiceFactory();
        scopeAdminFactory.init();
        register(ScopeAdmin.class, scopeAdminFactory, null);

    }

    protected <T> void register(Class<T> clazz, T service, Dictionary props) {
        registrations.add(context.registerService(clazz.getName(), service,
                props));
    }

    protected <T> void register(Class<T> clazz, ServiceFactory factory,
            Dictionary props) {
        registrations.add(context.registerService(clazz.getName(), factory,
                props));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext bundleContext) throws Exception {
        Activator.context = null;
        for (ServiceRegistration r : registrations) {
            try {
                r.unregister();
            } catch (Exception e) {
                // LOGGER.warn("Scope Activator shut down", e);
            }
        }

        scopeAdminFactory.destroy();
    }

    public static BundleContext getBundleContext() {
        return context;
    }

}
