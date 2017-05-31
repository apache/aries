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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.service.framework.CompositeBundle;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * A BundleTracker which will track bundles in the given context, and also 
 * bundles in any child contexts. This should be used instead of the
 * normal non-recursive BundleTracker when registering bundle tracker
 * customizers.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class InternalRecursiveBundleTracker extends BundleTracker
{
  private final int mask;

  private final ConcurrentMap<String,String> alreadyRecursedContexts = new ConcurrentHashMap<String, String>();

  private final BundleTrackerCustomizer customizer;

  private final boolean nested;

  public InternalRecursiveBundleTracker(BundleContext context, int stateMask,
      BundleTrackerCustomizer customizer, boolean nested)
  {
    super(context, stateMask, null);
    mask = stateMask;
    this.customizer = customizer;
    this.nested = nested;
  }

  /*
  * (non-Javadoc)
  * @see org.osgi.util.tracker.BundleTracker#addingBundle(org.osgi.framework.Bundle, org.osgi.framework.BundleEvent)
  */
  @Override
  public Object addingBundle(Bundle b, BundleEvent event)
  {
    Object o = null;

    if (b instanceof CompositeBundle) {
      customizedProcessBundle(this, b, event, false);
      o = b;
    } else if (nested) {
      // Delegate to our customizer for normal bundles
      if (customizer != null) {
        o = customizer.addingBundle(b, event);
      }
    }

    return o;
  }

  /*
   * (non-Javadoc)
   * @see org.osgi.util.tracker.BundleTracker#modifiedBundle(org.osgi.framework.Bundle, org.osgi.framework.BundleEvent, java.lang.Object)
   */
  @Override
  public void modifiedBundle(Bundle b, BundleEvent event, Object object)
  {
    if (b instanceof CompositeBundle) {
      customizedProcessBundle(this, b, event, false);
    } else {
      // Delegate to our customizer for normal bundles
      if (customizer != null) {
        customizer.modifiedBundle(b, event, object);
      }
    }
  }

  /*
   * (non-Javadoc)
   * @see org.osgi.util.tracker.BundleTracker#removedBundle(org.osgi.framework.Bundle, org.osgi.framework.BundleEvent, java.lang.Object)
   */
  @Override
  public void removedBundle(Bundle b, BundleEvent event, Object object)
  {
    if (b instanceof CompositeBundle) {
      customizedProcessBundle(this, b, event, true);
    } else {
      if (customizer != null) {
        customizer.removedBundle(b, event, object);
      }
    }
  }

  protected void customizedProcessBundle(BundleTrackerCustomizer btc, Bundle b, BundleEvent event, boolean removing)
  {
    if (b instanceof CompositeBundle) {
      CompositeBundle cb = (CompositeBundle) b;
      // check if the compositeBundle is already tracked in the
      // BundleTrackerFactory
      String bundleScope = cb.getSymbolicName() + "_" + cb.getVersion().toString();
      List<BundleTracker> btList = BundleTrackerFactory.getBundleTrackerList(bundleScope);

      // bundle is already active and there is no event associated
      // this can happen when bundle is first time added to the tracker 
      // or when the tracker is being closed.
      if (event == null && !!!removing) {
        if (cb.getState() == Bundle.INSTALLED || cb.getState() == Bundle.RESOLVED || cb.getState() == Bundle.STARTING || cb.getState() == Bundle.ACTIVE) {
          openTracker(btc, cb, bundleScope, mask);
        }
      } else {
        // if we are removing, or the event is of the right type then we need to shutdown.
        if (removing || event.getType() == BundleEvent.STOPPED || event.getType() == BundleEvent.UNRESOLVED || event.getType() == BundleEvent.UNINSTALLED) {
          // if CompositeBundle is being stopped, let's remove the bundle
          // tracker(s) associated with the composite bundle
          String bundleId = b.getSymbolicName()+"/"+b.getVersion();
          alreadyRecursedContexts.remove(bundleId);
          
          if (btList != null) {
            // unregister the bundlescope off the factory and close
            // bundle trackers
            BundleTrackerFactory.unregisterAndCloseBundleTracker(bundleScope);
          }
        } else if (event.getType() == BundleEvent.INSTALLED || event.getType() == BundleEvent.RESOLVED || event.getType() == BundleEvent.STARTING) {
          openTracker(btc, cb, bundleScope, mask);
        }
      }
    }
  }

  private synchronized void openTracker(BundleTrackerCustomizer btc, CompositeBundle cb,
      String bundleScope, int stateMask)
  {
    // let's process each of the bundle in the CompositeBundle
    BundleContext compositeBundleContext = cb.getCompositeFramework().getBundleContext();
    
    String bundleId = cb.getSymbolicName()+"/"+cb.getVersion();
    if (alreadyRecursedContexts.putIfAbsent(bundleId, bundleId) == null) {

      // let's track each of the bundle in the CompositeBundle
      BundleTracker bt = new InternalRecursiveBundleTracker(compositeBundleContext, stateMask,
          customizer, true);
      bt.open();
      BundleTrackerFactory.registerBundleTracker(bundleScope, bt);
    }
  }
}
