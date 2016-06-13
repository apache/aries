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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessManagedBean;
import javax.transaction.Transactional;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class TransactionExtension implements Extension {

    private Map<Method, Transactional> transactionAttributes;

    public TransactionExtension() {
        transactionAttributes = new HashMap<>();
    }

    <X> void processBean(@Observes ProcessManagedBean<X> event) {
        AnnotatedType<X> annotatedType = event.getAnnotatedBeanClass();
        Transactional classTx = annotatedType.getAnnotation(Transactional.class);
        for (AnnotatedMethod<? super X> am : annotatedType.getMethods()) {
            Transactional methodTx = am.getAnnotation(Transactional.class);
            if (classTx != null || methodTx != null) {
                Method method = am.getJavaMember();
                Transactional attrType = mergeTransactionAttributes(classTx, methodTx);
                transactionAttributes.put(method, attrType);
            }
        }
    }

    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery event, BeanManager manager) {
        event.addAnnotatedType(manager.createAnnotatedType(TransactionSupport.class));
        event.addAnnotatedType(manager.createAnnotatedType(TransactionalInterceptor.class));
        event.addInterceptorBinding(Transactional.class);
    }

    void afterBeanDiscovered(@Observes AfterBeanDiscovery event, BeanManager beanManager) {
        event.addContext(new TransactionalContext(beanManager));
    }

    private Transactional mergeTransactionAttributes(Transactional classAttribute, Transactional methodAttribute) {
        return methodAttribute != null ? methodAttribute : classAttribute;
    }

    Transactional getTransactionAttribute(Method method) {
        return transactionAttributes.get(method);
    }

}
