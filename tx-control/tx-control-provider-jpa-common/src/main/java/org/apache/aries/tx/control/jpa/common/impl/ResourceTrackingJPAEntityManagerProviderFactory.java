package org.apache.aries.tx.control.jpa.common.impl;

import java.util.Map;

import javax.persistence.EntityManagerFactory;

import org.apache.aries.tx.control.resource.common.impl.TrackingResourceProviderFactory;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;
import org.osgi.service.transaction.control.jpa.JPAEntityManagerProvider;
import org.osgi.service.transaction.control.jpa.JPAEntityManagerProviderFactory;

public class ResourceTrackingJPAEntityManagerProviderFactory extends 
	TrackingResourceProviderFactory<AbstractJPAEntityManagerProvider> implements 
	JPAEntityManagerProviderFactory {

	private final InternalJPAEntityManagerProviderFactory factory;
	
	public ResourceTrackingJPAEntityManagerProviderFactory(InternalJPAEntityManagerProviderFactory factory) {
		this.factory = factory;
	}

	@Override
	public JPAEntityManagerProvider getProviderFor(EntityManagerFactoryBuilder emfb, Map<String, Object> jpaProperties,
			Map<String, Object> resourceProviderProperties) {
		return doGetResult(() -> factory.getProviderFor(emfb, 
				jpaProperties, resourceProviderProperties));
	}

	@Override
	public JPAEntityManagerProvider getProviderFor(EntityManagerFactory emf, Map<String, Object> resourceProviderProperties) {
		return doGetResult(() -> factory.getProviderFor(emf, 
				resourceProviderProperties));
	}
}