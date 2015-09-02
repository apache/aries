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

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;

import org.apache.aries.subsystem.ContentHandler;
import org.apache.aries.subsystem.core.content.ConfigAdminContentHandler;
import org.apache.aries.util.filesystem.IDirectoryFinder;
import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.bundle.EventHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.service.resolver.Resolver;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The bundle activator for the this bundle. When the bundle is starting, this
 * activator will create and register the SubsystemAdmin service.
 */
public class Activator implements BundleActivator, ServiceTrackerCustomizer<Object, Object> {
    private static final Logger logger = LoggerFactory.getLogger(Activator.class);
    public static final String MODELLED_RESOURCE_MANAGER = "org.apache.aries.application.modelling.ModelledResourceManager";

    public static final String LOG_ENTRY = "Method entry: {}, args {}";
    public static final String LOG_EXIT = "Method exit: {}, returning {}";

    private static volatile Activator instance;
	public static Activator getInstance() {
	    Activator result = instance;
	    if (result == null)
            throw new IllegalStateException("The activator has not been initialized or has been shutdown");
		return result;
	}
	
	private volatile BundleContext bundleContext;
    private volatile ConfigAdminContentHandler configAdminHandler;
	private volatile Coordinator coordinator;
    private volatile Object modelledResourceManager;
    private volatile RegionDigraph regionDigraph;
	private volatile SubsystemServiceRegistrar registrar;
	private volatile Resolver resolver;
	private volatile ServiceModeller serviceModeller;
	private volatile Subsystems subsystems;
	private volatile SystemRepositoryManager systemRepositoryManager;
	
	private BundleEventHook bundleEventHook;
	private ServiceTracker<?,?> serviceTracker;

	private final Collection<IDirectoryFinder> finders = Collections.synchronizedSet(new HashSet<IDirectoryFinder>());
	private final Collection<ServiceRegistration<?>> registrations = new HashSet<ServiceRegistration<?>>();
	
	public BundleContext getBundleContext() {
		return bundleContext;
	}

	public Coordinator getCoordinator() {
		return coordinator;
	}

    public ServiceModeller getServiceModeller() {
        return serviceModeller;
    }

    public RegionDigraph getRegionDigraph() {
		return regionDigraph;
	}

	public Collection<IDirectoryFinder> getIDirectoryFinders() {
		return Collections.unmodifiableCollection(finders);
	}

	public Resolver getResolver() {
		return resolver;
	}

	public Subsystems getSubsystems() {
		return subsystems;
	}

	public SubsystemServiceRegistrar getSubsystemServiceRegistrar() {
		logger.debug(LOG_ENTRY, "getSubsystemServiceRegistrar");
		SubsystemServiceRegistrar result = registrar;
		logger.debug(LOG_EXIT, "getSubsystemServiceRegistrar", result);
		return result;
	}

	public SystemRepository getSystemRepository() {
		return systemRepositoryManager.getSystemRepository();
	}

	@Override
	public synchronized void start(BundleContext context) throws Exception {
		logger.debug(LOG_ENTRY, "start", context);
		bundleContext = context;
		serviceTracker = new ServiceTracker<Object, Object>(bundleContext, generateServiceFilter(), this);
		serviceTracker.open();
		logger.debug(LOG_EXIT, "start");
	}

	@Override
	public synchronized void stop(BundleContext context) {
		logger.debug(LOG_ENTRY, "stop", context);
		serviceTracker.close();
		serviceTracker = null;
		bundleContext = null;
		logger.debug(LOG_EXIT, "stop");
	}

	private void activate() {
		if (isActive() || !hasRequiredServices())
			return;
		synchronized (Activator.class) {
			instance = Activator.this;
		}
		subsystems = new Subsystems();
		registerBundleEventHook();
		registrations.add(bundleContext.registerService(ResolverHookFactory.class, new SubsystemResolverHookFactory(subsystems), null));
        Dictionary<String, Object> handlerProps = new Hashtable<String, Object>();
        handlerProps.put(ContentHandler.CONTENT_TYPE_PROPERTY, ConfigAdminContentHandler.CONTENT_TYPES);
        configAdminHandler = new ConfigAdminContentHandler(bundleContext);
        registrations.add(bundleContext.registerService(ContentHandler.class, configAdminHandler, handlerProps));
		registrar = new SubsystemServiceRegistrar(bundleContext);
		systemRepositoryManager = new SystemRepositoryManager(bundleContext.getBundle(0).getBundleContext());
        systemRepositoryManager.open();
		BasicSubsystem root = subsystems.getRootSubsystem();
		bundleEventHook.activate();
		root.start();
		registerWovenClassListener();
	}

	private void deactivate() {
		if (!isActive())
			return;
		bundleEventHook.deactivate();
		systemRepositoryManager.close();
		new StopAction(subsystems.getRootSubsystem(), subsystems.getRootSubsystem(), true).run();
		for (ServiceRegistration<?> registration : registrations) {
			try {
				registration.unregister();
			}
			catch (IllegalStateException e) {
				logger.debug("Service had already been unregistered", e);
			}
		}
        configAdminHandler.shutDown();
		bundleEventHook.processPendingEvents();
		synchronized (Activator.class) {
			instance = null;
		}
	}

