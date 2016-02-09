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
import java.util.Map;

import org.apache.aries.subsystem.core.archive.DeploymentManifest;
import org.apache.aries.subsystem.core.archive.ProvisionResourceHeader;
import org.apache.aries.subsystem.core.archive.SubsystemContentHeader;
import org.apache.aries.subsystem.core.archive.SubsystemManifest;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.CoordinationException;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;
import org.osgi.service.subsystem.Subsystem.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {
	private static final Logger logger = LoggerFactory.getLogger(Utils.class);
	
	public static String computeCoordinationName(Subsystem subsystem) {
		return subsystem.getSymbolicName() + '-' + subsystem.getSubsystemId();
	}
	
	public static Coordination createCoordination() {
		return Activator.getInstance().getCoordinator().begin(BasicSubsystem.ROOT_SYMBOLIC_NAME + "-0", 0);
	}
	
	public static Coordination createCoordination(BasicSubsystem subsystem) {
		return Activator.getInstance().getCoordinator().begin(computeCoordinationName(subsystem), 0);
	}
	
	public static BasicSubsystem findFirstSubsystemAcceptingDependenciesStartingFrom(BasicSubsystem subsystem) {
		// The following loop is guaranteed to end once the root subsystem has
		// been reached.
		while (!isAcceptDependencies(subsystem))
			subsystem = (BasicSubsystem)subsystem.getParents().iterator().next();
		return subsystem;
	}
	
	public static BasicSubsystem findFirstScopedAncestorWithSharingPolicy(SubsystemResource descendant) {
		Collection<BasicSubsystem> parents = descendant.getParents();
		if (parents == null || parents.isEmpty()) {
			return null;
		}
		BasicSubsystem result = (BasicSubsystem) parents.iterator().next();
		// The result is defined as the first scoped ancestor whose sharing 
		// policy has already been set. This covers the case of multiple 
		// subsystems from the same archive being installed whose regions will 
		// form a tree of depth N.
		while (	// We only want scoped subsystems because they control 
				// the region.
				!result.isScoped()
				// If the state is INSTALLING then the sharing policy 
				// has not yet been set. This means we cannot use the
				// region in order to test visibility and must proceed
				// to the next parent.
				|| result.getState().equals(State.INSTALLING)) {
			result = result.getResource().getParents().iterator().next();
		}
		return result;
	}
	
	public static BasicSubsystem findScopedSubsystemInRegion(BasicSubsystem subsystem) {
		while (!subsystem.isScoped())
			subsystem = (BasicSubsystem)subsystem.getParents().iterator().next();
		return subsystem;
	}

	public static int getActiveUseCount(Resource resource) {
		int result = 0;
		for (BasicSubsystem subsystem : Activator.getInstance().getSubsystems().getSubsystemsReferencing(resource))
			if (Subsystem.State.ACTIVE.equals(subsystem.getState()))
				result++;
		return result;
	}
	
	public static long getId(Resource resource) {
		if (resource instanceof BasicSubsystem)
			return ((BasicSubsystem)resource).getSubsystemId();
		if (resource instanceof BundleRevision)
			return ((BundleRevision)resource).getBundle().getBundleId();
		return -1;
	}
	
	public static void handleTrowable(Throwable t) {
		if (t instanceof SubsystemException) {
			throw (SubsystemException)t;
		}
		if (t instanceof SecurityException) {
			throw (SecurityException)t;
		}
		throw new SubsystemException(t);
	}
	
	public static void installResource(Resource resource, BasicSubsystem subsystem) {
		Coordination coordination = Utils.createCoordination(subsystem);
		try {
			ResourceInstaller.newInstance(coordination, resource, subsystem).install();
		}
		catch (Throwable t) {
			coordination.fail(t);
		}
		finally {
			try {
				coordination.end();
			}
			catch (CoordinationException e) {
				logger.error("Resource could not be installed", e);
			}
		}
	}
	
	public static boolean isAcceptDependencies(BasicSubsystem subsystem) {
		return subsystem.getSubsystemManifest().getSubsystemTypeHeader().getProvisionPolicyDirective().isAcceptDependencies();
	}
	
	public static boolean isBundle(Resource resource) {
		String type = ResourceHelper.getTypeAttribute(resource);
		return IdentityNamespace.TYPE_BUNDLE.equals(type) ||
				IdentityNamespace.TYPE_FRAGMENT.equals(type);
	}
	
	public static boolean isFragment(Resource resource) {
		String type = ResourceHelper.getTypeAttribute(resource);
		return IdentityNamespace.TYPE_FRAGMENT.equals(type);
	}
	
	public static boolean isEffectiveResolve(Requirement requirement) {
		Map<String, String> directives = requirement.getDirectives();
		String value = directives.get(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE);
		return value == null || Namespace.EFFECTIVE_RESOLVE.equals(value);
	}
	
	public static boolean isMandatory(Requirement requirement) {
		Map<String, String> directives = requirement.getDirectives();
		String value = directives.get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE);
		return value == null || Namespace.RESOLUTION_MANDATORY.equals(value);
	}
	
	/*
	 * The Deployed-Content header in the deployment manifest is used to store
	 * information about explicitly installed resources and provisioned
	 * dependencies in addition to content for persistence purposes. This method
	 * returns true only if the resource is "true" content of the subsystem and,
	 * therefore, uses the Subsystem-Content header from the subsystem manifest.
	 */
	public static boolean isContent(BasicSubsystem subsystem, Resource resource) {
		SubsystemManifest subsystemManifest = subsystem.getSubsystemManifest();
		if (subsystemManifest == null)
			return false;
		SubsystemContentHeader subsystemContentHeader = subsystemManifest.getSubsystemContentHeader();
		if (subsystemContentHeader == null)
			return false;
		return subsystemContentHeader.contains(resource);
	}
	
	public static boolean isDependency(BasicSubsystem subsystem, Resource resource) {
		DeploymentManifest manifest = subsystem.getDeploymentManifest();
		if (manifest == null)
			return false;
		ProvisionResourceHeader header = manifest.getProvisionResourceHeader();
		if (header == null)
			return false;
		return header.contains(resource);
	}
	
	public static boolean isInstallableResource(Resource resource) {
		return !isSharedResource(resource);
	}
	
	public static boolean isRegionContextBundle(Resource resource) {
		return ResourceHelper.getSymbolicNameAttribute(resource).startsWith(
				RegionContextBundleHelper.SYMBOLICNAME_PREFIX);
	}
	
	public static boolean isSharedResource(Resource resource) {
		return resource instanceof BasicSubsystem || resource instanceof BundleRevision || resource instanceof BundleRevisionResource;
	}
	
	public static boolean isSubsystem(Resource resource) {
		String type = ResourceHelper.getTypeAttribute(resource);
		return SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION.equals(type) ||
				SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE.equals(type) ||
				SubsystemConstants.SUBSYSTEM_TYPE_FEATURE.equals(type);
	}
	
	public static boolean isProvisionDependenciesInstall(BasicSubsystem subsystem) {
		return subsystem.getSubsystemManifest().getSubsystemTypeHeader().getAriesProvisionDependenciesDirective().isInstall();
	}
}
