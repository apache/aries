package org.apache.aries.tx.control.jdbc.local.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.transaction.control.jdbc.JDBCConnectionProviderFactory;

public class Activator implements BundleActivator {

	private ServiceRegistration<JDBCConnectionProviderFactory> reg;
	
	@Override
	public void start(BundleContext context) throws Exception {
		reg = context.registerService(JDBCConnectionProviderFactory.class, 
				new JDBCConnectionProviderFactoryImpl(), getProperties());
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		reg.unregister();
	}

	private Dictionary<String, Object> getProperties() {
		Dictionary<String, Object> props = new Hashtable<>();
		props.put("osgi.local.enabled", Boolean.TRUE);
		return props;
	}

}
