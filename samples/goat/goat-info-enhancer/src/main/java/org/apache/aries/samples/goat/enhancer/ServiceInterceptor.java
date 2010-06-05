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

package org.apache.aries.samples.goat.enhancer;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.aries.samples.goat.api.ModelInfoService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class ServiceInterceptor implements ServiceListener {

	private static final String DISPLAY_NAME = "displayName";
	/**
   * 
   */
	public static final String SERVICE_ID = "service.id";
	private final BundleContext ctx;
	private final Map<String, ServiceRegistration> registrations = new HashMap<String, ServiceRegistration>();

	public ServiceInterceptor(BundleContext ctx) {
		this.ctx = ctx;
		// Check all the existing services
		try {
			// Handle any existing services
			ServiceReference[] references = ctx.getAllServiceReferences(
					ModelInfoService.class.getName(), null);

			for (ServiceReference reference : references) {
				registerServiceEnhancer(reference);
			}

			ctx.addServiceListener(this, "(objectclass='"
					+ ModelInfoService.class.getName() + "')");

		} catch (InvalidSyntaxException e) {
			e.printStackTrace();
		}
		// We could listen for find events and mask the original services if we
		// wanted to
		// ServiceRegistration findRegistration =
		// ctx.registerService(FindHook.class.getName(),
		// new InterceptorFindHook(), null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.
	 * ServiceEvent)
	 */
	@Override
	public void serviceChanged(ServiceEvent event) {
		ServiceReference reference = event.getServiceReference();
		if (event != null && event.getType() == ServiceEvent.REGISTERED) {
			registerServiceEnhancer(reference);

		} else if (event != null
				&& event.getType() == ServiceEvent.UNREGISTERING) {
			// Better unregister our enhancer
			Object id = reference.getProperty(SERVICE_ID);
			ServiceRegistration registration = registrations.get(id);
			if (registration != null) {
				registration.unregister();
				registrations.remove(id);
			}
		}

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void registerServiceEnhancer(ServiceReference reference) {
		Object actualService = ctx.getService(reference);

		if (actualService instanceof ModelInfoService) {
			ModelInfoService infoService = (ModelInfoService) actualService;
			Object serviceId = reference.getProperty(SERVICE_ID);
			Object enhancer = new ModelInfoEnhancerService(infoService);
			Dictionary properties = new Hashtable();
			Object originalDisplayName = reference.getProperty(DISPLAY_NAME);
			properties.put(DISPLAY_NAME, originalDisplayName + " [enhanced]");
			ServiceRegistration registration = ctx.registerService(
					ModelInfoService.class.getName(), enhancer, properties);
			registrations.put(serviceId + "", registration);
		} else {
			System.out.println("Oh dear - unexpected service "
					+ actualService.getClass());
		}
	}

	/**
   * 
   */
	public void stop() {
		for (ServiceRegistration registration : registrations.values()) {
			registration.unregister();
		}

	}

}
