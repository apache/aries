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

import static java.util.Optional.ofNullable;
import static javax.persistence.spi.PersistenceUnitTransactionType.JTA;
import static org.apache.aries.tx.control.jpa.xa.impl.XAJPADataSourceSetup.JTA_DATA_SOURCE;
import static org.apache.aries.tx.control.jpa.xa.impl.XAJPADataSourceSetup.NON_JTA_DATA_SOURCE;
import static org.osgi.service.transaction.control.TransactionStatus.NO_TRANSACTION;
import static org.osgi.service.transaction.control.jpa.JPAEntityManagerProviderFactory.LOCAL_ENLISTMENT_ENABLED;
import static org.osgi.service.transaction.control.jpa.JPAEntityManagerProviderFactory.PRE_ENLISTED_DB_CONNECTION;
import static org.osgi.service.transaction.control.jpa.JPAEntityManagerProviderFactory.XA_ENLISTMENT_ENABLED;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Wrapper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.transaction.xa.XAResource;

import org.apache.aries.tx.control.jdbc.common.impl.ScopedConnectionWrapper;
import org.apache.aries.tx.control.jdbc.common.impl.TxConnectionWrapper;
import org.apache.aries.tx.control.jdbc.xa.connection.impl.XAConnectionWrapper;
import org.apache.aries.tx.control.jdbc.xa.connection.impl.XADataSourceMapper;
import org.apache.aries.tx.control.jpa.common.impl.AbstractJPAEntityManagerProvider;
import org.apache.aries.tx.control.jpa.common.impl.InternalJPAEntityManagerProviderFactory;
import org.apache.aries.tx.control.jpa.common.impl.JPADataSourceHelper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;
import org.osgi.service.transaction.control.TransactionContext;
import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.TransactionException;
import org.osgi.service.transaction.control.jdbc.JDBCConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariDataSource;

public class JPAEntityManagerProviderFactoryImpl implements InternalJPAEntityManagerProviderFactory {

	private static final String TRANSACTION_TYPE = "javax.persistence.transactionType";
	private static final Logger LOGGER = LoggerFactory.getLogger(JPAEntityManagerProviderFactoryImpl.class);
	private final BundleContext context;
	
	public JPAEntityManagerProviderFactoryImpl(BundleContext context) {
		this.context = context;
	}

	@Override
	public AbstractJPAEntityManagerProvider getProviderFor(EntityManagerFactoryBuilder emfb, Map<String, Object> jpaProperties,
			Map<String, Object> resourceProviderProperties) {
		
		Map<String, Object> jpaPropsToUse = new HashMap<>(jpaProperties);
		jpaPropsToUse.put(TRANSACTION_TYPE, JTA.name());
		
		Function<ThreadLocal<TransactionControl>, AbstractJPAEntityManagerProvider> create;
		if(jpaProperties.containsKey("osgi.jdbc.provider")) {
			create = handleJDBCResourceProvider(emfb, resourceProviderProperties, jpaPropsToUse);			
		} else if(toBoolean(jpaPropsToUse, PRE_ENLISTED_DB_CONNECTION, false)) {
			create = handlePreEnlistedConnection(emfb, resourceProviderProperties, jpaPropsToUse);
		} else {
			create = handleNormalDataSource(emfb, resourceProviderProperties, jpaPropsToUse);
		}
			
		return new DelayedJPAEntityManagerProvider(create);
	}

	private Function<ThreadLocal<TransactionControl>, AbstractJPAEntityManagerProvider> handleJDBCResourceProvider(
			EntityManagerFactoryBuilder emfb, Map<String, Object> resourceProviderProperties,
			Map<String, Object> jpaPropsToUse) {
		Function<ThreadLocal<TransactionControl>, AbstractJPAEntityManagerProvider> create;
		JDBCConnectionProvider provider = (JDBCConnectionProvider) jpaPropsToUse.get("osgi.jdbc.provider");
		
		create = tx -> {
				jpaPropsToUse.put(JTA_DATA_SOURCE, 
					new ScopedConnectionDataSource(provider.getResource(tx.get())));
				jpaPropsToUse.put(PRE_ENLISTED_DB_CONNECTION, Boolean.TRUE);
				
				return getProviderFor(emfb, jpaPropsToUse, resourceProviderProperties, tx, null); 
			};
		return create;
	}

