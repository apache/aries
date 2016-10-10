package org.apache.aries.tx.control.resource.common.impl;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

public abstract class ResourceProviderFactoryServiceFactory<R extends AutoCloseable,
	T extends TrackingResourceProviderFactory<R>> implements 
	ServiceFactory<TrackingResourceProviderFactory<R>> {

	Set<TrackingResourceProviderFactory<R>> factories = new CopyOnWriteArraySet<>();
	
	@Override
	public TrackingResourceProviderFactory<R> getService(Bundle bundle,
			ServiceRegistration<TrackingResourceProviderFactory<R>> registration) {
		TrackingResourceProviderFactory<R> factory = 
						getTrackingResourceManagerProviderFactory();
		factories.add(factory);
		return factory;
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<TrackingResourceProviderFactory<R>> registration,
			TrackingResourceProviderFactory<R> service) {
		factories.remove(service);
		service.closeAll();
	}
	
	public void close() {
		factories.stream()
			.forEach(r -> r.closeAll());
	}
	
	protected abstract TrackingResourceProviderFactory<R> 
		getTrackingResourceManagerProviderFactory();
}
