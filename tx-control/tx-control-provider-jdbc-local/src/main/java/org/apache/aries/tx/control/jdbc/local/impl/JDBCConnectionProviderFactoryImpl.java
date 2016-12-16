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
package org.apache.aries.tx.control.jdbc.local.impl;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.osgi.service.jdbc.DataSourceFactory.JDBC_URL;
import static org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory.CONNECTION_LIFETIME;
import static org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory.CONNECTION_POOLING_ENABLED;
import static org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory.CONNECTION_TIMEOUT;
import static org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory.IDLE_TIMEOUT;
import static org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory.LOCAL_ENLISTMENT_ENABLED;
import static org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory.MAX_CONNECTIONS;
import static org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory.MIN_CONNECTIONS;
import static org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory.USE_DRIVER;
import static org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory.XA_ENLISTMENT_ENABLED;

import java.sql.Driver;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.aries.tx.control.jdbc.common.impl.AbstractJDBCConnectionProvider;
import org.apache.aries.tx.control.jdbc.common.impl.DriverDataSource;
import org.apache.aries.tx.control.jdbc.common.impl.InternalJDBCConnectionProviderFactory;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.service.transaction.control.TransactionException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class JDBCConnectionProviderFactoryImpl implements InternalJDBCConnectionProviderFactory {

	@Override
	public AbstractJDBCConnectionProvider getProviderFor(DataSourceFactory dsf, Properties jdbcProperties,
			Map<String, Object> resourceProviderProperties) {

		checkEnlistment(resourceProviderProperties);

		DataSource unpooled;
		try {
			if (toBoolean(resourceProviderProperties, USE_DRIVER, false)) {
				unpooled = new DriverDataSource(dsf.createDriver(null), jdbcProperties.getProperty(JDBC_URL),
						jdbcProperties);
			} else {
				unpooled = dsf.createDataSource(jdbcProperties);
			}
		} catch (SQLException sqle) {
			throw new TransactionException("Unable to create the JDBC resource provider", sqle);
		}

		DataSource toUse = poolIfNecessary(resourceProviderProperties, unpooled);

		return new JDBCConnectionProviderImpl(toUse);
	}

	@Override
	public AbstractJDBCConnectionProvider getProviderFor(DataSource ds, Map<String, Object> resourceProviderProperties) {
		checkEnlistment(resourceProviderProperties);
		DataSource toUse = poolIfNecessary(resourceProviderProperties, ds);

		return new JDBCConnectionProviderImpl(toUse);
	}

	@Override
	public AbstractJDBCConnectionProvider getProviderFor(Driver driver, Properties jdbcProperties, 
			Map<String, Object> resourceProviderProperties) {
		checkEnlistment(resourceProviderProperties);
		DataSource toUse = poolIfNecessary(resourceProviderProperties, 
				new DriverDataSource(driver, jdbcProperties.getProperty(JDBC_URL), jdbcProperties));
		
		return new JDBCConnectionProviderImpl(toUse);
	}

	@Override
	public AbstractJDBCConnectionProvider getProviderFor(XADataSource ds, Map<String, Object> resourceProviderProperties) {
		checkEnlistment(resourceProviderProperties);
		
		DataSource unpooled;
		
		if(ds instanceof DataSource) {
			unpooled = (DataSource) ds;
		} else {
			throw new TransactionException("This resource Provider does not support XA transactions, and the supplied XADataSource is not a DataSource");
		}
		
		return new JDBCConnectionProviderImpl(poolIfNecessary(resourceProviderProperties, unpooled));
	}

	private void checkEnlistment(Map<String, Object> resourceProviderProperties) {
		if (toBoolean(resourceProviderProperties, XA_ENLISTMENT_ENABLED, false)) {
			throw new TransactionException("This Resource Provider does not support XA transactions");
		} else if (!toBoolean(resourceProviderProperties, LOCAL_ENLISTMENT_ENABLED, true)) {
			throw new TransactionException(
					"This Resource Provider always enlists in local transactions as it does not support XA");
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
