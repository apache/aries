package org.apache.aries.subsystem.scope.internal;

import org.apache.aries.subsystem.scope.Scope;
import org.apache.aries.subsystem.scope.impl.ScopeManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.bundle.EventHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.hooks.service.EventListenerHook;

public class Activator implements BundleActivator, ServiceFactory<Scope> {
	private static volatile BundleContext bundleContext;
	
	public static BundleContext getBundleContext() {
		return bundleContext;
	}
	
	private ScopeManager scopeManager;
	private ServiceRegistration<?> scopeManagerReg;
	private ServiceRegistration<Scope> scopeFactoryReg;
	
	public Scope getService(Bundle b, ServiceRegistration<Scope> sr) {
		return scopeManager.getScope(b);
	}
	
	@SuppressWarnings("unchecked")
	public void start(BundleContext bundleContext) throws Exception {
		Activator.bundleContext = bundleContext;
		scopeManager = new ScopeManager(bundleContext);
		scopeManagerReg = bundleContext.registerService(
				new String[] {
						EventHook.class.getName(),
						EventListenerHook.class.getName(),
						org.osgi.framework.hooks.bundle.FindHook.class.getName(),
						org.osgi.framework.hooks.service.FindHook.class.getName(),
						ResolverHookFactory.class.getName(),
				},
				scopeManager, 
				null);
		scopeFactoryReg = (ServiceRegistration<Scope>)bundleContext.registerService(Scope.class.getName(), this, null);
	}
	
	public void stop(BundleContext bc) throws Exception {
		unregisterQuietly();
		scopeManager.shutdown();
		Activator.bundleContext = null;
	}
	
	public void ungetService(Bundle b, ServiceRegistration<Scope> sr, Scope s) {
	}
	
	private void unregisterQuietly() {
		unregisterQuietly(scopeFactoryReg);
		unregisterQuietly(scopeManagerReg);
	}
	
	private void unregisterQuietly(ServiceRegistration<?> serviceRegistration) {
		try {
			serviceRegistration.unregister();
		}
		catch (Exception e) {
			// ignore
		}
	}
}
