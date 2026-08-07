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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
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
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.util.tracker.BundleTracker;

import aQute.bnd.header.Parameters;
import aQute.bnd.stream.MapStream;
import aQute.libg.glob.Glob;

public abstract class BaseActivator implements BundleActivator {
    private static final String PROCESSED_REQUIRE_CAPABILITY_HEADER =
            "X-SpiFly-Processed-Require-Capability";
    private static final Set<WeavingData> NON_WOVEN_BUNDLE = Collections.emptySet();
    private static final Logger logger = Logger.getLogger(BaseActivator.class.getName());

    // Static access to the activator used by the woven code, therefore
    // this bundle must be a singleton.
    // TODO see if we can get rid of the static access.
    public static volatile BaseActivator activator;

    /*
     * A ServiceLoader is lazy, so clearing activator alone is not sufficient:
     * a loader created before stop could otherwise discover or instantiate a
     * provider after this mediator instance has stopped. Each activation gets
     * a distinct token which is captured by the class loaders backing that
     * ServiceLoader view.
     */
    private volatile Object activeSession = new Object();

    private BundleContext bundleContext;
    @SuppressWarnings("rawtypes")
    private BundleTracker consumerBundleTracker;
    @SuppressWarnings("rawtypes")
    private BundleTracker providerBundleTracker;
    private ProviderBundleTrackerCustomizer providerBundleTrackerCustomizer;
    private Optional<Parameters> autoConsumerInstructions;
    private Optional<Parameters> autoProviderInstructions;

    private final ConcurrentMap<Bundle, Set<WeavingData>> bundleWeavingData =
        new ConcurrentHashMap<Bundle, Set<WeavingData>>();

    private final ConcurrentMap<String, SortedMap<Long, Pair<Bundle, Map<String, Object>>>> registeredProviders =
            new ConcurrentHashMap<String, SortedMap<Long, Pair<Bundle, Map<String, Object>>>>();

    private final ConcurrentMap<String, SortedMap<Long, ProviderAdvertisement>> providerAdvertisements =
            new ConcurrentHashMap<String, SortedMap<Long, ProviderAdvertisement>>();

    private final ConcurrentMap<Bundle, Map<ConsumerRestriction, List<BundleDescriptor>>> consumerRestrictions =
            new ConcurrentHashMap<Bundle, Map<ConsumerRestriction, List<BundleDescriptor>>>();

    private final ConcurrentMap<Bundle, StandardConsumerWiring> standardConsumerWirings =
            new ConcurrentHashMap<Bundle, StandardConsumerWiring>();

    private final Set<Pair<Bundle, BundleRevision>> requestedConsumerRefreshes =
            Collections.newSetFromMap(
                    new ConcurrentHashMap<Pair<Bundle, BundleRevision>, Boolean>());

    private final ConcurrentMap<Bundle, Set<Bundle>> consumersByProvider =
            new ConcurrentHashMap<Bundle, Set<Bundle>>();

    private final Set<Bundle> requestedProviderStopRefreshes =
            Collections.newSetFromMap(
                    new ConcurrentHashMap<Bundle, Boolean>());

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public synchronized void start(BundleContext context, final String consumerHeaderName) throws Exception {
        bundleContext = context;

        try {
            autoConsumerInstructions = Optional.ofNullable(
                bundleContext.getProperty("org.apache.aries.spifly.auto.consumers")
            ).map(Parameters::new);

            autoProviderInstructions = Optional.ofNullable(
                bundleContext.getProperty("org.apache.aries.spifly.auto.providers")
            ).map(Parameters::new);
        }
        catch (Throwable t) {
            log(Level.FINE, t.getMessage(), t);
        }

        providerBundleTrackerCustomizer =
                new ProviderBundleTrackerCustomizer(this, context.getBundle());
        providerBundleTracker = new BundleTracker(context,
                Bundle.ACTIVE | Bundle.STARTING, providerBundleTrackerCustomizer);
        providerBundleTracker.open();

        consumerBundleTracker = new BundleTracker(context,
                Bundle.INSTALLED | Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE, new ConsumerBundleTrackerCustomizer(this, consumerHeaderName));
        consumerBundleTracker.open();

        for (Bundle bundle : context.getBundles()) {
            addConsumerWeavingData(bundle, consumerHeaderName);
        }

        activeSession = new Object();
        activator = this;

        if (SpiFlyConstants.SPI_CONSUMER_HEADER.equals(consumerHeaderName)) {
            refreshActiveConsumers();
        }
    }

