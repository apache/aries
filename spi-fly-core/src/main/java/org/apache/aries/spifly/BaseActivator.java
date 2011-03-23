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
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.aries.spifly.api.SpiFlyConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.ServiceTracker;

public abstract class BaseActivator implements BundleActivator {
    private static final Set<WeavingData> NON_WOVEN_BUNDLE = Collections.emptySet();
    
    // Static access to the activator used by the woven code, therefore 
    // this bundle must be a singleton.
    // TODO see if we can get rid of the static access.
    static BaseActivator activator;
    
    private BundleContext bundleContext;
    private LogServiceTracker logServiceTracker;
    private List<LogService> logServices = new CopyOnWriteArrayList<LogService>();
    private BundleTracker<Object> consumerBundleTracker; 
    private BundleTracker<List<ServiceRegistration<?>>> providerBundleTracker;

    private final ConcurrentMap<Bundle, Set<WeavingData>> bundleWeavingData = 
        new ConcurrentHashMap<Bundle, Set<WeavingData>>();

    private final ConcurrentMap<String, SortedMap<Long, Bundle>> registeredProviders = 
            new ConcurrentHashMap<String, SortedMap<Long, Bundle>>();

    private final ConcurrentMap<Bundle, Map<ConsumerRestriction, List<BundleDescriptor>>> consumerRestrictions = 
            new ConcurrentHashMap<Bundle, Map<ConsumerRestriction, List<BundleDescriptor>>>();
    
    public synchronized void start(BundleContext context, final String consumerHeaderName) throws Exception {
        bundleContext = context;
        
        logServiceTracker = new LogServiceTracker(context);
        logServiceTracker.open();

        providerBundleTracker = new BundleTracker<List<ServiceRegistration<?>>>(context,
                Bundle.ACTIVE, new ProviderBundleTrackerCustomizer(this, context.getBundle()));
        providerBundleTracker.open();
        
        consumerBundleTracker = new BundleTracker<Object>(context, Bundle.INSTALLED, null) {
            @Override
            public Object addingBundle(Bundle bundle, BundleEvent event) {
                processBundle(bundle, consumerHeaderName);                    
                
                return super.addingBundle(bundle, event);
            }

            @Override
            public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {
                removedBundle(bundle, event, object);
                addingBundle(bundle, event);
            }

            @Override
            public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
                bundleWeavingData.remove(bundle);
            }
        };
        consumerBundleTracker.open();
        
        for (Bundle bundle : context.getBundles()) {
            processBundle(bundle, consumerHeaderName);
        }
        
        activator = this;
    }

    private void processBundle(Bundle bundle, String consumerHeaderName) {
        if (bundleWeavingData.containsKey(bundle)) {
            // This bundle was already processed
            return;
        }
        
        String consumerHeader = bundle.getHeaders().get(consumerHeaderName);
        if (consumerHeader != null) {
            Set<WeavingData> wd = ConsumerHeaderProcessor.processHeader(consumerHeader);
            bundleWeavingData.put(bundle, Collections.unmodifiableSet(wd));
            
            for (WeavingData w : wd) {
                registerConsumerBundle(bundle, w.getArgRestrictions(), w.getAllowedBundles());
            }
        } else {
            bundleWeavingData.put(bundle, NON_WOVEN_BUNDLE);
        }
    }

    @Override
    public synchronized void stop(BundleContext context) throws Exception {
        activator = null;
        
        consumerBundleTracker.close();
        providerBundleTracker.close();
        logServiceTracker.close();
    }

    public void log(int level, String message) {
        synchronized (logServices) {
            for (LogService log : logServices) {
                log.log(level, message);
            }
        }
    }

    public void log(int level, String message, Throwable th) {
        synchronized (logServices) {
            for (LogService log : logServices) {
                log.log(level, message, th);
            }
        }
    }
    
    public Set<WeavingData> getWeavingData(Bundle b) {
        // Simply return the value as it's already an unmovable set.
        Set<WeavingData> wd = bundleWeavingData.get(b);
        if (wd == null) 
            return null;
        
        if (wd.size() == 0) 
            return null;
        
        return wd;
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
            Set<ConsumerRestriction> restrictions, List<BundleDescriptor> allowedBundles) {
        consumerRestrictions.putIfAbsent(consumerBundle, new HashMap<ConsumerRestriction, List<BundleDescriptor>>());
        Map<ConsumerRestriction, List<BundleDescriptor>> map = consumerRestrictions.get(consumerBundle);
        for (ConsumerRestriction restriction : restrictions) {
            map.put(restriction, allowedBundles);
        }
    }

    public Collection<Bundle> findConsumerRestrictions(Bundle consumer, String className, String methodName,
            Map<Pair<Integer, String>, String> args) {
        Map<ConsumerRestriction, List<BundleDescriptor>> restrictions = consumerRestrictions.get(consumer);
        if (restrictions == null) {
            // Null means: no restrictions
            return null;
        }
        
        for (Map.Entry<ConsumerRestriction, List<BundleDescriptor>> entry : restrictions.entrySet()) {
            if (entry.getKey().matches(className, methodName, args)) {
                return getBundles(entry.getValue());
            }
        }
        
        // Empty collection: nothing matches
        return Collections.emptySet();
    }

    private Collection<Bundle> getBundles(List<BundleDescriptor> descriptors) {
        if (descriptors == null) {
            return null;
        }
        
        List<Bundle> bundles = new ArrayList<Bundle>();
        for (Bundle b : bundleContext.getBundles()) {
            for (BundleDescriptor desc : descriptors) {
                if (b.getSymbolicName().equals(desc.getSymbolicName())) {
                    if (desc.getVersion() == null || b.getVersion().equals(desc.getVersion())) {
                        bundles.add(b);
                    }
                }
            }
        }
        return bundles;
    }

    // TODO unRegisterConsumerBundle();
    
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
}