	private Function<ThreadLocal<TransactionControl>, AbstractJPAEntityManagerProvider> handlePreEnlistedConnection(
			EntityManagerFactoryBuilder emfb, Map<String, Object> resourceProviderProperties,
			Map<String, Object> jpaPropsToUse) {
		Function<ThreadLocal<TransactionControl>, AbstractJPAEntityManagerProvider> create;
		Object supplied = jpaPropsToUse.get(JTA_DATA_SOURCE);
		if(supplied == null) {
			LOGGER.error("No datasource supplied in the configuration");
			throw new IllegalArgumentException("No pre-enlisted datasource could be found to create the EntityManagerFactory. Please provide either a javax.persistence.jtaDataSource");
		}
		create = tx -> {
			DataSource toUse = JPADataSourceHelper.poolIfNecessary(resourceProviderProperties, (DataSource) supplied);
			jpaPropsToUse.put(JTA_DATA_SOURCE, toUse);
			return getProviderFor(emfb, jpaPropsToUse, resourceProviderProperties, tx, () -> {
					if (toUse instanceof HikariDataSource) {
						((HikariDataSource)toUse).close();
					}
				});
		};
		return create;
	}

	private Function<ThreadLocal<TransactionControl>, AbstractJPAEntityManagerProvider> handleNormalDataSource(
			EntityManagerFactoryBuilder emfb, Map<String, Object> resourceProviderProperties,
			Map<String, Object> jpaPropsToUse) {
		Function<ThreadLocal<TransactionControl>, AbstractJPAEntityManagerProvider> create;
		Object supplied = jpaPropsToUse.get(JTA_DATA_SOURCE);
		if(supplied == null) {
			supplied = jpaPropsToUse.get("javax.persistence.dataSource");
		}
		if(supplied == null) {
			supplied = jpaPropsToUse.get(NON_JTA_DATA_SOURCE);
		}

		if(supplied == null) {
			LOGGER.error("No datasource supplied in the configuration");
			throw new IllegalArgumentException("No datasource could be found to create the EntityManagerFactory. Please provide either a javax.persistence.jtaDataSource, a javax.persistence.nonJtaDataSource, or a javax.persistence.dataSource");
		}
		
		DataSource ds;
		
		try {
			if (supplied instanceof XADataSource) {
				ds = new XADataSourceMapper((XADataSource)supplied);
			} else if (supplied instanceof Wrapper && ((Wrapper)supplied).isWrapperFor(XADataSource.class)) {
				ds = new XADataSourceMapper(((Wrapper)supplied).unwrap(XADataSource.class));
			} else {
				LOGGER.error("The datasource supplied was not XA capable");
				throw new IllegalArgumentException("The datasource supplied to create the EntityManagerFactory is not an XADataSource and so cannot be enlisted. Please provide either a javax.persistence.jtaDataSource, a javax.persistence.nonJtaDataSource, or a javax.persistence.dataSource which implements XADataSource");
			}
		} catch (SQLException sqle) {
			LOGGER.error("Unable to obtain an XA DataSource for the JPAEntityManagerProvider", sqle);
			throw new IllegalArgumentException("The supplied DataSource could not be enlisted with XA transactions", sqle);
		}

		
		create = tx -> {
			DataSource toUse = JPADataSourceHelper.poolIfNecessary(resourceProviderProperties, ds);
			jpaPropsToUse.put(JTA_DATA_SOURCE, toUse);
			Object o = jpaPropsToUse.get(NON_JTA_DATA_SOURCE);
			if(o == null) {
				jpaPropsToUse.put(NON_JTA_DATA_SOURCE, toUse);
			} else if (o instanceof DataSource) {
				jpaPropsToUse.put(NON_JTA_DATA_SOURCE, JPADataSourceHelper
						.poolIfNecessary(resourceProviderProperties, (DataSource) o));
			}
			return getProviderFor(emfb, jpaPropsToUse, resourceProviderProperties, tx, () -> {
					if (toUse instanceof HikariDataSource) {
						((HikariDataSource)toUse).close();
					}
				});
		};
		return create;
	}

