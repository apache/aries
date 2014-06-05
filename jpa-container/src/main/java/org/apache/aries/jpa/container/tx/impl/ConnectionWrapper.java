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
package org.apache.aries.jpa.container.tx.impl;

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

import org.apache.aries.jpa.container.impl.NLS;

/**
 * This class is a wrapper around a {@link Connection} that performs
 * enlistment/delistment of an {@link XAResource} from a transaction.
 * 
 * @see XADatasourceEnlistingWrapper
 */
public class ConnectionWrapper implements Connection {
    
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
        return connection.createArrayOf(typeName, elements);
    }

    public Blob createBlob() throws SQLException {
        return connection.createBlob();
    }

    public Clob createClob() throws SQLException {
        return connection.createClob();
    }

    public NClob createNClob() throws SQLException {
        return connection.createNClob();
    }

    public SQLXML createSQLXML() throws SQLException {
        return connection.createSQLXML();
    }

    public Statement createStatement() throws SQLException {
        return connection.createStatement();
    }

    public Statement createStatement(int resultSetType,
            int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        return connection.createStatement(resultSetType, resultSetConcurrency,
                resultSetHoldability);
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency)
            throws SQLException {
        return connection.createStatement(resultSetType, resultSetConcurrency);
    }

    public Struct createStruct(String typeName, Object[] attributes)
            throws SQLException {
        return connection.createStruct(typeName, attributes);
    }

    public void setSchema(String schema) throws SQLException {
        invokeViaReflection("setSchema", new Class<?>[] {String.class},
                            new Object[] {schema});
    }

    public String getSchema() throws SQLException {
        return invokeViaReflection(String.class, "getSchema", new Class<?>[0],
                            new Object[0]);
    }

    public void abort(Executor executor) throws SQLException {
        invokeViaReflection("abort", new Class<?>[] {Executor.class},
                            new Object[] {executor});
    }

    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        invokeViaReflection("setNetworkTimeout", new Class<?>[] {Executor.class, Integer.TYPE},
                            new Object[] {executor, milliseconds});
    }

    public int getNetworkTimeout() throws SQLException {
        return invokeViaReflection(Integer.TYPE, "getNetworkTimeout", new Class<?>[0], new Object[0]);
    }
    
    private <T> T invokeViaReflection(Class<T> retType, String name,
                                      Class<?> argClasses[], Object args[]) throws SQLException {
        try {
            return retType.cast(connection.getClass().getMethod(name, argClasses).invoke(args));
        } catch (RuntimeException re) {
            throw re;
        } catch (Throwable t) {
            throw new SQLException(t);
        }
    }
    private void invokeViaReflection(String name,
                                    Class<?> argClasses[], Object args[]) throws SQLException {
        try {
            connection.getClass().getMethod(name, argClasses).invoke(args);
        } catch (RuntimeException re) {
            throw re;
        } catch (Throwable t) {
            throw new SQLException(t);
        }
    }

    public boolean getAutoCommit() throws SQLException {
        return connection.getAutoCommit();
    }

    public String getCatalog() throws SQLException {
        return connection.getCatalog();
    }

    public Properties getClientInfo() throws SQLException {
        return connection.getClientInfo();
    }

    public String getClientInfo(String name) throws SQLException {
        return connection.getClientInfo(name);
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
        return connection.isValid(timeout);
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return connection.isWrapperFor(iface);
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

    public void setClientInfo(Properties properties)
            throws SQLClientInfoException {
        connection.setClientInfo(properties);
    }

    public void setClientInfo(String name, String value)
            throws SQLClientInfoException {
        connection.setClientInfo(name, value);
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

    public <T> T unwrap(Class<T> iface) throws SQLException {
        return connection.unwrap(iface);
    }
}
