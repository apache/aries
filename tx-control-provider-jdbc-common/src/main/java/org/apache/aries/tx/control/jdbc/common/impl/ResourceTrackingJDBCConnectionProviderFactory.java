package org.apache.aries.tx.control.jdbc.common.impl;

import java.sql.Driver;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.service.transaction.control.jdbc.JDBCConnectionProvider;
import org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory;

class ResourceTrackingJDBCConnectionProviderFactory implements
	JDBCConnectionProviderFactory {

	private final List<AbstractJDBCConnectionProvider> toClose = new ArrayList<>();
	
	private final InternalJDBCConnectionProviderFactory factory;
	
	private boolean closed;
	
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
	
	private AbstractJDBCConnectionProvider doGetResult(Supplier<AbstractJDBCConnectionProvider> getter) {
		synchronized (getter) {
			if (closed) {
				throw new IllegalStateException("This ResourceProvider has been reclaimed because the factory service that provided it was released");
			}
		}
		AbstractJDBCConnectionProvider ajcp = getter.get();
		boolean destroy = false;
		synchronized (toClose) {
			if (closed) {
				destroy = true;
			} else {
			    toClose.add(ajcp);
			}
		}
		if(destroy) {
			ajcp.close();
			throw new IllegalStateException("This ResourceProvider has been reclaimed because the factory service that provided it was released");
		}
		return ajcp;
	}

	public void closeAll() {
		synchronized (toClose) {
			closed = true;
		}
		// toClose is now up to date and no other thread will write it
		toClose.stream().forEach(ajcp -> {
			try {
				ajcp.close();
			} catch (Exception e) {}
		});
		
		toClose.clear();
	}
}