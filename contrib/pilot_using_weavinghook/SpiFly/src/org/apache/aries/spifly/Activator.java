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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

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

    private BundleContext bundleContext;
    private ServiceRegistration<WeavingHook> weavingHookService;
    private LogServiceTracker lst;
    private List<LogService> logServices = new CopyOnWriteArrayList<LogService>();
    private BundleTracker<List<ServiceRegistration<?>>> bt;

    private final ConcurrentMap<String, SortedMap<Long, Bundle>>registeredProviders = 
            new ConcurrentHashMap<String, SortedMap<Long, Bundle>>();

    private final ConcurrentMap<Bundle, Collection<String>> consumerProviders = 
            new ConcurrentHashMap<Bundle, Collection<String>>();

    private final ConcurrentMap<Bundle, Map<Map<String, Map<String, Map<Integer, Map<String, String>>>>, List<BundleDescriptor>>> consumerRestrictions =
            new ConcurrentHashMap<Bundle, Map<Map<String, Map<String, Map<Integer, Map<String, String>>>>, List<BundleDescriptor>>>();
    
    public synchronized void start(BundleContext context) throws Exception {
        bundleContext = context;
        
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

    public void registerProviderBundle(String registrationClassName, Bundle bundle) {        
        registeredProviders.putIfAbsent(registrationClassName, Collections.synchronizedSortedMap(new TreeMap<Long, Bundle>()));
        SortedMap<Long, Bundle> map = registeredProviders.get(registrationClassName);
        map.put(bundle.getBundleId(), bundle);
    }

    public Collection<Bundle> findProviderBundles(String name) {
        SortedMap<Long, Bundle> map = registeredProviders.get(name);
        return map == null ? Collections.<Bundle>emptyList() : map.values();
    }
    
    // TODO unRegisterProviderBundle();
    public void registerConsumerBundle( Bundle consumerBundle,
            Map<String, Map<String, Map<Integer, Map<String, String>>>> restrictions, List<BundleDescriptor> allowedBundles) {
        consumerRestrictions.putIfAbsent(consumerBundle, 
                new HashMap<Map<String, Map<String, Map<Integer, Map<String, String>>>>, List<BundleDescriptor>>());
        consumerRestrictions.get(consumerBundle).put(restrictions, allowedBundles);
    }

    /* public void registerConsumerBundle(Bundle consumer, Collection<String> spiProviders, String className, String methodName, Map<Integer, String> argRestrictions) {
        if (spiProviders != null)
            consumerProviders.put(consumer, spiProviders);
        
        if (className != null) {            
            Map<String, Map<String, Map<Integer, String>>> restrictions = new HashMap<String, Map<String, Map<Integer, String>>>();
            Map<String, Map<Integer, String>> restriction = null;
            if (methodName != null) {
                restriction = new HashMap<String, Map<Integer,String>>();
                restriction.put(methodName, argRestrictions);
            }
            restrictions.put(className, restriction);
            // TODO consumerRestrictions.put(consumer, restrictions);
        }
        if (argRestrictions != null)
            consumerArgRestrictions.put(consumer, argRestrictions);
    } */
    
    public Collection<Bundle> findConsumerRestrictions(Bundle consumer, int argIdx, String argVal) {
        List<Bundle> result = new ArrayList<Bundle>();
        for (String allowedBundle : findComsumerRestrictions2(consumer, argIdx)) {
            for (Bundle b : bundleContext.getBundles()) {
                if (b.getSymbolicName().equals(allowedBundle)) {
                    result.add(b);
                }
            }
        }
        return result;
    }

    private Collection<String> findComsumerRestrictions2(Bundle consumer, int argIdx) {
        Collection<String> providers = consumerProviders.get(consumer);
        Map<Integer, String> argRestrictions = consumerRestrictions.get(consumer);

        if (providers != null) { 
            if (argRestrictions != null) {
                // Clone providers because we're going to modify it...
                providers = new ArrayList<String>(providers);
                providers.retainAll(Collections.singleton(argRestrictions.get(argIdx)));
            }
            return providers;
        } else {
            if (argRestrictions != null) {
                return Collections.singleton(argRestrictions.get(argIdx));
            }
            return null;
        }
    }

    // TODO unRegisterConsumerBundle();
}
