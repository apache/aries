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
package org.apache.aries.transaction.jms.internal;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.XAConnection;
import javax.jms.XAConnectionFactory;
import javax.transaction.TransactionManager;

import org.apache.aries.transaction.jms.PooledConnectionFactory;

/**
 * A pooled connection factory that automatically enlists
 * sessions in the current active XA transaction if any.
 */
public class XaPooledConnectionFactory extends PooledConnectionFactory {

    private XAConnectionFactory xaConnectionFactory;
    private TransactionManager transactionManager;
    
    public XaPooledConnectionFactory() {
        super();
    }

    public XAConnectionFactory getXaConnectionFactory() {
        return xaConnectionFactory;
    }

    public void setXaConnectionFactory(XAConnectionFactory xaConnectionFactory) {
    	this.xaConnectionFactory = xaConnectionFactory;
        setConnectionFactory(new ConnectionFactory() {
            public Connection createConnection() throws JMSException {
                return XaPooledConnectionFactory.this.xaConnectionFactory.createXAConnection();
            }
            public Connection createConnection(String userName, String password) throws JMSException {
                return XaPooledConnectionFactory.this.xaConnectionFactory.createXAConnection(userName, password);
            }
        });
    }

    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    /**
     * The XA TransactionManager to use to enlist the JMS sessions into.
     *
     * @org.apache.xbean.Property required=true
     */
    public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    protected ConnectionPool createConnectionPool(Connection connection) throws JMSException {
        return new XaConnectionPool((XAConnection) connection, getPoolFactory(), getTransactionManager());
    }
}
