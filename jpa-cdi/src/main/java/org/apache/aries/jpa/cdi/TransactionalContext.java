/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.jpa.cdi;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionScoped;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionalContext implements Context {

    private static final Object TRANSACTION_BEANS_KEY = TransactionalContext.class.getName() + ".TRANSACTION_BEANS";

    private static final Logger log = LoggerFactory.getLogger(TransactionalContext.class);

    private BeanManager beanManager;

    private volatile TransactionSupport transactionSupport;

    /**
     * Creates a new transactional context.
     *
     * @param beanManager {@link BeanManager}.
     */
    TransactionalContext(BeanManager beanManager) {
        this.beanManager = beanManager;
    }

    /**
     * Obtains a reference to a {@link TransactionSupport} bean.
     *
     * @return a bean that implements {@link TransactionSupport}.
     */
    private TransactionSupport getTransactionSupportReference() {
        @SuppressWarnings("unchecked")
        Bean<TransactionSupport> bean = (Bean<TransactionSupport>) beanManager
                .resolve(beanManager.getBeans(TransactionSupport.class));
        if (bean == null) {
            throw new RuntimeException("TransactionSupport was not found");
        }

        CreationalContext<TransactionSupport> ctx = beanManager.createCreationalContext(bean);
        return (TransactionSupport) beanManager.getReference(bean,
                TransactionSupport.class, ctx);
    }

    /**
     * Lazily initialize the object field and gets a reference to a bean that
     * implements {@link TransactionSupport}.
     *
     * @return a bean that implements {@link TransactionSupport}.
     */
    private TransactionSupport getTransactionSupport() {
        if (transactionSupport == null) {
            synchronized (this) {
                if (transactionSupport == null) {
                    transactionSupport = getTransactionSupportReference();
                }
            }
        }

        return transactionSupport;
    }

    /**
     * Registers a synchronization object for the current transaction.
     *
     * @param transactionSupport a {@link TransactionSupport} bean.
     * @param instances          a map that contains transaction scoped beans for the
     *                           current transaction.
     */
    private <T> void registerSynchronization(
            TransactionSupport transactionSupport,
            Map<Contextual<T>, ContextualInstance<T>> instances) {
        transactionSupport.getTransactionSynchronizationRegistry()
                .registerInterposedSynchronization(new TransactionSynchronization<T>(instances));
    }

    /**
     * Retrieves the map that contains transaction scoped beans for the current
     * transaction.
     *
     * @param transactionSupport a bean that implements {@link TransactionSupport}.
     * @return instances of transaction scoped beans for the current
     * transaction.
     */
    private <T> Map<Contextual<T>, ContextualInstance<T>> getInstances(
            TransactionSupport transactionSupport) {
        @SuppressWarnings("unchecked")
        Map<Contextual<T>, ContextualInstance<T>> instances =
                (Map<Contextual<T>, ContextualInstance<T>>) transactionSupport.getTransactionSynchronizationRegistry()
                        .getResource(TRANSACTION_BEANS_KEY);
        if (instances == null) {
            instances = new HashMap<Contextual<T>, ContextualInstance<T>>();
            transactionSupport
                    .getTransactionSynchronizationRegistry()
                    .putResource(TRANSACTION_BEANS_KEY, instances);

            registerSynchronization(transactionSupport, instances);
        }

        return instances;
    }

    /**
     * {@inheritDoc}
     */
    public <T> T get(
            Contextual<T> contextual) {
        return get(contextual, null);
    }

    /**
     * {@inheritDoc}
     */
    public <T> T get(
            Contextual<T> contextual,
            CreationalContext<T> creationalContext) {
        if (!isActive()) {
            throw new ContextNotActiveException();
        }

        if (contextual == null) {
            throw new IllegalArgumentException(
                    "No contextual specified to retrieve");
        }

        TransactionSupport transactionSupport = getTransactionSupport();
        Map<Contextual<T>, ContextualInstance<T>> instances =
                getInstances(transactionSupport);
        ContextualInstance<T> contextualInstance = instances.get(contextual);
        if (contextualInstance != null) {
            return contextualInstance.instance;
        } else if (creationalContext == null) {
            return null;
        } else {
            T instance = contextual.create(creationalContext);
            contextualInstance = new ContextualInstance<T>(instance,
                    creationalContext);
            instances.put(contextual, contextualInstance);

            if (log.isDebugEnabled()) {
                log.debug("Created a new transaction scoped instance "
                        + contextualInstance);
            }

            return instance;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Class<? extends Annotation> getScope() {
        return TransactionScoped.class;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isActive() {
        try {
            return getTransactionSupport().getTransactionManager().getStatus() == Status.STATUS_ACTIVE;
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * <p>
     * Synchronization object that destroys transaction scoped beans after the
     * transaction ends.
     * </p>
     *
     * @author Vlad Arkhipov
     */
    private static class TransactionSynchronization<T>
            implements Synchronization {
        private Map<Contextual<T>, ContextualInstance<T>> instances;

        /**
         * Creates a new synchronization.
         *
         * @param instances instances of transaction scoped beans for the
         *                  current transaction.
         */
        TransactionSynchronization(
                Map<Contextual<T>, ContextualInstance<T>> instances) {
            this.instances = instances;
        }

        /**
         * {@inheritDoc}
         */
        public void beforeCompletion() {
        }

        /**
         * {@inheritDoc}
         */
        public void afterCompletion(
                int status) {
            for (Map.Entry<Contextual<T>, ContextualInstance<T>> entry : instances.entrySet()) {
                entry.getValue().destroy(entry.getKey());
            }
        }
    }

    /**
     * <p>
     * An object that represents an instance of a bean. Contains a reference for
     * a bean and {@link CreationalContext}.
     * </p>
     *
     * @author Vlad Arkhipov
     */
    private static class ContextualInstance<T> {
        private T instance;

        private CreationalContext<T> creationalContext;

        /**
         * Creates a new object that represents an instance of a bean.
         *
         * @param instance          an instance of a bean.
         * @param creationalContext a {@link CreationalContext}.
         */
        public ContextualInstance(
                T instance,
                CreationalContext<T> creationalContext) {
            this.instance = instance;
            this.creationalContext = creationalContext;
        }

        /**
         * Destroys the bean.
         *
         * @param contextual a {@link Contextual}.
         */
        void destroy(
                Contextual<T> contextual) {
            if (log.isDebugEnabled()) {
                log.debug("Destroying transaction scoped bean instance "
                        + this);
            }

            contextual.destroy(instance, creationalContext);
            creationalContext.release();
        }
    }
}