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
package org.apache.aries.tx.control.jdbc.xa.impl;

import static java.util.Optional.ofNullable;
import static org.osgi.service.jdbc.DataSourceFactory.JDBC_URL;
import static org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory.LOCAL_ENLISTMENT_ENABLED;
import static org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory.OSGI_RECOVERY_IDENTIFIER;
import static org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory.USE_DRIVER;
import static org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory.XA_ENLISTMENT_ENABLED;

import java.sql.Driver;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.aries.tx.control.jdbc.common.impl.AbstractInternalJDBCConnectionProviderFactory;
import org.apache.aries.tx.control.jdbc.common.impl.AbstractJDBCConnectionProvider;
import org.apache.aries.tx.control.jdbc.common.impl.DriverDataSource;
import org.apache.aries.tx.control.jdbc.xa.connection.impl.XADataSourceMapper;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.service.transaction.control.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JDBCConnectionProviderFactoryImpl extends AbstractInternalJDBCConnectionProviderFactory {

	private static final Logger LOG = LoggerFactory.getLogger(ManagedServiceFactoryImpl.class);
	
	@Override
	public JDBCConnectionProviderImpl getProviderFor(DataSourceFactory dsf, Properties jdbcProperties,
			Map<String, Object> resourceProviderProperties) {

		boolean xaEnabled = toBoolean(resourceProviderProperties, XA_ENLISTMENT_ENABLED, true);
		boolean localEnabled = toBoolean(resourceProviderProperties, LOCAL_ENLISTMENT_ENABLED, true);
		boolean useDriver = toBoolean(resourceProviderProperties, USE_DRIVER, false);
		
		checkEnlistment(xaEnabled, localEnabled, !useDriver);

		DataSource unpooled;
		try {
			if (useDriver) {
				unpooled = new DriverDataSource(dsf.createDriver(null), jdbcProperties.getProperty(JDBC_URL),
						jdbcProperties);
			} else if (xaEnabled) {
				unpooled = new XADataSourceMapper(dsf.createXADataSource(jdbcProperties));
			} else {
				unpooled = dsf.createDataSource(jdbcProperties);
			}
		} catch (SQLException sqle) {
			throw new TransactionException("Unable to create the JDBC resource provider", sqle);
		}

		DataSource toUse = poolIfNecessary(resourceProviderProperties, unpooled);
		
		return new JDBCConnectionProviderImpl(toUse, xaEnabled, localEnabled, 
				getRecoveryId(resourceProviderProperties, xaEnabled));
	}

	private String getRecoveryId(Map<String, Object> resourceProviderProps, boolean xaEnabled) {
		String recoveryIdentifier = ofNullable(resourceProviderProps)
										.map(m -> m.get(OSGI_RECOVERY_IDENTIFIER))
										.map(String::valueOf)
										.orElse(null);
		
		if(recoveryIdentifier != null && !xaEnabled) {
			LOG.warn("A recovery identifier {} has been declared, but the JDBCConnectionProvider is configured to disable XA", recoveryIdentifier);
		}
		return recoveryIdentifier;
	}

	@Override
	public AbstractJDBCConnectionProvider getProviderFor(DataSource ds, Map<String, Object> resourceProviderProperties) {
		boolean xaEnabled = toBoolean(resourceProviderProperties, XA_ENLISTMENT_ENABLED, true);
		boolean localEnabled = toBoolean(resourceProviderProperties, LOCAL_ENLISTMENT_ENABLED, true);
		
		try {
			checkEnlistment(xaEnabled, localEnabled, ds.isWrapperFor(XADataSource.class));
			DataSource toUse = poolIfNecessary(resourceProviderProperties, xaEnabled ?
					new XADataSourceMapper(ds.unwrap(XADataSource.class)) : ds);
	
			return new JDBCConnectionProviderImpl(toUse, xaEnabled, localEnabled, 
					getRecoveryId(resourceProviderProperties, xaEnabled));
		} catch (SQLException sqle) {
			throw new TransactionException("Unable to create the JDBC resource provider", sqle);
		}
	}

	@Override
	public AbstractJDBCConnectionProvider getProviderFor(Driver driver, Properties jdbcProperties, 
			Map<String, Object> resourceProviderProperties) {
		
		boolean xaEnabled = toBoolean(resourceProviderProperties, XA_ENLISTMENT_ENABLED, false);
		boolean localEnabled = toBoolean(resourceProviderProperties, LOCAL_ENLISTMENT_ENABLED, true);
		
		checkEnlistment(xaEnabled, localEnabled, false);
		
		DataSource toUse = poolIfNecessary(resourceProviderProperties, 
				new DriverDataSource(driver, jdbcProperties.getProperty(JDBC_URL), jdbcProperties));
		
		return new JDBCConnectionProviderImpl(toUse, xaEnabled, localEnabled, 
				getRecoveryId(resourceProviderProperties, xaEnabled));
	}

	@Override
	public AbstractJDBCConnectionProvider getProviderFor(XADataSource ds, Map<String, Object> resourceProviderProperties) {
		
		boolean xaEnabled = toBoolean(resourceProviderProperties, XA_ENLISTMENT_ENABLED, true);
		boolean localEnabled = toBoolean(resourceProviderProperties, LOCAL_ENLISTMENT_ENABLED, true);
		
		checkEnlistment(xaEnabled, localEnabled, true);
		
		DataSource unpooled = new XADataSourceMapper(ds);
		
		return new JDBCConnectionProviderImpl(poolIfNecessary(resourceProviderProperties, unpooled),
				xaEnabled, localEnabled, getRecoveryId(resourceProviderProperties, xaEnabled));
	}

	private void checkEnlistment(boolean xaEnabled, boolean localEnabled, boolean isXA) {
		
		if (!xaEnabled && !localEnabled) {
			throw new TransactionException("The configuration supports neither local nor XA transactions");
		} 
		
		if(xaEnabled && !isXA) {
			throw new TransactionException("The configuration is XA enabled but the resource is not suitable for XA enlistment");
		}
	}
}
