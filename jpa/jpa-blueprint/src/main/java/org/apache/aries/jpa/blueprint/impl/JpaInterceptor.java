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
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Coordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JpaInterceptor implements Interceptor {
    private static final Logger LOG = LoggerFactory.getLogger(JpaInterceptor.class);
    private EntityManager em;
    private Boolean cachedIsResourceLocal;
    private Coordinator coordinator;
    private BlueprintContainer container;
    private String coordinatorId;
    private String emId;

    public JpaInterceptor(BlueprintContainer container, String coordinatorId, String emId) {
        this.container = container;
        this.coordinatorId = coordinatorId;
        this.emId = emId;
    }

    @Override
    public int getRank() {
        return 0;
    }

    @Override
    public Object preCall(ComponentMetadata cm, Method m, Object... parameters) throws Throwable {
        if (coordinator == null) {
            initServices();
        }
        try {
            LOG.debug("PreCall for bean {}, method {}", cm.getId(), m.getName());
            Coordination coordination = coordinator.begin("jpa", 0);
            boolean weControlTx = isResourceLocal(em) && !em.getTransaction().isActive();
            if (weControlTx) {
                coordination.addParticipant(new ResourceLocalTransactionParticipant(em));
            }
            return coordination;
        } catch (Exception e) {
            LOG.warn("Exception from EmSupplier.preCall", e);
            throw new RuntimeException(e); // NOSONAR
        }
    }

    private synchronized void initServices() {
        if (coordinator == null) {
            coordinator = (Coordinator)container.getComponentInstance(coordinatorId);
            em = (EntityManager)container.getComponentInstance(emId);
        }
    }

    @Override
    public void postCallWithException(ComponentMetadata cm, Method m, Throwable ex, Object preCallToken) {
        LOG.debug("PostCallWithException for bean {}, method {}", cm.getId(), m.getName(), ex);
        if (preCallToken != null) {
            ((Coordination)preCallToken).fail(ex);
        }
    }

    @Override
    public void postCallWithReturn(ComponentMetadata cm, Method m, Object returnType, Object preCallToken)
        throws Exception {
        LOG.debug("PostCallWithReturn for bean {}, method {}", cm.getId(), m.getName());
        if (preCallToken != null) {
            ((Coordination)preCallToken).end();
        }
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
        return transactionType == PersistenceUnitTransactionType.RESOURCE_LOCAL;
    }
}
