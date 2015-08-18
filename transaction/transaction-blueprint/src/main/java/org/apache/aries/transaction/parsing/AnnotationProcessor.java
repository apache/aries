/**
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
package org.apache.aries.transaction.parsing;

import javax.transaction.TransactionManager;

import org.apache.aries.blueprint.BeanProcessor;
import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.transaction.ComponentTxData;
import org.apache.aries.transaction.TxInterceptorImpl;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.coordinator.Coordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds the transactional interceptor if Transaction annotation is present
 * on bean class or superclasses.
 */
public class AnnotationProcessor implements BeanProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationProcessor.class);

    private final ComponentDefinitionRegistry cdr;
    private TransactionManager tm;
    private Coordinator coordinator;

    public AnnotationProcessor(ComponentDefinitionRegistry cdr, TransactionManager tm, Coordinator coordinator) {
        this.cdr = cdr;
        this.tm = tm;
        this.coordinator = coordinator;
    }

    public void afterDestroy(Object arg0, String arg1) {
    }

    public Object afterInit(Object arg0, String arg1, BeanCreator arg2, BeanMetadata arg3) {
        return arg0;
    }

    public void beforeDestroy(Object arg0, String arg1) {
    }

    public Object beforeInit(Object bean, String beanName, BeanCreator beanCreator, BeanMetadata beanData) {
        ComponentTxData txData = new ComponentTxData(bean.getClass());
        if (txData.isTransactional()) {
            LOGGER.debug("Adding transaction interceptor to bean {} with class {}.", beanName, bean.getClass());
            cdr.registerInterceptorWithComponent(beanData, new TxInterceptorImpl(tm, coordinator, txData));
        }
        return bean;
    }

}
