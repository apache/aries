package org.apache.felix.resolver.impl;

import java.util.Collection;
import java.util.Map;
import java.util.SortedSet;

import org.apache.aries.subsystem.core.Environment;
import org.apache.aries.subsystem.core.ResolutionException;
import org.apache.felix.resolver.FelixEnvironment;
import org.osgi.framework.resource.Capability;
import org.osgi.framework.resource.Requirement;
import org.osgi.framework.resource.Resource;
import org.osgi.framework.resource.Wiring;

public class EnvironmentAdaptor implements FelixEnvironment {
	private final Environment environment;
	
	public EnvironmentAdaptor(Environment environment) {
		if (environment == null)
			throw new NullPointerException();
		this.environment = environment;
	}

	@Override
	public SortedSet<Capability> findProviders(Requirement requirement) {
		return environment.findProviders(requirement);
	}

	@Override
	public Map<Requirement, SortedSet<Capability>> findProviders(
			Collection<? extends Requirement> requirements) {
		return environment.findProviders(requirements);
	}

	@Override
	public Map<Resource, Wiring> getWirings() {
		return environment.getWirings();
	}

	@Override
	public boolean isEffective(Requirement requirement) {
		return environment.isEffective(requirement);
	}

	@Override
	public void checkExecutionEnvironment(Resource resource)
			throws ResolutionException {
		// noop
	}

	@Override
	public void checkNativeLibraries(Resource resource)
			throws ResolutionException {
		// noop
	}
}
