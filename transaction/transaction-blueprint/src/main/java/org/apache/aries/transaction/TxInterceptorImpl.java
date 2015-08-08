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

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.apache.aries.blueprint.Interceptor;
import org.apache.aries.transaction.annotations.TransactionPropagationType;
import org.apache.aries.transaction.exception.TransactionRollbackException;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.CoordinationException;
import org.osgi.service.coordinator.Coordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TxInterceptorImpl implements Interceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(TxInterceptorImpl.class);

    private TransactionManager tm;
    private Coordinator coordinator;
    private TxComponentMetaDataHelper metaDataHelper;

    public int getRank() {
        return 1; // Higher rank than jpa interceptor to make sure transaction is started first
    }

    public Object preCall(ComponentMetadata cm, Method m, Object... parameters) throws Throwable {
        final String methodName = m.getName();
        final TransactionPropagationType type = metaDataHelper.getComponentMethodTxAttribute(cm, methodName);

        // attribute could be null here which means no transaction
        if (type == null) {
            return null;
        }
        TransactionAttribute txAttribute = TransactionAttribute.fromValue(type);

        LOGGER.debug("PreCall for bean {}, method {} with tx strategy {}.", getCmId(cm), m.getName(), txAttribute);
        TransactionToken token = txAttribute.begin(tm);
        String coordName = "txInterceptor." + m.getDeclaringClass().getName() + "." + m.getName();
        Coordination coord = coordinator.begin(coordName , 0);
        token.setCoordination(coord);
        return token;
    }

    public void postCallWithException(ComponentMetadata cm, Method m, Throwable ex, Object preCallToken) {
        LOGGER.debug("PostCallWithException for bean {}, method {}.", getCmId(cm), m.getName(), ex);
        if (!(preCallToken instanceof TransactionToken)) {
            return;
        }
        final TransactionToken token = (TransactionToken)preCallToken;
        try {
            token.getCoordination().end();
        } catch (CoordinationException e){
            LOGGER.debug(e.getMessage(), e);
        }
        try {
            Transaction tran = token.getActiveTransaction();
            if (tran != null && isRollBackException(ex)) {
                tran.setRollbackOnly();
            }
            token.getTransactionAttribute().finish(tm, token);
        } catch (Exception e) {
            // we do not throw the exception since there already is one, but we need to log it
            LOGGER.warn("Exception during transaction cleanup", e);
        }
    }

    private String getCmId(ComponentMetadata cm) {
        return cm == null ? null : cm.getId();
    }

    public void postCallWithReturn(ComponentMetadata cm, Method m, Object returnType, Object preCallToken)
        throws Exception {
        LOGGER.debug("PostCallWithReturn for bean {}, method {}.", getCmId(cm), m);
        // it is possible transaction is not involved at all
        if (preCallToken == null) {
            return;
        }
        if (preCallToken instanceof TransactionToken) {
            final TransactionToken token = (TransactionToken)preCallToken;
            token.getCoordination().end();
            try {
                token.getTransactionAttribute().finish(tm, token);
            } catch (Exception e) {
                // We are throwing an exception, so we don't error it out
                LOGGER.debug(Constants.MESSAGES.getMessage("exception.during.tx.finish"), e);
                throw new TransactionRollbackException(e);
            }
        } else {
            // TODO: what now?
        }
    }

    private boolean isRollBackException(Throwable ex) {
        return ex instanceof RuntimeException || ex instanceof Error;
    }

    public final void setTransactionManager(TransactionManager manager) {
        tm = manager;
    }

    public void setCoordinator(Coordinator coordinator) {
        this.coordinator = coordinator;
    }

    public final void setTxMetaDataHelper(TxComponentMetaDataHelper transactionEnhancer) {
        this.metaDataHelper = transactionEnhancer;
    }
}
