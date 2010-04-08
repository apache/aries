/*
 * Copyright (c) OSGi Alliance (2008). All Rights Reserved.
 *
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
package org.osgi.service.jdbc;

import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;

/**
 * DataSource providers should implement this interface and register it as an
 * OSGi service with the JDBC driver class name in the "jdbc.driver" property.
 */
public interface DataSourceFactory {
    /**
     * Common property keys that DataSource clients should supply values for.
     */
    public static final String JDBC_URL = "url";
    public static final String JDBC_USER = "user";
    public static final String JDBC_PASSWORD = "password";
    public static final String JDBC_DATABASE_NAME = "databaseName";
    public static final String JDBC_DATASOURCE_NAME = "dataSourceName";
    public static final String JDBC_DESCRIPTION = "description";
    public static final String JDBC_NETWORK_PROTOCOL = "networkProtocol";
    public static final String JDBC_PORT_NUMBER = "portNumber";
    public static final String JDBC_ROLE_NAME = "roleName";
    public static final String JDBC_SERVER_NAME = "serverName";
    
	/**
	 * XA specific. Additional property keys that ConnectionPoolDataSource and
	 * XADataSource clients should supply values for
	 */
    public static final String JDBC_INITIAL_POOL_SIZE = "initialPoolSize";
    public static final String JDBC_MAX_IDLE_TIME = "maxIdleTime";
    public static final String JDBC_MAX_POOL_SIZE = "maxPoolSize";
    public static final String JDBC_MAX_STATEMENTS = "maxStatements";
    public static final String JDBC_MIN_POOL_SIZE = "minPoolSize";
    public static final String JDBC_PROPERTY_CYCLE = "propertyCycle";
    
	/**
	 * Vendor-specific properties meant to further describe the driver. Clients
	 * may filter or test this property to determine if the driver is suitable,
	 * or the desired one.
	 */
    public static final String JDBC_DRIVER_CLASS = "osgi.jdbc.driver.class";
    public static final String JDBC_DRIVER_NAME = "osgi.jdbc.driver.name";
    public static final String JDBC_DRIVER_VERSION = "osgi.jdbc.driver.version";
    
    /**
     * Create a new {@link DataSource} using the given properties.
     * 
     * @param props properties used to configure the DataSource
     * @return configured DataSource
     */
    public DataSource createDataSource( Properties props ) throws SQLException;
    
    public XADataSource createXADataSource( Properties props ) throws SQLException;
    
    public ConnectionPoolDataSource createConnectionPoolDataSource( Properties props ) throws SQLException;
        
    public Driver getDriver( Properties props ) throws SQLException;
}
