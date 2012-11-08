/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.subsystem.core.internal;

import java.util.Collection;
import java.util.HashSet;

import org.osgi.framework.Version;
import org.osgi.service.subsystem.Subsystem;

public class TargetRegion {
	Collection<Subsystem> region = new HashSet<Subsystem>();

	public TargetRegion(BasicSubsystem subsystem) {
		// Find the scoped subsystem that controls the region.
		BasicSubsystem controllingScopedSubsystem = subsystem;
		while (controllingScopedSubsystem.isFeature())
			controllingScopedSubsystem = (BasicSubsystem)subsystem.getParents().iterator().next();
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
	
	private void addChildrenToRegion(BasicSubsystem controllingScopedSubsystem) {
		for (Subsystem child : controllingScopedSubsystem.getChildren()) {
			region.add(child);
			// If the child is a feature, all of its children that are features
			// must be added as well.
			if (((BasicSubsystem)child).isFeature())
				addFeatureDescendentsToRegion((BasicSubsystem)child);
		}
	}
	
	private void addFeatureDescendentsToRegion(BasicSubsystem parent) {
		for (Subsystem child : parent.getChildren())
			// If the descendant is not a feature, skip it.
			if (((BasicSubsystem)child).isFeature()) {
				region.add(child);
				// All descendants that are features and part of an unbroken
				// line of features must be added.
				addFeatureDescendentsToRegion((BasicSubsystem)child);
			}
	}
}
