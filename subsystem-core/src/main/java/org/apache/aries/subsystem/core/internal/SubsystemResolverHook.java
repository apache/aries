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
import java.util.EnumSet;
import java.util.Iterator;

import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.service.subsystem.Subsystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubsystemResolverHook implements ResolverHook {
	private static final Logger LOGGER = LoggerFactory.getLogger(SubsystemResolverHook.class);
	
	public void end() {
		// noop
	}

	public void filterMatches(BundleRequirement requirement, Collection<BundleCapability> candidates) {
		// noop
	}

	public void filterResolvable(Collection<BundleRevision> candidates) {
		try {
			for (Iterator<BundleRevision> iterator = candidates.iterator(); iterator.hasNext();) {
				Collection<AriesSubsystem> subsystems = AriesSubsystem.getSubsystems(iterator.next());
				for (AriesSubsystem subsystem : subsystems) {
					if (subsystem.isFeature()) {
						// Feature subsystems require no isolation.
						continue;
					}
					// Otherwise, the candidate is part of an application or composite subsystem requiring isolation.
					// But only when in the INSTALLING or INSTALLED state.
					if (EnumSet.of(Subsystem.State.INSTALLING, Subsystem.State.INSTALLED).contains(subsystem.getState())) {
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
}