	public AbstractJPAEntityManagerProvider getProviderFor(EntityManagerFactoryBuilder emfb, Map<String, Object> jpaProperties,
		Map<String, Object> resourceProviderProperties, ThreadLocal<TransactionControl> localStore, Runnable onClose) {
		Map<String, Object> toUse;
		if(checkEnlistment(resourceProviderProperties)) {
			toUse = enlistDataSource(localStore, jpaProperties);
		} else {
			toUse = jpaProperties;
		}
		
		setupTransactionManager(context, toUse, localStore, emfb);
		
		return localStore.get().notSupported(() -> internalBuilderCreate(emfb, toUse, localStore, onClose));
	}

	private boolean checkEnlistment(Map<String, Object> resourceProviderProperties) {
		if (toBoolean(resourceProviderProperties, LOCAL_ENLISTMENT_ENABLED, false)) {
			throw new TransactionException("This Resource Provider does not support Local transactions");
		} else if (!toBoolean(resourceProviderProperties, XA_ENLISTMENT_ENABLED, true)) {
			throw new TransactionException(
					"This Resource Provider always enlists in XA transactions as it does not support local transactions");
		}
		
		return !toBoolean(resourceProviderProperties, PRE_ENLISTED_DB_CONNECTION, false);
	}

	private Map<String, Object> enlistDataSource(ThreadLocal<TransactionControl> tx, Map<String, Object> jpaProperties) {
		Map<String, Object> toReturn = new HashMap<>(jpaProperties);
		
		DataSource enlistedDS = new EnlistingDataSource(tx, 
				(DataSource)jpaProperties.get(JTA_DATA_SOURCE));
		
		toReturn.put(JTA_DATA_SOURCE, enlistedDS);
		
		return toReturn;
	}

	private void setupTransactionManager(BundleContext context, Map<String, Object> props, 
			ThreadLocal<TransactionControl> t, EntityManagerFactoryBuilder builder) {
		String provider = builder.getPersistenceProviderName();
		Bundle providerBundle = builder.getPersistenceProviderBundle();
		
		if(providerBundle == null) {
			LOGGER.warn("Unable to find a Persistence Provider for the provider named {}, so no XA plugin can be registered. XA transactions are unlikely to function properly.", provider);
			return;
		}
	
		Bundle txControlProviderBundle = context.getBundle();
		
		try {
			if("org.hibernate.jpa.HibernatePersistenceProvider".equals(provider)) {
				
				if(props.containsKey("hibernate.transaction.coordinator_class")) {
					LOGGER.warn("The JPA configuration properties already define a Hibernate transaction coordinator. This resource provider will not install its own plugin.");
					return;
				}
				
				String pluginClass;
				
				Bundle toUse = findSource(providerBundle, "org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder");
				
				if(toUse != null) {
					
					try {
						toUse.loadClass("org.hibernate.resource.transaction.spi.DdlTransactionIsolator");
						LOGGER.debug("Detected Hibernate 5.2.2 or above when attempting to install the XA plugin.");
						pluginClass = "org.apache.aries.tx.control.jpa.xa.plugin.hibernate.impl.Hibernate522TxControlPlatform";
					} catch (Exception e) {
						LOGGER.debug("Detected Hibernate 5.2.0 or 5.2.1 when attempting to install the XA plugin.");
						pluginClass = "org.apache.aries.tx.control.jpa.xa.plugin.hibernate.impl.Hibernate520TxControlPlatform";
					}
				} else {
					toUse = findSource(providerBundle, "org.hibernate.resource.transaction.TransactionCoordinatorBuilder");
					if(toUse != null) {
						LOGGER.debug("Detected Hibernate 5.0.x or 5.1.x or above when attempting to install the XA plugin.");
						pluginClass = "org.apache.aries.tx.control.jpa.xa.plugin.hibernate.impl.HibernateTxControlPlatform";
					} else {
						LOGGER.warn("Detected a Hibernate provider, but we were unable to load an appropriate XA plugin");
						return;
					}
				}
				
				ClassLoader pluginLoader = getPluginLoader(toUse, txControlProviderBundle);
				
				Class<?> pluginClazz = pluginLoader.loadClass(pluginClass);
				Object plugin = pluginClazz.getConstructor(ThreadLocal.class)
					.newInstance(t);
				
				props.put("hibernate.transaction.coordinator_class", plugin);
				
			} else if("org.apache.openjpa.persistence.PersistenceProviderImpl".equals(provider)) {
				
				if(props.containsKey("openjpa.ManagedRuntime")) {
					LOGGER.warn("The JPA configuration properties already define an OpenJPA transaction runtime. This resource provider will not install its own plugin.");
					return;
				}
				
				ClassLoader pluginLoader = getPluginLoader(providerBundle, txControlProviderBundle);
					
				Class<?> pluginClazz = pluginLoader.loadClass("org.apache.aries.tx.control.jpa.xa.plugin.openjpa.impl.OpenJPATxControlPlatform");
				Object plugin = pluginClazz.getConstructor(ThreadLocal.class)
						.newInstance(t);
					
				props.put("openjpa.ManagedRuntime", plugin);
				props.put("openjpa.ConnectionFactoryMode", "managed");
				props.put("openjpa.TransactionMode", "managed");
					
			} else if("org.eclipse.persistence.jpa.PersistenceProvider".equals(provider)) {
				
				if(props.containsKey("eclipselink.target-server")) {
					LOGGER.warn("The JPA configuration properties already define an EclipseLink transaction target. This resource provider will not install its own plugin.");
					return;
				}
				
				ClassLoader pluginLoader = getPluginLoader(providerBundle, txControlProviderBundle);
				
				Class<?> pluginClazz = pluginLoader.loadClass("org.apache.aries.tx.control.jpa.xa.plugin.eclipse.impl.EclipseTxControlPlatform");
				
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
				LOGGER.warn("The persistence provider {} is not recognised, so no adapter plugin can be registered with it. XA transactions are unlikely to work properly", provider);
				return;
			} 
		} catch (Exception e) {
			LOGGER.error("There was a problem trying to install a transaction integration plugin for the JPA provider {}.", provider, e);
		}
	}

