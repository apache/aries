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
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.service.framework.CompositeBundle;
import org.osgi.util.tracker.BundleTracker;

/**
 * <p>This class supports the tracking of composite bundles. It allows clients to ignore any
 * events related to framework bundles, as it will automatically handle these events. In
 * order to use this class clients must create a subclass and implement the methods of the
 * <code>BundleTrackerCustomizer</code> interface. In spite of this, instances of this class
 * MUST NOT be passed as a parameter to any <code>BundleTracker</code>.</p> 
 * 
 * @author pradine
 *
 */
public abstract class MultiBundleTracker extends AbstractBundleTrackerCustomizer {
    private static final int COMPOSITE_BUNDLE_MASK =
        Bundle.STARTING | Bundle.ACTIVE | Bundle.STOPPING;
    
    private final BundleTracker tracker;
        
    /**
     * Constructor
     * 
     * @param context - The <code>BundleContext</code> against which the tracking is done.
     * @param stateMask - The bit mask of the ORing of the bundle states to be tracked. The
     * mask must contain the flags <code>Bundle.STARTING | Bundle.ACTIVE | Bundle.STOPPING</code>
     * as a minimum.
     * @throws IllegalArgumentException - If the provided bit mask does not contain required
     * flags
     */
    public MultiBundleTracker(BundleContext context, int stateMask) {
        if ((stateMask & COMPOSITE_BUNDLE_MASK) != COMPOSITE_BUNDLE_MASK)
            throw new IllegalArgumentException();
        
        if (areMultipleFrameworksAvailable(context)) {
          tracker = new InternalBundleTracker(context, stateMask);
        } else {
          tracker = new BundleTracker(context, stateMask, this);
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

    //This implementation of a BundleTracker is based on the implementation
    //in org.apache.aries.util.tracker.AriesBundleTrackerCustomizer
    private class InternalBundleTracker extends BundleTracker {
        private final int mask;
        
        public InternalBundleTracker(BundleContext context, int stateMask) {
            super(context, stateMask, null);
            
            mask = stateMask;
        }

        /*
         * (non-Javadoc)
         * @see org.osgi.util.tracker.BundleTracker#addingBundle(org.osgi.framework.Bundle, org.osgi.framework.BundleEvent)
         */
        public Object addingBundle(Bundle b, BundleEvent event) {
            Object o = null;
            
            if (b instanceof CompositeBundle) {
                customizedProcessBundle(this, b, event, mask);
                o = b;
            }
            else {
                o = MultiBundleTracker.this.addingBundle(b, event);
            }
            
            return o;
        }

        /*
         * (non-Javadoc)
         * @see org.osgi.util.tracker.BundleTracker#modifiedBundle(org.osgi.framework.Bundle, org.osgi.framework.BundleEvent, java.lang.Object)
         */
        public void modifiedBundle(Bundle b, BundleEvent event, Object object) {
            if (b instanceof CompositeBundle) {
                customizedProcessBundle(this, b, event, mask);
            }
            else {
                MultiBundleTracker.this.modifiedBundle(b, event, object);
            }
        }

        /*
         * (non-Javadoc)
         * @see org.osgi.util.tracker.BundleTracker#removedBundle(org.osgi.framework.Bundle, org.osgi.framework.BundleEvent, java.lang.Object)
         */
        public void removedBundle(Bundle b, BundleEvent event, Object object) {
            if (!(b instanceof CompositeBundle))
                MultiBundleTracker.this.removedBundle(b, event, object);
        }
    }
}
