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

import java.sql.SQLException;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.TransactionSupport;
import javax.security.auth.Subject;
import javax.sql.XAConnection;
import javax.sql.XADataSource;

import org.tranql.connector.CredentialExtractor;
import org.tranql.connector.jdbc.AbstractXADataSourceMCF;
import org.tranql.connector.jdbc.ManagedXAConnection;

public class XADataSourceMCFFactory extends AbstractMCFFactory {

    public void init() throws Exception {
        if (getDataSource() == null) {
            throw new IllegalArgumentException("dataSource must be set");
        }
        if (connectionFactory == null) {
            connectionFactory = new XADataSourceMCF();
        }
    }

    @SuppressWarnings("serial")
    public class XADataSourceMCF extends AbstractXADataSourceMCF<XADataSource> implements TransactionSupport {

        public XADataSourceMCF() {
            super((XADataSource) XADataSourceMCFFactory.this.getDataSource(), XADataSourceMCFFactory.this.getExceptionSorter());
        }

        public String getUserName() {
            return XADataSourceMCFFactory.this.getUserName();
        }

        @Override
        public String getPassword() {
            return XADataSourceMCFFactory.this.getPassword();
        }

        @Override
        public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo connectionRequestInfo) throws ResourceException {
            CredentialExtractor credentialExtractor = new CredentialExtractor(subject, connectionRequestInfo, this);

            XAConnection sqlConnection = getPhysicalConnection(credentialExtractor);
            try {
                return new ManagedXAConnection(this, sqlConnection, credentialExtractor, exceptionSorter) {
                    @Override
                    public void cleanup() throws ResourceException {
                        // ARIES-1279 - Transaction does not work on error SQLException
                        // that's why we don't call super.cleanup() which calls con.setAutocommit(true)
                        // super.cleanup();
                        dissociateConnections();
                    }
                };
            } catch (SQLException e) {
                throw new ResourceAdapterInternalException("Could not set up ManagedXAConnection", e);
            }
        }

        @Override
        protected XAConnection getPhysicalConnection(CredentialExtractor credentialExtractor) throws ResourceException {
            try {
                String userName = credentialExtractor.getUserName();
                String password = credentialExtractor.getPassword();
                if (userName != null) {
                    return xaDataSource.getXAConnection(userName, password);
                } else {
                    return xaDataSource.getXAConnection();
                }
            } catch (SQLException e) {
                throw new ResourceAdapterInternalException("Unable to obtain physical connection to " + xaDataSource, e);
            }
        }

        @Override
        public TransactionSupportLevel getTransactionSupport() {
            return TransactionSupportLevel.XATransaction;
        }
    }

}
