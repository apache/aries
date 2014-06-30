/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.transaction.jms;

import javax.jms.JMSException;
import javax.jms.Connection;
import javax.jms.XAConnection;

import org.apache.aries.transaction.jms.internal.ConnectionPool;
import org.apache.aries.transaction.jms.internal.GenericResourceManager;
import org.apache.aries.transaction.jms.internal.RecoverableConnectionPool;
import org.apache.aries.transaction.jms.internal.XaPooledConnectionFactory;

/**
 * A pooled connection factory which is dedicated to work with the Geronimo/Aries
 * transaction manager for proper recovery of in-flight transactions after a
 * crash.
 *
 * @org.apache.xbean.XBean element="xaPooledConnectionFactory"
 */
public class RecoverablePooledConnectionFactory extends XaPooledConnectionFactory {

    private String name;

    public RecoverablePooledConnectionFactory() {
        super();
    }

    public String getName() {
        return name;
    }

    /**
     * The unique name for this managed XAResource.  This name will be used
     * by the transaction manager to recover transactions.
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    protected ConnectionPool createConnectionPool(Connection connection) {
        return new RecoverableConnectionPool(connection, getTransactionManager(), getName());
    }

    /**
     * @org.apache.xbean.InitMethod
     */
    @Override
    public void start() {
        if (getConnectionFactory() == null) {
            throw new IllegalArgumentException("connectionFactory must be set");
        }
        if (getTransactionManager() == null) {
            throw new IllegalArgumentException("transactionManager must be set");
        }
        super.start();
        new GenericResourceManager(name, getTransactionManager(), getConnectionFactory()).recoverResource();
    }
}
