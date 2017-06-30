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

import static org.osgi.service.jdbc.DataSourceFactory.JDBC_DATABASE_NAME;
import static org.osgi.service.jdbc.DataSourceFactory.JDBC_DATASOURCE_NAME;
import static org.osgi.service.jdbc.DataSourceFactory.JDBC_DESCRIPTION;
import static org.osgi.service.jdbc.DataSourceFactory.JDBC_NETWORK_PROTOCOL;
import static org.osgi.service.jdbc.DataSourceFactory.JDBC_PASSWORD;
import static org.osgi.service.jdbc.DataSourceFactory.JDBC_PORT_NUMBER;
import static org.osgi.service.jdbc.DataSourceFactory.JDBC_ROLE_NAME;
import static org.osgi.service.jdbc.DataSourceFactory.JDBC_SERVER_NAME;
import static org.osgi.service.jdbc.DataSourceFactory.JDBC_URL;
import static org.osgi.service.jdbc.DataSourceFactory.JDBC_USER;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(factoryPid="org.apache.aries.tx.control.jdbc.xa", description="Aries Transaction Control Factory for XA enabled JDBCResourceProvider Services")
public @interface Config {

	// Most commonly used properties declared first so that they go into the metatype first
	
	@AttributeDefinition(required=false, 
			description="The name of the driver class for the DataSourceFactory service. This property need not be defined if aries.dsf.target.filter is defined.")
	String osgi_jdbc_driver_class();

	@AttributeDefinition(required=false, description="The JDBC URL to pass to the DataSourceFactory")
	String url();
	
	@AttributeDefinition(required=false, description="The userid to pass to the DataSourceFactory")
	String user();
	
	@AttributeDefinition(type=AttributeType.PASSWORD, required=false, 
			description="The password to pass to the DataSourceFactory (not visible as a service property)")
	String password();
	
	// Recovery configuration
	@AttributeDefinition(required=false, description="The recovery identifier for this resource. If not set then this resource will not be recoverable. This identifier must uniquely identify a single resource, and must not change if the framework is restarted.")
	String osgi_recovery_identifier();
	
	// Pool configuration properties
	
	@AttributeDefinition(required=false, description="Is connection pooling enabled for this JDBCResourceProvider")
	boolean osgi_connection_pooling_enabled() default true;
	
	@AttributeDefinition(required=false, description="The maximum number of connections in the pool")
	int osgi_connection_max() default 10;

	@AttributeDefinition(required=false, description="The minimum number of connections in the pool")
	int osgi_connection_min() default 10;
	
	@AttributeDefinition(required=false, description="The maximum time (in ms) that the pool will wait for a connection before failing")
	long osgi_connection_timeout() default 30000;
	
	@AttributeDefinition(required=false, description="The minimum time (in ms) a connection will be idle before being reclaimed by the pool")
	long osgi_idle_timeout() default 180000;
	
	@AttributeDefinition(required=false, description="The maximum time (in ms) that a connection will stay in the pool before being discarded")
	long osgi_connection_lifetime() default 10800000;
	
	@AttributeDefinition(required=false, description="The query that will be executed just before a connection is given to you from the pool to validate that the connection to the database is still alive. If your driver supports JDBC4 we strongly recommend not setting this property. This is for 'legacy' databases that do not support the JDBC Connection.isValid() API")
	String aries_connection_test_query();	
		
	// Recovery credential configuration
	
	@AttributeDefinition(required=false, description="The user that should be used for recovery. If not specified then recovery will use the same user credentials as normal operation")
	String recovery_user();
	
	@AttributeDefinition(type=AttributeType.PASSWORD, required=false, 
			description="The password that should be used for recovery. Only used if recovery.user is specified")
	String _recovery_password();
	
	// Transaction integration configuration
	
	@AttributeDefinition(required=false, description="Should this Resource participate in transactions using XA")
	boolean osgi_xa_enabled() default true;

	@AttributeDefinition(required=false, description="Should this Resource participate as a Local Resource if XA is not available")
	boolean osgi_local_enabled() default true;

	// Detailed Configuration
	
	@AttributeDefinition(required=false, description="The filter to use when finding the DataSourceFactory service. This property need not be defined if osgi.jdbc.driver.class is defined.")
	String aries_dsf_target_filter();
	
	@AttributeDefinition(required=false, description="The names of the properties from this configuration that should be passed to the DataSourceFactory")
	String[] aries_jdbc_property_names() default {JDBC_DATABASE_NAME, JDBC_DATASOURCE_NAME,
			JDBC_DESCRIPTION, JDBC_NETWORK_PROTOCOL, JDBC_PASSWORD, JDBC_PORT_NUMBER, JDBC_ROLE_NAME, JDBC_SERVER_NAME,
			JDBC_URL, JDBC_USER};
	

	//Raw JDBC configuration
	
	@AttributeDefinition(required=false, description="JDBC configuration property")
	String databaseName();
	
	@AttributeDefinition(required=false, description="JDBC configuration property")
	String dataSourceName();

	@AttributeDefinition(required=false, description="JDBC configuration property")
	String description();

	@AttributeDefinition(required=false, description="JDBC configuration property")
	String networkProtocol();

	@AttributeDefinition(required=false, description="JDBC configuration property")
	int portNumber();

	@AttributeDefinition(required=false, description="JDBC configuration property")
	String roleName();

	@AttributeDefinition(required=false, description="JDBC configuration property")
	String serverName();
}
