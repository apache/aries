/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.subsystem.core.internal;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.apache.aries.subsystem.Subsystem;
import org.apache.aries.subsystem.SubsystemConstants;
import org.apache.aries.subsystem.core.ResourceResolver;
import org.apache.aries.subsystem.spi.ResourceProcessor;
import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The bundle activator for the this bundle.
 * When the bundle is starting, this activator will create
 * and register the SubsystemAdmin service.
 */
public class Activator implements BundleActivator {
    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);
    
    static Coordinator getCoordinator() {
    	return context.getService(context.getServiceReference(Coordinator.class));
    }
    
    private static BundleContext context;
    private List<ServiceRegistration> registrations = new ArrayList<ServiceRegistration>();
    private static SubsystemEventDispatcher eventDispatcher;
    
    private ServiceTracker scopeServiceTracker;
    
    public void start(final BundleContext context) throws Exception {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("subsystem activator starting");
        }
        Activator.context = context;
        Activator.eventDispatcher = new SubsystemEventDispatcher(context);   
        register(ResourceResolver.class,
                 new NoOpResolver(),
                 DictionaryBuilder.build(Constants.SERVICE_RANKING, Integer.MIN_VALUE));
        register(ResourceResolver.class,
                new ResourceResolverImpl(context),
                DictionaryBuilder.build(Constants.SERVICE_RANKING, 0));
        register(ResourceProcessor.class,
                new BundleResourceProcessor(context),
                DictionaryBuilder.build(SubsystemConstants.SERVICE_RESOURCE_TYPE, SubsystemConstants.RESOURCE_TYPE_BUNDLE));
        register(ResourceProcessor.class,
                new SubsystemResourceProcessor(context),
                DictionaryBuilder.build(SubsystemConstants.SERVICE_RESOURCE_TYPE, SubsystemConstants.RESOURCE_TYPE_SUBSYSTEM));
        scopeServiceTracker = new ServiceTracker(
        		context,
        		RegionDigraph.class.getName(),
        		new ServiceTrackerCustomizer() {
        			private ServiceReference<?> scopeRef;
        			private ServiceRegistration<?> subsystemReg;
        			
					public synchronized Object addingService(ServiceReference reference) {
						if (subsystemReg != null) return null;
						RegionDigraph digraph = (RegionDigraph)context.getService(reference);
						if (digraph == null) return null;
						subsystemReg = context.registerService(Subsystem.class.getName(), new SubsystemServiceFactory(digraph.getRegion(context.getBundle())), null);
						return digraph;
					}

					public void modifiedService(ServiceReference reference, Object service) {
						// Do nothing.
					}

					public synchronized void removedService(ServiceReference reference, Object service) {
						if (reference != scopeRef) return;
						subsystemReg.unregister();
						subsystemReg = null;
						// TODO Look for another service?
					}
        		});
        scopeServiceTracker.open();
    }

    protected <T> void register(Class<T> clazz, T service, Dictionary props) {
         registrations.add(context.registerService(clazz.getName(), service, props));
    }

    protected <T> void register(Class<T> clazz, ServiceFactory factory, Dictionary props) {
         registrations.add(context.registerService(clazz.getName(), factory, props));
    }

    public void stop(BundleContext context) throws Exception {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("subsystem activator stopping");
        }
        scopeServiceTracker.close();
        for (ServiceRegistration<?> r : registrations) {
            try {
                r.unregister();
            } catch (Exception e) {
                LOGGER.warn("Subsystem Activator shut down", e);
            }
        }
        eventDispatcher.destroy();
    }
    
    public static BundleContext getBundleContext() {
        return context;
    }
    
    public static SubsystemEventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }
}
