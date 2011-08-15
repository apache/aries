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

import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.Resource;
import org.osgi.service.subsystem.Subsystem;

public class SubsystemResolverHook implements ResolverHook {
	public void end() {
		// noop
	}

	public void filterMatches(BundleRequirement requirement, Collection<BundleCapability> candidates) {
		// noop
	}

	public void filterResolvable(Collection<BundleRevision> candidates) {
		SubsystemManager manager = Activator.getSubsystemManager();
		for (Resource candidate : candidates) {
			Collection<AriesSubsystem> subsystems = manager.getSubsystems(candidate);
			for (AriesSubsystem subsystem : subsystems) {
				// TODO Uncomment when features are implemented.
//				if (subsystem instanceof FeatureSubsystem) {
//					// Feature subsystems require no isolation.
//					continue;
//				}
				// Otherwise, the candidate is part of an application or composite subsystem requiring isolation.
				// But only when in the INSTALLING or INSTALLED state.
				if (EnumSet.of(Subsystem.State.INSTALLING, Subsystem.State.INSTALLED).contains(subsystem.getState())) {
					candidates.remove(candidate);
				}
			}
		}
	}

	public void filterSingletonCollisions(BundleCapability singleton, Collection<BundleCapability> collisionCandidates) {
		// noop
	}
}
