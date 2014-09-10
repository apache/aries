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

import java.lang.IllegalStateException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.spi.InitialContextFactory;

import org.apache.aries.jndi.Utils;
import org.apache.aries.jndi.startup.Activator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jndi.JNDIConstants;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;


public class ServiceTrackerCustomizers 
{
  private static final Logger LOGGER = Logger.getLogger(ServiceTrackerCustomizers.class.getName());
  
  public static interface CachingServiceTracker extends ServiceTrackerCustomizer {
    public ServiceReference find(String identifier);
    public Object getServiceFromRef(BundleContext bCtx, ServiceReference ref);
  }
  
  private static abstract class BaseCachingServiceTracker implements CachingServiceTracker {
    /** The cached references */
    protected ConcurrentMap<String, ServiceReference> cache = new ConcurrentHashMap<String, ServiceReference>();
    /** A list of service references that are being tracked */
    protected List<ServiceReference> trackedReferences = new ArrayList<ServiceReference>();

    protected ConcurrentHashMap<BundleContext, ConcurrentHashMap<ServiceReference,Object>> ctxServiceRefServiceCache =
        new ConcurrentHashMap<BundleContext, ConcurrentHashMap<ServiceReference,Object>>();
    
    private void clearCache(Bundle b) {
      for (BundleContext bCtx : ctxServiceRefServiceCache.keySet()) {
        Bundle cacheB = null;
        try {
          cacheB = bCtx.getBundle();
        } catch (IllegalStateException ise) {
          if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("BaseCachingServiceTracker.clearCache IllegalStateException caught getting bundle on " + bCtx);
        }
        if (cacheB == null || cacheB.equals(b)) {
          Object removedObj = ctxServiceRefServiceCache.remove(bCtx);
          if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("BaseCachingServiceTracker.clearCache Removed " + removedObj);
        }
      }
    }
    
