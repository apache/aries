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
package org.apache.aries.util.tracker;

import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.service.framework.CompositeBundle;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

public abstract class AriesBundleTrackerCustomizer implements
        BundleTrackerCustomizer {

    public AriesBundleTrackerCustomizer() {
    }

    public Object addingBundle(Bundle b, BundleEvent event) {
        customizedProcessBundle(b, event, Bundle.STARTING | Bundle.STOPPING);
        return null;
    }

    public void modifiedBundle(Bundle b, BundleEvent event, Object arg2) {
        // we are only interested in uninstalled bundle state for composite
        // bundles
        // as we need to remove the bt off the bt factory
        if (event.getType() == BundleEvent.STOPPING) {
            customizedProcessBundle(b, event, 0);
        }
    }

    public void removedBundle(Bundle b, BundleEvent event, Object arg2) {
    }

    protected void customizedProcessBundle(Bundle b, BundleEvent event,
            int stateMask) {
        if (b instanceof CompositeBundle) {
            // check if the compositeBundle is already tracked in the
            // BundleTrackerFactory
            String bundleScope = b.getSymbolicName() + "_"
                    + b.getVersion().toString();
            List<BundleTracker> btList = BundleTrackerFactory
                    .getBundleTrackerList(bundleScope);

            if (event.getType() == BundleEvent.STOPPING) {
                // if CompositeBundle is being stopped, let's remove the bundle
                // tracker(s) associated with the composite bundle
                if (btList != null) {
                    // unregister the bundlescope off the factory and close
                    // bundle trackers
                    BundleTrackerFactory
                            .unregisterAndCloseBundleTracker(bundleScope);
                }
            } else if (event.getType() == BundleEvent.STARTING) {
                // let's process each of the bundle in the CompositeBundle
                CompositeBundle cb = (CompositeBundle) b;
                BundleContext compositeBundleContext = cb
                        .getCompositeFramework().getBundleContext();

                // let's track each of the bundle in the CompositeBundle
                BundleTracker bt = new BundleTracker(compositeBundleContext,
                        stateMask, this);
                bt.open();
                BundleTrackerFactory.registerBundleTracker(bundleScope, bt);
            }
        }
    }
}