	private Bundle findSource(Bundle providerBundle, String toFind) {
		try{
			providerBundle.loadClass(toFind);
			return providerBundle;
		} catch (Exception e) {
			BundleWiring wiring = providerBundle.adapt(BundleWiring.class);
			return wiring.getRequiredWires("osgi.wiring.package").stream()
						.filter(bw -> "org.hibernate".equals(bw.getCapability().getAttributes().get("osgi.wiring.package")))
						.map(BundleWire::getProviderWiring)
						.map(BundleWiring::getBundle)
						.findFirst()
						.filter(b -> {
								try {
									b.loadClass(toFind);
									return true;
								} catch (Exception e2) {
									return false;
								}
							}).orElse(null);
		}
	}

	private AbstractJPAEntityManagerProvider internalBuilderCreate(EntityManagerFactoryBuilder emfb,
			Map<String, Object> jpaProperties, ThreadLocal<TransactionControl> tx, Runnable onClose) {
		EntityManagerFactory emf = emfb.createEntityManagerFactory(jpaProperties);
		
		validateEMF(emf);
		
		return new JPAEntityManagerProviderImpl(emf, tx, () -> {
			try {
				emf.close();
			} catch (Exception e) {
			}
			if (onClose != null) {
				onClose.run();
			}
		});
	}

	private void validateEMF(EntityManagerFactory emf) {
		Object o = emf.getProperties().get(TRANSACTION_TYPE);
		
		PersistenceUnitTransactionType tranType;
		if(o instanceof PersistenceUnitTransactionType) {
			tranType = (PersistenceUnitTransactionType) o;
		} else if (o instanceof String) {
			tranType = PersistenceUnitTransactionType.valueOf(o.toString());
		} else {
			//TODO log this?
			tranType = JTA;
		}
		
		if(JTA != tranType) {
			throw new IllegalArgumentException("The supplied EntityManagerFactory is not declared JTA");
		}
	}

	@Override
	public AbstractJPAEntityManagerProvider getProviderFor(EntityManagerFactory emf,
			Map<String, Object> resourceProviderProperties) {
		checkEnlistment(resourceProviderProperties);
		validateEMF(emf);
		
		return new JPAEntityManagerProviderImpl(emf, new ThreadLocal<>(), null);
	}

