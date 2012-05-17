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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * <p>This class supports the tracking of composite bundles. It allows clients to ignore any
 * events related to framework bundles, as it will automatically handle these events. In
 * order to use this class clients must create a subclass and implement the methods of the
 * <code>BundleTrackerCustomizer</code> interface. In spite of this, instances of this class
 * MUST NOT be passed as a parameter to any <code>BundleTracker</code>.</p> 
 * 
 * The model for using this is that classes should instantiate it
 * and pass it a 'vanilla' bundle tracker.
 * @author pradine
 *
 */
public final class RecursiveBundleTracker  {
    private static final int COMPOSITE_BUNDLE_MASK =
      Bundle.INSTALLED | Bundle.STARTING | Bundle.ACTIVE | Bundle.STOPPING;
    
    private final BundleTracker tracker;
        
    /**
     * Constructor
     * 
     * @param context - The <code>BundleContext</code> against which the tracking is done.
     * @param stateMask - The bit mask of the ORing of the bundle states to be tracked. The
     * mask must contain the flags <code>Bundle.INSTALLED | Bundle.STARTING | Bundle.ACTIVE | Bundle.STOPPING</code>
     * as a minimum.
     * @throws IllegalArgumentException - If the provided bit mask does not contain required
     * flags
     */
    public RecursiveBundleTracker(BundleContext context, int stateMask, BundleTrackerCustomizer customizer) {
      // We always need INSTALLED events so we can recursively listen to the frameworks
      if ((stateMask & COMPOSITE_BUNDLE_MASK) != COMPOSITE_BUNDLE_MASK)
            throw new IllegalArgumentException();
       if (areMultipleFrameworksAvailable(context)) {
          tracker = new InternalRecursiveBundleTracker(context, stateMask, customizer);
        } else {
         tracker = new BundleTracker(context, stateMask, customizer);
        }
    }
    
    private static boolean areMultipleFrameworksAvailable(BundleContext context) {
      ServiceReference sr = context.getServiceReference("org.osgi.service.framework.CompositeBundleFactory");
      return sr != null;
    }
    
    /**
     * Start tracking bundles that match the bit mask provided at creation time.
     * 
     * @see BundleTracker#open()
     */
    public void open() {
        tracker.open();
    }
    
    /**
     * Stop the tracking of bundles
     * 
     * @see BundleTracker#close()
     */
    public void close() {
        tracker.close();
    }

}
