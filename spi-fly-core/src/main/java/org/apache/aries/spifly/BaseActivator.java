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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.BundleTracker;

public abstract class BaseActivator implements BundleActivator {
    private static final Set<WeavingData> NON_WOVEN_BUNDLE = Collections.emptySet();
    private static final Logger logger = Logger.getLogger(BaseActivator.class.getName());

    // Static access to the activator used by the woven code, therefore
    // this bundle must be a singleton.
    // TODO see if we can get rid of the static access.
    public static BaseActivator activator;

    private BundleContext bundleContext;
    private BundleTracker consumerBundleTracker;
    private BundleTracker providerBundleTracker;

    private final ConcurrentMap<Bundle, Set<WeavingData>> bundleWeavingData =
        new ConcurrentHashMap<Bundle, Set<WeavingData>>();

    private final ConcurrentMap<String, SortedMap<Long, Pair<Bundle, Map<String, Object>>>> registeredProviders =
            new ConcurrentHashMap<String, SortedMap<Long, Pair<Bundle, Map<String, Object>>>>();

    private final ConcurrentMap<Bundle, Map<ConsumerRestriction, List<BundleDescriptor>>> consumerRestrictions =
            new ConcurrentHashMap<Bundle, Map<ConsumerRestriction, List<BundleDescriptor>>>();

    public synchronized void start(BundleContext context, final String consumerHeaderName) throws Exception {
        bundleContext = context;

        providerBundleTracker = new BundleTracker(context,
                Bundle.ACTIVE, new ProviderBundleTrackerCustomizer(this, context.getBundle()));
        providerBundleTracker.open();

        consumerBundleTracker = new BundleTracker(context,
                Bundle.INSTALLED | Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE, new ConsumerBundleTrackerCustomizer(this, consumerHeaderName));
        consumerBundleTracker.open();

        for (Bundle bundle : context.getBundles()) {
            addConsumerWeavingData(bundle, consumerHeaderName);
        }

        activator = this;
    }

    public void addConsumerWeavingData(Bundle bundle, String consumerHeaderName) throws Exception {
        if (bundleWeavingData.containsKey(bundle)) {
            // This bundle was already processed
            return;
        }

        Map<String, List<String>> allHeaders = new HashMap<String, List<String>>();
        allHeaders.put(consumerHeaderName, getAllHeaders(consumerHeaderName, bundle));
        allHeaders.put(SpiFlyConstants.REQUIRE_CAPABILITY, getAllHeaders(SpiFlyConstants.REQUIRE_CAPABILITY, bundle));

        Set<WeavingData> wd = new HashSet<WeavingData>();
        for (Map.Entry<String, List<String>> entry : allHeaders.entrySet()) {
            String headerName = entry.getKey();
            for (String headerVal : entry.getValue()) {
                wd.addAll(ConsumerHeaderProcessor.processHeader(headerName, headerVal));
            }
        }

        if (!wd.isEmpty()) {
            bundleWeavingData.put(bundle, Collections.unmodifiableSet(wd));

            for (WeavingData w : wd) {
                registerConsumerBundle(bundle, w.getArgRestrictions(), w.getAllowedBundles());
            }
        } else {
            bundleWeavingData.put(bundle, NON_WOVEN_BUNDLE);
        }
    }

    private List<String> getAllHeaders(String headerName, Bundle bundle) {
        List<Bundle> bundlesFragments = new ArrayList<Bundle>();
        bundlesFragments.add(bundle);

        BundleRevision rev = bundle.adapt(BundleRevision.class);
        if (rev != null) {
            BundleWiring wiring = rev.getWiring();
            if (wiring != null) {
                for (BundleWire wire : wiring.getProvidedWires("osgi.wiring.host")) {
                    bundlesFragments.add(wire.getRequirement().getRevision().getBundle());
                }
            }
        }

        List<String> l = new ArrayList<String>();
        for (Bundle bf : bundlesFragments) {
            String header = bf.getHeaders().get(headerName);
            if (header != null) {
                l.add(header);
            }
        }

        return l;
    }

    public void removeWeavingData(Bundle bundle) {
        bundleWeavingData.remove(bundle);
    }

    @Override
    public synchronized void stop(BundleContext context) throws Exception {
        activator = null;

        consumerBundleTracker.close();
        providerBundleTracker.close();
    }

    public boolean isLogEnabled(int level) {
        switch (level) {
            case Integer.MAX_VALUE:
                return logger.isLoggable(Level.FINEST);
            case LogService.LOG_ERROR:
                return logger.isLoggable(Level.SEVERE);
            case LogService.LOG_INFO:
                return logger.isLoggable(Level.INFO);
            case LogService.LOG_DEBUG:
                return logger.isLoggable(Level.FINE);
            case LogService.LOG_WARNING:
            default:
                return logger.isLoggable(Level.WARNING);
        }
    }

