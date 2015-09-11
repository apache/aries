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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.jpa.support.impl;

import javax.persistence.EntityManager;
import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.apache.aries.jpa.supplier.EmSupplier;
import org.apache.aries.jpa.support.xa.impl.TransactionAttribute;
import org.apache.aries.jpa.support.xa.impl.TransactionToken;
import org.apache.aries.jpa.template.EmFunction;
import org.apache.aries.jpa.template.TransactionType;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Coordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XAJpaTemplate extends AbstractJpaTemplate {
    private static final Logger LOGGER = LoggerFactory.getLogger(XAJpaTemplate.class);
    protected EmSupplier emSupplier;
    protected TransactionManager tm;
    private Coordinator coordinator;

    public XAJpaTemplate(EmSupplier emSupplier, TransactionManager tm, Coordinator coordinator) {
        this.emSupplier = emSupplier;
        this.tm = tm;
        this.coordinator = coordinator;
    }

    @Override
    public <R> R txExpr(TransactionType type, EmFunction<R> code) {
        EntityManager em = null;
        TransactionToken tranToken = null;
        TransactionAttribute ta = TransactionAttribute.fromType(type);
        Coordination coord = null;
        try {
            tranToken = ta.begin(tm);
            coord = coordinator.begin(this.getClass().getName(), 0);
            em = emSupplier.get();
            if (tm.getStatus() != Status.STATUS_NO_TRANSACTION) {
                em.joinTransaction();
            }
            R result = (R)code.apply(em);
            return result;
        } catch (Throwable ex) {
            safeRollback(tranToken, ex);
            throw new RuntimeException(ex);
        } finally {
            try {
                ta.finish(tm, tranToken);
                coord.end();
            } catch (Exception e) {
                // We are throwing an exception, so we don't error it out
                LOGGER.debug("exception.during.tx.finish", e);
                throw new RuntimeException("Exception during finish of transaction", e);
            }
        }
    }

    private void safeRollback(TransactionToken token, Throwable ex) {
        try {
            Transaction tran = token.getActiveTransaction();
            if (tran != null) {
                if (ex instanceof RuntimeException || ex instanceof Error) {
                    tran.setRollbackOnly();
                } else {
                    // declared exception, we don't set rollback
                }
            }
        } catch (Exception e) {
            // we do not throw the exception since there already is one, but we
            // need to log it
            LOGGER.warn("exception.during.tx.cleanup", e);
        }
    }

}
