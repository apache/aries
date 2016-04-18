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
package org.apache.aries.jpa.container.context.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.aries.jpa.container.context.JTAPersistenceContextManager;
import org.apache.aries.jpa.container.context.PersistenceContextProvider;
import org.apache.aries.jpa.container.context.transaction.impl.DestroyCallback;
import org.apache.aries.jpa.container.context.transaction.impl.JTAPersistenceContextRegistry;
import org.apache.aries.util.AriesFrameworkUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that coordinates PersistenceContextManagers across multiple (nested) OSGi frameworks.
 */
public class GlobalPersistenceManager implements PersistenceContextProvider, SynchronousBundleListener, BundleActivator {
  /** The QuiesceParticipant implementation class name */
  private static final String QUIESCE_PARTICIPANT_CLASS = "org.apache.aries.quiesce.participant.QuiesceParticipant";

  /** Logger */
  private static final Logger _logger = LoggerFactory.getLogger("org.apache.aries.jpa.container.context");
  
  private JTAPersistenceContextRegistry registry;
  private ServiceRegistration registryReg;
  
  /** 
   *    The list of persistence context managers. Each is valid for exactly one framework
   *    as identified by the respective system bundle. This allows us to work properly in 
   *    a multi (nested) framework environment without using the (deprecated) CompositeBundle API.
   */
  private Map<Bundle, PersistenceContextManager> managers = 
    new HashMap<Bundle, PersistenceContextManager>();
  
  /**
   *  This Map relates persistence context clients to persistence contexts. A bundle may be
   *  a client of more than one persistence context. 
   */
  private Map<Bundle, Set<String>> persistenceContexts = 
    new HashMap<Bundle, Set<String>>();

  /** The registration for the quiesce participant */
  private ServiceRegistration quiesceReg;
  /** A callback to tidy up the quiesceParticipant */
  private DestroyCallback quiesceTidyUp;
  /** The registration for the persistence context manager */
  private ServiceRegistration pcpReg;
  /** Our bundle */
  private Bundle bundle;
  
  // Used to track the the region context bundles provided by subsystems so the
  // appropriate persistence context manager relationships are maintained.
  private BundleTracker subsystemContextBundleTracker;
   
  public void registerContext(String unitName, Bundle client, HashMap<String, Object> properties) {
    if (_logger.isDebugEnabled()) {
      _logger.debug("Registering bundle {} as a client of persistence unit {} with properties {}.", 
          new Object[] {client.getSymbolicName() + "_" + client.getVersion(), unitName, properties});
    }
    
    if(!!!registry.jtaIntegrationAvailable())
      _logger.warn(NLS.MESSAGES.getMessage("no.tran.manager.for.bundle", unitName, client.getSymbolicName(), client.getVersion()));
    
    //Find the framework for this bundle (we may be in a composite)
    Bundle frameworkBundle = findFrameworkBundle(client);
    PersistenceContextManager manager = null;
    boolean newManager = false;
    
    //Synchronize to update internal state atomically
    synchronized (this) {
      //If we already have a manager use it
      if (managers.containsKey(frameworkBundle)) {
        manager = managers.get(frameworkBundle);
      } else {
        if (_logger.isDebugEnabled()) {
          _logger.debug("No existing manager for the framework with identity hash code {}. Creating a new one.",
              new Object[] {System.identityHashCode(frameworkBundle)});
        }
        manager = new PersistenceContextManager(frameworkBundle.getBundleContext(), registry);
        managers.put(frameworkBundle, manager);
        //Remember to listen to this new framework so that we know when bundles are starting/stopping
        frameworkBundle.getBundleContext().addBundleListener(this);
        
        newManager = true;
      }
      
      //Register the new bundle as a client
      if (!persistenceContexts.containsKey(client)) {
        persistenceContexts.put(client, new HashSet<String>());
      }
      
      persistenceContexts.get(client).add(unitName);
    }
    //Remember to start the manager if it was new. This MUST occur outside the synchronized block.
    if (newManager)
      manager.open();
    
    manager.registerContext(unitName, client, properties);
  }

