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
package org.apache.aries.spifly;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {
    static Activator activator;
    
    private ServiceRegistration<WeavingHook> weavingHookService;
    private LogServiceTracker lst;
    private List<LogService> logServices = new CopyOnWriteArrayList<LogService>();
    private BundleTracker<List<ServiceRegistration<?>>> bt;

    private final ConcurrentMap<String, Collection<Bundle>>registeredSPIs = 
            new ConcurrentHashMap<String, Collection<Bundle>>();

    public synchronized void start(BundleContext context) throws Exception {
        lst = new LogServiceTracker(context);
        lst.open();

        WeavingHook wh = new ClientWeavingHook(context);
        weavingHookService = context.registerService(WeavingHook.class, wh,
                null);

        bt = new BundleTracker<List<ServiceRegistration<?>>>(context,
                Bundle.ACTIVE, new ProviderBundleTrackerCustomizer(this, context.getBundle()));
        bt.open();
        
        activator = this;
    }

    public synchronized void stop(BundleContext context) throws Exception {
        activator = null;
        bt.close();
        weavingHookService.unregister();
        lst.close();
    }

    void log(int level, String message) {
        synchronized (logServices) {
            for (LogService log : logServices) {
                log.log(level, message);
            }
        }
    }

    void log(int level, String message, Throwable th) {
        synchronized (logServices) {
            for (LogService log : logServices) {
                log.log(level, message, th);
            }
        }
    }

    private class LogServiceTracker extends ServiceTracker<LogService, LogService> {
        public LogServiceTracker(BundleContext context) {
            super(context, LogService.class, null);
        }

        public LogService addingService(ServiceReference<LogService> reference) {
            LogService svc = super.addingService(reference);
            if (svc != null)
                logServices.add(svc);
            return svc;
        }

        @Override
        public void removedService(ServiceReference<LogService> reference, LogService service) {
            logServices.remove(service);
        }        
    }

    public void registerSPIProviderBundle(String registrationClassName, Bundle bundle) {
        registeredSPIs.putIfAbsent(registrationClassName, new CopyOnWriteArraySet<Bundle>());
        Collection<Bundle> bl = registeredSPIs.get(registrationClassName);
        bl.add(bundle);
    }

    public Collection<Bundle> findSPIProviderBundles(String name) {
        Collection<Bundle> bundles = registeredSPIs.get(name);
        return bundles == null ? Collections.<Bundle>emptyList() : bundles;
    }
}