    public void addConsumerWeavingData(Bundle bundle, String consumerHeaderName) throws Exception {
        if (bundleWeavingData.containsKey(bundle)) {
            // This bundle was already processed
            return;
        }

        Bundle mediatorBundle = bundleContext == null ? null : bundleContext.getBundle();
        List<String> proprietaryHeaders = getAllHeaders(consumerHeaderName, bundle);
        boolean directRequirementCompatibility =
                SpiFlyConstants.REQUIRE_CAPABILITY.equals(consumerHeaderName);
        boolean standardCandidate = proprietaryHeaders.isEmpty()
                && !directRequirementCompatibility;
        BundleWiring wiring = mediatorBundle == null || !standardCandidate
                ? null : WiringUtils.getWiring(bundle);
        boolean staticMediator = SpiFlyConstants.PROCESSED_SPI_CONSUMER_HEADER.equals(consumerHeaderName);
        boolean processedStandardBundle = !staticMediator
                || !getAllHeaders(PROCESSED_REQUIRE_CAPABILITY_HEADER, bundle).isEmpty();

        if (mediatorBundle != null && processedStandardBundle
                && WiringUtils.isWiredToExtender(
                        wiring, mediatorBundle, SpiFlyConstants.PROCESSOR_EXTENDER_NAME)) {
            registerStandardConsumer(bundle, wiring);
            return;
        }

        // Older statically woven bundles had their processor requirement removed. Keep them
        // working as an explicitly separate compatibility path, but use their remaining resolved
        // ServiceLoader wires instead of reapplying the saved requirement filters.
        boolean legacyStaticBundle = staticMediator
                && !getAllHeaders(PROCESSED_REQUIRE_CAPABILITY_HEADER, bundle).isEmpty()
                && getAllHeaders(SpiFlyConstants.REQUIRE_CAPABILITY, bundle).stream()
                        .noneMatch(header -> header.contains(SpiFlyConstants.PROCESSOR_EXTENDER_NAME));
        if (legacyStaticBundle) {
            registerStandardConsumer(bundle, wiring);
            return;
        }

        // A statically woven standard consumer will call Util even when its processor
        // requirement resolves to another mediator. Record an explicit deny state so
        // those calls cannot fall through to the unrestricted compatibility path.
        if (staticMediator && processedStandardBundle && standardCandidate) {
            standardConsumerWirings.put(bundle, StandardConsumerWiring.denied());
        }

        Map<String, List<String>> allHeaders = new HashMap<String, List<String>>();
        if (!proprietaryHeaders.isEmpty()) {
            allHeaders.put(consumerHeaderName, proprietaryHeaders);
        }
        else {
            getAutoConsumerInstructions().map(Parameters::stream).orElseGet(MapStream::empty).filterKey(
                i -> Glob.toPattern(i).asPredicate().test(bundle.getSymbolicName())
            ).findFirst().ifPresent(
                un -> allHeaders.put(
                    SpiFlyConstants.REQUIRE_CAPABILITY,
                    Arrays.asList(
                        SpiFlyConstants.CLIENT_REQUIREMENT.concat(",osgi.serviceloader;filter:='(osgi.serviceloader=*)'")))
            );
        }

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

    void registerStandardConsumer(Bundle bundle, BundleWiring wiring) {
        Set<WeavingData> weavingData = ConsumerHeaderProcessor.createServiceLoaderWeavingData();
        standardConsumerWirings.put(bundle, StandardConsumerWiring.from(wiring));
        bundleWeavingData.put(bundle, Collections.unmodifiableSet(weavingData));
    }

    boolean isStandardConsumer(Bundle bundle) {
        return standardConsumerWirings.containsKey(bundle);
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
        consumerRestrictions.remove(bundle);
        standardConsumerWirings.remove(bundle);
    }

    void fragmentAttached(Bundle fragment, String consumerHeaderName) throws Exception {
        BundleWiring fragmentWiring = WiringUtils.getWiring(fragment);
        if (fragmentWiring == null) {
            return;
        }

        for (BundleWire hostWire : fragmentWiring.getRequiredWires(
                HostNamespace.HOST_NAMESPACE)) {
            BundleWiring hostWiring = hostWire.getProviderWiring();
            Bundle host = hostWiring == null ? null : hostWiring.getBundle();
            if (host == null) {
                continue;
            }

            if (providerBundleTracker != null && providerBundleTrackerCustomizer != null) {
                Object registrations = providerBundleTracker.getObject(host);
                if (registrations != null) {
                    providerBundleTrackerCustomizer.reprocessBundle(host, registrations);
                }
            }

            if (consumerBundleTracker != null && consumerBundleTracker.getObject(host) != null) {
                reprocessConsumerHost(host, fragmentWiring.getRevision(), consumerHeaderName);
            }
        }
    }

    void reprocessConsumerHost(Bundle host, BundleRevision fragmentRevision,
            String consumerHeaderName) throws Exception {
        boolean wasStandardConsumer = isStandardConsumer(host);
        removeWeavingData(host);
        addConsumerWeavingData(host, consumerHeaderName);

        if (SpiFlyConstants.SPI_CONSUMER_HEADER.equals(consumerHeaderName)
                && !wasStandardConsumer && isStandardConsumer(host)) {
            refreshConsumerHost(host, fragmentRevision);
        }
    }

    private void refreshConsumerHost(Bundle host, BundleRevision fragmentRevision) {
        if (fragmentRevision == null) {
            return;
        }
        requestConsumerRefreshes(
                Collections.singleton(new Pair<Bundle, BundleRevision>(host, fragmentRevision)),
                "after processor fragment attachment");
    }

    void refreshActiveConsumers() {
        if (bundleContext == null) {
            return;
        }

        Bundle mediatorBundle = bundleContext.getBundle();
        List<Pair<Bundle, BundleRevision>> refreshes =
                new ArrayList<Pair<Bundle, BundleRevision>>();
        for (Bundle bundle : bundleContext.getBundles()) {
            int state = bundle.getState();
            if ((state & (Bundle.ACTIVE | Bundle.STARTING)) == 0
                    || bundle.equals(mediatorBundle)
                    || !isStandardConsumer(bundle)) {
                continue;
            }

            BundleRevision revision = bundle.adapt(BundleRevision.class);
            if (revision == null
                    || (revision.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) {
                continue;
            }
            refreshes.add(new Pair<Bundle, BundleRevision>(bundle, revision));
        }

        Collections.sort(refreshes, (left, right) -> Long.compare(
                left.getLeft().getBundleId(), right.getLeft().getBundleId()));
        requestConsumerRefreshes(refreshes, "after dynamic mediator startup");
    }

    void recordProviderUse(Bundle consumer, Bundle provider, Object session) {
        if (!isSessionActive(session) || consumer == null || provider == null
                || !isStandardConsumer(consumer)) {
            return;
        }
        consumersByProvider.computeIfAbsent(provider,
                key -> Collections.newSetFromMap(
                        new ConcurrentHashMap<Bundle, Boolean>())).add(consumer);
    }

    void forgetConsumerProviderUses(Bundle consumer) {
        for (Map.Entry<Bundle, Set<Bundle>> entry : consumersByProvider.entrySet()) {
            Set<Bundle> consumers = entry.getValue();
            consumers.remove(consumer);
            if (consumers.isEmpty()) {
                consumersByProvider.remove(entry.getKey(), consumers);
            }
        }
    }

    void providerBundleStopped(Bundle provider) {
        Object session = getActiveSession();
        if (!isSessionActive(session)) {
            return;
        }
        int state = provider.getState();
        if ((state & (Bundle.ACTIVE | Bundle.STARTING)) != 0) {
            return;
        }

        Set<Bundle> consumers = consumersByProvider.remove(provider);
        if (consumers == null || consumers.isEmpty()) {
            return;
        }

        List<Bundle> refreshes = new ArrayList<Bundle>();
        for (Bundle consumer : consumers) {
            int consumerState = consumer.getState();
            BundleRevision revision = consumer.adapt(BundleRevision.class);
            if ((consumerState & (Bundle.ACTIVE | Bundle.STARTING)) != 0
                    && isStandardConsumer(consumer)
                    && revision != null
                    && (revision.getTypes() & BundleRevision.TYPE_FRAGMENT) == 0) {
                refreshes.add(consumer);
            }
        }
        Collections.sort(refreshes, (left, right) -> Long.compare(
                left.getBundleId(), right.getBundleId()));
        requestProviderStopRefreshes(refreshes, provider);
    }

    private void requestProviderStopRefreshes(
            Collection<Bundle> refreshes, Bundle provider) {
        if (refreshes.isEmpty()) {
            return;
        }
        Bundle systemBundle = bundleContext == null ? null : bundleContext.getBundle(0);
        FrameworkWiring frameworkWiring = systemBundle == null
                ? null : systemBundle.adapt(FrameworkWiring.class);
        if (frameworkWiring == null) {
            log(Level.WARNING, "Cannot refresh consumers after provider " + provider
                    + " stopped: FrameworkWiring is unavailable");
            return;
        }

        List<Bundle> newRefreshes = new ArrayList<Bundle>();
        for (Bundle consumer : refreshes) {
            if (requestedProviderStopRefreshes.add(consumer)) {
                newRefreshes.add(consumer);
            }
        }
        if (newRefreshes.isEmpty()) {
            return;
        }

        Set<Bundle> bundles = new LinkedHashSet<Bundle>(newRefreshes);
        try {
            frameworkWiring.refreshBundles(bundles, event -> {
                requestedProviderStopRefreshes.removeAll(newRefreshes);
                if (event.getType() == FrameworkEvent.ERROR) {
                    log(Level.WARNING, "Could not refresh consumers " + bundles
                            + " after provider " + provider + " stopped",
                            event.getThrowable());
                }
            });
        }
        catch (RuntimeException e) {
            requestedProviderStopRefreshes.removeAll(newRefreshes);
            log(Level.WARNING, "Could not request refresh of consumers " + bundles
                    + " after provider " + provider + " stopped", e);
        }
    }

    private void requestConsumerRefreshes(
            Collection<Pair<Bundle, BundleRevision>> refreshes, String reason) {
        if (refreshes.isEmpty()) {
            return;
        }
        Bundle systemBundle = bundleContext == null ? null : bundleContext.getBundle(0);
        FrameworkWiring frameworkWiring = systemBundle == null
                ? null : systemBundle.adapt(FrameworkWiring.class);
        if (frameworkWiring == null) {
            log(Level.WARNING, "Cannot refresh consumers " + reason
                    + ": FrameworkWiring is unavailable");
            return;
        }

        List<Pair<Bundle, BundleRevision>> newRefreshes =
                new ArrayList<Pair<Bundle, BundleRevision>>();
        Set<Bundle> bundles = new LinkedHashSet<Bundle>();
        for (Pair<Bundle, BundleRevision> refresh : refreshes) {
            if (requestedConsumerRefreshes.add(refresh)) {
                newRefreshes.add(refresh);
                bundles.add(refresh.getLeft());
            }
        }
        if (bundles.isEmpty()) {
            return;
        }

        try {
            frameworkWiring.refreshBundles(bundles, event -> {
                if (event.getType() == FrameworkEvent.ERROR) {
                    log(Level.WARNING, "Could not refresh consumers " + bundles + " "
                            + reason, event.getThrowable());
                }
            });
        }
        catch (RuntimeException e) {
            requestedConsumerRefreshes.removeAll(newRefreshes);
            log(Level.WARNING, "Could not request refresh of consumers " + bundles + " "
                    + reason, e);
        }
    }

    @Override
    public synchronized void stop(BundleContext context) throws Exception {
        activeSession = null;
        activator = null;

        if (consumerBundleTracker != null) {
            consumerBundleTracker.close();
        }
        if (providerBundleTracker != null) {
            providerBundleTracker.close();
        }
        requestedConsumerRefreshes.clear();
        consumersByProvider.clear();
        requestedProviderStopRefreshes.clear();
    }

    Object getActiveSession() {
        return activeSession;
    }

    boolean isSessionActive(Object session) {
        return session != null && activeSession == session && activator == this;
    }

    public boolean isLogEnabled(Level level) {
        return logger.isLoggable(level);
    }

    public void log(int level, String message) {
        log(level, message, null);
    }

    public void log(Level level, String message) {
        log(level, message, null);
    }

    public void log(int level, String message, Throwable th) {
        Level levelObject;

        if (Level.ALL.intValue() == level) {
            levelObject = Level.ALL;
        }
        else if (Level.CONFIG.intValue() == level) {
            levelObject = Level.CONFIG;
        }
        else if (Level.FINE.intValue() == level) {
            levelObject = Level.FINE;
        }
        else if (Level.FINER.intValue() == level) {
            levelObject = Level.FINER;
        }
        else if (Level.FINEST.intValue() == level) {
            levelObject = Level.FINEST;
        }
        else if (Level.INFO.intValue() == level) {
            levelObject = Level.INFO;
        }
        else if (Level.SEVERE.intValue() == level) {
            levelObject = Level.SEVERE;
        }
        else if (Level.WARNING.intValue() == level) {
            levelObject = Level.WARNING;
        }
        else {
            levelObject = Level.OFF;
        }

        log(levelObject, message, th);
    }

    public void log(Level level, String message, Throwable th) {
        if (logger.isLoggable(level)) {
            logger.log(level, message, th);
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
        SortedMap<Long, Pair<Bundle, Map<String, Object>>> map = registeredProviders.computeIfAbsent(registrationClassName,
            k -> Collections.synchronizedSortedMap(new TreeMap<Long, Pair<Bundle, Map<String, Object>>>()));

        map.compute(
            bundle.getBundleId(),
            (k,v) -> {
                if (v == null) {
                    return new Pair<Bundle, Map<String, Object>>(bundle, customAttributes);
                }
                else {
                    v.getRight().putAll(customAttributes);
                    return v;
                }
            });
    }

    void registerProviderBundle(String serviceType, String implementationName,
            Bundle bundle, Map<String, Object> customAttributes) {
        registerProviderBundle(serviceType, bundle, customAttributes);
        SortedMap<Long, ProviderAdvertisement> advertisements =
                providerAdvertisements.computeIfAbsent(serviceType,
                        key -> Collections.synchronizedSortedMap(
                                new TreeMap<Long, ProviderAdvertisement>()));
        synchronized (advertisements) {
            ProviderAdvertisement advertisement = advertisements.get(bundle.getBundleId());
            if (advertisement == null) {
                BundleWiring wiring = WiringUtils.getWiring(bundle);
                BundleRevision revision = wiring == null ? null : wiring.getRevision();
                advertisement = new ProviderAdvertisement(bundle, revision, wiring);
                advertisements.put(bundle.getBundleId(), advertisement);
            }
            advertisement.addImplementation(implementationName);
        }
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
        for (Map<Long, ProviderAdvertisement> advertisements
                : providerAdvertisements.values()) {
            advertisements.remove(bundle.getBundleId());
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

    Collection<Bundle> filterCompatibleProviderBundles(
            Bundle consumer, Class<?> serviceType, Collection<Bundle> providers) {
        if (!standardConsumerWirings.containsKey(consumer)) {
            return providers;
        }

        List<Bundle> compatible = new ArrayList<Bundle>();
        for (Bundle provider : providers) {
            try {
                if (provider.loadClass(serviceType.getName()) == serviceType) {
                    compatible.add(provider);
                }
            }
            catch (ClassNotFoundException | LinkageError e) {
                log(Level.FINE, "Provider " + provider
                        + " is not type-space compatible with " + serviceType.getName(), e);
            }
        }
        return compatible;
    }

    List<ProviderAdvertisement> findProviderAdvertisements(
            String serviceType, Collection<Bundle> selectedBundles) {
        SortedMap<Long, ProviderAdvertisement> advertisements =
                providerAdvertisements.get(serviceType);
        if (advertisements == null || selectedBundles.isEmpty()) {
            return Collections.emptyList();
        }

        List<ProviderAdvertisement> selected = new ArrayList<ProviderAdvertisement>();
        synchronized (advertisements) {
            for (ProviderAdvertisement advertisement : advertisements.values()) {
                if (selectedBundles.contains(advertisement.getBundle())) {
                    selected.add(advertisement.snapshot());
                }
            }
        }
        return selected;
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
        StandardConsumerWiring standardWiring = standardConsumerWirings.get(consumer);
        if (standardWiring != null && ServiceLoader.class.getName().equals(className)
                && isServiceLoaderMethod(methodName)) {
            String serviceType = args == null ? null
                    : args.get(new Pair<Integer, String>(0, Class.class.getName()));
            return standardWiring.getProviders(serviceType);
        }

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

    public Optional<Parameters> getAutoConsumerInstructions() {
        if (autoConsumerInstructions == null) return Optional.empty();
        return autoConsumerInstructions;
    }

    public void setAutoConsumerInstructions(Optional<Parameters> autoConsumerInstructions) {
        this.autoConsumerInstructions = autoConsumerInstructions;
    }

    public Optional<Parameters> getAutoProviderInstructions() {
        if (autoProviderInstructions == null) return Optional.empty();
        return autoProviderInstructions;
    }

    public void setAutoProviderInstructions(Optional<Parameters> autoProviderInstructions) {
        this.autoProviderInstructions = autoProviderInstructions;
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

                    if (ServiceLoader.class.getName().equals(className)
                            && isServiceLoaderMethod(methodName)) {
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

    private static boolean isServiceLoaderMethod(String methodName) {
        return "load".equals(methodName) || "loadInstalled".equals(methodName);
    }

    private static final class StandardConsumerWiring {
        private final boolean restricted;
        private final Map<String, Set<Bundle>> providersByServiceType;

        private StandardConsumerWiring(boolean restricted, Map<String, Set<Bundle>> providersByServiceType) {
            this.restricted = restricted;
            this.providersByServiceType = providersByServiceType;
        }

        static StandardConsumerWiring from(BundleWiring wiring) {
            if (wiring == null) {
                return new StandardConsumerWiring(true, Collections.<String, Set<Bundle>>emptyMap());
            }

            if (!hasDeclaredServiceLoaderRequirement(wiring)) {
                return new StandardConsumerWiring(false, Collections.<String, Set<Bundle>>emptyMap());
            }

            Map<String, Set<Bundle>> providers = new HashMap<String, Set<Bundle>>();
            for (BundleWire wire : wiring.getRequiredWires(
                    SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE)) {
                Object serviceType = wire.getCapability().getAttributes().get(
                        SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE);
                BundleWiring providerWiring = wire.getProviderWiring();
                if (serviceType instanceof String && providerWiring != null) {
                    providers.computeIfAbsent((String) serviceType, key -> new HashSet<Bundle>())
                            .add(providerWiring.getBundle());
                }
            }

            Map<String, Set<Bundle>> immutableProviders = new HashMap<String, Set<Bundle>>();
            for (Map.Entry<String, Set<Bundle>> entry : providers.entrySet()) {
                immutableProviders.put(entry.getKey(), Collections.unmodifiableSet(entry.getValue()));
            }
            return new StandardConsumerWiring(
                    true, Collections.unmodifiableMap(immutableProviders));
        }

        private static boolean hasDeclaredServiceLoaderRequirement(BundleWiring wiring) {
            BundleRevision hostRevision = wiring.getRevision();
            if (hostRevision == null) {
                // Compatibility for older wiring implementations and test doubles. A real
                // R8 wiring supplies its revision, whose declared view is authoritative.
                List<BundleRequirement> requirements = wiring.getRequirements(
                        SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE);
                return requirements != null && !requirements.isEmpty();
            }

            if (hasDeclaredServiceLoaderRequirement(hostRevision)) {
                return true;
            }
            List<BundleWire> hostWires = wiring.getProvidedWires(
                    HostNamespace.HOST_NAMESPACE);
            if (hostWires == null) {
                return false;
            }
            for (BundleWire hostWire : hostWires) {
                BundleRequirement hostRequirement = hostWire.getRequirement();
                BundleRevision fragmentRevision = hostRequirement == null
                        ? null : hostRequirement.getRevision();
                if (hasDeclaredServiceLoaderRequirement(fragmentRevision)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean hasDeclaredServiceLoaderRequirement(
                BundleRevision revision) {
            if (revision == null) {
                return false;
            }
            List<BundleRequirement> requirements = revision.getDeclaredRequirements(
                    SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE);
            return requirements != null && !requirements.isEmpty();
        }

        static StandardConsumerWiring denied() {
            return new StandardConsumerWiring(
                    true, Collections.<String, Set<Bundle>>emptyMap());
        }

        Collection<Bundle> getProviders(String serviceType) {
            if (!restricted) {
                return null;
            }
            Set<Bundle> providers = providersByServiceType.get(serviceType);
            return providers == null ? Collections.<Bundle>emptySet() : providers;
        }
    }

    static final class ProviderAdvertisement {
        private final Bundle bundle;
        private final BundleRevision revision;
        private final BundleWiring wiring;
        private final Set<String> implementationNames =
                new java.util.LinkedHashSet<String>();

        private ProviderAdvertisement(Bundle bundle, BundleRevision revision,
                BundleWiring wiring) {
            this.bundle = bundle;
            this.revision = revision;
            this.wiring = wiring;
        }

        private synchronized void addImplementation(String implementationName) {
            implementationNames.add(implementationName);
        }

        private synchronized ProviderAdvertisement snapshot() {
            ProviderAdvertisement snapshot = new ProviderAdvertisement(
                    bundle, revision, wiring);
            snapshot.implementationNames.addAll(implementationNames);
            return snapshot;
        }

        Bundle getBundle() {
            return bundle;
        }

        BundleRevision getRevision() {
            return revision;
        }

        BundleWiring getWiring() {
            return wiring;
        }

        synchronized List<String> getImplementationNames() {
            return new ArrayList<String>(implementationNames);
        }
    }

}
