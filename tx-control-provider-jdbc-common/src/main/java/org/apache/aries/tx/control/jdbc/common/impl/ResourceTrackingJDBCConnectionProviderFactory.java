package org.apache.aries.tx.control.jdbc.common.impl;

import java.sql.Driver;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.aries.tx.control.resource.common.impl.TrackingResourceProviderFactory;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.service.transaction.control.jdbc.JDBCConnectionProvider;
import org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory;

public class ResourceTrackingJDBCConnectionProviderFactory extends 
	TrackingResourceProviderFactory<AbstractJDBCConnectionProvider>
	implements JDBCConnectionProviderFactory {

	private final InternalJDBCConnectionProviderFactory factory;
	
	public ResourceTrackingJDBCConnectionProviderFactory(InternalJDBCConnectionProviderFactory factory) {
		this.factory = factory;
	}

	@Override
	public JDBCConnectionProvider getProviderFor(DataSourceFactory dsf, Properties jdbcProperties,
			Map<String, Object> resourceProviderProperties) {
		return doGetResult(() -> factory.getProviderFor(dsf, 
				jdbcProperties, resourceProviderProperties));
	}

	@Override
	public JDBCConnectionProvider getProviderFor(DataSource ds, Map<String, Object> resourceProviderProperties) {
		return doGetResult(() -> factory.getProviderFor(ds, 
				resourceProviderProperties));
	}

	@Override
	public JDBCConnectionProvider getProviderFor(Driver driver, Properties jdbcProperties,
			Map<String, Object> resourceProviderProperties) {
		return doGetResult(() -> factory.getProviderFor(driver, 
				jdbcProperties, resourceProviderProperties));
	}

	@Override
	public JDBCConnectionProvider getProviderFor(XADataSource ds, Map<String, Object> resourceProviderProperties) {
		return doGetResult(() -> factory.getProviderFor(ds, 
				resourceProviderProperties));
	}
}