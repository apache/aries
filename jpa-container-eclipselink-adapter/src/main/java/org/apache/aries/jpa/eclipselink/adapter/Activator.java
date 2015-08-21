/*
// * Licensed to the Apache Software Foundation (ASF) under one
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
package org.apache.aries.jpa.eclipselink.adapter;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;

/**
 * Eclipselink adapter main class.
 * 
 * The purpose of this class is to:
 * <ul>
 * <li>publish an OSGi-compatible Eclipselink {@link PersistenceProvider} service in the service registry</li>
 * <li>intercept {@link EntityManagerFactory} creation to ensure that the Eclipselink target server (if not specified otherwise)
 *     is OSGi compatible</li>
 * </ul>
 */
public class Activator implements BundleActivator, BundleListener {
    public static final String ECLIPSELINK_JPA_PROVIDER_BUNDLE_SYMBOLIC_NAME = "org.eclipse.persistence.jpa";
    public static final String ECLIPSELINK_JPA_PROVIDER_CLASS_NAME = "org.eclipse.persistence.jpa.PersistenceProvider";
    private final ConcurrentMap<Bundle, ServiceRegistration<?>> registeredProviders = new ConcurrentHashMap<Bundle, ServiceRegistration<?>>();
    
    private static final Logger logger = LoggerFactory.getLogger(Activator.class);
    
    private BundleContext context;
    
    public void start(BundleContext ctx) {
        logger.debug("Starting EclipseLink adapter");
        
        context = ctx;
        
        ctx.addBundleListener(this);
        
        for (Bundle b : ctx.getBundles()) {
            if ((b.getState() & (Bundle.ACTIVE | Bundle.STARTING | Bundle.RESOLVED | Bundle.STOPPING)) != 0) 
                handlePotentialEclipseLink(b);
        }
    }
    
    public void stop(BundleContext ctx) {
        logger.debug("Stopping EclipseLink adapter");

        for (ServiceRegistration<?> reg : registeredProviders.values()) {
          reg.unregister();
        }
      }
    
    public void bundleChanged(BundleEvent event) {
        if ((event.getType() & (BundleEvent.RESOLVED)) != 0) {
            handlePotentialEclipseLink(event.getBundle());
        } else if (event.getType() == BundleEvent.UNRESOLVED | event.getType() == BundleEvent.UNINSTALLED) {
            ServiceRegistration<?> reg = registeredProviders.remove(event.getBundle());
            if (reg != null) {
                reg.unregister();
            }
        }
    }
    
    private void handlePotentialEclipseLink(Bundle b) {
        if (b.getSymbolicName().equals(ECLIPSELINK_JPA_PROVIDER_BUNDLE_SYMBOLIC_NAME)) {
            logger.debug("Found EclipseLink bundle {}", b);
            
            // make sure we can actually find the JPA provider we expect to find
            try {
                b.loadClass(ECLIPSELINK_JPA_PROVIDER_CLASS_NAME);
            } catch (ClassNotFoundException cnfe) {
                logger.debug("Did not find provider class, exiting");
                // not one we can handle
                return;
            }
            
            if (!!!registeredProviders.containsKey(b)) {
                logger.debug("Adding new EclipseLink provider for bundle {}", b);
                
                ServiceFactory<PersistenceProvider> factory = new EclipseLinkProviderService(b);
                
                Dictionary<String, Object> props = new Hashtable<String, Object>();
                props.put("org.apache.aries.jpa.container.weaving.packages", getJPAPackages(b));
                props.put("javax.persistence.provider", ECLIPSELINK_JPA_PROVIDER_CLASS_NAME);
                            
                ServiceRegistration<?> reg = context.registerService(
                        PersistenceProvider.class.getName(), factory, props);
                
                ServiceRegistration<?> old = registeredProviders.putIfAbsent(b, reg);
                if (old != null) {
                    reg.unregister();
                }
            }
        }
    }
    
    /**
     * Get all the relevant packages that the EclipseLink JPA provider exports or persistence packages it uses itself. These are needed
     * so that the woven proxy (for runtime enhancement) can be used later on :)
     * 
     * Note that differently to OpenJPA the relevant classes are actually in more than just one bundle (org.eclipse.persistence.jpa and org.eclipse.persistence.core
     * at the time of this writing). Hence, we have to take more than just the packages of the JPA provider bundle into account ...
     * 
     * @param jpaBundle
     * @return
     */
    private String[] getJPAPackages(Bundle jpaBundle) {
        Set<String> result = new HashSet<String>();
        
        for (Bundle b : context.getBundles()) {
            BundleWiring bw = b.adapt(BundleWiring.class);
            if(bw != null) {
	            List<BundleWire> wires = bw.getProvidedWires(BundleRevision.PACKAGE_NAMESPACE);
	
	            for (BundleWire w : wires) {
	                String pkgName = (String) w.getCapability().getAttributes().get(BundleRevision.PACKAGE_NAMESPACE);
	
	                boolean add = false;
	                if (b.equals(jpaBundle)) {
	                    add = true;
	                } else if (pkgName.startsWith("org.eclipse.persistence")) {
	                    add = true;
	                }
	                
	                if (add) {
	                    String suffix = ";" + Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE + "=" + b.getSymbolicName() + ";" + Constants.BUNDLE_VERSION_ATTRIBUTE  + "=" + b.getVersion();                    
	                    result.add(pkgName + suffix);
	                }
	            }
            }
        }
        
        result.add("org.apache.aries.jpa.eclipselink.adapter.platform;" + 
                Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE + "=" + context.getBundle().getSymbolicName() + ";" + 
                Constants.BUNDLE_VERSION_ATTRIBUTE  + "=" + context.getBundle().getVersion());        
        
        logger.debug("Found JPA packages {}", result);
        
        return result.toArray(new String[0]);
    }
}
