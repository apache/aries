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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.aries.subsystem.core.archive.PreferredProviderHeader;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Resource;
import org.osgi.service.subsystem.Subsystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubsystemResolverHook implements ResolverHook {
	private static final Logger LOGGER = LoggerFactory.getLogger(SubsystemResolverHook.class);
	
	private final Subsystems subsystems;
	
	public SubsystemResolverHook(Subsystems subsystems) {
		if (subsystems == null)
			throw new NullPointerException("Missing required parameter: subsystems");
		this.subsystems = subsystems;
	}
	
	public void end() {
		// noop
	}

	public void filterMatches(BundleRequirement requirement, Collection<BundleCapability> candidates) {
		// Filter out candidates that don't come from preferred providers when
		// there is at least one preferred provider.
		// (1) Find the subsystem(s) containing requirement.getResource() as a
		// constituent.
		Collection<BasicSubsystem> requirers = subsystems.getSubsystemsReferencing(requirement.getResource());
		// (2) For each candidate, ask each subsystem if the candidate or any of
		// the candidate's containing subsystems is a preferred provider. If at
		// least one preferred provider exists, filter out all other candidates
		// that are not also preferred providers.
		Collection<BundleCapability> preferredProviders = new ArrayList<BundleCapability>(candidates.size());
		for (BundleCapability candidate : candidates)
			for (BasicSubsystem subsystem : requirers) {
				PreferredProviderHeader header = subsystem.getSubsystemManifest().getPreferredProviderHeader();
				if (header != null && (header.contains(candidate.getResource()) || isResourceConstituentOfPreferredSubsystem(candidate.getResource(), subsystem)))
					preferredProviders.add(candidate);
			}
		if (!preferredProviders.isEmpty())
			candidates.retainAll(preferredProviders);
	}

	public void filterResolvable(Collection<BundleRevision> candidates) {
		try {
			for (Iterator<BundleRevision> iterator = candidates.iterator(); iterator.hasNext();) {
				BundleRevision revision = iterator.next();
				if (revision.equals(ThreadLocalBundleRevision.get())) {
					// The candidate is a bundle whose INSTALLED event is
					// currently being processed on this thread.
					iterator.remove();
					continue;
				}
				if (revision.getSymbolicName().startsWith(Constants.RegionContextBundleSymbolicNamePrefix))
					// Don't want to filter out the region context bundle.
					continue;
				Collection<BasicSubsystem> subsystems = this.subsystems.getSubsystemsReferencing(revision);
				if (subsystems.isEmpty()) {
				    // This is the revision of a bundle being installed as part of a subsystem installation
				    // before it has been added as a reference or constituent.
				    iterator.remove();
				    continue;
				}
				for (BasicSubsystem subsystem : subsystems) {
					if (subsystem.isFeature()) {
						// Feature subsystems require no isolation.
						continue;
					}
					// Otherwise, the candidate is part of an application or composite subsystem requiring isolation.
					// But only when in the INSTALLING state.
					if (Subsystem.State.INSTALLING.equals(subsystem.getState())) {
						iterator.remove();
					}
				}
			}
		}
		catch (RuntimeException e) {
			// This try/catch block is in place because exceptions occurring here are not showing up in the console during testing.
			LOGGER.debug("Unexpected exception while filtering resolution candidates: " + candidates, e);
		}
	}

	public void filterSingletonCollisions(BundleCapability singleton, Collection<BundleCapability> collisionCandidates) {
		// noop
	}
	
	private boolean isResourceConstituentOfPreferredSubsystem(Resource resource, BasicSubsystem preferer) {
		Collection<BasicSubsystem> subsystems = this.subsystems.getSubsystemsReferencing(resource);
		for (BasicSubsystem subsystem : subsystems)
			if (preferer.getSubsystemManifest().getPreferredProviderHeader().contains(subsystem))
				return true;
		return false;
	}
}
