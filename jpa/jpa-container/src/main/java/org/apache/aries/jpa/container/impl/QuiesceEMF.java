package org.apache.aries.jpa.container.impl;

import javax.persistence.EntityManagerFactory;

import org.apache.aries.jpa.container.impl.EntityManagerFactoryManager.NamedCallback;
import org.osgi.framework.ServiceRegistration;

public interface QuiesceEMF extends EntityManagerFactory {
    void clearQuiesce();
    void quiesce(NamedCallback callback, ServiceRegistration value);
}
