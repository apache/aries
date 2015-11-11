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

import org.apache.aries.jpa.supplier.EmSupplier;
import org.apache.aries.jpa.template.EmFunction;
import org.apache.aries.jpa.template.TransactionType;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Coordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceLocalJpaTemplate extends AbstractJpaTemplate {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceLocalJpaTemplate.class);
    private EmSupplier emSupplier;
    private Coordinator coordinator;

    public ResourceLocalJpaTemplate(EmSupplier emSupplier, Coordinator coordinator) {
        this.emSupplier = emSupplier;
        this.coordinator = coordinator;
    }

    @Override
    public <R> R txExpr(TransactionType type, EmFunction<R> code) {
        EntityManager em = null;
        boolean weControlTx = false;
        if (type != TransactionType.Required) {
            throw new IllegalStateException("Only transation propagation type REQUIRED is supported");
        }
        Coordination coord = coordinator.begin(this.getClass().getName(), 0);
        try {
            em = emSupplier.get();
            weControlTx = !em.getTransaction().isActive();
            if (weControlTx) {
                em.getTransaction().begin();
            }
            R result = (R)code.apply(em);
            if (weControlTx) {
                em.getTransaction().commit();
            }
            return result;
        } catch (Exception e) {
            if (weControlTx) {
                safeRollback(em);
            }
            throw wrapThrowable(e, "Exception occured in transactional code");
        } finally {
            coord.end();
        }
    }

    private static void safeRollback(EntityManager em) {
        if (em != null) {
            try {
                em.getTransaction().rollback();
            } catch (Exception e) {
                LOGGER.warn("Exception during transaction rollback", e);
            }
        }
    }

}
