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

public class ResourceLocalJpaTemplate extends AbstractJpaTemplate {
    private EmSupplier emSupplier;

    public ResourceLocalJpaTemplate(EmSupplier emSupplier) {
        this.emSupplier = emSupplier;
    }

    @Override
    public <R> R txExpr(TransactionType type, EmFunction<R> code) {
        EntityManager em = null;
        boolean weControlTx = false;
        if (type != TransactionType.Required) {
            throw new IllegalStateException("Only transation propagation type REQUIRED is supported");
        }
        try {
            emSupplier.preCall();
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
                safeRollback(em, e);
            }
            throw new RuntimeException(e);
        } finally {
            emSupplier.postCall();
        }
    }

    private void safeRollback(EntityManager em, Exception e) {
        if (em != null) {
            try {
                em.getTransaction().rollback();
            } catch (Exception e1) {
            }
        }
    }

}
