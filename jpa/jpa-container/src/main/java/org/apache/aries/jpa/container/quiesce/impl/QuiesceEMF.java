package org.apache.aries.jpa.container.quiesce.impl;

import javax.persistence.EntityManagerFactory;

import org.osgi.framework.ServiceRegistration;

public interface QuiesceEMF extends EntityManagerFactory {
    void clearQuiesce();
    @SuppressWarnings("rawtypes")
    void quiesce(NamedCallback callback, ServiceRegistration value);
}
