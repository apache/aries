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
package org.apache.aries.transaction;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComponentTxData {
    private static final Logger LOG = LoggerFactory.getLogger(ComponentTxData.class);
    private static final int BANNED_MODIFIERS = Modifier.PRIVATE | Modifier.STATIC;
    
    private Map<Method, Optional<TransactionalAnnotationAttributes>> txMap = new ConcurrentHashMap<Method, Optional<TransactionalAnnotationAttributes>>();
    private boolean isTransactional;
    private Class<?> beanClass;
    
    public ComponentTxData(Class<?> c) {
        beanClass = c;
        isTransactional = false;
        // Check class hierarchy
        Class<?> current = c;
        while (current != Object.class) {
            isTransactional |= parseTxData(current);
            for (Class<?> iface : current.getInterfaces()) {
                isTransactional |= parseTxData(iface);
            }
            current = current.getSuperclass();
        }
    }

    Optional<TransactionalAnnotationAttributes> getEffectiveType(Method m) {
        if (txMap.containsKey(m)) {
            return getTxAttr(m);
        }
        try {
            Method effectiveMethod = beanClass.getDeclaredMethod(m.getName(), m.getParameterTypes());
            Optional<TransactionalAnnotationAttributes> txAttr = getTxAttr(effectiveMethod);
            txMap.put(m, txAttr);
            return txAttr;
        } catch (NoSuchMethodException e) { // NOSONAR
            return getFromMethod(m);
        } catch (SecurityException e) {
            throw new RuntimeException("Security exception when determining effective method", e); // NOSONAR
        }
    }

    private Optional<TransactionalAnnotationAttributes> getFromMethod(Method m) {
        try {
            Method effectiveMethod = beanClass.getMethod(m.getName(), m.getParameterTypes());
            Optional<TransactionalAnnotationAttributes> txAttr = getTxAttr(effectiveMethod);
            txMap.put(m, txAttr);
            return txAttr;
        } catch (NoSuchMethodException e1) {
            LOG.debug("No method found when scanning for transactions", e1);
            return Optional.empty();
        } catch (SecurityException e1) {
            throw new RuntimeException("Security exception when determining effective method", e1); // NOSONAR
        }
    }
    
    private Optional<TransactionalAnnotationAttributes> getTxAttr(Method method) {
        Optional<TransactionalAnnotationAttributes> txAttr = txMap.get(method);
        if (txAttr == null) {
            return Optional.empty();
        } else {
            return txAttr;
        }
    }

    private boolean parseTxData(Class<?> c) {
        boolean shouldAssignInterceptor = false;
        Transactional classAnnotation = c.getAnnotation(Transactional.class);
        TxType defaultType = getType(classAnnotation);
        if (defaultType != null) {
            shouldAssignInterceptor = true;
        }
        for (Method m : c.getDeclaredMethods()) {
            try {
                Transactional methodAnnotation = m.getAnnotation(Transactional.class);
                TxType t = getType(methodAnnotation);
                if (t != null) {
                   TransactionalAnnotationAttributes txData = new TransactionalAnnotationAttributes(t,
                            methodAnnotation.dontRollbackOn(), methodAnnotation.rollbackOn());
                   assertAllowedModifier(m);
                   txMap.put(m, Optional.of(txData));
                   shouldAssignInterceptor = true;
                } else if (defaultType != null){
                    txMap.put(m, Optional.of(new TransactionalAnnotationAttributes(defaultType, classAnnotation.dontRollbackOn(),
                            classAnnotation.rollbackOn())));
                }
            } catch(IllegalStateException e) {
                LOG.warn("Invalid transaction annoation found", e);
            }
        }

        return shouldAssignInterceptor;
    }

    private static TxType getType(Transactional jtaT) {
        return (jtaT != null) ? jtaT.value() : null;
    }

    private static void assertAllowedModifier(Method m) {
        if ((m.getModifiers() & BANNED_MODIFIERS) != 0) {
            throw new IllegalArgumentException("Transaction annotation is not allowed on private or static method " + m);
        }
    }

    public boolean isTransactional() {
        return isTransactional;
    }
    
    public Class<?> getBeanClass() {
        return beanClass;
    }
}
