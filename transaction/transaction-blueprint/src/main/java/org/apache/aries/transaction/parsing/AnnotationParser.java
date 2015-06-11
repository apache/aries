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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.apache.aries.blueprint.BeanProcessor;
import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.Interceptor;
import org.apache.aries.transaction.Constants;
import org.apache.aries.transaction.TxComponentMetaDataHelper;
import org.apache.aries.transaction.annotations.Transaction;
import org.apache.aries.transaction.annotations.TransactionPropagationType;
import org.osgi.service.blueprint.reflect.BeanMetadata;

import javax.transaction.Transactional;

/**
 * Adds the transactional interceptor if Transaction annotation is present
 * on bean class or superclasses.
 */
public class AnnotationParser implements BeanProcessor {

    private final ComponentDefinitionRegistry cdr;
    private final Interceptor interceptor;
    private final TxComponentMetaDataHelper helper;

    public AnnotationParser(ComponentDefinitionRegistry cdr, Interceptor i, TxComponentMetaDataHelper helper) {
        this.cdr = cdr;
        this.interceptor = i;
        this.helper = helper;
    }

    public void afterDestroy(Object arg0, String arg1) {
    }

    public Object afterInit(Object arg0, String arg1, BeanCreator arg2, BeanMetadata arg3) {
        return arg0;
    }

    public void beforeDestroy(Object arg0, String arg1) {
    }

    public Object beforeInit(Object bean, String beanName, BeanCreator beanCreator, BeanMetadata beanData) {
        Class<?> c = bean.getClass();
        boolean interceptorAssigned = isInterceptorAssigned(beanData);
        while (c != Object.class) {
            for (Method m : c.getDeclaredMethods()) {
                try {
                    if (beanData != null && helper.getComponentMethodTxAttribute(beanData, m.getName()) == null) {
                        Transaction t = tryGetTransaction(m);
                        if (t != null) {
                            assertAllowedModifier(m);
                            helper.setComponentTransactionData(cdr, beanData, t.value(), m.getName());
                            if (!interceptorAssigned) {
                                cdr.registerInterceptorWithComponent(beanData, interceptor);
                                interceptorAssigned = true;
                            }
                        }
                    }
                } catch(IllegalStateException e) {
                    // don't break bean creation due to invalid transaction attribute
                }

            }
            c = c.getSuperclass();
        }
        return bean;
    }

    private Transaction tryGetTransaction(Method m) {
        Transaction t = m.getAnnotation(Transaction.class);
        if (t == null) {
            final Transactional jtaT = m.getAnnotation(Transactional.class);
            if (jtaT != null) {
                t = new Transaction() {
                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return Transactional.class;
                    }

                    @Override
                    public TransactionPropagationType value() {
                        return convert(jtaT.value());
                    }
                };
            }
        }
        return t;
    }

    private static TransactionPropagationType convert(Transactional.TxType type) {
        switch(type) {
            case MANDATORY:
                return TransactionPropagationType.Mandatory;
            case REQUIRED:
                return TransactionPropagationType.Required;
            case REQUIRES_NEW:
                return TransactionPropagationType.RequiresNew;
            case SUPPORTS:
                return TransactionPropagationType.Supports;
            case NOT_SUPPORTED:
                return TransactionPropagationType.NotSupported;
            case NEVER:
                return TransactionPropagationType.Never;
        }
        throw new IllegalArgumentException(Constants.MESSAGES.getMessage("unknown.jta.transaction.type", type.name()));
    }

    private boolean isInterceptorAssigned(BeanMetadata beanData) {
        for (Interceptor i : cdr.getInterceptors(beanData)) {
            if (i == interceptor) {
                return true;
            }
        }
        return false;
    }

    private void assertAllowedModifier(Method m) {
        int modifiers = m.getModifiers();
        if ((modifiers & Constants.BANNED_MODIFIERS) != 0)
            throw new IllegalArgumentException(Constants.MESSAGES.getMessage("private.or.static.method", m));
    }

}
