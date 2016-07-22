package org.apache.aries.tx.control.jdbc.common.impl;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

public abstract class JDBCConnectionProviderFactoryServiceFactory implements 
	ServiceFactory<ResourceTrackingJDBCConnectionProviderFactory> {

	Set<ResourceTrackingJDBCConnectionProviderFactory> factories = new CopyOnWriteArraySet<>();
	
	@Override
	public ResourceTrackingJDBCConnectionProviderFactory getService(Bundle bundle,
			ServiceRegistration<ResourceTrackingJDBCConnectionProviderFactory> registration) {
		ResourceTrackingJDBCConnectionProviderFactory factory = new ResourceTrackingJDBCConnectionProviderFactory(
						getInternalJDBCConnectionProviderFactory());
		factories.add(factory);
		return factory;
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<ResourceTrackingJDBCConnectionProviderFactory> registration,
			ResourceTrackingJDBCConnectionProviderFactory service) {
		factories.remove(service);
		service.closeAll();
	}
	
	public void close() {
		factories.stream()
			.forEach(r -> r.closeAll());
	}
	
	protected abstract InternalJDBCConnectionProviderFactory 
		getInternalJDBCConnectionProviderFactory();
}
