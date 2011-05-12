package org.apache.aries.subsystem.scope.itests;

import org.apache.aries.subsystem.scope.Scope;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class Utils {
	public static Bundle findBundle(String symbolicName, BundleContext bundleContext) {
		Bundle[] bundles = bundleContext.getBundles();
		for (Bundle bundle : bundles) {
			if (bundle.getSymbolicName().equals(symbolicName)) {
				return bundle;
			}
		}
		return null;
	}
	
	public static Bundle findBundle(String symbolicName, Scope scope) {
		if (scope == null) return null;
		for (Bundle b : scope.getBundles()) {
			if (symbolicName == null) {
				if (b.getSymbolicName() == null)
					return b;
			}
			else if (symbolicName.equals(b.getSymbolicName()))
				return b;
		}
		return null;
	}
	
	public static void ungetQuietly(ServiceReference<?> serviceReference, BundleContext bundleContext) {
		if (serviceReference == null) return;
		try {
			bundleContext.ungetService(serviceReference);
		}
		catch (Exception e) {
			// ignore
		}
	}
	
	public static void uninstallQuietly(Bundle bundle) {
		if (bundle == null) return;
		try {
			bundle.uninstall();
		}
		catch (Exception e) {
			// ignore
		}
	}
	
	public static void unregisterQuietly(ServiceRegistration<?> serviceRegistration) {
		if (serviceRegistration == null) return;
		try {
			serviceRegistration.unregister();
		}
		catch (Exception e) {
			// ignore
		}
	}
}
