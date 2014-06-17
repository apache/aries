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

import org.apache.aries.transaction.AriesTransactionManager;
import org.apache.geronimo.connector.outbound.GenericConnectionManager;
import org.apache.geronimo.connector.outbound.SubjectSource;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.LocalTransactions;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.NoPool;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.NoTransactions;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.PartitionedPool;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.PoolingSupport;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.SinglePool;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.TransactionSupport;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.XATransactions;
import org.apache.geronimo.connector.outbound.connectiontracking.ConnectionTrackingCoordinator;
import org.apache.geronimo.connector.outbound.connectiontracking.GeronimoTransactionListener;
import org.apache.geronimo.transaction.manager.TransactionManagerMonitor;
import org.tranql.connector.UserPasswordManagedConnectionFactory;

import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ManagedConnectionFactory;

public class ConnectionManagerFactory {

    private AriesTransactionManager transactionManager;
    private ManagedConnectionFactory managedConnectionFactory;

    private TransactionSupport transactionSupport;
    private String transaction;

    private PoolingSupport poolingSupport;
    private boolean pooling = true;
    private String partitionStrategy; //: none, by-subject, by-connector-properties
    private int poolMaxSize = 10;
    private int poolMinSize = 0;
    private boolean allConnectionsEqual = true;
    private int connectionMaxWaitMilliseconds = 5000;
    private int connectionMaxIdleMinutes = 15;

    private boolean validateOnMatch = true;
    private boolean backgroundValidation = false;
    private int backgroundValidationMilliseconds = 600000;

    private SubjectSource subjectSource;

    private ConnectionTrackingCoordinator connectionTracker;
    private TransactionManagerMonitor transactionManagerMonitor;
    private GenericConnectionManager connectionManager;

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public void init() throws Exception {
        if (transactionManager == null) {
            throw new IllegalArgumentException("transactionManager must be set");
        }
        if (managedConnectionFactory == null) {
            throw new IllegalArgumentException("managedConnectionFactory must be set");
        }
        // Apply the default value for property if necessary
        if (transactionSupport == null) {
            // No transaction
            if (transaction == null || "local".equalsIgnoreCase(transaction)) {
                transactionSupport = LocalTransactions.INSTANCE;
            } else if ("none".equalsIgnoreCase(transaction)) {
                transactionSupport = NoTransactions.INSTANCE;
            } else if ("xa".equalsIgnoreCase(transaction)) {
                transactionSupport = new XATransactions(true, false);
            } else {
                throw new IllegalArgumentException("Unknown transaction type " + transaction + " (must be local, none or xa)");
            }
        }
        if (poolingSupport == null) {
            // No pool
            if (!pooling) {
                poolingSupport = new NoPool();
            } else {
                if (partitionStrategy == null || "none".equalsIgnoreCase(partitionStrategy)) {

                    // unpartitioned pool
                    poolingSupport = new SinglePool(poolMaxSize,
                            poolMinSize,
                            connectionMaxWaitMilliseconds,
                            connectionMaxIdleMinutes,
                            allConnectionsEqual,
                            !allConnectionsEqual,
                            false);

                } else if ("by-connector-properties".equalsIgnoreCase(partitionStrategy)) {

                    // partition by connector properties such as username and password on a jdbc connection
                    poolingSupport = new PartitionedPool(poolMaxSize,
                            poolMinSize,
                            connectionMaxWaitMilliseconds,
                            connectionMaxIdleMinutes,
                            allConnectionsEqual,
                            !allConnectionsEqual,
                            false,
                            true,
                            false);
                } else if ("by-subject".equalsIgnoreCase(partitionStrategy)) {

                    // partition by caller subject
                    poolingSupport = new PartitionedPool(poolMaxSize,
                            poolMinSize,
                            connectionMaxWaitMilliseconds,
                            connectionMaxIdleMinutes,
                            allConnectionsEqual,
                            !allConnectionsEqual,
                            false,
                            false,
                            true);
                } else {
                    throw new IllegalArgumentException("Unknown partition strategy " + partitionStrategy + " (must be none, by-connector-properties or by-subject)");
                }
            }
        }
        if (connectionTracker == null) {
            connectionTracker = new ConnectionTrackingCoordinator();
        }
        if (transactionManagerMonitor == null) {
            transactionManagerMonitor = new GeronimoTransactionListener(connectionTracker);
            transactionManager.addTransactionAssociationListener(transactionManagerMonitor);
        }
        if (connectionManager == null) {
            if (validateOnMatch || backgroundValidation) {
                // Wrap the original ManagedConnectionFactory to add validation capability
                managedConnectionFactory = new ValidatingDelegatingManagedConnectionFactory((UserPasswordManagedConnectionFactory) managedConnectionFactory);
            }
            if (backgroundValidation) {
                // Instantiate the Validating Connection Manager
                connectionManager = new ValidatingGenericConnectionManager(
                        transactionSupport,
                        poolingSupport,
                        subjectSource,
                        connectionTracker,
                        transactionManager,
                        managedConnectionFactory,
                        getClass().getName(),
                        getClass().getClassLoader(),
                        backgroundValidationMilliseconds);
            } else {
                // Instantiate the Geronimo Connection Manager
                connectionManager = new GenericConnectionManager(
                        transactionSupport,
                        poolingSupport,
                        subjectSource,
                        connectionTracker,
                        transactionManager,
                        managedConnectionFactory,
                        getClass().getName(),
                        getClass().getClassLoader());
            }

            connectionManager.doStart();
        }
    }