  /**
   * This method is used to track the lifecycle of bundles inside composites
   */
  public void bundleChanged(BundleEvent event) {
    Bundle bundle = event.getBundle();
    
    //We only care about bundles stopping
    if (event.getType() == BundleEvent.STOPPING) {
      Set<String> contextsToBeRemoved = new HashSet<String>();
      Bundle frameworkBundle = findFrameworkBundle(bundle);
      PersistenceContextManager manager = null;
      boolean removeManager = false;
      
      //Synchronize to update internal state atomically
      synchronized (this) {
        if (persistenceContexts.containsKey(bundle)) {
          //This is a client, find the contexts to remove
          contextsToBeRemoved = persistenceContexts.remove(bundle);
          
          if (_logger.isDebugEnabled()) {
            _logger.debug("The bundle {} in framework {}, which is a client of the persistence contexts {} is stopping.",
                new Object[] {bundle.getSymbolicName() + "_" + bundle.getVersion(), 
                System.identityHashCode(frameworkBundle), contextsToBeRemoved});
          }
          
          manager = managers.get(frameworkBundle);
          if (manager == null) {
              _logger.error(NLS.MESSAGES.getMessage("no.context.manager.for.framework", frameworkBundle.getSymbolicName(), frameworkBundle.getVersion()));
            throw new IllegalStateException();
          }
        } else if (managers.containsKey(bundle)) {
          //The framework is stopping, tidy up the manager
          if (_logger.isDebugEnabled()) {
            _logger.debug("The framework {} is stopping.",
                new Object[] {bundle.getSymbolicName() + "_" + bundle.getVersion(), 
                System.identityHashCode(frameworkBundle)});
          }
          removeManager = true;
          manager = managers.remove(bundle);
          bundle.getBundleContext().removeBundleListener(this);
          
          for (Bundle b : bundle.getBundleContext().getBundles()) {
              if (persistenceContexts.containsKey(b)) {
                contextsToBeRemoved.addAll(persistenceContexts.remove(b));
              }
            }
        }
      }
      
      if (removeManager) {
        manager.close();
      } else {
        for (String context : contextsToBeRemoved) {
          manager.unregisterContext(context, bundle);
        }
      }
    }
  }

  /**
   * Quiesce the supplied bundle
   * @param bundleToQuiesce
   * @param callback
   */
  public void quiesceBundle(Bundle bundleToQuiesce, final DestroyCallback callback) {
    //If this is our bundle then get all the managers to clean up everything
    if(bundleToQuiesce == bundle) {
      AriesFrameworkUtil.safeUnregisterService(pcpReg);
      pcpReg = null;
      final Collection<Entry<Bundle, PersistenceContextManager>> mgrs = new ArrayList<Entry<Bundle, PersistenceContextManager>>();
      synchronized (this) {
        mgrs.addAll(managers.entrySet());
      }

      if(managers.isEmpty())
        callback.callback();
      else {
        final GlobalPersistenceManager gpm = this;
        //A special callback to say we're done when everyone has replied!
        DestroyCallback destroy = new DestroyCallback() {
          int count = mgrs.size();
          public void callback() {
            count--;
            if(count == 0) {
              for(Entry<Bundle, PersistenceContextManager> entry : mgrs){
                BundleContext ctx = entry.getKey().getBundleContext();
                if(ctx != null) {
                  ctx.removeBundleListener(gpm);
                }
              }
              callback.callback();
            }
          }
        };
        for (Entry<Bundle, PersistenceContextManager> entry : mgrs)
          entry.getValue().quiesceAllUnits(destroy);
      }
    } else {
      //Just quiesce the one bundle
      Bundle frameworkBundle = bundleToQuiesce.getBundleContext().getBundle(0);
      PersistenceContextManager mgr = managers.get(frameworkBundle);
    
      //If there is no manager for this framework, then we aren't managing the bundle
      //and can stop now
      if(mgr == null) {
        callback.callback();
      } else {
        mgr.quiesceUnits(bundleToQuiesce, callback);
      }
    }
  }

