package org.apache.aries.transaction;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComponentTxData {
    private static final Logger LOG = LoggerFactory.getLogger(ComponentTxData.class);
    private static final int BANNED_MODIFIERS = Modifier.PRIVATE | Modifier.STATIC;
    
    private Map<Method, TransactionalAnnotationAttributes> txMap = new HashMap<Method, TransactionalAnnotationAttributes>();
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

    TransactionalAnnotationAttributes getEffectiveType(Method m) {
        if (txMap.containsKey(m)) {
                return txMap.get(m);
        }
        try {
            Method effectiveMethod = beanClass.getDeclaredMethod(m.getName(), m.getParameterTypes());
            TransactionalAnnotationAttributes txData = txMap.get(effectiveMethod);
            txMap.put(m, txData);
            return txData;
        } catch (NoSuchMethodException e) { // NOSONAR
            return getFromMethod(m);
        } catch (SecurityException e) {
            throw new RuntimeException("Security exception when determining effective method", e); // NOSONAR
        }
    }

    private TransactionalAnnotationAttributes getFromMethod(Method m) {
        try {
            Method effectiveMethod = beanClass.getMethod(m.getName(), m.getParameterTypes());
            TransactionalAnnotationAttributes txData = txMap.get(effectiveMethod);
            txMap.put(m, txData);
            return txData;
        } catch (NoSuchMethodException e1) {
            LOG.debug("No method found when scanning for transactions", e1);
            return null;
        } catch (SecurityException e1) {
            throw new RuntimeException("Security exception when determining effective method", e1); // NOSONAR
        }
    }

    private boolean parseTxData(Class<?> c) {
        boolean shouldAssignInterceptor = false;
        TxType defaultType = getType(c.getAnnotation(Transactional.class));
        if (defaultType != null) {
            shouldAssignInterceptor = true;
        }
        for (Method m : c.getDeclaredMethods()) {
            try {
                Transactional annotation = m.getAnnotation(Transactional.class);
                TxType t = getType(annotation);
                if (t != null) {
                    TransactionalAnnotationAttributes txData = new TransactionalAnnotationAttributes(t, annotation.dontRollbackOn(), annotation.rollbackOn());
                    assertAllowedModifier(m);
                   txMap.put(m, txData);
                   shouldAssignInterceptor = true;
                } else if (defaultType != null){
                    txMap.put(m, new TransactionalAnnotationAttributes(defaultType));
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
