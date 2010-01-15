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

package org.apache.aries.application.runtime.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.aries.application.management.ApplicationContext;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.BundleInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class ApplicationContextImpl implements ApplicationContext {
  
  private AriesApplication _application;
  private Map<BundleInfo, Bundle> _bundles;
  private ApplicationState _state;
  private BundleContext _bundleContext;
  
  public ApplicationContextImpl (BundleContext b, AriesApplication app) { 
    _application = app;
    _bundles = new HashMap<BundleInfo, Bundle>();
    _state = ApplicationState.INSTALLED;
  }

  public AriesApplication getApplication() {
    return _application;
  }
  
  public Set<Bundle> getApplicationContent() {
    Set<Bundle> result = new HashSet<Bundle>();
    for (Map.Entry<BundleInfo, Bundle> entry : _bundles.entrySet()) { 
      result.add (entry.getValue());
    }
    return result;
  } 

  public ApplicationState getApplicationState() {
    return _state;
  }

  public void start() throws BundleException {
    Set<BundleInfo> bundleInfo = _application.getBundleInfo();
    for (BundleInfo bi : bundleInfo) { 
      // TODO: proper synchronisation!
      if (_bundles.containsKey(bi)) { 
        Bundle b = _bundles.get(bi);
        if (b.getState() != Bundle.ACTIVE) { 
          b.start();
        }
      } else { 
        Bundle b = _bundleContext.installBundle(bi.getLocation()); 
        b.start();
        _bundles.put(bi, b);
      }
    }
    _state = ApplicationState.ACTIVE;
  }

  public void stop() throws BundleException {
    for (Map.Entry<BundleInfo, Bundle> entry : _bundles.entrySet()) { 
      Bundle b = entry.getValue();
      b.stop();
    }
    _state = ApplicationState.RESOLVED;
  }
}
