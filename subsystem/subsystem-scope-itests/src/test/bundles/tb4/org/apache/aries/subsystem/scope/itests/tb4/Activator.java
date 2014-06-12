package org.apache.aries.subsystem.scope.itests.tb4;

import java.util.Arrays;
import java.util.Collection;

import org.apache.aries.subsystem.scope.itests.BundleProvider;
import org.apache.aries.subsystem.scope.itests.Utils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {
	public void start(final BundleContext bundleContext) throws Exception {
		bundleContext.registerService(
				BundleProvider.class, 
				new BundleProvider() {
					public Bundle getBundle(long id) {
						return bundleContext.getBundle(id);
					}
					
					public Collection<Bundle> getBundles() {
						return Arrays.asList(bundleContext.getBundles());
					}
				}, 
				null);
	}

	public void stop(BundleContext bundleContext) throws Exception {
	}
}
