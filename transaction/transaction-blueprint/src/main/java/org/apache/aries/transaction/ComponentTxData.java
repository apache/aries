package org.apache.aries.transaction;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

public class ComponentTxData {
    private static final int BANNED_MODIFIERS = Modifier.PRIVATE | Modifier.STATIC;
    
    Map<Method, TxType> txMap = new HashMap<Method, Transactional.TxType>();
    private boolean isTransactional;
    private Class<?> beanClass;
    
    TxType getEffectiveType(Method m) {
        try {
            Method effectiveMethod = beanClass.getDeclaredMethod(m.getName(), m.getParameterTypes());
            return txMap.get(effectiveMethod);
        } catch (NoSuchMethodException e) {
            try {
                Method effectiveMethod = beanClass.getMethod(m.getName(), m.getParameterTypes());
                return txMap.get(effectiveMethod);
            } catch (NoSuchMethodException e1) {
                return null;
            } catch (SecurityException e1) {
                throw new RuntimeException("Security exception when determining effective method", e1);
            }
        } catch (SecurityException e) {
            throw new RuntimeException("Security exception when determining effective method", e);
        }
    }
    
    public ComponentTxData(Class<?> c) {
        beanClass = c;
        isTransactional = false;
        // Check class hierarchy
        while (c != Object.class) {
            isTransactional |= parseTxData(c);
            for (Class<?> iface : c.getInterfaces()) {
                isTransactional |= parseTxData(iface);
            }
            c = c.getSuperclass();
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
                TxType t = getType(m.getAnnotation(Transactional.class));
                if (t != null) {
                   assertAllowedModifier(m);
                   txMap.put(m, t);
                   shouldAssignInterceptor = true;
                } else if (defaultType != null){
                   txMap.put(m, defaultType);
                }
            } catch(IllegalStateException e) {
                // don't break bean creation due to invalid transaction attribute
            }
        }

        return shouldAssignInterceptor;
    }

    private TxType getType(Transactional jtaT) {
        return (jtaT != null) ? jtaT.value() : null;
    }

    private void assertAllowedModifier(Method m) {
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
