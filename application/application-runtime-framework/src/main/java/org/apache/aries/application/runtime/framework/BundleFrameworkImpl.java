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
package org.apache.aries.application.runtime.framework;

import static org.apache.aries.application.utils.AppConstants.LOG_ENTRY;
import static org.apache.aries.application.utils.AppConstants.LOG_EXCEPTION;
import static org.apache.aries.application.utils.AppConstants.LOG_EXIT;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.spi.framework.BundleFramework;
import org.apache.aries.application.management.spi.repository.BundleRepository.BundleSuggestion;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.service.framework.CompositeBundle;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BundleFrameworkImpl implements BundleFramework
{
  private static final Logger LOGGER = LoggerFactory.getLogger(BundleFrameworkImpl.class);

  List<Bundle> _bundles;
  CompositeBundle _compositeBundle;
  Framework _framework;

  ServiceTracker _packageAdminTracker;
  
  private final AtomicBoolean startLevelIncreased = new AtomicBoolean(false);

  BundleFrameworkImpl(CompositeBundle cb)
  {
    _compositeBundle = cb;
    _framework = cb.getCompositeFramework();
    _bundles = new ArrayList<Bundle>();
  }

  @Override
  public void start() throws BundleException
  {
    _compositeBundle.getCompositeFramework().init();
    _compositeBundle.start(Bundle.START_ACTIVATION_POLICY);
    if ( _packageAdminTracker == null)
    {
      _packageAdminTracker = new ServiceTracker(_compositeBundle.getCompositeFramework().getBundleContext(),
          PackageAdmin.class.getName(), null);
      _packageAdminTracker.open();
    }
    
    // make sure inner bundles are now startable
    if (startLevelIncreased.compareAndSet(false, true)) {
        increaseStartLevel(_compositeBundle.getCompositeFramework().getBundleContext());
    }
  }
  
  @Override
  public void init() throws BundleException
  {
    if (_compositeBundle.getCompositeFramework().getState() != Framework.ACTIVE)
    {
      _compositeBundle.getCompositeFramework().start();
  
      _packageAdminTracker = new ServiceTracker(_compositeBundle.getCompositeFramework().getBundleContext(),
          PackageAdmin.class.getName(), null);
      _packageAdminTracker.open();
      
      setupStartLevelToPreventAutostart(_compositeBundle.getCompositeFramework().getBundleContext());
    }
  }
  
  /**
   * Name says it all if we don't make some adjustments bundles will be autostarted, which in the
   * grand scheme of things causes extenders to act on the inner bundles before the outer composite is even
   * resolved ...
   */
  private void setupStartLevelToPreventAutostart(BundleContext frameworkBundleContext)
  {
    ServiceReference ref = frameworkBundleContext.getServiceReference(StartLevel.class.getName());
    if (ref != null) {
      StartLevel sl = (StartLevel) frameworkBundleContext.getService(ref);
      if (sl != null) {
        // make sure new bundles are *not* automatically started (because that causes havoc)
        sl.setInitialBundleStartLevel(sl.getStartLevel()+1);
        frameworkBundleContext.ungetService(ref);
      }
    }
  }

  private void increaseStartLevel(BundleContext context) {
      /*
       * Algorithm for doing this
       * 
       * 1. Set up a framework listener that will tell us when the start level has been set.
       * 
       * 2. Change the start level. This is asynchronous so by the time the method returned the event 
       *    could have been sent. This is why we set up the listener in step 1.
       * 
       * 3. Wait until the start level has been set appropriately. At this stage all the bundles are startable
       *    and some have been started (most notably lazy activated bundles it appears). Other bundles are still
       *    in resolved state.
       */    
      
      ServiceReference ref = context.getServiceReference(StartLevel.class.getName());
      if (ref != null) {
        StartLevel sl = (StartLevel) context.getService(ref);
        if (sl != null) {

          final Semaphore waitForStartLevelChangedEventToOccur = new Semaphore(0);
          
          // step 1
          FrameworkListener listener = new FrameworkListener() {
            public void frameworkEvent(FrameworkEvent event)
            {
              if (event.getType() == FrameworkEvent.STARTLEVEL_CHANGED) {
                waitForStartLevelChangedEventToOccur.release();
              }
            }
          };
          
          context.addFrameworkListener(listener);
          
          // step 2
          sl.setStartLevel(sl.getStartLevel()+1);
          
          // step 3
          try {
            if (!!!waitForStartLevelChangedEventToOccur.tryAcquire(60, TimeUnit.SECONDS)) {
              LOGGER.debug("Starting CBA child bundles took longer than 60 seconds");
            }
          } catch (InterruptedException e) {
            // restore the interrupted status
            Thread.currentThread().interrupt();
          }
          
          context.removeFrameworkListener(listener);
        }
        context.ungetService(ref);
      }
  }
  
  public void close() throws BundleException
  {
    // close out packageadmin service tracker
    if (_packageAdminTracker != null) {
      try {
        _packageAdminTracker.close();
      } catch (IllegalStateException ise) {
        // Ignore this error because this can happen when we're trying to close the tracker on a
        // framework that has closed/is closing.
      }
    }

    // We used to call stop before uninstall but this seems to cause NPEs in equinox. It isn't
    // all the time, but I put in a change to resolution and it started NPEing all the time. This
    // was because stop caused everything to go back to the RESOLVED state, so equinox inited the
    // framework during uninstall and then tried to get the surrogate bundle, but init did not
    // create a surroage, so we got an NPE. I removed the stop and added this comment in the hope
    // that the stop doesn't get added back in. 
    _compositeBundle.uninstall();
  }

  public void start(Bundle b) throws BundleException
  {
    if (b.getState() != Bundle.ACTIVE && !isFragment(b)) 
      b.start(Bundle.START_ACTIVATION_POLICY);
  }

  public void stop(Bundle b) throws BundleException
  {
    if (!isFragment(b))
      b.stop();
  }

  public Bundle getFrameworkBundle()
  {
    return _compositeBundle;
  }

  public BundleContext getIsolatedBundleContext()
  {
    return _compositeBundle.getCompositeFramework().getBundleContext();
  }

  public List<Bundle> getBundles()
  {
    // Ensure our bundle list is refreshed
    ArrayList latestBundles = new ArrayList<Bundle>();
    for (Bundle appBundle : _framework.getBundleContext().getBundles())
    {
      for (Bundle cachedBundle : _bundles)
      {
        // Look for a matching name and version (this doesnt make it the same bundle
        // but it means we find the one we want)
        if (cachedBundle.getSymbolicName().equals(appBundle.getSymbolicName()) &&
            cachedBundle.getVersion().equals(appBundle.getVersion()))
        {
          // Now check if it has changed - the equals method will check more thoroughly
          // to ensure this is the exact bundle we cached.
          if (!cachedBundle.equals(appBundle))
            latestBundles.add(appBundle); // bundle updated
          else
            latestBundles.add(cachedBundle); // bundle has not changed
        }
      }
    }
    
    _bundles = latestBundles;
    
    return _bundles;
  }

  /**
   * This method uses the PackageAdmin service to identify if a bundle
   * is a fragment.
   * @param b
   * @return
   */
  private boolean isFragment(Bundle b)
  {
    LOGGER.debug(LOG_ENTRY, "isFragment", new Object[] { b });

    PackageAdmin admin = null;
    boolean isFragment = false;

    try {
      if (_packageAdminTracker != null) {
        admin = (PackageAdmin) _packageAdminTracker.getService();
        if (admin != null) {
          isFragment = (admin.getBundleType(b) == PackageAdmin.BUNDLE_TYPE_FRAGMENT);
        }
      }
    } catch (RuntimeException re) {
      LOGGER.debug(LOG_EXCEPTION, re);
    }

    LOGGER.debug(LOG_EXIT, "isFragment", new Object[] { Boolean.valueOf(isFragment) });

    return isFragment;
  }

  public Bundle install(BundleSuggestion suggestion, AriesApplication app) throws BundleException
  {
    Bundle installedBundle = suggestion.install(this, app);
    _bundles.add(installedBundle);
    
    return installedBundle;
  }

  public void uninstall(Bundle b) throws BundleException
  {
    b.uninstall();
    _bundles.remove(b);
  }
}

