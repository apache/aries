package org.apache.aries.subsystem.core.internal;

import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Resource;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.CoordinationException;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {
	private static final Logger logger = LoggerFactory.getLogger(Utils.class);
	
	public static Coordination createCoordination() {
		return Activator.getInstance().getCoordinator().begin(AriesSubsystem.ROOT_SYMBOLIC_NAME + "-0", 0);
	}
	
	public static Coordination createCoordination(AriesSubsystem subsystem) {
		return Activator.getInstance().getCoordinator().begin(subsystem.getSymbolicName() + '-' + subsystem.getSubsystemId(), 0);
	}
	
	public static AriesSubsystem findFirstSubsystemAcceptingDependenciesStartingFrom(AriesSubsystem subsystem) {
		// The following loop is guaranteed to end once the root subsystem has
		// been reached.
		while (!isAcceptDependencies(subsystem))
			subsystem = (AriesSubsystem)subsystem.getParents().iterator().next();
		return subsystem;
	}
	
	public static AriesSubsystem findScopedSubsystemInRegion(AriesSubsystem subsystem) {
		while (!subsystem.isScoped())
			subsystem = (AriesSubsystem)subsystem.getParents().iterator().next();
		return subsystem;
	}
	
	public static int getActiveUseCount(Resource resource) {
		int result = 0;
		for (AriesSubsystem subsystem : Activator.getInstance().getSubsystems().getSubsystemsReferencing(resource))
			if (Subsystem.State.ACTIVE.equals(subsystem.getState()))
				result++;
		return result;
	}
	
	public static long getId(Resource resource) {
		if (resource instanceof AriesSubsystem)
			return ((AriesSubsystem)resource).getSubsystemId();
		if (resource instanceof BundleRevision)
			return ((BundleRevision)resource).getBundle().getBundleId();
		return -1;
	}
	
	public static void installResource(Resource resource, AriesSubsystem subsystem) {
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
	
	public static boolean isAcceptDependencies(AriesSubsystem subsystem) {
		return subsystem.getSubsystemManifest().getSubsystemTypeHeader().getProvisionPolicyDirective().isAcceptDependencies();
	}
	
	public static boolean isBundle(Resource resource) {
		String type = ResourceHelper.getTypeAttribute(resource);
		return IdentityNamespace.TYPE_BUNDLE.equals(type) ||
				IdentityNamespace.TYPE_FRAGMENT.equals(type);
	}
	
	public static boolean isInstallableResource(Resource resource) {
		return !isSharedResource(resource);
	}
	
	public static boolean isSharedResource(Resource resource) {
		return resource instanceof AriesSubsystem || resource instanceof BundleRevision;
	}
	
	public static boolean isSubsystem(Resource resource) {
		String type = ResourceHelper.getTypeAttribute(resource);
		return SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION.equals(type) ||
				SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE.equals(type) ||
				SubsystemConstants.SUBSYSTEM_TYPE_FEATURE.equals(type);
	}
}
