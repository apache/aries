package org.apache.aries.subsystem.core.internal;

import java.util.Collection;
import java.util.HashSet;

import org.osgi.framework.Version;
import org.osgi.service.subsystem.Subsystem;

public class TargetRegion {
	Collection<Subsystem> region = new HashSet<Subsystem>();

	public TargetRegion(AriesSubsystem subsystem) {
		// Find the scoped subsystem that controls the region.
		AriesSubsystem controllingScopedSubsystem = subsystem;
		while (controllingScopedSubsystem.isFeature())
			controllingScopedSubsystem = (AriesSubsystem)subsystem.getParents().iterator().next();
		// The scoped subsystem controlling the region is part of the region.
		region.add(controllingScopedSubsystem);
		// All children of the scoped subsystem are part of the region. If the
		// child is a feature, then all descendants of the child that are
		// features and part of an unbroken line of features are part of the
		// region.
		addChildrenToRegion(controllingScopedSubsystem);
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
	
	private void addChildrenToRegion(AriesSubsystem controllingScopedSubsystem) {
		for (Subsystem child : controllingScopedSubsystem.getChildren()) {
			region.add(child);
			// If the child is a feature, all of its children that are features
			// must be added as well.
			if (((AriesSubsystem)child).isFeature())
				addFeatureDescendentsToRegion((AriesSubsystem)child);
		}
	}
	
	private void addFeatureDescendentsToRegion(AriesSubsystem parent) {
		for (Subsystem child : parent.getChildren())
			// If the descendant is not a feature, skip it.
			if (((AriesSubsystem)child).isFeature()) {
				region.add(child);
				// All descendants that are features and part of an unbroken
				// line of features must be added.
				addFeatureDescendentsToRegion((AriesSubsystem)child);
			}
	}
}
