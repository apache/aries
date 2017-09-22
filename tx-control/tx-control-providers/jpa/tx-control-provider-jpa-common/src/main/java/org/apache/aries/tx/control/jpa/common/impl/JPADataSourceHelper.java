package org.apache.aries.tx.control.jpa.common.impl;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory.CONNECTION_LIFETIME;
import static org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory.CONNECTION_POOLING_ENABLED;
import static org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory.CONNECTION_TIMEOUT;
import static org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory.IDLE_TIMEOUT;
import static org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory.MAX_CONNECTIONS;
import static org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory.MIN_CONNECTIONS;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class JPADataSourceHelper {
	
	public static final String CONNECTION_TEST_QUERY = "aries.connection.test.query";
	
	public static DataSource poolIfNecessary(Map<String, Object> resourceProviderProperties, DataSource unpooled) {
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
	
			hcfg.setConnectionTestQuery((String)resourceProviderProperties.get(CONNECTION_TEST_QUERY));
			
			toUse = new HikariDataSource(hcfg);

		} else {
			toUse = unpooled;
		}
		return toUse;
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

	public static int toInt(Map<String, Object> props, String key, int defaultValue) {
		
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

	public static long toLong(Map<String, Object> props, String key, long defaultValue) {
		
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
