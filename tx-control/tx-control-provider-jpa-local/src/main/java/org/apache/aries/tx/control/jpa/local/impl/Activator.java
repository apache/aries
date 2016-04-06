package org.apache.aries.tx.control.jpa.local.impl;

import static org.osgi.framework.Constants.SERVICE_PID;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.geronimo.specs.jpa.PersistenceActivator;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.transaction.control.jpa.JPAEntityManagerProviderFactory;

public class Activator implements BundleActivator {

	private final BundleActivator geronimoActivator;
	
	private ServiceRegistration<JPAEntityManagerProviderFactory> reg;
	private ServiceRegistration<ManagedServiceFactory> factoryReg;
	
	public Activator() {
		geronimoActivator = new PersistenceActivator();
	}
	
	@Override
	public void start(BundleContext context) throws Exception {
		geronimoActivator.start(context);
		
		reg = context.registerService(JPAEntityManagerProviderFactory.class, 
				new JPAEntityManagerProviderFactoryImpl(), getProperties());
		
		factoryReg = context.registerService(ManagedServiceFactory.class, 
				new ManagedServiceFactoryImpl(context), getMSFProperties());
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		reg.unregister();
		factoryReg.unregister();
		geronimoActivator.stop(context);
	}

	private Dictionary<String, Object> getProperties() {
		Dictionary<String, Object> props = new Hashtable<>();
		props.put("osgi.local.enabled", Boolean.TRUE);
		return props;
	}

	private Dictionary<String, ?> getMSFProperties() {
		Dictionary<String, Object> props = new Hashtable<>();
		props.put(SERVICE_PID, "org.apache.aries.tx.control.jpa.local");
		return props;
	}

}
