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

    TransactionalAnnotationAttributes getEffectiveType(Method m) {
        if (txMap.containsKey(m)) {
            Optional<TransactionalAnnotationAttributes> optional = txMap.get(m);
            if(optional == null || !optional.isPresent()) {
                return null;
            }
            return optional.get();
        }
        try {
            Method effectiveMethod = beanClass.getDeclaredMethod(m.getName(), m.getParameterTypes());
            Optional<TransactionalAnnotationAttributes> optional = txMap.get(effectiveMethod);
            if(optional == null) {
                optional = Optional.empty();
            }
            txMap.put(m, optional);
            return optional.isPresent() ? optional.get() : null;
        } catch (NoSuchMethodException e) { // NOSONAR
            return getFromMethod(m);
        } catch (SecurityException e) {
            throw new RuntimeException("Security exception when determining effective method", e); // NOSONAR
        }
    }

    private TransactionalAnnotationAttributes getFromMethod(Method m) {
        try {
            Method effectiveMethod = beanClass.getMethod(m.getName(), m.getParameterTypes());
            Optional<TransactionalAnnotationAttributes> optional = txMap.get(effectiveMethod);
            if(optional == null) {
                optional = Optional.empty();
            }
            txMap.put(m, optional);
            return optional.isPresent() ? optional.get() : null;
        } catch (NoSuchMethodException e1) {
            LOG.debug("No method found when scanning for transactions", e1);
            return null;
        } catch (SecurityException e1) {
            throw new RuntimeException("Security exception when determining effective method", e1); // NOSONAR
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
