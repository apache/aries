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
package org.apache.aries.samples.ariestrader.datasource.derby;

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

public class DerbyActivator implements BundleActivator {

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
    public static final String JDBC_CREATE_DATABASE = "createDatabase";

    /**
     * OSGi-specific properties
     */
    public static final String OSGI_JNDI_SERVICE_NAME = "osgi.jndi.serviceName";

    public ConnectionPoolDataSource createConnectionPoolDataSource( Properties props) {
        EmbeddedConnectionPoolDataSource embeddedCPDataSource = new EmbeddedConnectionPoolDataSource();
        embeddedCPDataSource.setDataSourceName(props.getProperty(JDBC_DATASOURCE_NAME));
        embeddedCPDataSource.setDatabaseName(props.getProperty(JDBC_DATABASE_NAME));
        embeddedCPDataSource.setUser(props.getProperty(JDBC_USER));
        embeddedCPDataSource.setPassword(props.getProperty(JDBC_PASSWORD));
        embeddedCPDataSource.setCreateDatabase(props.getProperty(JDBC_CREATE_DATABASE));
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

//  public Driver getDriver(Properties props) {
//      EmbeddedDriver embeddedDriver = new EmbeddedDriver();
//      return embeddedDriver;
//  }

    public void start(BundleContext context) throws Exception {
        new EmbeddedDriver();   

        // Set the needed properties for the TradeDataSource
        Properties tds_props = new Properties();
        tds_props.put(JDBC_DATASOURCE_NAME, "TradeDataSource");
        tds_props.put(JDBC_DATABASE_NAME, "tradedb");
        tds_props.put(JDBC_USER, "");
        tds_props.put(JDBC_PASSWORD, "");
        tds_props.put(JDBC_CREATE_DATABASE, "create");

        ConnectionPoolDataSource tds_datasource = createConnectionPoolDataSource(tds_props);

        Properties tds_service_props = new Properties();
        tds_service_props.put(OSGI_JNDI_SERVICE_NAME, "jdbc/TradeDataSource");

        context.registerService(DataSource.class.getName(), tds_datasource, tds_service_props);


        // Set the needed properties for the NoTxTradeDataSource
        Properties ntds_props = new Properties();
        ntds_props.put(JDBC_DATASOURCE_NAME, "NoTxTradeDataSource");
        ntds_props.put(JDBC_DATABASE_NAME, "tradedb");
        ntds_props.put(JDBC_USER, "");
        ntds_props.put(JDBC_PASSWORD, "");
        ntds_props.put(JDBC_CREATE_DATABASE, "create");

        ConnectionPoolDataSource ntds_datasource = createConnectionPoolDataSource(ntds_props);

        Properties ntds_services_props = new Properties();
        ntds_services_props.put(OSGI_JNDI_SERVICE_NAME, "jdbc/NoTxTradeDataSource");

        context.registerService(DataSource.class.getName(), ntds_datasource, ntds_services_props);
    }

    public void stop(BundleContext context) throws Exception {

        try {
            DriverManager.getConnection("jdbc:derby:;shutdown=true");
        }
        catch (SQLException sqlexception) {
        }
    }

}
