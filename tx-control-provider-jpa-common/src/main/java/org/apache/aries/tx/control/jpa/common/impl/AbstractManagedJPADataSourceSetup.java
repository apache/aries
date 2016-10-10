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
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.service.jdbc.DataSourceFactory.OSGI_JDBC_DRIVER_CLASS;
import static org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory.CONNECTION_LIFETIME;
import static org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory.CONNECTION_POOLING_ENABLED;
import static org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory.CONNECTION_TIMEOUT;
import static org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory.IDLE_TIMEOUT;
import static org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory.MAX_CONNECTIONS;
import static org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory.MIN_CONNECTIONS;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

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
	
	protected DataSource poolIfNecessary(Map<String, Object> resourceProviderProperties, DataSource unpooled) {
		DataSource toUse;

		if (toBoolean(resourceProviderProperties, CONNECTION_POOLING_ENABLED, true)) {
			HikariConfig hcfg = new HikariConfig();
			hcfg.setDataSource(unpooled);

			// Sizes
			hcfg.setMaximumPoolSize(toInt(resourceProviderProperties, MAX_CONNECTIONS, 10));
			hcfg.setMinimumIdle(toInt(resourceProviderProperties, MIN_CONNECTIONS, 10));

			// Timeouts
			hcfg.setConnectionTimeout(toLong(resourceProviderProperties, CONNECTION_TIMEOUT, SECONDS.toMillis(30)));
			hcfg.setIdleTimeout(toLong(resourceProviderProperties, IDLE_TIMEOUT, TimeUnit.MINUTES.toMillis(3)));
			hcfg.setMaxLifetime(toLong(resourceProviderProperties, CONNECTION_LIFETIME, HOURS.toMillis(3)));

			toUse = new HikariDataSource(hcfg);

		} else {
			toUse = unpooled;
		}
		return toUse;
	}

	protected boolean toBoolean(Map<String, Object> props, String key, boolean defaultValue) {
		Object o =  ofNullable(props)
			.map(m -> m.get(key))
			.orElse(defaultValue);
		
		if (o instanceof Boolean) {
			return ((Boolean) o).booleanValue();
		} else if(o instanceof String) {
			return Boolean.parseBoolean((String) o);
		} else {
			throw new IllegalArgumentException("The property " + key + " cannot be converted to a boolean");
		}
	}

	protected int toInt(Map<String, Object> props, String key, int defaultValue) {
		
		Object o =  ofNullable(props)
				.map(m -> m.get(key))
				.orElse(defaultValue);
		
		if (o instanceof Number) {
			return ((Number) o).intValue();
		} else if(o instanceof String) {
			return Integer.parseInt((String) o);
		} else {
			throw new IllegalArgumentException("The property " + key + " cannot be converted to an int");
		}
	}

	private long toLong(Map<String, Object> props, String key, long defaultValue) {
		
		Object o =  ofNullable(props)
				.map(m -> m.get(key))
				.orElse(defaultValue);
		
		if (o instanceof Number) {
			return ((Number) o).longValue();
		} else if(o instanceof String) {
			return Long.parseLong((String) o);
		} else {
			throw new IllegalArgumentException("The property " + key + " cannot be converted to a long");
		}
	}

}