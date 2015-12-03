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

import org.apache.aries.subsystem.core.archive.AriesProvisionDependenciesDirective;
import org.apache.aries.subsystem.core.archive.PreferredProviderHeader;
import org.apache.aries.subsystem.core.archive.ProvisionPolicyDirective;
import org.apache.aries.subsystem.core.archive.SubsystemContentHeader;
import org.apache.aries.subsystem.core.archive.SubsystemManifest;
import org.apache.aries.subsystem.core.archive.SubsystemTypeHeader;
import org.osgi.service.subsystem.Subsystem.State;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

public class SubsystemManifestValidator {
	public static void validate(BasicSubsystem subsystem, SubsystemManifest manifest) {
		if (subsystem.getResource().getId() == 0) {
			return;
		}
		validatePreferredProviderHeader(manifest.getPreferredProviderHeader());
		validateAriesProvisionDependenciesDirective(subsystem);
		if (subsystem.isComposite()) {
			SubsystemContentHeader header = manifest.getSubsystemContentHeader();
			if (header == null) {
				return;
			}
			for (SubsystemContentHeader.Clause clause : header.getClauses()) {
				if (!clause.getVersionRange().isExact()) {
					throw new SubsystemException("Composite subsystem using version range for content: " + clause);
				}
			}
		}
		else if (subsystem.isFeature()) {
			SubsystemTypeHeader subsystemTypeHeader = manifest.getSubsystemTypeHeader();
			ProvisionPolicyDirective provisionPolicyDirective = subsystemTypeHeader.getProvisionPolicyDirective();
			if (provisionPolicyDirective.isAcceptDependencies()) {
				throw new SubsystemException("Feature subsystems may not declare a provision-policy of acceptDependencies");
			}
			if (manifest.getHeaders().get(SubsystemConstants.PREFERRED_PROVIDER) != null) {
				throw new SubsystemException("Feature subsystems may not declare a " + SubsystemConstants.PREFERRED_PROVIDER + " header");
			}
		}
	}
	
	private static void validatePreferredProviderHeader(PreferredProviderHeader header) {
		if (header == null) {
			return;
		}
		for (PreferredProviderHeader.Clause clause : header.getClauses()) {
			String type = (String)clause.getAttribute(PreferredProviderHeader.Clause.ATTRIBUTE_TYPE).getValue();
			if (!(SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE.equals(type) ||
					SubsystemConstants.SUBSYSTEM_TYPE_FEATURE.equals(type) ||
					Constants.ResourceTypeBundle.equals(type))) {
				throw new SubsystemException("Unsupported " + PreferredProviderHeader.NAME + " type: " + type);
			}
		}
	}
	
	private static void validateAriesProvisionDependenciesDirective(BasicSubsystem subsystem) {
		AriesProvisionDependenciesDirective directive = subsystem.getAriesProvisionDependenciesDirective();
		BasicSubsystem parent = subsystem.getResource().getParents().iterator().next();
		AriesProvisionDependenciesDirective parentDirective = parent.getAriesProvisionDependenciesDirective();
		if (!directive.equals(parentDirective) 
				&& (subsystem.isFeature()
						|| State.INSTALLING.equals(parent.getState()))) {
			throw new SubsystemException("The value of the " 
						+ AriesProvisionDependenciesDirective.NAME 
						+ " directive must be the same as the parent subsystem for features and implicitly installed subsystems.");
		}
	}
}
