package org.apache.aries.tx.control.jpa.xa.impl;

import static java.util.Optional.ofNullable;
import static javax.persistence.spi.PersistenceUnitTransactionType.JTA;
import static org.osgi.service.transaction.control.TransactionStatus.NO_TRANSACTION;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import javax.transaction.xa.XAResource;

import org.apache.aries.tx.control.jdbc.common.impl.ScopedConnectionWrapper;
import org.apache.aries.tx.control.jdbc.common.impl.TxConnectionWrapper;
import org.apache.aries.tx.control.jdbc.xa.connection.impl.XAConnectionWrapper;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;
import org.osgi.service.transaction.control.TransactionContext;
import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.TransactionException;
import org.osgi.service.transaction.control.jpa.JPAEntityManagerProvider;
import org.osgi.service.transaction.control.jpa.JPAEntityManagerProviderFactory;

public class JPAEntityManagerProviderFactoryImpl implements JPAEntityManagerProviderFactory {

	@Override
	public JPAEntityManagerProvider getProviderFor(EntityManagerFactoryBuilder emfb, Map<String, Object> jpaProperties,
			Map<String, Object> resourceProviderProperties) {
		return new DelayedJPAEntityManagerProvider(tx -> {
				Map<String, Object> toUse;
				if(checkEnlistment(resourceProviderProperties)) {
					toUse = enlistDataSource(tx, jpaProperties);
				} else {
					toUse = jpaProperties;
				}
				return tx.notSupported(() -> internalBuilderCreate(emfb, toUse));
			});
	}

	private Map<String, Object> enlistDataSource(TransactionControl tx, Map<String, Object> jpaProperties) {
		Map<String, Object> toReturn = new HashMap<>(jpaProperties);
		
		DataSource ds = (DataSource) jpaProperties.get("javax.persistence.jtaDataSource");

		if(!jpaProperties.containsKey("javax.persistence.nonJtaDataSource")) {
			toReturn.put("javax.persistence.jtaDataSource", ds);
		}
		
		toReturn.put("javax.persistence.jtaDataSource", new EnlistingDataSource(tx, ds));
		
		return toReturn;
	}

	private JPAEntityManagerProvider internalBuilderCreate(EntityManagerFactoryBuilder emfb,
			Map<String, Object> jpaProperties) {
		EntityManagerFactory emf = emfb.createEntityManagerFactory(jpaProperties);
		
		validateEMF(emf);
		
		return new JPAEntityManagerProviderImpl(emf);
	}

	private void validateEMF(EntityManagerFactory emf) {
		Object o = emf.getProperties().get("javax.persistence.transactionType");
		
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
			throw new IllegalArgumentException("The supplied EntityManagerFactory is not declared RESOURCE_LOCAL");
		}
	}

	@Override
	public JPAEntityManagerProvider getProviderFor(EntityManagerFactory emf,
			Map<String, Object> resourceProviderProperties) {
		checkEnlistment(resourceProviderProperties);
		validateEMF(emf);
		
		return new JPAEntityManagerProviderImpl(emf);
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
	
	public class EnlistingDataSource implements DataSource {
		
		private final TransactionControl txControl;
		
		private final DataSource delegate;

		private final UUID resourceId = UUID.randomUUID();
		
		public EnlistingDataSource(TransactionControl txControl, DataSource delegate) {
			this.txControl = txControl;
			this.delegate = delegate;
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

		public Logger getParentLogger() throws SQLFeatureNotSupportedException {
			return delegate.getParentLogger();
		}
		
		private Connection enlistedConnection(Callable<Connection> supplier) {
			TransactionContext txContext = txControl.getCurrentContext();

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
					txContext.registerXAResource(getXAResource(toClose));
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