    public void destroy() throws Exception {
        if (connectionManager != null) {
            connectionManager.doStop();
            connectionManager = null;
        }
        if (transactionManagerMonitor != null && transactionManager != null) {
            transactionManager.removeTransactionAssociationListener(transactionManagerMonitor);
        }
    }

    public AriesTransactionManager getTransactionManager() {
        return transactionManager;
    }

    public void setTransactionManager(AriesTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public ManagedConnectionFactory getManagedConnectionFactory() {
        return managedConnectionFactory;
    }

    public void setManagedConnectionFactory(ManagedConnectionFactory managedConnectionFactory) {
        this.managedConnectionFactory = managedConnectionFactory;
    }

    public TransactionSupport getTransactionSupport() {
        return transactionSupport;
    }

    public void setTransactionSupport(TransactionSupport transactionSupport) {
        this.transactionSupport = transactionSupport;
    }

    public String getTransaction() {
        return transaction;
    }

    public void setTransaction(String transaction) {
        this.transaction = transaction;
    }

    public PoolingSupport getPoolingSupport() {
        return poolingSupport;
    }

    public void setPoolingSupport(PoolingSupport poolingSupport) {
        this.poolingSupport = poolingSupport;
    }

    public boolean isPooling() {
        return pooling;
    }

    public void setPooling(boolean pooling) {
        this.pooling = pooling;
    }

    public String getPartitionStrategy() {
        return partitionStrategy;
    }

    public void setPartitionStrategy(String partitionStrategy) {
        this.partitionStrategy = partitionStrategy;
    }

    public int getPoolMaxSize() {
        return poolMaxSize;
    }

    public void setPoolMaxSize(int poolMaxSize) {
        this.poolMaxSize = poolMaxSize;
    }

    public int getPoolMinSize() {
        return poolMinSize;
    }

    public void setPoolMinSize(int poolMinSize) {
        this.poolMinSize = poolMinSize;
    }

    public boolean isAllConnectionsEqual() {
        return allConnectionsEqual;
    }

    public void setAllConnectionsEqual(boolean allConnectionsEqual) {
        this.allConnectionsEqual = allConnectionsEqual;
    }

    public int getConnectionMaxWaitMilliseconds() {
        return connectionMaxWaitMilliseconds;
    }

    public void setConnectionMaxWaitMilliseconds(int connectionMaxWaitMilliseconds) {
        this.connectionMaxWaitMilliseconds = connectionMaxWaitMilliseconds;
    }

    public int getConnectionMaxIdleMinutes() {
        return connectionMaxIdleMinutes;
    }

    public void setConnectionMaxIdleMinutes(int connectionMaxIdleMinutes) {
        this.connectionMaxIdleMinutes = connectionMaxIdleMinutes;
    }

    public boolean isValidateOnMatch() {
        return validateOnMatch;
    }

    public void setValidateOnMatch(boolean validateOnMatch) {
        this.validateOnMatch = validateOnMatch;
    }

    public boolean isBackgroundValidation() {
        return backgroundValidation;
    }

    public void setBackgroundValidation(boolean backgroundValidation) {
        this.backgroundValidation = backgroundValidation;
    }

    public int getBackgroundValidationMilliseconds() {
        return backgroundValidationMilliseconds;
    }

    public void setBackgroundValidationMilliseconds(int backgroundValidationMilliseconds) {
        this.backgroundValidationMilliseconds = backgroundValidationMilliseconds;
    }

    public SubjectSource getSubjectSource() {
        return subjectSource;
    }

    public void setSubjectSource(SubjectSource subjectSource) {
        this.subjectSource = subjectSource;
    }
}
