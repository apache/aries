package org.apache.aries.tx.control.jpa.xa.impl;

import static org.apache.aries.tx.control.jpa.xa.impl.ManagedServiceFactoryImpl.EMF_BUILDER_TARGET_FILTER;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.service.jdbc.DataSourceFactory.JDBC_PASSWORD;
import static org.osgi.service.jpa.EntityManagerFactoryBuilder.JPA_UNIT_NAME;
import static org.osgi.service.jpa.EntityManagerFactoryBuilder.JPA_UNIT_PROVIDER;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.persistence.spi.PersistenceProvider;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;
import org.osgi.service.transaction.control.TransactionControl;
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

		updateService(reference, service);
		return service;
	}

	private void updateService(ServiceReference<EntityManagerFactoryBuilder> reference, EntityManagerFactoryBuilder service) {
		boolean setEMFB;
		synchronized (this) {
			setEMFB = activeDsf.compareAndSet(null, service);
		}

		if (setEMFB) {
			try {
				JPAEntityManagerProvider jpaEM = new DelayedJPAEntityManagerProvider(t -> {
					
					Map<String, Object> jpaProps = new HashMap<String, Object>(jpaProperties);
					Map<String, Object> providerProps = new HashMap<String, Object>(providerProperties);
					
					setupTransactionManager(jpaProps, providerProps, t, reference);
					
					return new JPAEntityManagerProviderFactoryImpl().getProviderFor(service,
							jpaProps, providerProps);
				});
				ServiceRegistration<JPAEntityManagerProvider> reg = context
						.registerService(JPAEntityManagerProvider.class, jpaEM, getServiceProperties());
				if (!serviceReg.compareAndSet(null, reg)) {
					throw new IllegalStateException("Unable to set the JDBC connection provider registration");
				}
			} catch (Exception e) {
				ManagedServiceFactoryImpl.LOG.error("An error occurred when creating the connection provider for {}.", pid, e);
				activeDsf.compareAndSet(service, null);
			}
		}
	}

	private void setupTransactionManager(Map<String, Object> props, Map<String, Object> providerProps, 
			TransactionControl txControl, ServiceReference<EntityManagerFactoryBuilder> reference) {
		String provider = (String) reference.getProperty(JPA_UNIT_PROVIDER);
		
		ServiceReference<PersistenceProvider> providerRef = getPersistenceProvider(provider);
		
		if(providerRef == null) {
			// TODO log a warning and give up
			return;
		}

		Bundle providerBundle = providerRef.getBundle();
		Bundle txControlProviderBundle = context.getBundle();
		
		try {
			if("org.hibernate.jpa.HibernatePersistenceProvider".equals(provider)) {
				
				try{
					providerBundle.loadClass("org.hibernate.resource.transaction.TransactionCoordinatorBuilder");
				} catch (Exception e) {
					BundleWiring wiring = providerBundle.adapt(BundleWiring.class);
					providerBundle = wiring.getRequiredWires("osgi.wiring.package").stream()
								.filter(bw -> "org.hibernate".equals(bw.getCapability().getAttributes().get("osgi.wiring.package")))
								.map(BundleWire::getProviderWiring)
								.map(BundleWiring::getBundle)
								.findFirst().get();
				}
				
				ClassLoader pluginLoader = getPluginLoader(providerBundle, txControlProviderBundle);
				
				Class<?> pluginClazz = pluginLoader.loadClass("org.apache.aries.tx.control.jpa.xa.hibernate.impl.HibernateTxControlPlatform");
				Object plugin = pluginClazz.getConstructor(TransactionControl.class)
					.newInstance(txControl);
				
				props.put("hibernate.transaction.coordinator_class", plugin);
				
			} else if("org.apache.openjpa.persistence.PersistenceProviderImpl".equals(provider)) {
					
				ClassLoader pluginLoader = getPluginLoader(providerBundle, txControlProviderBundle);
					
				Class<?> pluginClazz = pluginLoader.loadClass("org.apache.aries.tx.control.jpa.xa.openjpa.impl.OpenJPATxControlPlatform");
				Object plugin = pluginClazz.getConstructor(TransactionControl.class)
						.newInstance(txControl);
					
				props.put("openjpa.ManagedRuntime", plugin);
					
			} else if("org.eclipse.persistence.jpa.PersistenceProvider".equals(provider)) {
				
				ClassLoader pluginLoader = getPluginLoader(providerBundle, txControlProviderBundle);
				
				Class<?> pluginClazz = pluginLoader.loadClass("org.apache.aries.tx.control.jpa.xa.eclipse.impl.EclipseTxControlPlatform");
				
				pluginClazz.getMethod("setTransactionControl", TransactionControl.class)
						.invoke(null, txControl);
				
				props.put("eclipselink.target-server", pluginClazz.getName());
				props.put("org.apache.aries.jpa.eclipselink.plugin.types", pluginClazz);
				// This is needed to ensure that sequences can be generated in nested
				// transactions without blowing up.
				if(!props.containsKey("eclipselink.jdbc.sequence-connection-pool")) {
					props.put("eclipselink.jdbc.sequence-connection-pool", "true");
				}
				
			} else {
				// TODO log a warning and give up
				return;
			} 
		} catch (Exception e) {
			//TODO log a warning and give up
			e.printStackTrace();
		}
	}

	private ClassLoader getPluginLoader(Bundle providerBundle, Bundle txControlProviderBundle) {
		return new ClassLoader() {

			ConcurrentMap<String, Class<?>> loaded = new ConcurrentHashMap<>();
			
			@Override
			public Class<?> loadClass(String name) throws ClassNotFoundException {
				if(name.startsWith("org.apache.aries.tx.control.jpa.xa.hibernate") ||
					name.startsWith("org.apache.aries.tx.control.jpa.xa.openjpa") ||
					name.startsWith("org.apache.aries.tx.control.jpa.xa.eclipse")) {
					
					Class<?> c = loaded.get(name);
					
					if(c != null) {
						return c;
					}
					
					String resource = name.replace('.', '/') + ".class";
					
					try (InputStream is = txControlProviderBundle.getResource(resource).openStream()) {
						ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
						byte[] b = new byte[4096];
						int read;
						while ((read = is.read(b)) != -1) {
							baos.write(b, 0, read);
						}
						byte[] clazzBytes = baos.toByteArray();
						c = defineClass(name, clazzBytes, 0, clazzBytes.length, 
								ManagedJPAEMFLocator.class.getProtectionDomain());
						loaded.putIfAbsent(name, c);
						return c;
					} catch (IOException e) {
						throw new ClassNotFoundException("Unable to load class " + name, e);
					}
				}
				
				if(name.startsWith("org.apache.aries.tx.control") ||
						name.startsWith("org.osgi.service.transaction.control")) {
					return txControlProviderBundle.loadClass(name);
				}
				return providerBundle.loadClass(name);
			}
		};
	}

	private ServiceReference<PersistenceProvider> getPersistenceProvider(String provider) {
		if(provider == null) {
			return null;
		}
		try {
			return context.getServiceReferences(PersistenceProvider.class, 
							"(javax.persistence.provider=" + provider + ")").stream()
								.findFirst()
								.orElse(null);
		} catch (InvalidSyntaxException e) {
			//TODO log a warning
			return null;
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
				updateService(reference, newEMFBuilder);
			}
		}
	}
}