package org.apache.aries.subsystem.core.internal;

import java.util.HashMap;
import java.util.Map;

import org.apache.aries.subsystem.Subsystem;
import org.eclipse.equinox.region.Region;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

public class SubsystemServiceFactory implements ServiceFactory<Subsystem> {
	private final Map<Bundle, Subsystem> bundlesToSubsystems = new HashMap<Bundle, Subsystem>();
	private final Subsystem rootSubsystem;
	
	public SubsystemServiceFactory(Region region) {
		rootSubsystem = new RootSubsystem(region);
	}
	
	public Subsystem getService(Bundle bundle, ServiceRegistration<Subsystem> registration) {
		Subsystem result = bundlesToSubsystems.get(bundle);
		if (result == null) {
			result = rootSubsystem;
			bundlesToSubsystems.put(bundle, result);
		}
		return result;
	}

	public void ungetService(Bundle bundle, ServiceRegistration<Subsystem> registration, Subsystem service) {
		bundlesToSubsystems.remove(bundle);
	}
}
