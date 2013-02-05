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

import static org.apache.aries.application.utils.AppConstants.LOG_ENTRY;
import static org.apache.aries.application.utils.AppConstants.LOG_EXIT;

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;

import org.apache.aries.application.modelling.ModelledResourceManager;
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
import org.osgi.service.repository.Repository;
import org.osgi.service.resolver.Resolver;
import org.osgi.service.subsystem.SubsystemException;
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
	
	private static Activator instance;
	
	public static synchronized Activator getInstance() {
		logger.debug(LOG_ENTRY, "getInstance");
		checkInstance();
		logger.debug(LOG_EXIT, "getInstance", instance);
		return instance;
	}
	
	private static synchronized void checkInstance() {
		logger.debug(LOG_ENTRY, "checkInstance");
		if (instance == null)
			throw new IllegalStateException("The activator has not been initialized or has been shutdown");
		logger.debug(LOG_EXIT, "checkInstance");
	}
	
	// @GuardedBy("this")
	private BundleEventHook bundleEventHook;
	private volatile BundleContext bundleContext;
	private volatile Coordinator coordinator;
	private volatile ModelledResourceManager modelledResourceManager;
	private volatile SubsystemServiceRegistrar registrar;
	private volatile RegionDigraph regionDigraph;
	private volatile Resolver resolver;
	private ServiceTracker<?,?> serviceTracker;

	private volatile Subsystems subsystems;
	
	private final Collection<ServiceRegistration<?>> registrations = new HashSet<ServiceRegistration<?>>();
	private final Collection<Repository> repositories = Collections.synchronizedSet(new HashSet<Repository>());
	private final Collection<IDirectoryFinder> finders = Collections.synchronizedSet(new HashSet<IDirectoryFinder>());
	
	public BundleContext getBundleContext() {
		return bundleContext;
	}
	
	public Coordinator getCoordinator() {
		return coordinator;
	}
	
	public ModelledResourceManager getModelledResourceManager() {
		return modelledResourceManager;
	}
	
	public RegionDigraph getRegionDigraph() {
		return regionDigraph;
	}
	
	public Collection<Repository> getRepositories() {
		return Collections.unmodifiableCollection(repositories);
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
	
	public Repository getSystemRepository() {
		return new SystemRepository(getSubsystems().getRootSubsystem());
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
		try {
			subsystems = new Subsystems();
		}
		catch (SubsystemException e) {
			throw e;
		}
		catch (Exception e) {
			throw new SubsystemException(e);
		}
		registerBundleEventHook();
		registrations.add(bundleContext.registerService(ResolverHookFactory.class, new SubsystemResolverHookFactory(subsystems), null));
		registrar = new SubsystemServiceRegistrar(bundleContext);
		BasicSubsystem root = subsystems.getRootSubsystem();
		bundleEventHook.activate();
		root.start();
	}
	
	private void deactivate() {
		if (!isActive())
			return;
		bundleEventHook.deactivate();
		new StopAction(subsystems.getRootSubsystem(), subsystems.getRootSubsystem(), true).run();
		for (ServiceRegistration<?> registration : registrations) {
			try {
				registration.unregister();
			}
			catch (IllegalStateException e) {
				logger.debug("Service had already been unregistered", e);
			}
		}
		bundleEventHook.processPendingEvents();
		synchronized (Activator.class) {
			instance = null;
		}
	}
	
	private Object findAlternateServiceFor(Object service) {
		Object[] services = serviceTracker.getServices();
		if (services == null)
			return null;
		for (Object alternate : services)
			if (alternate.getClass().equals(service.getClass()))
					return alternate;
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
				.append(Repository.class.getName()).append(")(")
				.append(org.osgi.framework.Constants.OBJECTCLASS).append('=')
				.append(ModelledResourceManager.class.getName()).append(")(")
				.append(org.osgi.framework.Constants.OBJECTCLASS).append('=')
				.append(IDirectoryFinder.class.getName()).append("))").toString();
	}
	
	private boolean hasRequiredServices() {
		return coordinator != null &&
				regionDigraph != null &&
				resolver != null &&
				modelledResourceManager != null;
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
	
	/* Begin ServiceTrackerCustomizer methods */

	@Override
	public synchronized Object addingService(ServiceReference<Object> reference) {
		Object service = bundleContext.getService(reference);
		if (service instanceof Coordinator) {
			if (coordinator == null) {
				coordinator = (Coordinator)service;
				activate();
			}
		}
		else if (service instanceof RegionDigraph) {
			if (regionDigraph == null) {
				regionDigraph = (RegionDigraph)service;
				activate();
			}
		}
		else if (service instanceof Resolver) {
			if (resolver == null) {
				resolver = (Resolver)service;
				activate();
			}
		}
		else if (service instanceof ModelledResourceManager) {
			if (modelledResourceManager == null) {
				modelledResourceManager = (ModelledResourceManager)service;
				activate();
			}
		}
		else if (service instanceof IDirectoryFinder)
			finders.add((IDirectoryFinder)service);
		else
			repositories.add((Repository)service);
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
				Coordinator coordinator = (Coordinator)findAlternateServiceFor(this.coordinator);
				if (coordinator == null)
					deactivate();
				this.coordinator = coordinator;
			}
		}
		else if (service instanceof RegionDigraph) {
			if (service.equals(regionDigraph)) {
				RegionDigraph regionDigraph = (RegionDigraph)findAlternateServiceFor(this.regionDigraph);
				if (regionDigraph == null)
					deactivate();
				this.regionDigraph = regionDigraph;
			}
		}
		else if (service instanceof Resolver) {
			if (service.equals(resolver)) {
				Resolver resolver = (Resolver)findAlternateServiceFor(this.resolver);
				if (resolver == null)
					deactivate();
				this.resolver = resolver;
			}
		}
		else if (service instanceof ModelledResourceManager) {
			if (service.equals(modelledResourceManager)) {
				ModelledResourceManager modelledResourceManager = (ModelledResourceManager)findAlternateServiceFor(this.modelledResourceManager);
				if (modelledResourceManager == null)
					deactivate();
				this.modelledResourceManager = modelledResourceManager;
			}
		}
		else if (service instanceof IDirectoryFinder)
			finders.remove(service);
		else
			repositories.remove(service);
	}
	
	/* End ServiceTrackerCustomizer methods */
}
