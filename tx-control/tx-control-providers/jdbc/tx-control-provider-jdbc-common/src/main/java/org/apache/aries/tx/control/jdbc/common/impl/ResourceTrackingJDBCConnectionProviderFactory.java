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
package org.apache.aries.tx.control.jdbc.common.impl;

import java.sql.Driver;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.aries.tx.control.resource.common.impl.TrackingResourceProviderFactory;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.service.transaction.control.jdbc.JDBCConnectionProvider;
import org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory;

public class ResourceTrackingJDBCConnectionProviderFactory extends 
	TrackingResourceProviderFactory<AbstractJDBCConnectionProvider>
	implements JDBCConnectionProviderFactory {

	private final InternalJDBCConnectionProviderFactory factory;
	
	public ResourceTrackingJDBCConnectionProviderFactory(InternalJDBCConnectionProviderFactory factory) {
		this.factory = factory;
	}

	@Override
	public JDBCConnectionProvider getProviderFor(DataSourceFactory dsf, Properties jdbcProperties,
			Map<String, Object> resourceProviderProperties) {
		return doGetResult(() -> factory.getProviderFor(dsf, 
				jdbcProperties, resourceProviderProperties));
	}

	@Override
	public JDBCConnectionProvider getProviderFor(DataSource ds, Map<String, Object> resourceProviderProperties) {
		return doGetResult(() -> factory.getProviderFor(ds, 
				resourceProviderProperties));
	}

	@Override
	public JDBCConnectionProvider getProviderFor(Driver driver, Properties jdbcProperties,
			Map<String, Object> resourceProviderProperties) {
		return doGetResult(() -> factory.getProviderFor(driver, 
				jdbcProperties, resourceProviderProperties));
	}

	@Override
	public JDBCConnectionProvider getProviderFor(XADataSource ds, Map<String, Object> resourceProviderProperties) {
		return doGetResult(() -> factory.getProviderFor(ds, 
				resourceProviderProperties));
	}

	@Override
	public void releaseProvider(JDBCConnectionProvider provider) {
		if(provider instanceof AbstractJDBCConnectionProvider) {
			release((AbstractJDBCConnectionProvider)provider);
		} else {
			throw new IllegalArgumentException(
					"The supplied JDBCConnectionProvider was not created by this JDBCConnectionProviderFactory");
		}
	}
}