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
import javax.transaction.Status;
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
        this.tm = tm;
        this.coordinator = coordinator;
        this.txData = txData;
    }

    @Override
    public int getRank() {
        return 1; // Higher rank than jpa interceptor to make sure transaction is started first
    }

    @Override
    public Object preCall(ComponentMetadata cm, Method m, Object... parameters) throws Throwable {
        final Optional<TransactionalAnnotationAttributes> type = txData.getEffectiveType(m);
        if (!type.isPresent()) {
            // No transaction
            return null;
        }
        TransactionAttribute txAttribute = TransactionAttribute.fromValue(type.get().getTxType());

        LOGGER.debug("PreCall for bean {}, method {} with tx strategy {}.", getCmId(cm), m.getName(), txAttribute);
        TransactionToken token = txAttribute.begin(tm);
        boolean requiresNewCoordination = token.requiresNewCoordination();
        if (!requiresNewCoordination && !transactionExists(coordinator)) {
            // in case the txAttribute doesn't require new coordination AND there's active transaction, like in:
            //  - REQUIRED, when there was TX active before the method
            //  - SUPPORTS, when there was TX active before the method
            //  - MANDATORY, when there was TX active before the method
            // we still have to create coordination
            if (token.getActiveTransaction() != null && tm.getStatus() == Status.STATUS_ACTIVE) {
                requiresNewCoordination = true;
            }
        }
        if (requiresNewCoordination) {
            String coordName = "txInterceptor." + m.getDeclaringClass().getName() + "." + m.getName();
            Coordination coord = coordinator.begin(coordName , 0);
            // @javax.transaction.Transactional is only part of 1.2 and even if it's about time that all previous
            // JTA versions should be forgotten, we can't rely on it...
            coord.getVariables().put(Transaction.class, txAttribute.name());
            token.setCoordination(coord);
        }
        return token;
    }

    /**
     * Checks whether there's already a {@link Coordination} with {@link Transaction} variable
     * @param coordinator
     * @return
     */
    private boolean transactionExists(Coordinator coordinator) {
        boolean exists = false;
        Coordination coord = coordinator.peek();
        while (coord != null) {
            if (coord.getVariables().containsKey(Transaction.class)) {
                return true;
            }
            coord = coord.getEnclosingCoordination();
        }
        return exists;
    }

    @Override
    public void postCallWithException(ComponentMetadata cm, Method m, Throwable ex, Object preCallToken) {
        if (!(preCallToken instanceof TransactionToken)) {
            return;
        }
        LOGGER.debug("PostCallWithException for bean {}, method {}.", getCmId(cm), m.getName(), ex);
        final TransactionToken token = (TransactionToken)preCallToken;
        safeEndCoordination(token);
        try {
            Transaction tran = token.getActiveTransaction();
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
        if (preCallToken == null) {
            return;
        }
        if (!(preCallToken instanceof TransactionToken)) {
            throw new IllegalStateException("Expected a TransactionToken from preCall but got " + preCallToken);
        }
        final TransactionToken token = (TransactionToken)preCallToken;
        safeEndCoordination(token);
        try {
            token.getTransactionAttribute().finish(tm, token);
        } catch (Exception e) {
            // We are throwing an exception, so we don't error it out
            LOGGER.debug("Exception while completing transaction.", e);
            RollbackException rbe = new javax.transaction.RollbackException();
            rbe.addSuppressed(e);
            throw rbe;
        }
    }

    private void safeEndCoordination(final TransactionToken token) {
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
        if (m != null) {
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
