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

package org.apache.aries.jpa.container.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.jpa.container.parser.impl.PersistenceUnit;
import org.apache.aries.jpa.container.parser.impl.PersistenceUnitParser;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Looks for bundles containing a persistence.xml. For each persistence unit
 * found a PersistenceProviderTracker is installed that tracks matching providers.
 */
public class PersistenceBundleTracker implements BundleTrackerCustomizer<Bundle> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistenceBundleTracker.class);
    Map<Bundle, Collection<PersistenceProviderTracker>> trackers;
    private BundleContext context;

    public PersistenceBundleTracker(BundleContext context) {
        this.context = context;
        trackers = new HashMap<Bundle, Collection<PersistenceProviderTracker>>();
    }

    public synchronized Bundle addingBundle(Bundle bundle, BundleEvent event) {
        if (getTrackers(bundle).size() == 0) {
            findPersistenceUnits(bundle);
        }
        return bundle;
    }

    public synchronized void removedBundle(Bundle bundle, BundleEvent event, Bundle object) {
        Collection<PersistenceProviderTracker> providerTrackers = trackers.remove(bundle);
        if (providerTrackers == null || providerTrackers.size() == 0) {
            return;
        }
        LOGGER.info("removing persistence units for " + bundle.getSymbolicName() + " " + getType(event));
        for (PersistenceProviderTracker providerTracker : providerTrackers) {
            providerTracker.close();
        }
        providerTrackers.clear();
    }

    private void findPersistenceUnits(Bundle bundle) {
        for (PersistenceUnit punit : PersistenceUnitParser.getPersistenceUnits(bundle)) {
            punit.addAnnotated();
            trackProvider(bundle, punit);
        }
    }

    private void trackProvider(Bundle bundle, PersistenceUnit punit) {
        LOGGER.info(String.format("Found persistence unit %s in bundle %s with provider %s.",
                                  punit.getPersistenceUnitName(), bundle.getSymbolicName(),
                                  punit.getPersistenceProviderClassName()));
        PersistenceProviderTracker tracker = new PersistenceProviderTracker(context, punit);
        tracker.open();
        getTrackers(bundle).add(tracker);
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, Bundle object) {
    }

    private static String getType(BundleEvent event) {
        if (event == null) {
            return "null";
        }
        int type = event.getType();
        switch (type) {
        case BundleEvent.INSTALLED:
            return "INSTALLED";
        case BundleEvent.LAZY_ACTIVATION:
            return "LAZY_ACTIVATION";
        case BundleEvent.RESOLVED:
            return "RESOLVED";
        case BundleEvent.STARTED:
            return "STARTED";
        case BundleEvent.STARTING:
            return "Starting";
        case BundleEvent.STOPPED:
            return "STOPPED";
        case BundleEvent.UNINSTALLED:
        case 256: // Not sure why but this event is sent when a bundle is uninstalled
            return "UNINSTALLED";
        case BundleEvent.UNRESOLVED:
            return "UNRESOLVED";
        case BundleEvent.UPDATED:
            return "UPDATED";
        default:
            return "unknown event type: " + type;
        }
    }
    
    private Collection<PersistenceProviderTracker> getTrackers(Bundle bundle) {
        Collection<PersistenceProviderTracker> providerTrackers = trackers.get(bundle);
        if (providerTrackers == null) {
            providerTrackers = new ArrayList<>();
            trackers.put(bundle, providerTrackers);
        }
        return providerTrackers;
    }

}
