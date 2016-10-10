package org.apache.aries.tx.control.jpa.common.impl;

import java.util.Map;

import javax.persistence.EntityManagerFactory;

import org.osgi.service.jpa.EntityManagerFactoryBuilder;
import org.osgi.service.transaction.control.jpa.JPAEntityManagerProviderFactory;

public interface InternalJPAEntityManagerProviderFactory extends JPAEntityManagerProviderFactory {

	@Override
	AbstractJPAEntityManagerProvider getProviderFor(EntityManagerFactoryBuilder emfb, 
			Map<String, Object> jpaProperties, Map<String, Object> resourceProviderProperties);

	@Override
	AbstractJPAEntityManagerProvider getProviderFor(EntityManagerFactory emf,
			Map<String, Object> resourceProviderProperties);

}
