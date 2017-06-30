/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.tx.control.jpa.common.impl;

import static org.apache.aries.tx.control.jpa.common.impl.AbstractJPAManagedServiceFactory.EMF_BUILDER_TARGET_FILTER;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.service.jdbc.DataSourceFactory.JDBC_PASSWORD;
import static org.osgi.service.jpa.EntityManagerFactoryBuilder.JPA_UNIT_NAME;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import javax.persistence.EntityManagerFactory;

import org.apache.aries.tx.control.resource.common.impl.LifecycleAware;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;
import org.osgi.service.transaction.control.jpa.JPAEntityManagerProvider;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractManagedJPAEMFLocator implements LifecycleAware,
	ServiceTrackerCustomizer<EntityManagerFactoryBuilder, EntityManagerFactoryBuilder> {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractJPAEntityManagerProvider.class);
	
	private final BundleContext context;
	private final String pid;
	private final Map<String, Object> jpaProperties;
	private final Map<String, Object> providerProperties;
	private final Runnable onClose;
	private final ServiceTracker<EntityManagerFactoryBuilder, EntityManagerFactoryBuilder> emfBuilderTracker;

	private final AtomicReference<EntityManagerFactoryBuilder> activeEMFB = new AtomicReference<>();
	private final AtomicReference<AbstractJPAEntityManagerProvider> providerObject = new AtomicReference<>();
	
	private final AtomicReference<ServiceRegistration<JPAEntityManagerProvider>> serviceReg = new AtomicReference<>();

	public AbstractManagedJPAEMFLocator(BundleContext context, String pid, Map<String, Object> jpaProperties,
			Map<String, Object> providerProperties, Runnable onClose) throws InvalidSyntaxException, ConfigurationException {
		this.context = context;
		this.pid = pid;
		this.jpaProperties = jpaProperties;
		this.providerProperties = providerProperties;
		this.onClose = onClose;

		String unitName = (String) providerProperties.get(JPA_UNIT_NAME);
		if (unitName == null) {
			LOG.error("The configuration {} must specify a persistence unit name", pid);
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
		// We track all services because most persistence bundles
		// do not import the JPA service API!
		emfBuilderTracker.open(true);
	}

	public void stop() {
		emfBuilderTracker.close();
	}

	@Override
	public EntityManagerFactoryBuilder addingService(ServiceReference<EntityManagerFactoryBuilder> reference) {
		EntityManagerFactoryBuilder service;

		Object o = context.getService(reference);

		if(o instanceof EntityManagerFactoryBuilder) {
			service = (EntityManagerFactoryBuilder) o;
		} else if (o == null) {
			return null;
		} else {
			service = fixClassSpace(o);
		}
		
		if(service != null) {
			updateService(reference, service);
		} else {
			context.ungetService(reference);
		}
		return service;
	}

	private EntityManagerFactoryBuilder fixClassSpace(Object o) {
		
		Method creationMethod;
		try {
			Class<?> clz = o.getClass().getClassLoader().loadClass(EntityManagerFactoryBuilder.class.getName());
			
			Version pkgVersion = getJPAServicePkgVersion(clz);
			
			if(Version.parseVersion("2").compareTo(pkgVersion) <= 0) {
				throw new IllegalArgumentException("The JPA Service Package version is too high " + pkgVersion);
			}
		
			creationMethod = clz.getMethod("createEntityManagerFactory", Map.class);
			
			if(!EntityManagerFactory.class.equals(creationMethod.getReturnType())) {
				throw new IllegalArgumentException("The EntityManagerFactoryBuilder service does not share a class space for javax.persistence");
			}
			
			return (EntityManagerFactoryBuilder) Proxy.newProxyInstance(getClass().getClassLoader(), 
					new Class<?>[] {EntityManagerFactoryBuilder.class}, 
					(p, m, a) -> o.getClass().getMethod(m.getName(), m.getParameterTypes()).invoke(o, a));
			
		} catch (Exception e) {
			LOG.error("The located EntityManagerFactoryBuilder service {} cannot be made compatible", o, e);
			return null;
		}
	}

	protected Version getJPAServicePkgVersion(Class<?> clz) {
		Function<List<? extends Capability>, Optional<Version>> capMapper = caps -> caps.stream()
				.map(Capability::getAttributes)
				.filter(m -> "org.osgi.service.jpa".equals(m.get("osgi.wiring.package")))
				.map(m -> (Version) m.get("version"))
				.findFirst();
		
		return Optional.ofNullable(FrameworkUtil.getBundle(clz))
			.map(b -> b.adapt(BundleWiring.class))
			.map(bw -> bw.getCapabilities("osgi.wiring.package"))
			.flatMap(capMapper)
			.orElse(Version.emptyVersion);
	}
	
	private void updateService(ServiceReference<EntityManagerFactoryBuilder> reference, EntityManagerFactoryBuilder service) {
		boolean setEMFB;
		synchronized (this) {
			setEMFB = activeEMFB.compareAndSet(null, service);
		}

		if (setEMFB) {
			AbstractJPAEntityManagerProvider provider = null;
			try {
				provider = getResourceProvider(context, service, reference, jpaProperties, providerProperties, onClose);
				providerObject.set(provider);
				ServiceRegistration<JPAEntityManagerProvider> reg = context
						.registerService(JPAEntityManagerProvider.class, provider, getServiceProperties());
				if (!serviceReg.compareAndSet(null, reg)) {
					throw new IllegalStateException("Unable to set the JDBC connection provider registration");
				}
			} catch (Exception e) {
				LOG.error("An error occurred when creating the resource provider for {}.", pid, e);
				activeEMFB.compareAndSet(service, null);
				if(provider != null) {
					provider.close();
				}
					
			}
		}
	}

	protected abstract AbstractJPAEntityManagerProvider getResourceProvider(BundleContext context, 
			EntityManagerFactoryBuilder service, ServiceReference<EntityManagerFactoryBuilder> reference, 
			Map<String, Object> jpaProperties, Map<String, Object> providerProperties, Runnable onClose);

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
		boolean emfbLeft;
		ServiceRegistration<JPAEntityManagerProvider> oldReg = null;
		AbstractJPAEntityManagerProvider toClose = null;
		synchronized (this) {
			emfbLeft = activeEMFB.compareAndSet(service, null);
			if (emfbLeft) {
				toClose = providerObject.get();
				oldReg = serviceReg.getAndSet(null);
			}
		}

		if (oldReg != null) {
			try {
				oldReg.unregister();
			} catch (IllegalStateException ise) {
				LOG.debug("An exception occurred when unregistering a service for {}", pid);
			}
		}
		
		if(toClose != null) {
			try {
				toClose.close();
			} catch (Exception e) {
				LOG.debug("An Exception occured when closing the Resource provider for {}", pid, e);
			}
		}
		
		try {
			context.ungetService(reference);
		} catch (IllegalStateException ise) {
			LOG.debug("An exception occurred when ungetting the service for {}", reference);
		}

		if (emfbLeft) {
			ServiceReference<EntityManagerFactoryBuilder> newEMFBuilderRef = emfBuilderTracker
					.getServiceReference();
			if (newEMFBuilderRef != null) {
				EntityManagerFactoryBuilder newEMFBuilder = emfBuilderTracker.getService(newEMFBuilderRef);
				if(newEMFBuilder != null) {
					updateService(newEMFBuilderRef, newEMFBuilder);
				}
			}
		}
	}
}