package org.apache.aries.tx.control.jdbc.common.impl;

import java.sql.Driver;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory;

public interface InternalJDBCConnectionProviderFactory extends JDBCConnectionProviderFactory {

	@Override
	AbstractJDBCConnectionProvider getProviderFor(DataSourceFactory dsf, Properties jdbcProperties,
			Map<String, Object> resourceProviderProperties);

	AbstractJDBCConnectionProvider getProviderFor(DataSource ds, Map<String, Object> resourceProviderProperties);

	@Override
	AbstractJDBCConnectionProvider getProviderFor(Driver driver, Properties jdbcProperties,
			Map<String, Object> resourceProviderProperties);

	AbstractJDBCConnectionProvider getProviderFor(XADataSource ds, Map<String, Object> resourceProviderProperties);

}
