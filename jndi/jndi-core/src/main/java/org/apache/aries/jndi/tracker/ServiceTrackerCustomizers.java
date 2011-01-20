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
package org.apache.aries.jndi.tracker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.naming.spi.InitialContextFactory;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jndi.JNDIConstants;
import org.osgi.util.tracker.ServiceTrackerCustomizer;


public class ServiceTrackerCustomizers 
{
  public static interface CachingServiceTracker extends ServiceTrackerCustomizer {
    public ServiceReference find(String identifier);
  }
  
  private static abstract class BaseCachingServiceTracker implements CachingServiceTracker {
    /** The cached references */
    protected ConcurrentMap<String, ServiceReference> cache = new ConcurrentHashMap<String, ServiceReference>();
    /** A list of service references that are being tracked */
    protected List<ServiceReference> trackedReferences = new ArrayList<ServiceReference>();

    public ServiceReference find(String identifier) 
    {
      return cache.get(identifier);
    }

    public synchronized Object addingService(ServiceReference reference) 
    {
      List<String> cacheKeys = getProperty(reference);
      
      for (String key : cacheKeys) { 
        cache.putIfAbsent(key, reference);
      }
      
      trackedReferences.add(reference);
      
      return reference;
    }

    protected abstract List<String> getProperty(ServiceReference reference);

    public synchronized void removedService(ServiceReference reference, Object service) 
    {
      trackedReferences.remove(reference);
      
      List<String> keysToProcess = new ArrayList<String>(getProperty(reference));
      
      refLoop: for (ServiceReference ref : trackedReferences) {
        List<String> refInt = getProperty(ref);
        for (String interfaceName : refInt) {
          int index = keysToProcess.indexOf(interfaceName);
          if (index >= 0) {
            keysToProcess.remove(index);
            if (cache.replace(interfaceName, reference, ref)) {
              if (keysToProcess.isEmpty()) break refLoop;
            }
          }
        }
      }
      
      for (String interfaceName : keysToProcess) {
        cache.remove(interfaceName, reference);
      }
    }

    public void modifiedService(ServiceReference reference, Object service) { }
  }
  
  public static final ServiceTrackerCustomizer LAZY = new ServiceTrackerCustomizer() {
    public Object addingService(ServiceReference reference) 
    {
      return reference;
    }
    public void modifiedService(ServiceReference reference, Object service)  { }
    public void removedService(ServiceReference reference, Object service)  { }
  };

  public static final CachingServiceTracker ICF_CACHE = new BaseCachingServiceTracker() {
    public List<String> getProperty(ServiceReference ref)
    {
      String[] interfaces = (String[]) ref.getProperty(Constants.OBJECTCLASS);
      List<String> resultList = new ArrayList<String>();
      for (String interfaceName : interfaces) {
        if (!!!InitialContextFactory.class.getName().equals(interfaceName)) {
          resultList.add(interfaceName);
        }
      }
      
      return resultList;
    }
  };
  
  // TODO we should probably cope with the url.scheme property changing.
  public static final CachingServiceTracker URL_FACTORY_CACHE = new BaseCachingServiceTracker() {
    protected List<String> getProperty(ServiceReference reference) {
      Object scheme = reference.getProperty(JNDIConstants.JNDI_URLSCHEME);
      List<String> result;
      
      if (scheme instanceof String) {
        result = new ArrayList<String>();
        result.add((String) scheme);
      } else if (scheme instanceof String[]) {
        result = Arrays.asList((String[])scheme);
      } else {
        result = Collections.emptyList();
      }
      
      return result;
    }
  };
}
