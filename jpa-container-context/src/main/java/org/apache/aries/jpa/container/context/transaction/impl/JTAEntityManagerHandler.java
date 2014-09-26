package org.apache.aries.jpa.container.context.transaction.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.LockModeType;
import javax.persistence.TransactionRequiredException;

import org.apache.aries.jpa.container.context.impl.NLS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JTAEntityManagerHandler implements InvocationHandler {
    private static final String[] TRANSACTED_METHODS = {"flush", "lock", "merge", "isJoinedToTransaction", "persist", "remove", "getLockMode", "lock", "refresh"};
    
    /**
     * Logger
     */
    private static final Logger _logger = LoggerFactory.getLogger("org.apache.aries.jpa.container.context");
    
    private Set<String> transactedMethods = new HashSet<String>(Arrays.asList(TRANSACTED_METHODS));

    /**
     * The {@link EntityManagerFactory} that can create new {@link EntityManager} instances
     */
    private final EntityManagerFactory emf;
    /**
     * The map of properties to pass when creating EntityManagers
     */
    private final Map<String, Object> props;
    /**
     * A registry for creating new persistence contexts
     */
    private final JTAPersistenceContextRegistry reg;
    /**
     * The number of EntityManager instances that are open
     */
    private final AtomicLong instanceCount;
    /**
     * A callback for when we're quiescing
     */
    private final DestroyCallback callback;


    private final ThreadLocal<AtomicInteger> activeCalls = new ThreadLocal<AtomicInteger>() {
        @Override
        protected AtomicInteger initialValue() {
            return new AtomicInteger(0);
        }
    };

    private final ThreadLocal<EntityManager> activeManager = new ThreadLocal<EntityManager>();

    private final ConcurrentLinkedQueue<EntityManager> pool = new ConcurrentLinkedQueue<EntityManager>();
    
    public JTAEntityManagerHandler(EntityManagerFactory factory,
                            Map<String, Object> properties, JTAPersistenceContextRegistry registry, AtomicLong activeCount,
                            DestroyCallback onDestroy) {
        emf = factory;
        props = properties;
        reg = registry;
        instanceCount = activeCount;
        callback = onDestroy;
    }

    public void preCall() {
        activeCalls.get().incrementAndGet();
    }

    public void postCall() {
        if (activeCalls.get().decrementAndGet() == 0) {
            EntityManager manager = activeManager.get();
            if (manager != null) {
                activeManager.set(null);
                manager.clear();
                pool.add(manager);
            }
        }
    }
    
    /**
     * Get the target persistence context
     *
     * @param forceTransaction Whether the returned entity manager needs to be bound to a transaction
     * @return
     * @throws TransactionRequiredException if forceTransaction is true and no transaction is available
     */
    private EntityManager getPersistenceContext(boolean forceTransaction) {
        if (forceTransaction) {
            EntityManager manager = activeManager.get();
            if (manager != null) {
                manager.clear();
            }
            return reg.getCurrentPersistenceContext(emf, props, instanceCount, callback);
        } else {
            if (reg.isTransactionActive()) {
                EntityManager manager = activeManager.get();
                if (manager != null) {
                    manager.clear();
                }
                return reg.getCurrentPersistenceContext(emf, props, instanceCount, callback);
            } else {
                if (!!!reg.jtaIntegrationAvailable() && _logger.isDebugEnabled())
                    _logger.debug("No integration with JTA transactions is available. No transaction context is active.");

                EntityManager manager = activeManager.get();
                if (manager == null) {
                    manager = pool.poll();
                    if (manager == null) {
                        manager = emf.createEntityManager(props);
                    }
                    activeManager.set(manager);
                }
                return manager;
            }
        }
    }

    /**
     * Called reflectively by blueprint
     */
    public void internalClose() {
        EntityManager temp;
        while ((temp = pool.poll()) != null) {
            temp.close();
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("close".equals(method.getName())) {
            throw new IllegalStateException(NLS.MESSAGES.getMessage("close.called.on.container.manged.em"));
        }
        if ("getTransaction".equals(method.getName())) {
            throw new IllegalStateException(NLS.MESSAGES.getMessage("getTransaction.called.on.container.managed.em"));
        }
        if ("isOpen".equals(method.getName())) {
            return true;
        }
        if ("joinTransaction".equals(method.getName())) {
            // This should be a no-op for a JTA entity manager
            return null;
        }
        
        boolean forceTransaction = transactedMethods.contains(method.getName());
        
        if ("joinTransaction".equals(method.getName())) {
            forceTransaction = args[2] != LockModeType.NONE;
        }
        
        EntityManager delegate = getPersistenceContext(forceTransaction); 
        Object res = method.invoke(delegate, args);
        return res;

    }

}
