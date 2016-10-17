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
package org.apache.aries.tx.control.jpa.xa.impl;

import static org.osgi.service.jpa.EntityManagerFactoryBuilder.JPA_UNIT_PROVIDER;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.persistence.spi.PersistenceProvider;

import org.apache.aries.tx.control.jpa.common.impl.AbstractJPAEntityManagerProvider;
import org.apache.aries.tx.control.jpa.common.impl.AbstractManagedJPAEMFLocator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;
import org.osgi.service.transaction.control.TransactionControl;

public class XAJPAEMFLocator extends AbstractManagedJPAEMFLocator {

	public XAJPAEMFLocator(BundleContext context, String pid, Map<String, Object> jpaProperties,
			Map<String, Object> providerProperties, Runnable onClose) throws InvalidSyntaxException, ConfigurationException {
		super(context, pid, jpaProperties, providerProperties, onClose);
	}

	@Override
	protected AbstractJPAEntityManagerProvider getResourceProvider(BundleContext context,
			EntityManagerFactoryBuilder service, ServiceReference<EntityManagerFactoryBuilder> reference,
			Map<String, Object> jpaProperties, Map<String, Object> providerProperties, Runnable onClose) {
		return new DelayedJPAEntityManagerProvider(t -> {
			
			Map<String, Object> jpaProps = new HashMap<String, Object>(jpaProperties);
			Map<String, Object> providerProps = new HashMap<String, Object>(providerProperties);
			
			setupTransactionManager(context, jpaProps, providerProps, t, reference);
			
			return new JPAEntityManagerProviderFactoryImpl().getProviderFor(service,
					jpaProps, providerProps, t, onClose);
		});
	}

	private void setupTransactionManager(BundleContext context, Map<String, Object> props, 
			Map<String, Object> providerProps, ThreadLocal<TransactionControl> t, ServiceReference<EntityManagerFactoryBuilder> reference) {
		String provider = (String) reference.getProperty(JPA_UNIT_PROVIDER);
		
		ServiceReference<PersistenceProvider> providerRef = getPersistenceProvider(provider, context);
		
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
				Object plugin = pluginClazz.getConstructor(ThreadLocal.class)
					.newInstance(t);
				
				props.put("hibernate.transaction.coordinator_class", plugin);
				
			} else if("org.apache.openjpa.persistence.PersistenceProviderImpl".equals(provider)) {
					
				ClassLoader pluginLoader = getPluginLoader(providerBundle, txControlProviderBundle);
					
				Class<?> pluginClazz = pluginLoader.loadClass("org.apache.aries.tx.control.jpa.xa.openjpa.impl.OpenJPATxControlPlatform");
				Object plugin = pluginClazz.getConstructor(ThreadLocal.class)
						.newInstance(t);
					
				props.put("openjpa.ManagedRuntime", plugin);
					
			} else if("org.eclipse.persistence.jpa.PersistenceProvider".equals(provider)) {
				
				ClassLoader pluginLoader = getPluginLoader(providerBundle, txControlProviderBundle);
				
				Class<?> pluginClazz = pluginLoader.loadClass("org.apache.aries.tx.control.jpa.xa.eclipse.impl.EclipseTxControlPlatform");
				
				pluginClazz.getMethod("setTransactionControl", ThreadLocal.class)
						.invoke(null, t);
				
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
								XAJPAEMFLocator.class.getProtectionDomain());
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

	private ServiceReference<PersistenceProvider> getPersistenceProvider(String provider, BundleContext context) {
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
}