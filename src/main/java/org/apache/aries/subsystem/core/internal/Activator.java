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

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.resolver.impl.ResolverImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.service.resolver.Resolver;
import org.osgi.service.subsystem.Subsystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The bundle activator for the this bundle. When the bundle is starting, this
 * activator will create and register the SubsystemAdmin service.
 */
public class Activator implements BundleActivator {
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
	
	private final BundleListener bundleListener = new SubsystemSynchronousBundleListener();
	private final List<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();
	
	private BundleContext bundleContext;
	private SubsystemServiceRegistrar registrar;
	private AriesSubsystem root;
	private ServiceProviderImpl serviceProvider;
	
	public synchronized BundleContext getBundleContext() {
		logger.debug(LOG_ENTRY, "getBundleContext");
		BundleContext result = bundleContext;
		logger.debug(LOG_EXIT, "getBundleContext", result);
		return result;
	}
	
	public synchronized ServiceProvider getServiceProvider() {
		logger.debug(LOG_ENTRY, "getServiceProvider");
		ServiceProvider result = serviceProvider;
		logger.debug(LOG_EXIT, "getServiceProvider", result);
		return result;
	}
	
	public synchronized SubsystemServiceRegistrar getSubsystemServiceRegistrar() {
		logger.debug(LOG_ENTRY, "getSubsystemServiceRegistrar");
		SubsystemServiceRegistrar result = registrar;
		logger.debug(LOG_EXIT, "getSubsystemServiceRegistrar", result);
		return result;
	}

	@Override
	public synchronized void start(final BundleContext context) throws Exception {
		logger.debug(LOG_ENTRY, "start", context);
		synchronized (Activator.class) {
			instance = this;
		}
		bundleContext = context;
		serviceProvider = new ServiceProviderImpl(bundleContext);
		context.getBundle(0).getBundleContext().addBundleListener(bundleListener);
		registrations.add(bundleContext.registerService(ResolverHookFactory.class, new SubsystemResolverHookFactory(), null));
		// TODO The registration of the Resolver service should be temporary, unless Felix 
		// does not have an official release at the time.
		registrations.add(bundleContext.registerService(Resolver.class, new ResolverImpl(null), null));
		registrar = new SubsystemServiceRegistrar(bundleContext);
		root = new AriesSubsystem();
		root.install();
		root.start();
		logger.debug(LOG_EXIT, "start");
	}

	@Override
	public synchronized void stop(BundleContext context) /*throws Exception*/ {
		logger.debug(LOG_ENTRY, "stop", context);
		root.stop();
		registrar.unregisterAll();
		for (int i = registrations.size() - 1; i >= 0; i--) {
			try {
				registrations.get(i).unregister();
			}
			catch (IllegalStateException e) {
				logger.debug("Service had already been unregistered", e);
			}
		}
		context.getBundle(0).getBundleContext().removeBundleListener(bundleListener);
		serviceProvider.shutdown();
		synchronized (Activator.class) {
			instance = null;
		}
		logger.debug(LOG_EXIT, "stop");
	}
}
