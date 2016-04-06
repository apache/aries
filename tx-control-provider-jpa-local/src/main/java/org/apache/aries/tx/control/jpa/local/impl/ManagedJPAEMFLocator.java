package org.apache.aries.tx.control.jpa.local.impl;

import static org.apache.aries.tx.control.jpa.local.impl.ManagedServiceFactoryImpl.EMF_BUILDER_TARGET_FILTER;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.service.jdbc.DataSourceFactory.JDBC_PASSWORD;
import static org.osgi.service.jpa.EntityManagerFactoryBuilder.JPA_UNIT_NAME;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;
import org.osgi.service.transaction.control.jpa.JPAEntityManagerProvider;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ManagedJPAEMFLocator implements LifecycleAware,
	ServiceTrackerCustomizer<EntityManagerFactoryBuilder, EntityManagerFactoryBuilder> {

	private final BundleContext context;
	private final String pid;
	private final Map<String, Object> jpaProperties;
	private final Map<String, Object> providerProperties;
	private final ServiceTracker<EntityManagerFactoryBuilder, EntityManagerFactoryBuilder> emfBuilderTracker;

	private final AtomicReference<EntityManagerFactoryBuilder> activeDsf = new AtomicReference<>();
	private final AtomicReference<ServiceRegistration<JPAEntityManagerProvider>> serviceReg = new AtomicReference<>();

	public ManagedJPAEMFLocator(BundleContext context, String pid, Map<String, Object> jpaProperties,
			Map<String, Object> providerProperties) throws InvalidSyntaxException, ConfigurationException {
		this.context = context;
		this.pid = pid;
		this.jpaProperties = jpaProperties;
		this.providerProperties = providerProperties;

		String unitName = (String) providerProperties.get(JPA_UNIT_NAME);
		if (unitName == null) {
			ManagedServiceFactoryImpl.LOG.error("The configuration {} must specify a persistence unit name", pid);
			throw new ConfigurationException(JPA_UNIT_NAME,
					"The configuration must specify a persistence unit name");
		}
		
		String targetFilter = (String) providerProperties.get(EMF_BUILDER_TARGET_FILTER);
		if (targetFilter == null) {
			targetFilter = "(" + JPA_UNIT_NAME + "=" + unitName + ")";
		}

		targetFilter = "(&(" + OBJECTCLASS + "=" + EntityManagerFactoryBuilder.class.getName() + ")" + targetFilter + ")";

		this.emfBuilderTracker = new ServiceTracker<>(context, context.createFilter(targetFilter), this);
	}

	public void start() {
		emfBuilderTracker.open();
	}

	public void stop() {
		emfBuilderTracker.close();
	}

	@Override
	public EntityManagerFactoryBuilder addingService(ServiceReference<EntityManagerFactoryBuilder> reference) {
		EntityManagerFactoryBuilder service = context.getService(reference);

		updateService(service);
		return service;
	}

	private void updateService(EntityManagerFactoryBuilder service) {
		boolean setEMFB;
		synchronized (this) {
			setEMFB = activeDsf.compareAndSet(null, service);
		}

		if (setEMFB) {
			try {
				JPAEntityManagerProvider provider = new JPAEntityManagerProviderFactoryImpl().getProviderFor(service,
						jpaProperties, providerProperties);
				ServiceRegistration<JPAEntityManagerProvider> reg = context
						.registerService(JPAEntityManagerProvider.class, provider, getServiceProperties());
				if (!serviceReg.compareAndSet(null, reg)) {
					throw new IllegalStateException("Unable to set the JDBC connection provider registration");
				}
			} catch (Exception e) {
				ManagedServiceFactoryImpl.LOG.error("An error occurred when creating the connection provider for {}.", pid, e);
				activeDsf.compareAndSet(service, null);
			}
		}
	}

	private Dictionary<String, ?> getServiceProperties() {
		Hashtable<String, Object> props = new Hashtable<>();
		providerProperties.keySet().stream().filter(s -> !JDBC_PASSWORD.equals(s))
				.forEach(s -> props.put(s, providerProperties.get(s)));
		return props;
	}

	@Override
	public void modifiedService(ServiceReference<EntityManagerFactoryBuilder> reference, EntityManagerFactoryBuilder service) {
	}

	@Override
	public void removedService(ServiceReference<EntityManagerFactoryBuilder> reference, EntityManagerFactoryBuilder service) {
		boolean dsfLeft;
		ServiceRegistration<JPAEntityManagerProvider> oldReg = null;
		synchronized (this) {
			dsfLeft = activeDsf.compareAndSet(service, null);
			if (dsfLeft) {
				oldReg = serviceReg.getAndSet(null);
			}
		}

		if (oldReg != null) {
			try {
				oldReg.unregister();
			} catch (IllegalStateException ise) {
				ManagedServiceFactoryImpl.LOG.debug("An exception occurred when unregistering a service for {}", pid);
			}
		}
		try {
			context.ungetService(reference);
		} catch (IllegalStateException ise) {
			ManagedServiceFactoryImpl.LOG.debug("An exception occurred when ungetting the service for {}", reference);
		}

		if (dsfLeft) {
			EntityManagerFactoryBuilder newEMFBuilder = emfBuilderTracker.getService();
			if (newEMFBuilder != null) {
				updateService(newEMFBuilder);
			}
		}
	}
}