    public void log(int level, String message) {
        log(level, message, null);
    }

    public void log(int level, String message, Throwable th) {
        switch (level) {
            case Integer.MAX_VALUE:
                logger.log(Level.FINEST, message, th);
                break;
            case LogService.LOG_ERROR:
                logger.log(Level.SEVERE, message, th);
                break;
            case LogService.LOG_WARNING:
                logger.log(Level.WARNING, message, th);
                break;
            case LogService.LOG_INFO:
                logger.log(Level.INFO, message, th);
                break;
            case LogService.LOG_DEBUG:
                logger.log(Level.FINE, message, th);
                break;
            default:
                logger.log(Level.SEVERE, "[Unknown Level: " + level + "] " + message, th);
        }
    }

    public Set<WeavingData> getWeavingData(Bundle b) {
        // Simply return the value as it's already an immutable set.
        Set<WeavingData> wd = bundleWeavingData.get(b);
        if (wd == null)
            return null;

        if (wd.size() == 0)
            return null;

        return wd;
    }

    public void registerProviderBundle(String registrationClassName, Bundle bundle, Map<String, Object> customAttributes) {
        registrationClassName = registrationClassName.trim();
        registeredProviders.putIfAbsent(registrationClassName,
                Collections.synchronizedSortedMap(new TreeMap<Long, Pair<Bundle, Map<String, Object>>>()));

        SortedMap<Long, Pair<Bundle, Map<String, Object>>> map = registeredProviders.get(registrationClassName);
        map.put(bundle.getBundleId(), new Pair<Bundle, Map<String, Object>>(bundle, customAttributes));
    }

    public void unregisterProviderBundle(Bundle bundle) {
        for (Map<Long, Pair<Bundle, Map<String, Object>>> value : registeredProviders.values()) {
            for(Iterator<Entry<Long, Pair<Bundle, Map<String, Object>>>> it = value.entrySet().iterator(); it.hasNext(); ) {
                Entry<Long, Pair<Bundle, Map<String, Object>>> entry = it.next();
                if (entry.getValue().getLeft().equals(bundle)) {
                    it.remove();
                }
            }
        }
    }

    public Collection<Bundle> findProviderBundles(String name) {
        SortedMap<Long, Pair<Bundle, Map<String, Object>>> map = registeredProviders.get(name);
        if (map == null)
            return Collections.emptyList();

        List<Bundle> bundles = new ArrayList<Bundle>(map.size());
        for(Pair<Bundle, Map<String, Object>> value : map.values()) {
            bundles.add(value.getLeft());
        }

        return bundles;
    }

    public Map<String, Object> getCustomBundleAttributes(String name, Bundle b) {
        SortedMap<Long, Pair<Bundle, Map<String, Object>>> map = registeredProviders.get(name);
        if (map == null)
            return Collections.emptyMap();

        Pair<Bundle, Map<String, Object>> data = map.get(b.getBundleId());
        if (data == null)
            return Collections.emptyMap();

        return data.getRight();
    }

    public void registerConsumerBundle(Bundle consumerBundle,
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
                return getBundles(entry.getValue(), className, methodName, args);
            }
        }

        // Empty collection: nothing matches
        return Collections.emptySet();
    }

    private Collection<Bundle> getBundles(List<BundleDescriptor> descriptors, String className, String methodName,
            Map<Pair<Integer, String>, String> args) {
        if (descriptors == null) {
            return null;
        }

        List<Bundle> bundles = new ArrayList<Bundle>();
        for (Bundle b : bundleContext.getBundles()) {
            for (BundleDescriptor desc : descriptors) {
                if (desc.getBundleID() != BundleDescriptor.BUNDLE_ID_UNSPECIFIED) {
                    if (b.getBundleId() == desc.getBundleID()) {
                        bundles.add(b);
                    }
                } else if (desc.getFilter() != null) {
                    Hashtable<String, Object> d = new Hashtable<String, Object>();

                    if (ServiceLoader.class.getName().equals(className) &&
                        "load".equals(methodName)) {
                        String type = args.get(new Pair<Integer, String>(0, Class.class.getName()));
                        if (type != null) {
                            d.put(SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE, type);
                            d.putAll(getCustomBundleAttributes(type, b));
                        }
                    }
                    if (desc.getFilter().match(d))
                        bundles.add(b);
                } else {
                    if (b.getSymbolicName().equals(desc.getSymbolicName())) {
                        if (desc.getVersion() == null || b.getVersion().equals(desc.getVersion())) {
                            bundles.add(b);
                        }
                    }
                }
            }
        }
        return bundles;
    }

}
