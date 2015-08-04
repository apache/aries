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
package org.apache.aries.jpa.blueprint.impl;

import java.lang.reflect.Method;

import javax.persistence.EntityManager;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.apache.aries.blueprint.Interceptor;
import org.apache.aries.jpa.supplier.EmSupplier;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Coordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JpaInterceptor implements Interceptor {
    private static Logger LOG = LoggerFactory.getLogger(JpaInterceptor.class);
    EmSupplier emSupplier;
    private Boolean cachedIsResourceLocal;
    private Coordinator coordinator;

    public JpaInterceptor(EmSupplier emSupplier, Coordinator coordinator) {
        this.emSupplier = emSupplier;
        this.coordinator = coordinator;
    }

    public int getRank() {
        return 0;
    }

    public Object preCall(ComponentMetadata cm, Method m, Object... parameters) throws Throwable {
        try {
            LOG.debug("PreCall for bean {}, method {}", cm.getId(), m.getName());
            Coordination coordination = coordinator.begin("jpa", 0);
            final EntityManager em = emSupplier.get();
            boolean weControlTx = isResourceLocal(em) && !em.getTransaction().isActive();
            if (weControlTx) {
                em.getTransaction().begin();
                coordination.addParticipant(new JPAParticipant(em));
            }
            return coordination;
        } catch (Exception e) {
            LOG.warn("Exception from EmSupplier.preCall", e);
            throw new RuntimeException(e);
        }
    }

    public void postCallWithException(ComponentMetadata cm, Method m, Throwable ex, Object preCallToken) {
        LOG.debug("PostCallWithException for bean {}, method {}", cm.getId(), m.getName(), ex);
        ((Coordination) preCallToken).fail(ex);
    }

    public void postCallWithReturn(ComponentMetadata cm, Method m, Object returnType, Object preCallToken)
        throws Exception {
        LOG.debug("PostCallWithReturn for bean {}, method {}", cm.getId(), m.getName());
        ((Coordination) preCallToken).end();
    }



    private boolean isResourceLocal(EntityManager em) {
        if (cachedIsResourceLocal == null) {
            cachedIsResourceLocal = isResourceLocalInternal(em);
        }
        return cachedIsResourceLocal;
    }

    /**
     * @param em
     * @return
     */
    private boolean isResourceLocalInternal(EntityManager em) {
        PersistenceUnitTransactionType transactionType = (PersistenceUnitTransactionType)em.getProperties()
            .get(PersistenceUnitTransactionType.class.getName());
        if (transactionType == PersistenceUnitTransactionType.RESOURCE_LOCAL) {
            return true;
        } else {
            return false;
        }
    }
}
