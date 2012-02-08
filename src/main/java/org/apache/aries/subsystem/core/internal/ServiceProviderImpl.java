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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceProviderImpl implements ServiceProvider {
	private static final Logger logger = LoggerFactory.getLogger(ServiceProviderImpl.class);
	
	private final BundleContext bundleContext;
	private final Map<Class<?>, ServiceTracker> serviceTrackers = new HashMap<Class<?>, ServiceTracker>();
	
	private boolean shutdown;
	
	public ServiceProviderImpl(BundleContext bundleContext) {
		if (bundleContext == null)
			throw new NullPointerException();
		this.bundleContext = bundleContext;
	}

	@Override
	public <C> C getService(Class<C> clazz) {
		logger.debug(LOG_ENTRY, "getService", clazz);
		Object result = null;
		ServiceTracker serviceTracker = getServiceTracker(clazz);
		try {
			result = serviceTracker.waitForService(5000);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		logger.debug(LOG_EXIT, "getService", result);
		return (C) result;
	}

	@Override
	public <C> Collection<C> getServices(Class<C> clazz) {
		logger.debug(LOG_ENTRY, "getServices", clazz);
		ServiceTracker serviceTracker = getServiceTracker(clazz);
		Collection<C> result = Collections.emptyList();
		Object[] o = serviceTracker.getServices();
		if (o != null)
			result = (Collection<C>)Arrays.asList(o);
		logger.debug(LOG_EXIT, "getSevices", result);
		return (Collection<C>) result;
	}
	
	public void shutdown() {
		logger.debug(LOG_ENTRY, "shutdown");
		synchronized (this) {
			shutdown = true;
		}
		for (Class<?> clazz : serviceTrackers.keySet()) {
			ServiceTracker serviceTracker = serviceTrackers.get(clazz);
			logger.debug("Closing service tracker {}", clazz);
			serviceTracker.close();
		}
		logger.debug(LOG_EXIT, "shutdown");
	}
	
	private synchronized void checkShutdown() {
		logger.debug(LOG_ENTRY, "checkShutdown");
		if (shutdown == true)
			throw new IllegalStateException("This service provider has been shutdown");
		logger.debug(LOG_EXIT, "checkShutdown");
	}
	
	private synchronized ServiceTracker getServiceTracker(Class<?> clazz) {
		logger.debug(LOG_ENTRY, "getServiceTracker", clazz);
		checkShutdown();
		ServiceTracker serviceTracker = serviceTrackers.get(clazz);
		if (serviceTracker == null) {
			serviceTracker = new ServiceTracker(bundleContext, clazz.getName(), null);
			logger.debug("Opening new service tracker {}", clazz);
			serviceTracker.open();
			serviceTrackers.put(clazz, serviceTracker);
		}
		logger.debug(LOG_EXIT, "getServiceTracker", serviceTracker);
		return serviceTracker;
	}
}
