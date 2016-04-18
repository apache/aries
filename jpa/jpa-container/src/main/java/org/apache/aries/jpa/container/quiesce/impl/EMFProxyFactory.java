package org.apache.aries.jpa.container.quiesce.impl;

import java.lang.reflect.Proxy;

import javax.persistence.EntityManagerFactory;

public class EMFProxyFactory {
    public static EntityManagerFactory createProxy(EntityManagerFactory delegate, String name) {
        ClassLoader cl = QuiesceEMF.class.getClassLoader();
        Class<?>[] ifAr = new Class[] { QuiesceEMF.class };
        return (QuiesceEMF) Proxy.newProxyInstance(cl, ifAr, new QuiesceEMFHandler(delegate, name));
    }
}
