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
package org.apache.aries.samples.osgijdbc.derby;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource;
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.apache.derby.jdbc.EmbeddedDriver;
import org.apache.derby.jdbc.EmbeddedXADataSource;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.jdbc.DataSourceFactory;

public class DerbyActivator implements DataSourceFactory, BundleActivator {

	public ConnectionPoolDataSource createConnectionPoolDataSource(	Properties props) {
		EmbeddedConnectionPoolDataSource embeddedCPDataSource = new EmbeddedConnectionPoolDataSource();
		embeddedCPDataSource.setDataSourceName(props.getProperty(JDBC_DATASOURCE_NAME));
		embeddedCPDataSource.setDatabaseName(props.getProperty(JDBC_DATABASE_NAME));
		embeddedCPDataSource.setUser(props.getProperty(JDBC_USER));
		embeddedCPDataSource.setPassword(props.getProperty(JDBC_PASSWORD));
		return embeddedCPDataSource;
	}

	public DataSource createDataSource(Properties props) {
		EmbeddedDataSource embeddedDataSource = new EmbeddedDataSource();
		embeddedDataSource.setDataSourceName(props.getProperty(JDBC_DATASOURCE_NAME));
		embeddedDataSource.setDatabaseName(props.getProperty(JDBC_DATABASE_NAME));
		embeddedDataSource.setUser(props.getProperty(JDBC_USER));
		embeddedDataSource.setPassword(props.getProperty(JDBC_PASSWORD));
		return embeddedDataSource;
	}

	public XADataSource createXADataSource(Properties props) {
		EmbeddedXADataSource embeddedXADataSource = new EmbeddedXADataSource();
		embeddedXADataSource.setDataSourceName(props.getProperty(JDBC_DATASOURCE_NAME));
		embeddedXADataSource.setDatabaseName(props.getProperty(JDBC_DATABASE_NAME));
		embeddedXADataSource.setUser(props.getProperty(JDBC_USER));
		embeddedXADataSource.setPassword(props.getProperty(JDBC_PASSWORD));
		return embeddedXADataSource;
	}

	public Driver getDriver(Properties props) {
		EmbeddedDriver embeddedDriver = new EmbeddedDriver();
		return embeddedDriver;
	}

	public void start(BundleContext context) throws Exception {
		new EmbeddedDriver();	
		context.registerService(DataSourceFactory.class.getName(), this, new Properties());
	}

	public void stop(BundleContext context) throws Exception {

		try {
			DriverManager.getConnection("jdbc:derby:;shutdown=true");
		} catch (SQLException sqlexception) {
		}
	}

}
