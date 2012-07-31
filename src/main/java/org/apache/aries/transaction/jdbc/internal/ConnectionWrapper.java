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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.transaction.jdbc.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import javax.transaction.xa.XAResource;

import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a wrapper around a {@link Connection} that performs
 * enlistment/delistment of an {@link XAResource} from a transaction.
 * 
 * @see XADatasourceEnlistingWrapper
 */
public class ConnectionWrapper implements Connection {
    
    private static final Logger logger = LoggerFactory.getLogger("org.apache.aries.transaction.jdbc");

    private Connection connection;
    
    private boolean closed;
    
    private boolean enlisted;
    
    public ConnectionWrapper(Connection connection, boolean enlisted) {
        this.enlisted = enlisted;
        this.connection = connection;
    }
            
    public void close() throws SQLException {
        if (!closed) {
            try {                       
                // don't close connection if enlisted in a transaction
                // the connection will be closed in once the transaction completes
                if (!enlisted) {
                    connection.close();
                }
            } finally {
                closed = true;
            }            
        }
    }

    // cannot be used while enrolled in a transaction 
    
    public void commit() throws SQLException {
        if (enlisted) {
            throw new SQLException(NLS.MESSAGES.getMessage("datasource.enlised.commit"));
        }
        connection.commit();
    }

    public void rollback() throws SQLException {
        if (enlisted) {
            throw new SQLException(NLS.MESSAGES.getMessage("datasource.enlised.rollback"));
        }
        connection.rollback();
    }

    public void rollback(Savepoint savepoint) throws SQLException {
        if (enlisted) {
            throw new SQLException(NLS.MESSAGES.getMessage("datasource.enlised.rollback"));
        }
        connection.rollback(savepoint);
    }
    
    public Savepoint setSavepoint() throws SQLException {
        if (enlisted) {
            throw new SQLException(NLS.MESSAGES.getMessage("datasource.enlised.savepoint"));
        }
        return connection.setSavepoint();
    }

    public Savepoint setSavepoint(String name) throws SQLException {
        if (enlisted) {
            throw new SQLException(NLS.MESSAGES.getMessage("datasource.enlised.savepoint"));
        }
        return connection.setSavepoint(name);
    }
    
    // rest of the methods
    
    public void clearWarnings() throws SQLException {
        connection.clearWarnings();
    }
    
    public Array createArrayOf(String typeName, Object[] elements)
            throws SQLException {
		Method method= getMethod("createArrayOf", String.class, Object[].class);
 		return (Array) invokeByReflection(method, typeName, elements);
    }

    public Blob createBlob() throws SQLException {
		Method method= getMethod("createBlob");
 		return (Blob) invokeByReflection(method);
    }

    public Clob createClob() throws SQLException {
		Method method= getMethod("createClob");
 		return (Clob) invokeByReflection(method);
    }

    @IgnoreJRERequirement
    public NClob createNClob() throws SQLException {
		Method method= getMethod("createNClob");
 		return (NClob) invokeByReflection(method);
    }

    @IgnoreJRERequirement
    public SQLXML createSQLXML() throws SQLException {
		Method method= getMethod("createSQLXML");
 		return (SQLXML) invokeByReflection(method);
    }

    public Statement createStatement() throws SQLException {
		Method method= getMethod("createStatement");
 		return (Statement) invokeByReflection(method);
    }

    public Statement createStatement(int resultSetType,
            int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
		Method method= getMethod("createStatement", int.class, int.class, int.class);
 		return (Statement) invokeByReflection(method, resultSetType, resultSetConcurrency, resultSetHoldability);
   }

    public Statement createStatement(int resultSetType, int resultSetConcurrency)
            throws SQLException {
		Method method= getMethod("createStatement", int.class, int.class);
 		return (Statement) invokeByReflection(method, resultSetType, resultSetConcurrency);
    }

    public Struct createStruct(String typeName, Object[] attributes)
            throws SQLException {
		Method method= getMethod("createStruct", String.class, Object[].class);
 		return (Struct) invokeByReflection(method, typeName, attributes);
    }

    public boolean getAutoCommit() throws SQLException {
        return connection.getAutoCommit();
    }

    public String getCatalog() throws SQLException {
        return connection.getCatalog();
    }

    public Properties getClientInfo() throws SQLException {
		Method method= getMethod("getClientInfo");
 		Properties properties = (Properties) invokeByReflection(method);
 		if (properties == null)
 		{
 			properties = new Properties();
 		}
 		return properties;
    }

    public String getClientInfo(String name) throws SQLException {
		Method method= getMethod("getClientInfo", String.class);
	   	// This method may return null if the specified client info property 
        // has not been set and does not have a default value,
        // so we can return the result even if it's null
 		return (String) invokeByReflection(method, name);
     }

    public int getHoldability() throws SQLException {
        return connection.getHoldability();
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        return connection.getMetaData();
    }

