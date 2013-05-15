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

import org.apache.geronimo.transaction.manager.NamedXAResource;
import org.apache.geronimo.transaction.manager.NamedXAResourceFactory;
import org.apache.geronimo.transaction.manager.RecoverableTransactionManager;
import org.apache.geronimo.transaction.manager.WrapperNamedXAResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.SystemException;
import javax.transaction.xa.XAResource;
import java.io.IOException;

/**
 * This class will ensure the broker is properly recovered when wired with
 * the Geronimo transaction manager.
 */
public class Recovery {

    private static final Logger LOGGER = LoggerFactory.getLogger(Recovery.class);

    public static boolean recover(final String name,
                                  final XADataSource dataSource,
                                  final RecoverableTransactionManager transactionManager) throws IOException {

        if (name != null && name.length() > 0) {
            transactionManager.registerNamedXAResourceFactory(new NamedXAResourceFactory() {

                public String getName() {
                    return name;
                }

                public NamedXAResource getNamedXAResource() throws SystemException {
                    try {
                        final XAConnection connection = dataSource.getXAConnection();
                        LOGGER.debug("new namedXAResource's connection: " + connection);

                        return new ConnectionAndWrapperNamedXAResource(connection.getXAResource(), getName(), connection);
                    } catch (Exception e) {
                        SystemException se =  new SystemException("Failed to create ConnectionAndWrapperNamedXAResource, " + e.getLocalizedMessage());
                        se.initCause(e);
                        LOGGER.error(se.getLocalizedMessage(), se);
                        throw se;
                    }
                }

                public void returnNamedXAResource(NamedXAResource namedXaResource) {
                    if (namedXaResource instanceof ConnectionAndWrapperNamedXAResource) {
                        try {
                            LOGGER.debug("closing returned namedXAResource's connection: " + ((ConnectionAndWrapperNamedXAResource)namedXaResource).connection);
                            ((ConnectionAndWrapperNamedXAResource)namedXaResource).connection.close();
                        } catch (Exception ignored) {
                            LOGGER.debug("failed to close returned namedXAResource: " + namedXaResource, ignored);
                        }
                    }
                }
            });
            return true;
        } else {
            LOGGER.warn("Unable to recover XADataSource: aries.xa.name property not set");
            return false;
        }
    }

    public static class ConnectionAndWrapperNamedXAResource extends WrapperNamedXAResource {

        final XAConnection connection;

        public ConnectionAndWrapperNamedXAResource(XAResource xaResource, String name, XAConnection connection) {
            super(xaResource, name);
            this.connection = connection;
        }
    }
}
