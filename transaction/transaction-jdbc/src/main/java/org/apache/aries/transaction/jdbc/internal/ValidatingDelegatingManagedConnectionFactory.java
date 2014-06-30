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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tranql.connector.AbstractManagedConnection;
import org.tranql.connector.ManagedConnectionHandle;
import org.tranql.connector.UserPasswordManagedConnectionFactory;
import org.tranql.connector.jdbc.ConnectionHandle;
import org.tranql.connector.jdbc.TranqlDataSource;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.TransactionSupport;
import javax.resource.spi.ValidatingManagedConnectionFactory;
import javax.security.auth.Subject;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public final class ValidatingDelegatingManagedConnectionFactory implements UserPasswordManagedConnectionFactory, ValidatingManagedConnectionFactory, TransactionSupport {

    private static final Logger LOG = LoggerFactory.getLogger(ValidatingDelegatingManagedConnectionFactory.class);
    private final ManagedConnectionFactory delegate;

    public ValidatingDelegatingManagedConnectionFactory(ManagedConnectionFactory managedConnectionFactory) {
        this.delegate = managedConnectionFactory;
    }

    private boolean isValidConnection(Connection c) {
        try {
            if (c.isValid(0)) {
                LOG.debug("Connection validation succeeded for managed connection {}.", c);
                return true;
            } else {
                LOG.debug("Connection validation failed for managed connection {}.", c);
            }
        } catch (SQLException e) {
            // no-op
        }
        return false;
    }

    @Override
    public TransactionSupportLevel getTransactionSupport() {
        return TransactionSupport.class.cast(delegate).getTransactionSupport();
    }

    @Override
    public Set getInvalidConnections(Set connectionSet) throws ResourceException {
        Set<ManagedConnection> invalid = new HashSet<ManagedConnection>();

        for (Object o : connectionSet) {
            if (o instanceof AbstractManagedConnection) {
                AbstractManagedConnection<Connection, ConnectionHandle> amc = AbstractManagedConnection.class.cast(o);

                if (!isValidConnection(amc.getPhysicalConnection())) {
                    invalid.add(amc);
                }
            }
        }

        return invalid;
    }

    @Override
    public String getUserName() {
        return UserPasswordManagedConnectionFactory.class.cast(delegate).getUserName();
    }

    @Override
    public String getPassword() {
        return UserPasswordManagedConnectionFactory.class.cast(delegate).getPassword();
    }

    @Override
    public Object createConnectionFactory(ConnectionManager cxManager) throws ResourceException {
        return new TranqlDataSource(this, cxManager);
    }

    @Override
    public Object createConnectionFactory() throws ResourceException {
        throw new NotSupportedException("ConnectionManager is required");
    }

    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        return delegate.createManagedConnection(subject, cxRequestInfo);
    }

    @Override
    public ManagedConnection matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        for (Object o : connectionSet) {
            if (o instanceof ManagedConnectionHandle) {
                ManagedConnectionHandle mch = ManagedConnectionHandle.class.cast(o);
                if (mch.matches(this, subject, cxRequestInfo)) {
                    if (mch instanceof AbstractManagedConnection) {
                        AbstractManagedConnection<Connection, ConnectionHandle> amc = AbstractManagedConnection.class.cast(mch);
                        if (isValidConnection(amc.getPhysicalConnection())) {
                            return amc;
                        }
                    } else {
                        return mch;
                    }
                }
            }
        }

        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws ResourceException {
        delegate.setLogWriter(out);
    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return delegate.getLogWriter();
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return delegate.equals(other);
    }
}
