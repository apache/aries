package org.apache.aries.jpa.container.quiesce.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

import org.apache.aries.jpa.container.impl.NLS;
import org.apache.aries.util.AriesFrameworkUtil;
import org.osgi.framework.ServiceRegistration;

class QuiesceEMFHandler implements InvocationHandler, DestroyCallback {
    /**
     * Number of open EntityManagers
     */
    private final AtomicLong count = new AtomicLong(0);
    /**
     * The real EMF
     */
    private final EntityManagerFactory delegate;
    /**
     * The name of this unit
     */
    private final String name;
    /**
     * A quiesce callback to call
     */
    private final AtomicReference<NamedCallback> callback = new AtomicReference<NamedCallback>();
    /**
     * The service registration to unregister if we can quiesce
     */
    @SuppressWarnings("rawtypes")
    private final AtomicReference<ServiceRegistration> reg = new AtomicReference<ServiceRegistration>();

    public QuiesceEMFHandler(EntityManagerFactory delegate, String name) {
        this.delegate = delegate;
        this.name = name;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("quiesce".equals(method.getName())) {
            quiesce((NamedCallback)args[0], (ServiceRegistration)args[1]);
            return null;
        }
        if ("clearQuiesce".equals(method.getName())) {
            clearQuiesce();
            return null;
        }
        Object res = null;
        try {
          res = method.invoke(delegate, args);
        } catch (IllegalArgumentException e) {
          new PersistenceException(NLS.MESSAGES.getMessage("wrong.JPA.version", new Object[]{
              method.getName(), delegate
          }), e);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        } 

        // This will only ever be called once, the second time there
        // will be an IllegalStateException from the line above
        if ("createEntityManager".equals(method.getName())) {
            count.incrementAndGet();
            return EntityManagerProxyFactory.create((EntityManager)res,  this);
        }
        return res;

    }
    
    @SuppressWarnings("rawtypes")
    public void quiesce(NamedCallback callback, ServiceRegistration reg) {
        this.reg.compareAndSet(null, reg);
        this.callback.compareAndSet(null, callback);
        if (count.get() == 0) {
            AriesFrameworkUtil.safeUnregisterService(this.reg.getAndSet(null));
            this.callback.set(null);
            callback.callback(name);
        }
    }
    
    /**
     * Called on EntityManager.close()
     */
    public void callback() {
        if (count.decrementAndGet() == 0) {
            NamedCallback c = callback.getAndSet(null);
            if (c != null) {
                AriesFrameworkUtil.safeUnregisterService(reg.getAndSet(null));
                c.callback(name);
            }
        }
    }

    public void clearQuiesce() {
        //We will already be unregistered
        reg.set(null);
        NamedCallback c = callback.getAndSet(null);
        //If there was a callback then call it in case time hasn't run out.
        if (c != null) {
            c.callback(name);
        }
    }

}