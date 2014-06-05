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

import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.apache.aries.jpa.container.impl.NLS;
import org.osgi.framework.FrameworkUtil;

/**
 * This class allows JDBC XA data sources to participate in global transactions,
 * via the {@link ConnectionWrapper} that is returned. The only service provided
 * is enlistment/delistment of the associated {@link XAResource} in transactions.
 * Important consideration such as connection pooling and error handling are
 * completely ignored.
 *
 */
public class XADatasourceEnlistingWrapper implements DataSource, Serializable {
    /** The serial version UID */
    private static final long serialVersionUID = -3200389791205501228L;

    private final XADataSource wrappedDS;
    
    private transient Map<Object, Connection> connectionMap = 
        new ConcurrentHashMap<Object, Connection>();
    
    public XADatasourceEnlistingWrapper(XADataSource toWrap) {
      wrappedDS = toWrap;
      OSGiTransactionManager.init(FrameworkUtil.getBundle(
          XADatasourceEnlistingWrapper.class).getBundleContext());
    }
    
    public Connection getConnection() throws SQLException {
        Transaction transaction = getTransaction();
        if (transaction != null) {
            Object key = transaction;
            Connection connection = connectionMap.get(key);
            if (connection == null) {
                XAConnection xaConnection = wrappedDS.getXAConnection();                                
                connection = xaConnection.getConnection();
                enlist(transaction, xaConnection.getXAResource(), key);
                connectionMap.put(key, connection);                
            }
            return getEnlistedConnection(connection, true);
        } else {
            return getEnlistedConnection(wrappedDS.getXAConnection().getConnection(), false);
        }
    }

    public Connection getConnection(String username, String password) throws SQLException {
        Transaction transaction = getTransaction();
        if (transaction != null) {
            Object key = new ConnectionKey(username, password, transaction);
            Connection connection = connectionMap.get(key);
            if (connection == null) {
                XAConnection xaConnection = wrappedDS.getXAConnection(username, password);
                connection = xaConnection.getConnection();
                enlist(transaction, xaConnection.getXAResource(), key);               
                connectionMap.put(key, connection);
            }
            return getEnlistedConnection(connection, true);
        } else {
            return getEnlistedConnection(wrappedDS.getXAConnection(username, password).getConnection(), false);
        }
    }

    private Transaction getTransaction() throws SQLException {
        try {
            return (OSGiTransactionManager.get().getStatus() == Status.STATUS_ACTIVE) ? 
                (Transaction)OSGiTransactionManager.get().getTransaction() : null;
        } catch (SystemException e) {
            throw new SQLException(NLS.MESSAGES.getMessage("unable.to.get.tx"), e);
        }
    }
    
    private void enlist(Transaction transaction, XAResource xaResource, Object key) throws SQLException {
        try {
            transaction.enlistResource(xaResource);            
            transaction.registerSynchronization(new TransactionListener(key));
        } catch (Exception e) {
            try {
                OSGiTransactionManager.get().setRollbackOnly();
            } catch (IllegalStateException e1) {
                e1.printStackTrace();
            } catch (SystemException e1) {
                e1.printStackTrace();
            }
        } 
    }
    
    private class TransactionListener implements Synchronization {

        private final Object key;
        
        public TransactionListener(Object key) {
            this.key = key;
        }
        
        public void afterCompletion(int status) {
            Connection connection = connectionMap.remove(key);
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    // ignore
                }
            }
        }

        public void beforeCompletion() {
        }
        
    }
    
    public PrintWriter getLogWriter() throws SQLException
    {
      return wrappedDS.getLogWriter();
    }

    public int getLoginTimeout() throws SQLException
    {
      return wrappedDS.getLoginTimeout();
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }

    public void setLogWriter(PrintWriter out) throws SQLException
    {
      wrappedDS.setLogWriter(out);
    }

    public void setLoginTimeout(int seconds) throws SQLException
    {
      wrappedDS.setLoginTimeout(seconds);
    }

    private Connection getEnlistedConnection(Connection connection, boolean enlisted) throws SQLException
    {
        return new ConnectionWrapper(connection, enlisted);
    }

    
    @Override
    public boolean equals(Object other)
    {
      if (other == this) return true;
      if (other == null) return false;
      
      if (other.getClass() == this.getClass()) {
        return wrappedDS.equals(((XADatasourceEnlistingWrapper)other).wrappedDS);
      }
      
      return false;
    }
    
    @Override
    public int hashCode()
    {
      return wrappedDS.hashCode();
    }

    public boolean isWrapperFor(Class<?> arg0) throws SQLException
    {
      return false;
    }

    public <T> T unwrap(Class<T> arg0) throws SQLException
    {
      return null;
    }
}