	public static boolean toBoolean(Map<String, Object> props, String key, boolean defaultValue) {
		Object o =  ofNullable(props)
			.map(m -> m.get(key))
			.orElse(defaultValue);
		
		if (o instanceof Boolean) {
			return ((Boolean) o).booleanValue();
		} else if(o instanceof String) {
			return Boolean.parseBoolean((String) o);
		} else {
			throw new IllegalArgumentException("The property " + key + " cannot be converted to a boolean");
		}
	}
	
	private ClassLoader getPluginLoader(Bundle providerBundle, Bundle txControlProviderBundle) {
		return new ClassLoader() {

			ConcurrentMap<String, Class<?>> loaded = new ConcurrentHashMap<>();
			
			@Override
			public Class<?> loadClass(String name) throws ClassNotFoundException {
				if(name.startsWith("org.apache.aries.tx.control.jpa.xa.plugin")) {
					
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
				   name.startsWith("org.osgi.service.transaction.control") ||
						name.startsWith("org.slf4j")) {
					return txControlProviderBundle.loadClass(name);
				}
				return providerBundle.loadClass(name);
			}
		};
	}
	
	public static class EnlistingDataSource implements DataSource {
		
		private final DataSource delegate;

		private final UUID resourceId = UUID.randomUUID();
		
		private final ThreadLocal<TransactionControl> txControlToUse;
		
		public EnlistingDataSource(ThreadLocal<TransactionControl> txControlToUse, DataSource delegate) {
			this.txControlToUse = txControlToUse;
			this.delegate = delegate;
		}
		
		public TransactionControl getTxControl() {
			TransactionControl transactionControl = txControlToUse.get();
			if(transactionControl == null) {
				throw new TransactionException("A No Transaction Context could not be created because there is no associated Transaction Control");
			}
			return transactionControl;
		}

		public PrintWriter getLogWriter() throws SQLException {
			return delegate.getLogWriter();
		}

		public <T> T unwrap(Class<T> iface) throws SQLException {
			return delegate.unwrap(iface);
		}

		public void setLogWriter(PrintWriter out) throws SQLException {
			delegate.setLogWriter(out);
		}

		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			return delegate.isWrapperFor(iface);
		}

		public Connection getConnection() throws SQLException {
			return enlistedConnection(() -> delegate.getConnection());
		}

		public void setLoginTimeout(int seconds) throws SQLException {
			delegate.setLoginTimeout(seconds);
		}

		public Connection getConnection(String username, String password) throws SQLException {
			return enlistedConnection(() -> delegate.getConnection(username, password));
		}

		public int getLoginTimeout() throws SQLException {
			return delegate.getLoginTimeout();
		}

		public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
			return delegate.getParentLogger();
		}
		
		private Connection enlistedConnection(Callable<Connection> supplier) {
			TransactionContext txContext = getTxControl().getCurrentContext();

			if (txContext == null) {
				throw new TransactionException("The resource " + resourceId
						+ " cannot be accessed outside of an active Transaction Context");
			}

			Connection existing = (Connection) txContext.getScopedValue(resourceId);

			if (existing != null) {
				return existing;
			}

			Connection toReturn;
			Connection toClose;

			try {
				toClose = supplier.call();
				if (txContext.getTransactionStatus() == NO_TRANSACTION) {
					toReturn = new ScopedConnectionWrapper(toClose);
				} else if (txContext.supportsXA()) {
					toReturn = new TxConnectionWrapper(toClose);
					txContext.registerXAResource(getXAResource(toClose), null);
				} else {
					throw new TransactionException(
							"There is a transaction active, but it does not support XA participants");
				}
			} catch (Exception sqle) {
				throw new TransactionException(
						"There was a problem getting hold of a database connection",
						sqle);
			}

			
			txContext.postCompletion(x -> {
					try {
						toClose.close();
					} catch (SQLException sqle) {
						// TODO log this
					}
				});
			
			txContext.putScopedValue(resourceId, toReturn);
			
			return toReturn;
		}
		
		private XAResource getXAResource(Connection conn) throws SQLException {
			if(conn instanceof XAConnectionWrapper) {
				return ((XAConnectionWrapper)conn).getXaResource();
			} else if(conn.isWrapperFor(XAConnectionWrapper.class)){
				return conn.unwrap(XAConnectionWrapper.class).getXaResource();
			} else {
				throw new IllegalArgumentException("The XAResource for the connection cannot be found");
			}
		}
	}
}
