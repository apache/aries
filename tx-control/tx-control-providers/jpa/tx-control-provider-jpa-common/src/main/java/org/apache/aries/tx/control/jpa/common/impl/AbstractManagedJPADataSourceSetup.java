/*
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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.tx.control.jpa.common.impl;

import static java.util.Collections.unmodifiableMap;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.service.jdbc.DataSourceFactory.OSGI_JDBC_DRIVER_CLASS;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.aries.tx.control.resource.common.impl.LifecycleAware;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractManagedJPADataSourceSetup implements LifecycleAware,
		ServiceTrackerCustomizer<DataSourceFactory, AbstractManagedJPAEMFLocator> {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractManagedJPADataSourceSetup.class);
	
	private final BundleContext context;
	private final String pid;
	private final Properties jdbcProperties;
	private final Map<String, Object> baseJPAProperties;
	private final Map<String, Object> providerProperties;
	
	private final ServiceTracker<DataSourceFactory, AbstractManagedJPAEMFLocator> dsfTracker;
	private final AtomicReference<ServiceReference<DataSourceFactory>> activeDsf = new AtomicReference<>();

	public AbstractManagedJPADataSourceSetup(BundleContext context, String pid, Properties jdbcProperties,
			Map<String, Object> baseJPAProperties, Map<String, Object> providerProperties) throws InvalidSyntaxException, ConfigurationException {
		this.context = context;
		this.pid = pid;
		this.jdbcProperties = jdbcProperties;
		this.baseJPAProperties = baseJPAProperties;
		this.providerProperties = providerProperties;

		String targetFilter = (String) providerProperties.get(AbstractJPAManagedServiceFactory.DSF_TARGET_FILTER);
		if (targetFilter == null) {
			String driver = (String) providerProperties.get(OSGI_JDBC_DRIVER_CLASS);
			if (driver == null) {
				LOG.error("The configuration {} must specify a target filter or a JDBC driver class", pid);
				throw new ConfigurationException(OSGI_JDBC_DRIVER_CLASS,
						"The configuration must specify either a target filter or a JDBC driver class");
			}
			targetFilter = "(" + OSGI_JDBC_DRIVER_CLASS + "=" + driver + ")";
		}

		targetFilter = "(&(" + OBJECTCLASS + "=" + DataSourceFactory.class.getName() + ")" + targetFilter + ")";

		this.dsfTracker = new ServiceTracker<>(context, context.createFilter(targetFilter), this);
	}

	public void start() {
		dsfTracker.open();
	}

	public void stop() {
		dsfTracker.close();
	}

	@Override
	public AbstractManagedJPAEMFLocator addingService(ServiceReference<DataSourceFactory> reference) {
		DataSourceFactory service = context.getService(reference);
		AbstractManagedJPAEMFLocator toReturn;
		try {
			Map<String, Object> jpaProps = decorateJPAProperties(service, 
					unmodifiableMap(providerProperties), (Properties) jdbcProperties.clone(), 
					new HashMap<>(baseJPAProperties));
			toReturn = getManagedJPAEMFLocator(context, pid, jpaProps, providerProperties, 
					() -> cleanupOnClose(jpaProps));
		} catch (Exception e) {
			LOG.error("An error occured creating the Resource provider for pid {}", pid, e);
			return null;
		}
		updateService(reference, toReturn);
		
		return toReturn;
	}

	protected abstract Map<String, Object> decorateJPAProperties(DataSourceFactory dsf, 
			Map<String, Object> providerProperties, Properties jdbcProperties,
			Map<String, Object> jpaProperties) throws Exception;
	
	protected abstract void cleanupOnClose(Map<String, Object> jpaProperties);

	protected abstract AbstractManagedJPAEMFLocator getManagedJPAEMFLocator(BundleContext context, String pid, 
			Map<String, Object> jpaProps, Map<String, Object> providerProperties, Runnable onClose) throws Exception;

	private void updateService(ServiceReference<DataSourceFactory> reference, AbstractManagedJPAEMFLocator locator) {
		boolean setDsf;
		synchronized (this) {
			setDsf = activeDsf.compareAndSet(null, reference);
		}
		try {
			if (setDsf) {
				locator.start();
			}
		} catch (Exception e) {
			LOG.error("An error occurred when creating the connection provider for {}.", pid, e);
			activeDsf.compareAndSet(reference, null);
			throw new IllegalStateException("An error occurred when creating the connection provider", e);
		}
	}

	@Override
	public void modifiedService(ServiceReference<DataSourceFactory> reference, AbstractManagedJPAEMFLocator service) {
	}

	@Override
	public void removedService(ServiceReference<DataSourceFactory> reference, AbstractManagedJPAEMFLocator service) {
		service.stop();

		if (activeDsf.compareAndSet(reference, null)) {
			Map<ServiceReference<DataSourceFactory>,AbstractManagedJPAEMFLocator> tracked = dsfTracker.getTracked();
			if (!tracked.isEmpty()) {
				Entry<ServiceReference<DataSourceFactory>, AbstractManagedJPAEMFLocator> e = tracked.entrySet().iterator().next();
				updateService(e.getKey(), e.getValue());
			}
		}
	}
}