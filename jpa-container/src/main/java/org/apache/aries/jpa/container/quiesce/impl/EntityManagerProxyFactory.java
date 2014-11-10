package org.apache.aries.jpa.container.quiesce.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import org.apache.aries.jpa.container.impl.NLS;

class EntityManagerProxyFactory {
    static EntityManager create(EntityManager delegate, DestroyCallback destroyCallback) {
        ClassLoader cl = delegate.getClass().getClassLoader();
        Class<?>[] ifAr = new Class[]{EntityManager.class};
        return (EntityManager)Proxy.newProxyInstance(cl, ifAr, new EMHandler(delegate, destroyCallback));
    }
    
    static class EMHandler implements InvocationHandler {

        private EntityManager delegate;
        private DestroyCallback destroyCallback;

        public EMHandler(EntityManager delegate, DestroyCallback destroyCallback) {
            this.delegate = delegate;
            this.destroyCallback = destroyCallback;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object res = null;
            try {
              res = method.invoke(delegate, args);
            } catch (IllegalArgumentException e) {
              new PersistenceException(NLS.MESSAGES.getMessage("wrong.JPA.version", new Object[]{
                  method.getName(), delegate
              }), e);
            }
            
            // This will only ever be called once, the second time there
            // will be an IllegalStateException from the line above
            if ("close".equals(method.getName())) {
                destroyCallback.callback();
            }
            return res;
            
        }

    }
}
