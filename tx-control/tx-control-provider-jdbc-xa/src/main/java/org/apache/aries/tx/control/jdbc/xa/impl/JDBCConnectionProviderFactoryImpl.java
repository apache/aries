package org.apache.aries.tx.control.jdbc.xa.impl;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.osgi.service.jdbc.DataSourceFactory.JDBC_URL;

import java.sql.Driver;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.aries.tx.control.jdbc.common.impl.DriverDataSource;
import org.apache.aries.tx.control.jdbc.xa.connection.impl.XADataSourceMapper;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.service.transaction.control.TransactionException;
import org.osgi.service.transaction.control.jdbc.JDBCConnectionProvider;
import org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class JDBCConnectionProviderFactoryImpl implements JDBCConnectionProviderFactory {

	@Override
	public JDBCConnectionProvider getProviderFor(DataSourceFactory dsf, Properties jdbcProperties,
			Map<String, Object> resourceProviderProperties) {

		boolean xaEnabled = toBoolean(resourceProviderProperties, XA_ENLISTMENT_ENABLED, true);
		boolean localEnabled = toBoolean(resourceProviderProperties, LOCAL_ENLISTMENT_ENABLED, true);
		boolean useDriver = toBoolean(resourceProviderProperties, USE_DRIVER, false);
		
		checkEnlistment(xaEnabled, localEnabled, !useDriver);

		DataSource unpooled;
		try {
			if (useDriver) {
				unpooled = new DriverDataSource(dsf.createDriver(null), jdbcProperties.getProperty(JDBC_URL),
						jdbcProperties);
			} else if (xaEnabled) {
				unpooled = new XADataSourceMapper(dsf.createXADataSource(jdbcProperties));
			} else {
				unpooled = dsf.createDataSource(jdbcProperties);
			}
		} catch (SQLException sqle) {
			throw new TransactionException("Unable to create the JDBC resource provider", sqle);
		}

		DataSource toUse = poolIfNecessary(resourceProviderProperties, unpooled);

		return new JDBCConnectionProviderImpl(toUse, xaEnabled, localEnabled);
	}

	@Override
	public JDBCConnectionProvider getProviderFor(DataSource ds, Map<String, Object> resourceProviderProperties) {
		boolean xaEnabled = toBoolean(resourceProviderProperties, XA_ENLISTMENT_ENABLED, true);
		boolean localEnabled = toBoolean(resourceProviderProperties, LOCAL_ENLISTMENT_ENABLED, true);
		
		try {
			checkEnlistment(xaEnabled, localEnabled, ds.isWrapperFor(XADataSource.class));
			DataSource toUse = poolIfNecessary(resourceProviderProperties, xaEnabled ?
					new XADataSourceMapper(ds.unwrap(XADataSource.class)) : ds);
	
			return new JDBCConnectionProviderImpl(toUse, xaEnabled, localEnabled);
		} catch (SQLException sqle) {
			throw new TransactionException("Unable to create the JDBC resource provider", sqle);
		}
	}

	@Override
	public JDBCConnectionProvider getProviderFor(Driver driver, Properties jdbcProperties, 
			Map<String, Object> resourceProviderProperties) {
		
		boolean xaEnabled = toBoolean(resourceProviderProperties, XA_ENLISTMENT_ENABLED, false);
		boolean localEnabled = toBoolean(resourceProviderProperties, LOCAL_ENLISTMENT_ENABLED, true);
		
		checkEnlistment(xaEnabled, localEnabled, false);
		
		DataSource toUse = poolIfNecessary(resourceProviderProperties, 
				new DriverDataSource(driver, jdbcProperties.getProperty(JDBC_URL), jdbcProperties));
		
		return new JDBCConnectionProviderImpl(toUse, xaEnabled, localEnabled);
	}

	@Override
	public JDBCConnectionProvider getProviderFor(XADataSource ds, Map<String, Object> resourceProviderProperties) {
		
		boolean xaEnabled = toBoolean(resourceProviderProperties, XA_ENLISTMENT_ENABLED, true);
		boolean localEnabled = toBoolean(resourceProviderProperties, LOCAL_ENLISTMENT_ENABLED, true);
		
		checkEnlistment(xaEnabled, localEnabled, true);
		
		DataSource unpooled = new XADataSourceMapper(ds);
		
		return new JDBCConnectionProviderImpl(poolIfNecessary(resourceProviderProperties, unpooled),
				xaEnabled, localEnabled);
	}

	private void checkEnlistment(boolean xaEnabled, boolean localEnabled, boolean isXA) {
		
		if (!xaEnabled && !localEnabled) {
			throw new TransactionException("The configuration supports neither local nor XA transactions");
		} 
		
		if(xaEnabled && !isXA) {
			throw new TransactionException("The configuration is XA enabled but the resource is not suitable for XA enlistment");
		}
	}

	private DataSource poolIfNecessary(Map<String, Object> resourceProviderProperties, DataSource unpooled) {
		DataSource toUse;

		if (toBoolean(resourceProviderProperties, CONNECTION_POOLING_ENABLED, true)) {
			HikariConfig hcfg = new HikariConfig();
			hcfg.setDataSource(unpooled);

			// Sizes
			hcfg.setMaximumPoolSize(toInt(resourceProviderProperties, MAX_CONNECTIONS, 10));
			hcfg.setMinimumIdle(toInt(resourceProviderProperties, MIN_CONNECTIONS, 10));

			// Timeouts
			hcfg.setConnectionTimeout(toLong(resourceProviderProperties, CONNECTION_TIMEOUT, SECONDS.toMillis(30)));
			hcfg.setIdleTimeout(toLong(resourceProviderProperties, IDLE_TIMEOUT, TimeUnit.MINUTES.toMillis(3)));
			hcfg.setMaxLifetime(toLong(resourceProviderProperties, CONNECTION_LIFETIME, HOURS.toMillis(3)));

			toUse = new HikariDataSource(hcfg);

		} else {
			toUse = unpooled;
		}
		return toUse;
	}

	private boolean toBoolean(Map<String, Object> props, String key, boolean defaultValue) {
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

	private int toInt(Map<String, Object> props, String key, int defaultValue) {
		
		Object o =  ofNullable(props)
				.map(m -> m.get(key))
				.orElse(defaultValue);
		
		if (o instanceof Number) {
			return ((Number) o).intValue();
		} else if(o instanceof String) {
			return Integer.parseInt((String) o);
		} else {
			throw new IllegalArgumentException("The property " + key + " cannot be converted to an int");
		}
	}

	private long toLong(Map<String, Object> props, String key, long defaultValue) {
		
		Object o =  ofNullable(props)
				.map(m -> m.get(key))
				.orElse(defaultValue);
		
		if (o instanceof Number) {
			return ((Number) o).longValue();
		} else if(o instanceof String) {
			return Long.parseLong((String) o);
		} else {
			throw new IllegalArgumentException("The property " + key + " cannot be converted to a long");
		}
	}

}
