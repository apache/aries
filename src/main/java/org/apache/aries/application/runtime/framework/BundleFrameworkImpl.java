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

import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.BundleFramework;
import org.apache.aries.application.management.BundleRepository.BundleSuggestion;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.service.framework.CompositeBundle;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

public class BundleFrameworkImpl implements BundleFramework
{
  private static final Logger LOGGER = LoggerFactory.getLogger(BundleFrameworkImpl.class);

  List<Bundle> _bundles;
  CompositeBundle _compositeBundle;

  ServiceTracker _packageAdminTracker;

  BundleFrameworkImpl(CompositeBundle cb)
  {
    _compositeBundle = cb;
    _bundles = new ArrayList<Bundle>();
  }

  public void init() throws BundleException
  {
    if (_compositeBundle.getCompositeFramework().getState() != Framework.ACTIVE)
    {
      _compositeBundle.start(Bundle.START_ACTIVATION_POLICY);
  
      _packageAdminTracker = new ServiceTracker(_compositeBundle.getBundleContext(),
          PackageAdmin.class.getName(), null);
      _packageAdminTracker.open();
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

    _compositeBundle.stop();
  }

  public void start(Bundle b) throws BundleException
  {
    if (b.getState() != Bundle.ACTIVE && !isFragment(b)) 
      b.start(Bundle.START_ACTIVATION_POLICY);
  }

  public void stop(Bundle b) throws BundleException
  {
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
    Bundle installedBundle = suggestion.install(getIsolatedBundleContext(), app);
    _bundles.add(installedBundle);
    
    return installedBundle;
  }

  public void uninstall(Bundle b) throws BundleException
  {
    b.uninstall();
    _bundles.remove(b);
    
    /* Call PackageAdmin.refreshPackages() after uninstall 
	 * to clean out a partially removed bundle. Just to be sure. 
	 */ 
    PackageAdmin admin = null;
    try {
      if (_packageAdminTracker != null) {
        admin = (PackageAdmin) _packageAdminTracker.getService();
        admin.refreshPackages(new Bundle[]{b});
      }
    } catch (RuntimeException re) {
      LOGGER.debug(LOG_EXCEPTION, re);
    }
  }
}
