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
package org.apache.aries.tx.control.jpa.local.impl;

import static org.apache.aries.tx.control.jpa.common.impl.JPADataSourceHelper.poolIfNecessary;
import static org.apache.aries.tx.control.jpa.common.impl.JPADataSourceHelper.toBoolean;
import static org.osgi.service.jdbc.DataSourceFactory.JDBC_URL;
import static org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory.USE_DRIVER;

import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.aries.tx.control.jpa.common.impl.AbstractManagedJPADataSourceSetup;
import org.apache.aries.tx.control.jpa.common.impl.AbstractManagedJPAEMFLocator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.service.transaction.control.TransactionException;

import com.zaxxer.hikari.HikariDataSource;

public class LocalJPADataSourceSetup extends AbstractManagedJPADataSourceSetup {

	private static final String JAVAX_PERSISTENCE_NON_JTA_DATA_SOURCE = "javax.persistence.nonJtaDataSource";

	public LocalJPADataSourceSetup(BundleContext context, String pid, Properties jdbcProperties,
			Map<String, Object> baseJPAProperties, Map<String, Object> providerProperties) throws InvalidSyntaxException, ConfigurationException {
		super(context, pid, jdbcProperties, baseJPAProperties, providerProperties);
	}

	@Override
	protected Map<String, Object> decorateJPAProperties(DataSourceFactory dsf, 
			Map<String, Object> providerProperties, Properties jdbcProperties, 
			Map<String, Object> jpaProperties) {
		DataSource unpooled;
		try {
			if (toBoolean(providerProperties, USE_DRIVER, false)) {
				unpooled = new DriverDataSource(dsf.createDriver(null), jdbcProperties.getProperty(JDBC_URL),
						jdbcProperties);
			} else {
				unpooled = dsf.createDataSource(jdbcProperties);
			}
		} catch (SQLException sqle) {
			throw new TransactionException("Unable to create the JDBC resource provider", sqle);
		}

		DataSource toUse = poolIfNecessary(providerProperties, unpooled);
		
		jpaProperties.put(JAVAX_PERSISTENCE_NON_JTA_DATA_SOURCE, toUse);
		
		return jpaProperties;
	}

	@Override
	protected void cleanupOnClose(Map<String, Object> jpaProperties) {
		Object o = jpaProperties.get(JAVAX_PERSISTENCE_NON_JTA_DATA_SOURCE);
		if (o instanceof HikariDataSource) {
			((HikariDataSource)o).close();
		}
	}

	@Override
	protected AbstractManagedJPAEMFLocator getManagedJPAEMFLocator(BundleContext context, String pid,
			Map<String, Object> jpaProps, Map<String, Object> providerProperties, Runnable onClose) 
					throws InvalidSyntaxException, ConfigurationException {
		return new LocalJPAEMFLocator(context, pid, jpaProps, providerProperties, onClose);
	}
}