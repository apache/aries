package org.apache.aries.subsystem.scope.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.aries.subsystem.scope.Scope;
import org.apache.aries.subsystem.scope.ScopeUpdate;
import org.apache.aries.subsystem.scope.impl.ScopeManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.bundle.EventHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.hooks.service.EventListenerHook;

public class Activator implements BundleActivator {
	private final Collection<ServiceRegistration<?>> serviceRegistrations = new ArrayList<ServiceRegistration<?>>();
	
	public void start(BundleContext bundleContext) throws Exception {
		ScopeManager sm = new ScopeManager(bundleContext);
		ServiceRegistration<?> sr = bundleContext.registerService(
				EventHook.class, 
				sm.newEventHook(), 
				null);
		serviceRegistrations.add(sr);
		ScopeUpdate su = sm.getRootScope().newScopeUpdate();
		su.getBundles().addAll(Arrays.asList(bundleContext.getBundles()));
		su.commit();
		sr = bundleContext.registerService(
				org.osgi.framework.hooks.bundle.FindHook.class,
				sm.newBundleFindHook(), 
				null);
		serviceRegistrations.add(sr);
		sr = bundleContext.registerService(
				ResolverHookFactory.class,
				sm.newResolverHookFactory(), 
				null);
		serviceRegistrations.add(sr);
		sr = bundleContext.registerService(
				EventListenerHook.class,
				sm.newEventListenerHook(), 
				null);
		serviceRegistrations.add(sr);
		sr = bundleContext.registerService(
				org.osgi.framework.hooks.service.FindHook.class,
				sm.newServiceFindHook(), 
				null);
		serviceRegistrations.add(sr);
		sr = bundleContext.registerService(
				Scope.class.getName(), 
				sm.newServiceFactory(), 
				null);
		serviceRegistrations.add(sr);
	}
	
	public void stop(BundleContext bc) throws Exception {
		unregisterQuietly();
	}
	
	private void unregisterQuietly() {
		for (ServiceRegistration<?> sr : serviceRegistrations)
			unregisterQuietly(sr);
		serviceRegistrations.clear();
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
