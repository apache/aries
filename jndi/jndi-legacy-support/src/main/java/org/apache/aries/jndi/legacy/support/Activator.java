package org.apache.aries.jndi.legacy.support;

import javax.naming.spi.InitialContextFactoryBuilder;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception 
	{
		context.registerService(InitialContextFactoryBuilder.class.getName(), new LegacyInitialContextFinder(), null);
	}

	@Override
	public void stop(BundleContext context) throws Exception { }

}
