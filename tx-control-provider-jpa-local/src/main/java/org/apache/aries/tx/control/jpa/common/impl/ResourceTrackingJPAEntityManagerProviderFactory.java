package org.apache.aries.tx.control.jpa.common.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.persistence.EntityManagerFactory;

import org.osgi.service.jpa.EntityManagerFactoryBuilder;
import org.osgi.service.transaction.control.jpa.JPAEntityManagerProvider;
import org.osgi.service.transaction.control.jpa.JPAEntityManagerProviderFactory;

class ResourceTrackingJPAEntityManagerProviderFactory implements
	JPAEntityManagerProviderFactory {

	private final List<AbstractJPAEntityManagerProvider> toClose = new ArrayList<>();
	
	private final InternalJPAEntityManagerProviderFactory factory;
	
	private boolean closed;
	
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

	private AbstractJPAEntityManagerProvider doGetResult(Supplier<AbstractJPAEntityManagerProvider> getter) {
		synchronized (getter) {
			if (closed) {
				throw new IllegalStateException("This ResourceProvider has been reclaimed because the factory service that provided it was released");
			}
		}
		AbstractJPAEntityManagerProvider ajcp = getter.get();
		boolean destroy = false;
		synchronized (toClose) {
			if (closed) {
				destroy = true;
			} else {
			    toClose.add(ajcp);
			}
		}
		if(destroy) {
			ajcp.close();
			throw new IllegalStateException("This ResourceProvider has been reclaimed because the factory service that provided it was released");
		}
		return ajcp;
	}

	public void closeAll() {
		synchronized (toClose) {
			closed = true;
		}
		// toClose is now up to date and no other thread will write it
		toClose.stream().forEach(ajcp -> {
			try {
				ajcp.close();
			} catch (Exception e) {}
		});
		
		toClose.clear();
	}
}