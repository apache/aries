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
package org.apache.aries.tx.control.jpa.xa.impl;

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
import static org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory.USE_DRIVER;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import org.apache.aries.tx.control.jdbc.xa.connection.impl.XADataSourceMapper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.service.transaction.control.TransactionException;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class ManagedJPADataSourceSetup implements LifecycleAware,
		ServiceTrackerCustomizer<DataSourceFactory, ManagedJPAEMFLocator> {

	private final BundleContext context;
	private final String pid;
	private final Properties jdbcProperties;
	private final Map<String, Object> baseJPAProperties;
	private final Map<String, Object> providerProperties;
	
	private final ServiceTracker<DataSourceFactory, ManagedJPAEMFLocator> dsfTracker;
	private final AtomicReference<ServiceReference<DataSourceFactory>> activeDsf = new AtomicReference<>();

	public ManagedJPADataSourceSetup(BundleContext context, String pid, Properties jdbcProperties,
			Map<String, Object> baseJPAProperties, Map<String, Object> providerProperties) throws InvalidSyntaxException, ConfigurationException {
		this.context = context;
		this.pid = pid;
		this.jdbcProperties = jdbcProperties;
		this.baseJPAProperties = baseJPAProperties;
		this.providerProperties = providerProperties;

		String targetFilter = (String) providerProperties.get(ManagedServiceFactoryImpl.DSF_TARGET_FILTER);
		if (targetFilter == null) {
			String driver = (String) providerProperties.get(OSGI_JDBC_DRIVER_CLASS);
			if (driver == null) {
				ManagedServiceFactoryImpl.LOG.error("The configuration {} must specify a target filter or a JDBC driver class", pid);
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
	public ManagedJPAEMFLocator addingService(ServiceReference<DataSourceFactory> reference) {
		DataSourceFactory service = context.getService(reference);
		ManagedJPAEMFLocator toReturn;
		try {
			toReturn = new ManagedJPAEMFLocator(context, pid, 
					getJPAProperties(service), providerProperties);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		updateService(reference, toReturn);
		
		return toReturn;
	}

	private void updateService(ServiceReference<DataSourceFactory> reference, ManagedJPAEMFLocator locator) {
		boolean setDsf;
		synchronized (this) {
			setDsf = activeDsf.compareAndSet(null, reference);
		}
		try {
			if (setDsf) {
				locator.start();
			}
		} catch (Exception e) {
			ManagedServiceFactoryImpl.LOG.error("An error occurred when creating the connection provider for {}.", pid, e);
			activeDsf.compareAndSet(reference, null);
			throw new IllegalStateException("An error occurred when creating the connection provider", e);
		}
	}

	private Map<String, Object> getJPAProperties(DataSourceFactory dsf) {
		Map<String, Object> props = new HashMap<>(baseJPAProperties);
		
		DataSource unpooled;
		try {
			if (toBoolean(providerProperties, USE_DRIVER, false)) {
				throw new TransactionException("The Database must use an XA connection");
			} else {
				unpooled = new XADataSourceMapper(dsf.createXADataSource(jdbcProperties));
			}
		} catch (SQLException sqle) {
			throw new TransactionException("Unable to create the JDBC resource provider", sqle);
		}

		DataSource toUse = poolIfNecessary(providerProperties, unpooled);
		
		props.put("javax.persistence.jtaDataSource", toUse);
		
		return props;
	}
	
	@Override
	public void modifiedService(ServiceReference<DataSourceFactory> reference, ManagedJPAEMFLocator service) {
	}

	@Override
	public void removedService(ServiceReference<DataSourceFactory> reference, ManagedJPAEMFLocator service) {
		service.stop();

		if (activeDsf.compareAndSet(reference, null)) {
			Map<ServiceReference<DataSourceFactory>,ManagedJPAEMFLocator> tracked = dsfTracker.getTracked();
			if (!tracked.isEmpty()) {
				Entry<ServiceReference<DataSourceFactory>, ManagedJPAEMFLocator> e = tracked.entrySet().iterator().next();
				updateService(e.getKey(), e.getValue());
			}
		}
	}
	
	private DataSource poolIfNecessary(Map<String, Object> resourceProviderProperties, DataSource unpooled) {
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

	private boolean toBoolean(Map<String, Object> props, String key, boolean defaultValue) {
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

	private int toInt(Map<String, Object> props, String key, int defaultValue) {
		
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