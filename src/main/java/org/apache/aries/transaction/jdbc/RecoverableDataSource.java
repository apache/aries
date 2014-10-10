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
package org.apache.aries.transaction.jdbc;

import org.apache.aries.transaction.AriesTransactionManager;
import org.apache.aries.transaction.jdbc.internal.AbstractMCFFactory;
import org.apache.aries.transaction.jdbc.internal.ConnectionManagerFactory;
import org.apache.aries.transaction.jdbc.internal.DataSourceMCFFactory;
import org.apache.aries.transaction.jdbc.internal.Recovery;
import org.apache.aries.transaction.jdbc.internal.XADataSourceMCFFactory;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

/**
 * Defines a JDBC DataSource that will auto-enlist into existing XA transactions.
 * The DataSource will also be registered with the Aries/Geronimo transaction
 * manager in order to provide proper transaction recovery at startup.
 * Other considerations such as connection pooling and error handling are
 * completely ignored.
 *
 * @org.apache.xbean.XBean
 */
public class RecoverableDataSource implements DataSource {

    private CommonDataSource dataSource;
    private AriesTransactionManager transactionManager;
    private String name;
    private String exceptionSorter = "all";
    private String username = "";
    private String password = "";
    private boolean allConnectionsEquals = true;
    private int connectionMaxIdleMinutes = 15;
    private int connectionMaxWaitMilliseconds = 5000;
    private String partitionStrategy = "none";
    private boolean pooling = true;
    private int poolMaxSize = 10;
    private int poolMinSize = 0;
    private String transaction;
    private boolean validateOnMatch = true;
    private boolean backgroundValidation = false;
    private int backgroundValidationMilliseconds = 600000;

    private DataSource delegate;

    /**
     * The unique name for this managed XAResource.  This name will be used
     * by the transaction manager to recover transactions.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * The CommonDataSource to wrap.
     *
     * @org.apache.xbean.Property required=true
     */
    public void setDataSource(CommonDataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * The XA TransactionManager to use to enlist the JDBC connections into.
     *
     * @org.apache.xbean.Property required=true
     */
    public void setTransactionManager(AriesTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /**
     * Specify which SQL exceptions are fatal.
     * Can be all, none, known or custom(xx,yy...).
     */
    public void setExceptionSorter(String exceptionSorter) {
        this.exceptionSorter = exceptionSorter;
    }

    /**
     * The user name used to establish the connection.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * The password credential used to establish the connection.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    public void setAllConnectionsEquals(boolean allConnectionsEquals) {
        this.allConnectionsEquals = allConnectionsEquals;
    }

    public void setConnectionMaxIdleMinutes(int connectionMaxIdleMinutes) {
        this.connectionMaxIdleMinutes = connectionMaxIdleMinutes;
    }

    public void setConnectionMaxWaitMilliseconds(int connectionMaxWaitMilliseconds) {
        this.connectionMaxWaitMilliseconds = connectionMaxWaitMilliseconds;
    }

    /**
     * Pool partition strategy.
     * Can be none, by-connector-properties or by-subject (defaults to none).
     */
    public void setPartitionStrategy(String partitionStrategy) {
        this.partitionStrategy = partitionStrategy;
    }

    /**
     * If pooling is enabled (defaults to true).
     * @param pooling
     */
    public void setPooling(boolean pooling) {
        this.pooling = pooling;
    }

    /**
     * Maximum pool size (defaults to 10).
     */
    public void setPoolMaxSize(int poolMaxSize) {
        this.poolMaxSize = poolMaxSize;
    }

    /**
     * Minimum pool size (defaults to 0).
     */
    public void setPoolMinSize(int poolMinSize) {
        this.poolMinSize = poolMinSize;
    }

     /**
     * If validation on connection matching is enabled (defaults to true).
     * @param validateOnMatch
     */
    public void setValidateOnMatch(boolean validateOnMatch) {
        this.validateOnMatch = validateOnMatch;
    }

    /**
     * If periodically background validation is enabled (defaults to false).
     * @param backgroundValidation
     */
    public void setBackgroundValidation(boolean backgroundValidation) {
        this.backgroundValidation = backgroundValidation;
    }

    /**
     * Background validation period (defaults to 600000)
     * @param backgroundValidationMilliseconds
     */
    public void setBackgroundValidationMilliseconds(int backgroundValidationMilliseconds) {
        this.backgroundValidationMilliseconds = backgroundValidationMilliseconds;
    }

    /**
     * Transaction support.
     * Can be none, local or xa (defaults to xa).
     */
    public void setTransaction(String transaction) {
        this.transaction = transaction;
    }

    /**
     * @org.apache.xbean.InitMethod
     */
    public void start() throws Exception {
        AbstractMCFFactory mcf;
        if (("xa".equals(transaction) || "local".equals(transaction)) && transactionManager == null) {
            throw new IllegalArgumentException("xa or local transactions specified, but no TransactionManager set");
        }
        if ("xa".equals(transaction) && !(dataSource instanceof XADataSource)) {
            throw new IllegalArgumentException("xa transactions specified, but DataSource does not implement javax.sql.XADataSource");
        }
        if ("xa".equals(transaction) || (transactionManager != null && dataSource instanceof XADataSource)) {
            mcf = new XADataSourceMCFFactory();
            if (transaction == null) {
                transaction = "xa";
            }
        } else if (dataSource instanceof DataSource) {
            mcf = new DataSourceMCFFactory();
            if (transaction == null) {
                transaction = transactionManager != null ? "local" : "none";
            }
        } else {
            throw new IllegalArgumentException("dataSource must be of type javax.sql.DataSource/XADataSource");
        }
        mcf.setDataSource(dataSource);
        mcf.setExceptionSorterAsString(exceptionSorter);
        mcf.setUserName(username);
        mcf.setPassword(password);
        mcf.init();

        ConnectionManagerFactory cm = new ConnectionManagerFactory();
        cm.setManagedConnectionFactory(mcf.getConnectionFactory());
        cm.setTransactionManager(transactionManager);
        cm.setAllConnectionsEqual(allConnectionsEquals);
        cm.setConnectionMaxIdleMinutes(connectionMaxIdleMinutes);
        cm.setConnectionMaxWaitMilliseconds(connectionMaxWaitMilliseconds);
        cm.setPartitionStrategy(partitionStrategy);
        cm.setPooling(pooling);
        cm.setPoolMaxSize(poolMaxSize);
        cm.setPoolMinSize(poolMinSize);
        cm.setValidateOnMatch(validateOnMatch);
        cm.setBackgroundValidation(backgroundValidation);
        cm.setBackgroundValidationMilliseconds(backgroundValidationMilliseconds);
        cm.setTransaction(transaction);
        cm.setName(name);
        cm.init();

        delegate = (DataSource) mcf.getConnectionFactory().createConnectionFactory(cm.getConnectionManager());

        if (dataSource instanceof XADataSource) {
            Recovery.recover(name, (XADataSource) dataSource, transactionManager);
        }
    }

    //---------------------------
    // DataSource implementation
    //---------------------------

    public Connection getConnection() throws SQLException {
        return delegate.getConnection();
    }

    public Connection getConnection(String username, String password) throws SQLException {
        return delegate.getConnection(username, password);
    }

    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    /**
     * @org.apache.xbean.Property hidden=true
     */
    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }

    /**
     * @org.apache.xbean.Property hidden=true
     */
    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    @IgnoreJRERequirement
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

}
