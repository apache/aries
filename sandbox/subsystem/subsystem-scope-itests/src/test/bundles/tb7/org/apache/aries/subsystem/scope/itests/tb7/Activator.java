package org.apache.aries.subsystem.scope.itests.tb7;

import org.apache.aries.subsystem.scope.itests.Service;
import org.apache.aries.subsystem.scope.itests.Utils;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {
	private ServiceRegistration<Service> serviceReg;

	public void start(final BundleContext bundleContext) throws Exception {
		serviceReg = bundleContext.registerService(
				Service.class, 
				new Service() {}, 
				null);
	}

	public void stop(BundleContext bundleContext) throws Exception {
		Utils.unregisterQuietly(serviceReg);
	}
}
