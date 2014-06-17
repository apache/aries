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

import org.apache.geronimo.connector.outbound.AbstractSinglePoolConnectionInterceptor;
import org.apache.geronimo.connector.outbound.ConnectionInfo;
import org.apache.geronimo.connector.outbound.ConnectionInterceptor;
import org.apache.geronimo.connector.outbound.ConnectionReturnAction;
import org.apache.geronimo.connector.outbound.GenericConnectionManager;
import org.apache.geronimo.connector.outbound.ManagedConnectionInfo;
import org.apache.geronimo.connector.outbound.MultiPoolConnectionInterceptor;
import org.apache.geronimo.connector.outbound.SinglePoolConnectionInterceptor;
import org.apache.geronimo.connector.outbound.SinglePoolMatchAllConnectionInterceptor;
import org.apache.geronimo.connector.outbound.SubjectSource;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.PoolingSupport;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.TransactionSupport;
import org.apache.geronimo.connector.outbound.connectiontracking.ConnectionTracker;
import org.apache.geronimo.transaction.manager.RecoverableTransactionManager;

import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ValidatingManagedConnectionFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReadWriteLock;

public final class ValidatingGenericConnectionManager extends GenericConnectionManager {

    private static final Timer TIMER = new Timer("ValidatingGenericConnectionManagerTimer", true);

    private transient final TimerTask validatingTask;
    private final long validatingInterval;

    private final ReadWriteLock lock;
    private final Object pool;

    public ValidatingGenericConnectionManager(TransactionSupport transactionSupport, PoolingSupport pooling, SubjectSource subjectSource, ConnectionTracker connectionTracker, RecoverableTransactionManager transactionManager, ManagedConnectionFactory mcf, String name, ClassLoader classLoader, long interval) {
        super(transactionSupport, pooling, subjectSource, connectionTracker, transactionManager, mcf, name, classLoader);
        validatingInterval = interval;

        ConnectionInterceptor stack = interceptors.getStack();

        ReadWriteLock foundLock = null;
        ConnectionInterceptor current = stack;
        do {
            if (current instanceof AbstractSinglePoolConnectionInterceptor) {
                try {
                    foundLock = (ReadWriteLock) Reflections.get(current, "resizeLock");
                } catch (Exception e) {
                    // no-op
                }
                break;
            }

            // look next
            try {
                current = (ConnectionInterceptor) Reflections.get(current, "next");
            } catch (Exception e) {
                current = null;
            }
        } while (current != null);

        this.lock = foundLock;

        Object foundPool = null;
        if (current instanceof AbstractSinglePoolConnectionInterceptor) {
            foundPool = Reflections.get(stack, "pool");
        } else if (current instanceof MultiPoolConnectionInterceptor) {
            log.warn("validation on stack {} not supported", stack);
        }
        this.pool = foundPool;

        if (pool != null) {
            validatingTask = new ValidatingTask(current, lock, pool);
        } else {
            validatingTask = null;
        }
    }

    @Override
    public void doStart() throws Exception {
        super.doStart();
        if (validatingTask != null) {
            TIMER.schedule(validatingTask, validatingInterval, validatingInterval);
        }
    }

    @Override
    public void doStop() throws Exception {
        if (validatingTask != null) {
            validatingTask.cancel();
        }
        super.doStop();
    }

    private class ValidatingTask extends TimerTask {

        private final ConnectionInterceptor stack;
        private final ReadWriteLock lock;
        private final Object pool;

        public ValidatingTask(ConnectionInterceptor stack, ReadWriteLock lock, Object pool) {
            this.stack = stack;
            this.lock = lock;
            this.pool = pool;
        }

        @Override
        public void run() {
            if (lock != null) {
                lock.writeLock().lock();
            }

            try {
                final Map<ManagedConnection, ManagedConnectionInfo> connections;
                if (stack instanceof SinglePoolConnectionInterceptor) {
                    connections = new HashMap<ManagedConnection, ManagedConnectionInfo>();
                    for (ManagedConnectionInfo info : (List<ManagedConnectionInfo>) pool) {
                        connections.put(info.getManagedConnection(), info);
                    }
                } else if (stack instanceof SinglePoolMatchAllConnectionInterceptor) {
                    connections = (Map<ManagedConnection, ManagedConnectionInfo>) pool;
                } else {
                    log.warn("stack {} currently not supported", stack);
                    return;
                }

                // destroy invalid connections
                try {
                    Set<ManagedConnection> invalids = ValidatingManagedConnectionFactory.class.cast(getManagedConnectionFactory()).getInvalidConnections(connections.keySet());
                    if (invalids != null) {
                        for (ManagedConnection invalid : invalids) {
                            stack.returnConnection(new ConnectionInfo(connections.get(invalid)), ConnectionReturnAction.DESTROY);
                        }
                    }
                } catch (ResourceException e) {
                    log.error(e.getMessage(), e);
                }
            } finally {
                if (lock != null) {
                    lock.writeLock().unlock();
                }
            }
        }
    }
}