	private <T> T findAlternateServiceFor(Class<T> service) {
		Object[] services = serviceTracker.getServices();
		if (services == null)
			return null;
		for (Object alternate : services)
			if (service.isInstance(alternate))
					return service.cast(alternate);
		return null;
	}

	private Filter generateServiceFilter() throws InvalidSyntaxException {
		return FrameworkUtil.createFilter(generateServiceFilterString());
	}

	private String generateServiceFilterString() {
		return new StringBuilder("(|(")
				.append(org.osgi.framework.Constants.OBJECTCLASS).append('=')
				.append(Coordinator.class.getName()).append(")(")
				.append(org.osgi.framework.Constants.OBJECTCLASS).append('=')
				.append(RegionDigraph.class.getName()).append(")(")
				.append(org.osgi.framework.Constants.OBJECTCLASS).append('=')
				.append(Resolver.class.getName()).append(")(")
				.append(org.osgi.framework.Constants.OBJECTCLASS).append('=')
				.append("org.osgi.service.repository.Repository").append(")(")
				.append(org.osgi.framework.Constants.OBJECTCLASS).append('=')
				.append(MODELLED_RESOURCE_MANAGER).append(")(")
				.append(org.osgi.framework.Constants.OBJECTCLASS).append('=')
				.append(IDirectoryFinder.class.getName()).append("))").toString();
	}

	private boolean hasRequiredServices() {
		return coordinator != null &&
				regionDigraph != null &&
				resolver != null;
	}

	private boolean isActive() {
		synchronized (Activator.class) {
			return instance != null && getSubsystems() != null;
		}
	}

	private void registerBundleEventHook() {
		Dictionary<String, Object> properties = new Hashtable<String, Object>(1);
		properties.put(org.osgi.framework.Constants.SERVICE_RANKING, Integer.MAX_VALUE);
		bundleEventHook = new BundleEventHook();
		registrations.add(bundleContext.registerService(EventHook.class, bundleEventHook, properties));
	}

	private void registerWovenClassListener() {
		registrations.add(
				bundleContext.registerService(
						org.osgi.framework.hooks.weaving.WovenClassListener.class,
						new WovenClassListener(bundleContext, subsystems),
						null));
	}
	
	/* Begin ServiceTrackerCustomizer methods */

	@Override
	public synchronized Object addingService(ServiceReference<Object> reference) {
		Object service = bundleContext.getService(reference);
		// Use all of each type of the following services.
		if (service instanceof IDirectoryFinder)
			finders.add((IDirectoryFinder) service);
		// Use only one of each type of the following services.
		else if (service instanceof Coordinator && coordinator == null)
			coordinator = (Coordinator) service;
		else if (service instanceof RegionDigraph && regionDigraph == null)
			regionDigraph = (RegionDigraph) service;
		else if (service instanceof Resolver && resolver == null)
			resolver = (Resolver) service;
		else {
			try {
				Class clazz = getClass().getClassLoader().loadClass(MODELLED_RESOURCE_MANAGER);
				if (clazz.isInstance(service) && serviceModeller == null) {
					modelledResourceManager = service;
					serviceModeller = new ApplicationServiceModeller(service);
				} else {
					service = null;
				}
			} catch (ClassNotFoundException e) {
				service = null;
			} catch (NoClassDefFoundError e) {
				service = null;
			}
		}
		// Activation is harmless if already active or all required services
		// have not yet been found.
		activate();
		// Filter guarantees we want to track all services received.
		return service;
	}

	@Override
	public void modifiedService(ServiceReference<Object> reference, Object service) {
		// Nothing
	}

	@Override
	public synchronized void removedService(ServiceReference<Object> reference, Object service) {
		if (service instanceof Coordinator) {
			if (service.equals(coordinator)) {
				Coordinator coordinator = findAlternateServiceFor(Coordinator.class);
				if (coordinator == null)
					deactivate();
				this.coordinator = coordinator;
			}
		}
		else if (service instanceof RegionDigraph) {
			if (service.equals(regionDigraph)) {
				RegionDigraph regionDigraph = findAlternateServiceFor(RegionDigraph.class);
				if (regionDigraph == null)
					deactivate();
				this.regionDigraph = regionDigraph;
			}
		}
		else if (service instanceof Resolver) {
			if (service.equals(resolver)) {
				Resolver resolver = findAlternateServiceFor(Resolver.class);
				if (resolver == null)
					deactivate();
				this.resolver = resolver;
			}
		}
		else if (service instanceof IDirectoryFinder)
			finders.remove(service);
        else {
            if (service.equals(modelledResourceManager)) {
                try {
                    Class clazz = getClass().getClassLoader().loadClass(MODELLED_RESOURCE_MANAGER);
                    Object manager = findAlternateServiceFor(clazz);
                    if (manager == null) {
                        modelledResourceManager = null;
                        serviceModeller = null;
                    } else {
                        modelledResourceManager = service;
                        serviceModeller = new ApplicationServiceModeller(service);
                    }
                } catch (ClassNotFoundException e) {
                    // ignore
                } catch (NoClassDefFoundError e) {
                    // ignore
                }
            }
        }
	}

	/* End ServiceTrackerCustomizer methods */
}
