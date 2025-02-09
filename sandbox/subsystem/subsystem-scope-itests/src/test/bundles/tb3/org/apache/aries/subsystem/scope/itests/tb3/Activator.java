package org.apache.aries.subsystem.scope.itests.tb3;

import org.apache.aries.subsystem.scope.Scope;
import org.apache.aries.subsystem.scope.itests.ScopeProvider;
import org.apache.aries.subsystem.scope.itests.Utils;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {
	private ServiceRegistration<ScopeProvider> scopeProviderReg;
	private ServiceReference<Scope> scopeRef;

	public void start(BundleContext bundleContext) throws Exception {
		scopeRef = bundleContext.getServiceReference(Scope.class);
		final Scope scope = bundleContext.getService(scopeRef);
		scopeProviderReg = bundleContext.registerService(
				ScopeProvider.class, 
				new ScopeProvider() {
					public Scope getScope() {
						return scope;
					}
				}, 
				null);
	}

	public void stop(BundleContext bundleContext) throws Exception {
		Utils.unregisterQuietly(scopeProviderReg);
		Utils.ungetQuietly(scopeRef, bundleContext);
	}
}
