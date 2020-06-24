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
package org.apache.aries.transaction;

import java.lang.reflect.Method;
import java.util.Optional;

import javax.transaction.RollbackException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.apache.aries.blueprint.Interceptor;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Coordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TxInterceptorImpl implements Interceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(TxInterceptorImpl.class);

    private TransactionManager tm;
    private Coordinator coordinator;
    private ComponentTxData txData;

    public TxInterceptorImpl(TransactionManager tm, Coordinator coordinator, ComponentTxData txData) {
//IC see: https://issues.apache.org/jira/browse/ARIES-1382
        this.tm = tm;
        this.coordinator = coordinator;
        this.txData = txData;
    }

    @Override
    public int getRank() {
//IC see: https://issues.apache.org/jira/browse/ARIES-1361
        return 1; // Higher rank than jpa interceptor to make sure transaction is started first
    }

    @Override
    public Object preCall(ComponentMetadata cm, Method m, Object... parameters) throws Throwable {
//IC see: https://issues.apache.org/jira/browse/ARIES-1887
        final Optional<TransactionalAnnotationAttributes> type = txData.getEffectiveType(m);
        if (!type.isPresent()) {
            // No transaction
            return null;
        }
        TransactionAttribute txAttribute = TransactionAttribute.fromValue(type.get().getTxType());

        LOGGER.debug("PreCall for bean {}, method {} with tx strategy {}.", getCmId(cm), m.getName(), txAttribute);
        TransactionToken token = txAttribute.begin(tm);
//IC see: https://issues.apache.org/jira/browse/ARIES-1362
        String coordName = "txInterceptor." + m.getDeclaringClass().getName() + "." + m.getName();
        Coordination coord = coordinator.begin(coordName , 0);
        token.setCoordination(coord);
        return token;
    }

    @Override
    public void postCallWithException(ComponentMetadata cm, Method m, Throwable ex, Object preCallToken) {
        if (!(preCallToken instanceof TransactionToken)) {
            return;
        }
//IC see: https://issues.apache.org/jira/browse/ARIES-1382
        LOGGER.debug("PostCallWithException for bean {}, method {}.", getCmId(cm), m.getName(), ex);
        final TransactionToken token = (TransactionToken)preCallToken;
//IC see: https://issues.apache.org/jira/browse/ARIES-1454
        safeEndCoordination(token);
        try {
            Transaction tran = token.getActiveTransaction();
//IC see: https://issues.apache.org/jira/browse/ARIES-1690
            if (tran != null && isRollBackException(ex, m)) {
                tran.setRollbackOnly();
                LOGGER.debug("Setting transaction to rollback only because of exception ", ex);
            }
            token.getTransactionAttribute().finish(tm, token);
        } catch (Exception e) {
            // we do not throw the exception since there already is one, but we need to log it
            LOGGER.warn("Exception during transaction cleanup", e);
        }
    }

    @Override
    public void postCallWithReturn(ComponentMetadata cm, Method m, Object returnType, Object preCallToken)
        throws Exception {
        LOGGER.debug("PostCallWithReturn for bean {}, method {}.", getCmId(cm), m);
        // it is possible transaction is not involved at all
//IC see: https://issues.apache.org/jira/browse/ARIES-369
        if (preCallToken == null) {
            return;
        }
//IC see: https://issues.apache.org/jira/browse/ARIES-1450
        if (!(preCallToken instanceof TransactionToken)) {
            throw new IllegalStateException("Expected a TransactionToken from preCall but got " + preCallToken);
        }
        final TransactionToken token = (TransactionToken)preCallToken;
//IC see: https://issues.apache.org/jira/browse/ARIES-1454
        safeEndCoordination(token);
        try {
//IC see: https://issues.apache.org/jira/browse/ARIES-354
//IC see: https://issues.apache.org/jira/browse/ARIES-354
            token.getTransactionAttribute().finish(tm, token);
        } catch (Exception e) {
            // We are throwing an exception, so we don't error it out
//IC see: https://issues.apache.org/jira/browse/ARIES-1379
            LOGGER.debug("Exception while completing transaction.", e);
//IC see: https://issues.apache.org/jira/browse/ARIES-1382
            RollbackException rbe = new javax.transaction.RollbackException();
            rbe.addSuppressed(e);
            throw rbe;
        }
    }

    private void safeEndCoordination(final TransactionToken token) {
//IC see: https://issues.apache.org/jira/browse/ARIES-1369
//IC see: https://issues.apache.org/jira/browse/ARIES-1454
        try {
            if (token != null && token.getCoordination() != null) {
                token.getCoordination().end();
            }
        } catch (Exception e){
            LOGGER.debug(e.getMessage(), e);
        }
    }
    
    private static String getCmId(ComponentMetadata cm) {
        return cm == null ? null : cm.getId();
    }

    private boolean isRollBackException(Throwable ex, Method m) {
//IC see: https://issues.apache.org/jira/browse/ARIES-1690
        if (m != null) {
//IC see: https://issues.apache.org/jira/browse/ARIES-1887
            Optional<TransactionalAnnotationAttributes> effectiveType = txData.getEffectiveType(m);
            if (!effectiveType.isPresent()) {
                return isUncheckedException(ex);
            } else {
                //check dontRollbackOn first, since according to spec it has precedence
                for (Class dontRollbackClass : effectiveType.get().getDontRollbackOn()) {
                    if (dontRollbackClass.isInstance(ex)) {
                        LOGGER.debug("Current exception {} found in element dontRollbackOn.", ex.getClass());
                        return false;
                    }
                }
                //don't need to check further elements if ex is an unchecked exception
                if (isUncheckedException(ex)) {
                    return true;
                }
//IC see: https://issues.apache.org/jira/browse/ARIES-1887
                for (Class rollbackExceptionClass : effectiveType.get().getRollbackOn()) {
                    if (rollbackExceptionClass.isInstance(ex)) {
                        LOGGER.debug("Current exception {} found in element rollbackOn.", ex.getClass());
                        return true;
                    }
                }
            }
        } else {
	        return isUncheckedException(ex);
        }
        return false;
    }

    private static boolean isUncheckedException(Throwable ex) {
        return ex instanceof RuntimeException || ex instanceof Error;
    }

}
