package org.apache.aries.tx.control.jpa.common.impl;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

public abstract class JPAEntityManagerProviderFactoryServiceFactory implements 
	ServiceFactory<ResourceTrackingJPAEntityManagerProviderFactory> {

	Set<ResourceTrackingJPAEntityManagerProviderFactory> factories = new CopyOnWriteArraySet<>();
	
	@Override
	public ResourceTrackingJPAEntityManagerProviderFactory getService(Bundle bundle,
			ServiceRegistration<ResourceTrackingJPAEntityManagerProviderFactory> registration) {
		ResourceTrackingJPAEntityManagerProviderFactory factory = new ResourceTrackingJPAEntityManagerProviderFactory(
						getInternalJPAEntityManagerProviderFactory());
		factories.add(factory);
		return factory;
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<ResourceTrackingJPAEntityManagerProviderFactory> registration,
			ResourceTrackingJPAEntityManagerProviderFactory service) {
		factories.remove(service);
		service.closeAll();
	}
	
	public void close() {
		factories.stream()
			.forEach(r -> r.closeAll());
	}
	
	protected abstract InternalJPAEntityManagerProviderFactory 
		getInternalJPAEntityManagerProviderFactory();
}
