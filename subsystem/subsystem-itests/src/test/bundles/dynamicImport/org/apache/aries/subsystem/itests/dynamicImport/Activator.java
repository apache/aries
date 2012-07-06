package org.apache.aries.subsystem.itests.dynamicImport;

import org.apache.aries.subsystem.itests.hello.api.Hello;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator 
{

	ServiceRegistration _sr = null;
	
	@Override
	public void start(BundleContext bc) throws Exception 
	{
		System.out.println ("into " + this.getClass().getCanonicalName() + ".start()");
		
		Hello helloService = new DynamicImportHelloImpl();
		
		_sr = bc.registerService(Hello.class, helloService, null);
		
		System.out.println ("exiting " + this.getClass().getCanonicalName() + ".start()");
	}

	@Override
	public void stop(BundleContext bc) throws Exception 
	{
		if (_sr != null) { 
			_sr.unregister();
		}
	}
}