  public void start(BundleContext context) throws Exception {
    
    bundle = context.getBundle();
    registry = new JTAPersistenceContextRegistry(context);
    
    // Create and open the subsystem bundle context tracker before registering
    // as a PersistenceContextProvider service.
    subsystemContextBundleTracker = createSubsystemContextBundleTracker();
    subsystemContextBundleTracker.open();
    
    //Register our service
    pcpReg = context.registerService(PersistenceContextProvider.class.getName(), this, null);
    registryReg = context.registerService(JTAPersistenceContextManager.class.getName(), registry, null);
    try{
      context.getBundle().loadClass(QUIESCE_PARTICIPANT_CLASS);
      //Class was loaded, register
      quiesceTidyUp = new QuiesceParticipantImpl(this);
      quiesceReg = context.registerService(QUIESCE_PARTICIPANT_CLASS,
          quiesceTidyUp, null);
    } catch (ClassNotFoundException e) {
      _logger.info(NLS.MESSAGES.getMessage("quiesce.manager.not.there"));
    }
  }

  public void stop(BundleContext context) throws Exception {
    //Clean up
    AriesFrameworkUtil.safeUnregisterService(pcpReg);
    AriesFrameworkUtil.safeUnregisterService(registryReg);
    AriesFrameworkUtil.safeUnregisterService(quiesceReg);
    if(quiesceTidyUp != null)
      quiesceTidyUp.callback();
    Collection<PersistenceContextManager> mgrs = new ArrayList<PersistenceContextManager>();
    synchronized (persistenceContexts) {
      mgrs.addAll(managers.values());
    }
    
    for(PersistenceContextManager mgr : mgrs)
      mgr.close();
    
    subsystemContextBundleTracker.close();
    
    registry.close();
  }
  
  public Bundle getBundle() {
    return bundle;
  }
  
  private BundleTracker createSubsystemContextBundleTracker() {
	  BundleContext context = getSystemBundleContext();
	  BundleTrackerCustomizer customizer = createSubsystemContextBundleTrackerCustomizer();
	  return new BundleTracker(context, Bundle.ACTIVE, customizer);
  }
  
  private BundleContext getSystemBundleContext() {
	  // This assumes the JPA bundle has visibility to the system bundle, which
	  // may not be true. Ideally the system bundle should be gotten by
	  // location, but the method is not available in the OSGi version used by
	  // JPA.
	  return bundle.getBundleContext().getBundle(0).getBundleContext();
  }
  
  private BundleTrackerCustomizer createSubsystemContextBundleTrackerCustomizer() {
	  // Assume only the ACTIVE state is being tracked.
	  return new BundleTrackerCustomizer() {
		public Bundle addingBundle(Bundle bundle, BundleEvent event) {
		        String symName = bundle.getSymbolicName(); 
			// Only track the region context bundles of subsystems.
			if (symName != null && symName.startsWith("org.osgi.service.subsystem.region.context."))
				return bundle;
			return null;
		}

		public void modifiedBundle(Bundle bundle, BundleEvent event,
				Object object) {
			// Nothing.
		}

		public void removedBundle(Bundle bundle, BundleEvent event,
				Object object) {
			// Nothing.
		}
	  };
  }
  
  /*
   * Find the bundle with which to associate the persistence context manager.
   * For subsystems, this will be the region context bundle of the subsystem of
   * which the client is a constituent (which might be the root subsystem in
   * which case the system bundle of the framework is returned). For composite 
   * bundles, this will be the system bundle of the composite bundle framework. 
   * Otherwise, this will be the system bundle of the framework.
   */
  private Bundle findFrameworkBundle(Bundle client) {
	  BundleContext clientBundleContext = client.getBundleContext();
	  Bundle[] tracked = subsystemContextBundleTracker.getBundles();
	  if (tracked != null)
		  for (Object subsystemContextBundle : tracked)
			  if (clientBundleContext.getBundle(((Bundle)subsystemContextBundle).getBundleId()) != null)
				  // If the subsystem context bundle is visible to the client bundle,
				  // then the client is a constituent of the subsystem.
				  return (Bundle)subsystemContextBundle;
	  return clientBundleContext.getBundle(0);
  }
}