    public int getTransactionIsolation() throws SQLException {
        return connection.getTransactionIsolation();
    }

    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return connection.getTypeMap();
    }

    public SQLWarning getWarnings() throws SQLException {
        return connection.getWarnings();
    }

    public boolean isClosed() throws SQLException {
        return connection.isClosed();
    }

    public boolean isReadOnly() throws SQLException {
        return connection.isReadOnly();
    }

    public boolean isValid(int timeout) throws SQLException {
		Method method= getMethod("isValid", int.class);
		Boolean answer = (Boolean) invokeByReflection(method, timeout);
    	
    	if (answer != null)
    	{
    		return answer.booleanValue();
    	}
    	
        return false;
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
    	// Try and work out the answer ourselves to support older drivers
    	if (iface.isInstance(connection)) {
    		return true;
    	}

    	Method method= getMethod("isWrapperFor", Class.class);
		Boolean answer = (Boolean) invokeByReflection(method, iface);
    	
    	if (answer != null)
    	{
    		return answer.booleanValue();
    	}
    	
         return false;
    }

    public String nativeSQL(String sql) throws SQLException {
        return connection.nativeSQL(sql);
    }

    public CallableStatement prepareCall(String sql, int resultSetType,
            int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        return connection.prepareCall(sql, resultSetType, resultSetConcurrency,
                resultSetHoldability);
    }

    public CallableStatement prepareCall(String sql, int resultSetType,
            int resultSetConcurrency) throws SQLException {
        return connection.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    public CallableStatement prepareCall(String sql) throws SQLException {
        return connection.prepareCall(sql);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType,
            int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        return connection.prepareStatement(sql, resultSetType,
                resultSetConcurrency, resultSetHoldability);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType,
            int resultSetConcurrency) throws SQLException {
        return connection.prepareStatement(sql, resultSetType,
                resultSetConcurrency);
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
            throws SQLException {
        return connection.prepareStatement(sql, autoGeneratedKeys);
    }

    public PreparedStatement prepareStatement(String sql, int[] columnIndexes)
            throws SQLException {
        return connection.prepareStatement(sql, columnIndexes);
    }

    public PreparedStatement prepareStatement(String sql, String[] columnNames)
            throws SQLException {
        return connection.prepareStatement(sql, columnNames);
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return connection.prepareStatement(sql);
    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        connection.releaseSavepoint(savepoint);
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        connection.setAutoCommit(autoCommit);
    }

    public void setCatalog(String catalog) throws SQLException {
        connection.setCatalog(catalog);
    }

    @IgnoreJRERequirement
    public void setClientInfo(Properties properties)
            throws SQLClientInfoException {
		Method method= getMethod("setClientInfo", Properties.class);
		try {
			invokeByReflection(method, properties);
		} catch (SQLException e) {
			logger.debug(e.toString());
			throw new SQLClientInfoException(e.toString(), null);
		}
    }

    @IgnoreJRERequirement
    public void setClientInfo(String name, String value)
            throws SQLClientInfoException {
		Method method= getMethod("setClientInfo", String.class, String.class);
		try {
			invokeByReflection(method, name, value);
		} catch (SQLException e) {
			logger.debug(e.toString());
			throw new SQLClientInfoException(e.toString(), null);
		}
    }

    public void setHoldability(int holdability) throws SQLException {
        connection.setHoldability(holdability);
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        connection.setReadOnly(readOnly);
    }

    public void setTransactionIsolation(int level) throws SQLException {
        connection.setTransactionIsolation(level);
    }

    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        connection.setTypeMap(map);
    }

	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> iface) throws SQLException {
	   	// Try and work out the answer ourselves to support older drivers
		if (iface.isInstance(connection)) {
    		return (T) connection;
    	}

	   	Method method= getMethod("unwrap", Class.class);
		return (T) invokeByReflection(method, iface);
    	
    }

    public void setSchema(String schema) throws SQLException {
        Method method= getMethod("setSchema", String.class);
        invokeByReflection(method, schema);
    }

    public String getSchema() throws SQLException {
        Method method= getMethod("getSchema");
        return (String) invokeByReflection(method);
    }

    public void abort(Executor executor) throws SQLException {
        Method method= getMethod("abort", Executor.class);
        invokeByReflection(method, executor);
    }

    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        Method method= getMethod("setNetworkTimeout", int.class);
        invokeByReflection(method, executor, milliseconds);
    }

    public int getNetworkTimeout() throws SQLException {
        Method method= getMethod("getNetworkTimeout");
        return (Integer) invokeByReflection(method);
    }

    private Method getMethod(String methodName, Class<?> ...paramTypes) {
		Method method = null;
		try {
			method = getClass().getMethod(methodName, paramTypes);
		} catch (SecurityException e) {
			// Famous last words: this should never happen :) 
			logger.debug(e.toString());
		} catch (NoSuchMethodException e) {
			// If this happens it's a developer error, not a user one, so debug only
			logger.debug(e.toString());
		}
		return method;
	}

	private Object invokeByReflection(Method method, Object ... params) throws SQLException {
		Object answer = null;
		try {
    	Method m = connection.getClass().getMethod(method.getName(), method.getParameterTypes());
    	answer = m.invoke(connection, params);
    	} catch (NoSuchMethodException e)
    	{
    		// That's fine, we're probably looking at a Java 5 interface
			logger.debug(e.toString());
    	} catch (IllegalArgumentException e) {
			logger.debug(e.toString());
		} catch (IllegalAccessException e) {
			logger.debug(e.toString());
		} catch (InvocationTargetException e) {
        	// Don't pass the exception as a cause, since that method isn't available on Java 5
			throw new SQLException(e.getCause().toString());
		}
		
		return answer;
	}
}
