package org.apache.aries.subsystem.core.internal;

import java.util.Collection;
import java.util.HashSet;

import org.osgi.framework.Version;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

public class TargetRegion {
	Collection<Subsystem> region = new HashSet<Subsystem>();

	public TargetRegion(AriesSubsystem target) {
		region.add(target);
		addToRegion(target.getChildren());
	}

	public boolean contains(Subsystem subsystem) {
		for (Subsystem s : region) {
			if (s.getSymbolicName().equals(subsystem.getSymbolicName())
					&& s.getVersion().equals(subsystem.getVersion()))
				return true;
		}
		return false;
	}
	
	public Subsystem find(String symbolicName, Version version) {
		for (Subsystem s : region) {
			if (s.getSymbolicName().equals(symbolicName)
					&& s.getVersion().equals(version))
				return s;
		}
		return null;
	}

	private void addToRegion(Collection<Subsystem> children) {
		for (Subsystem child : children) {
			if (SubsystemConstants.SUBSYSTEM_TYPE_FEATURE.equals(child
					.getSubsystemHeaders(null).get(
							SubsystemConstants.SUBSYSTEM_TYPE))) {
				addToRegion(child.getChildren());
			}
			region.add(child);
		}
	}
}