    public Object getServiceFromRef(BundleContext bCtx, ServiceReference ref) {
      if (ctxServiceRefServiceCache.get(bCtx) == null) {
        ctxServiceRefServiceCache.put(bCtx, new ConcurrentHashMap<ServiceReference,Object>());
      }
      ConcurrentHashMap<ServiceReference,Object> ctxServiceCache = ctxServiceRefServiceCache.get(bCtx);
      Object service = ctxServiceCache.get(ref);
      if (service == null) {
        service = Utils.getServicePrivileged(bCtx,ref);
        ctxServiceCache.put(ref, service);
      }
      return service;
    }
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
      //Work through the contexts to clear out this serviceref
      for (BundleContext bCtx : ctxServiceRefServiceCache.keySet())  {
        ctxServiceRefServiceCache.get(bCtx).remove(reference);
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
  
  //An empty BaseCachingServiceTracker, just to use the caching.
  public static final CachingServiceTracker ICFB_CACHE = new BaseCachingServiceTracker() {
    @Override
    protected List<String> getProperty(ServiceReference reference) {
      return new ArrayList<String>();
    }};
  
  //An empty BaseCachingServiceTracker, just to use the caching.
  public static final CachingServiceTracker URLOBJFACTORYFINDER_CACHE = new BaseCachingServiceTracker() {
    @Override
    protected List<String> getProperty(ServiceReference reference) {
      return new ArrayList<String>();
    }};
    
  
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
  
//Links between the BundleContext, the classes they have registered an interest in, and the service references from those.
  private static ConcurrentHashMap<BundleContext,ConcurrentHashMap<String,CopyOnWriteArrayList<ServiceReference>>> srCache 
            = new ConcurrentHashMap<BundleContext, ConcurrentHashMap<String,CopyOnWriteArrayList<ServiceReference>>>();
  
  //Links between references and services
  private static ConcurrentHashMap<ServiceReference, Object> refToService = new ConcurrentHashMap<ServiceReference, Object>();
  
  //Links between BundleContexts and the classes they have registered an interest in, and the ServiceTrackerCustomizers 
  //running for those classes.
  private static ConcurrentHashMap<BundleContext, ConcurrentHashMap<String,ContextServiceTrackerCustomizer>> serviceTrackerCustomizerCache 
              = new ConcurrentHashMap<BundleContext, ConcurrentHashMap<String,ContextServiceTrackerCustomizer>>();
  
  //Maintain a list of service trackers created, so they can be closed off.
  private static ConcurrentHashMap<ServiceTrackerCustomizer, ServiceTracker> serviceTrackers = new ConcurrentHashMap<ServiceTrackerCustomizer, ServiceTracker>();
  
  public static ContextServiceTrackerCustomizer getOrRegisterServiceTracker(BundleContext ctx,String clazz) {
    
    ConcurrentHashMap<String,ContextServiceTrackerCustomizer> stCacheForCtx = serviceTrackerCustomizerCache.get(ctx);
    if (stCacheForCtx == null) {
      synchronized (serviceTrackerCustomizerCache) {
        //Check the ctx is still not known, and not set by another thread.
        stCacheForCtx = serviceTrackerCustomizerCache.get(ctx);
        if (stCacheForCtx == null) {
          if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("Creating cache details for context " + ctx);
          stCacheForCtx = new ConcurrentHashMap<String, ServiceTrackerCustomizers.ContextServiceTrackerCustomizer>();
          serviceTrackerCustomizerCache.put(ctx, stCacheForCtx);
        }
      }
    }

    ContextServiceTrackerCustomizer stc = stCacheForCtx.get(clazz);
    if (stc == null) {
      synchronized (stCacheForCtx) {
        stc = stCacheForCtx.get(clazz);
        if (stc == null) {
          if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "Creating " + clazz + " tracker for " + ctx);
          final BundleContext _ctx = ctx;
          stc = new ServiceTrackerCustomizers.ContextServiceTrackerCustomizer(_ctx, clazz);
          ServiceTracker st = new ServiceTracker(
              _ctx,
              clazz,
              stc);
          serviceTrackers.put(stc, st);
          stCacheForCtx.put(clazz,stc);
          if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "Opening " + clazz + " tracker " + st);
          final ServiceTracker _st = st;
          try {
            Utils.doPrivileged(new PrivilegedExceptionAction<Object>() {
              public Object run() throws Exception {
                  _st.open(true);
                  return null;
              }            
            });
            
          } catch (Exception e) {
            if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "Exception opening " + clazz + " tracker " + e.getMessage());       
          }
        }
      }
    }
    return stc;
    
  }
  
  //Class to track services for a given context
  public static class ContextServiceTrackerCustomizer implements ServiceTrackerCustomizer {
    
    private String clazz;
    final BundleContext _callerContext;
    
    public BundleContext getCallerContext() {
      return _callerContext;
    }
    
    private ConcurrentHashMap<String,ServiceReference> classToSR = new ConcurrentHashMap<String,ServiceReference>();
    
    public ContextServiceTrackerCustomizer(BundleContext _callerContext, String clazz) {
      this._callerContext = _callerContext;
      this.clazz = clazz;
    }
    
    public Object addingService(ServiceReference reference) {
      if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "addingService: " + reference + " for context " + _callerContext + " class " + clazz);
      Object service = null;
      //Get the appropriate cache for references
      ConcurrentHashMap<String,CopyOnWriteArrayList<ServiceReference>> ctxCache = srCache.get(_callerContext);
      if(ctxCache == null) {
      if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "Creating cache for " + _callerContext);
      ctxCache = new ConcurrentHashMap<String,CopyOnWriteArrayList<ServiceReference>>();
        srCache.put(_callerContext,ctxCache);
      }
      
      if (!ctxCache.contains(clazz)) {
        if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "Creating class based cache for " + clazz);
          ctxCache.put(clazz,new CopyOnWriteArrayList<ServiceReference>());
      }
      CopyOnWriteArrayList<ServiceReference> cache = ctxCache.get(clazz);
      //Now see if the cache already has the ServiceReference (presumably it shouldn't)
      if (!cache.contains(reference)) {
        cache.add(reference);
        service = _callerContext.getService(reference);
        refToService.put(reference, service);
        if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "Storing reference in cache: " + reference + " and service " + service);
      } else {
      service = refToService.get(reference);
      if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "Reference already in cache: " + reference + " returning service " + service);
      }
      //Get a list of the classNames the service is registered under, and add to the class -> ServiceReference list
      String[] classNames = (String[]) reference.getProperty(Constants.OBJECTCLASS);
      if (classNames != null) {
        for (String cl : classNames) {
          if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("Storing link from class " + cl + " to reference " + reference);
          classToSR.put(cl, reference);
        }
      }
      //Presumably we'll always want to track this reference.
      return service;
    }
    public void modifiedService(ServiceReference reference,
        java.lang.Object service) {
      //TODO do anything here??
      if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "modifiedService: " + reference);
    }
    
    public void removedService(ServiceReference reference,
        java.lang.Object service) {
      //Unget the service to maintain the count correctly (got in addingService)
      if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "removedService: " + reference + " object: " + service);
      try {
        _callerContext.ungetService(reference);
      } catch (IllegalStateException e) {
        //Shouldn't matter that we get an IllegalStateException here.
        if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "IllegalStateException ungetting " + reference + " from " + _callerContext);
      }
      Object removedService = refToService.remove(reference);
      if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "refToService removed: " + removedService);
      //Get the appropriate cache for references
      if(srCache.containsKey(_callerContext)) {
        CopyOnWriteArrayList<ServiceReference> cache = srCache.get(_callerContext).get(clazz);
        if (cache != null && cache.contains(reference)) {
          if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "removing reference from cache");
          cache.remove(reference);
        } else {
          if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "cache did not contain reference to remove");
        }
      } else {
        if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "removedService: no cache for callerContext: " + _callerContext);
      }
    }
    
    public ServiceReference getServiceRef(String clazz) {
      ServiceReference retObj = classToSR.get(clazz);
      if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("getServiceRef: Returning " + retObj);
      return retObj;
    }
    
    
    public ServiceReference[] getServiceRefs() {
      ServiceReference[] refs = null;
      if (srCache.containsKey(_callerContext)) {
        CopyOnWriteArrayList<ServiceReference> cache = srCache.get(_callerContext).get(clazz);
        if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "Getting service refs from " + cache  + " for " + _callerContext);
        if (cache != null) {
        refs = cache.toArray(new ServiceReference[cache.size()]); 
        if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "Found refs: " + refs.length);
        } else {
          if (LOGGER.isLoggable(Level.FINE))LOGGER.log(Level.FINE, "Cache for class " + clazz + " in context " + _callerContext + " does not exist");
        }
      } else {
        if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "Cache for context " + _callerContext + " does not exist");
      }
      return refs;
    }
    
    
    public Object getService(ServiceReference ref) {
      Object obj = refToService.get(ref);
      if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "getService returning: " + obj);
      return obj; 
    }
  }
  
  public static class CacheBundleTrackerCustomizer implements BundleTrackerCustomizer {

    @Override
    public Object addingBundle(Bundle arg0, BundleEvent arg1) {
      //Request that all bundles are tracked, as even if it's not in the cache, it might be later...
      return arg0;
    }

    @Override
    public void modifiedBundle(Bundle arg0, BundleEvent arg1, Object arg2) {
      
    }

    @Override
    public void removedBundle(Bundle arg0, BundleEvent arg1, Object arg2) {
      //Work through srCache to find the bundle by matching to the context
      if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("removedBundle: Bundle " + arg0);
      for (BundleContext bCtx : srCache.keySet()) {
        Bundle cacheB = null;
        try {
          cacheB = bCtx.getBundle();
        } catch (IllegalStateException ise) {
          if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("CacheBundleTrackerCustomizer.removedBundle IllegalStateException caught getting bundle on " + bCtx);
        }
        //If we found no bundle on the context, or one that matches, then remove it.
        if (cacheB == null || cacheB.equals(arg0)) {
          if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("Found matching bundleContext " + bCtx);
          //Removing the bundle in the cache, so clear it out.
          ConcurrentHashMap<String,CopyOnWriteArrayList<ServiceReference>> classesCached = srCache.remove(bCtx);
          if (classesCached != null) {
            for (String clazz : classesCached.keySet()) {
              CopyOnWriteArrayList<ServiceReference> serviceRefs = classesCached.get(clazz);
              //Now work through the serviceRefs, and clear out the refsToService cache
              if (serviceRefs != null) {
                for (ServiceReference serviceRef : serviceRefs.toArray(new ServiceReference[serviceRefs.size()])) {
                  Object service = refToService.remove(serviceRef);
                  //Unget the service from the framework, as it won't be required now
                  if (service != null) {
                    try {
                      bCtx.ungetService(serviceRef);
                    } catch (IllegalStateException e) {
                      //Shouldn't matter that we get an IllegalStateException here.
                      if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "CacheBundleTrackerCustomizer.removedBundle IllegalStateException ungetting " + serviceRef + " from " + bCtx);
                    }
                  }
                }
              }
            }
          }
          //Remove the service tracker customizer and stop the service tracker
          ConcurrentHashMap<String,ContextServiceTrackerCustomizer> trackedClasses = serviceTrackerCustomizerCache.remove(bCtx);
          if (trackedClasses != null) {
            for (String classes : trackedClasses.keySet()) {
              ServiceTracker st = serviceTrackers.remove(trackedClasses.get(classes));
              if (st != null) {
                final ServiceTracker _st = st;
                try {
                  if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("Closing ServiceTracker " + st);
                  Utils.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    public Object run() throws Exception {
                        _st.close();
                        return null;
                    }            
                  });
                } catch (Exception e) {
                  //TODO logging?       
                }
              }
            }
          }
        }
      }
      //Remove bundle from BaseCachingServiceTrackers
      ((BaseCachingServiceTracker) ICF_CACHE).clearCache(arg0);
      ((BaseCachingServiceTracker) ICFB_CACHE).clearCache(arg0);
      ((BaseCachingServiceTracker) URL_FACTORY_CACHE).clearCache(arg0);
      ((BaseCachingServiceTracker) URLOBJFACTORYFINDER_CACHE).clearCache(arg0);
    }
  }
}
