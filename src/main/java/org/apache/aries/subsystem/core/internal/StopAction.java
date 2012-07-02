package org.apache.aries.subsystem.core.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.apache.aries.subsystem.core.archive.DeploymentManifest;
import org.apache.aries.subsystem.core.archive.SubsystemContentHeader;
import org.osgi.framework.BundleException;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Resource;
import org.osgi.service.subsystem.Subsystem.State;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StopAction extends AbstractAction {
	private static final Logger logger = LoggerFactory.getLogger(StopAction.class);
	
	public StopAction(AriesSubsystem subsystem, boolean disableRootCheck, boolean explicit) {
		super(subsystem, disableRootCheck, explicit);
	}
	
	@Override
	public Object run() {
		checkRoot();
		if (explicit)
			subsystem.setAutostart(false);
		State state = subsystem.getState();
		if (EnumSet.of(State.INSTALLED, State.RESOLVED).contains(state))
			return null;
		else if (EnumSet.of(State.INSTALL_FAILED, State.UNINSTALLING, State.UNINSTALLED).contains(state))
			throw new IllegalStateException("Cannot stop from state " + state);
		else if (EnumSet.of(State.INSTALLING, State.RESOLVING, State.STARTING, State.STOPPING).contains(state)) {
			waitForStateChange();
			return new StopAction(subsystem, disableRootCheck, explicit).run();
		}
		subsystem.setState(State.STOPPING);
		List<Resource> resources = new ArrayList<Resource>(Activator.getInstance().getSubsystems().getResourcesReferencedBy(subsystem));
		SubsystemContentHeader header = subsystem.getSubsystemManifest().getSubsystemContentHeader();
		if (header != null) {
			Collections.sort(resources, new StartResourceComparator(subsystem.getSubsystemManifest().getSubsystemContentHeader()));
			Collections.reverse(resources);
		}
		for (Resource resource : resources) {
			// Don't stop the region context bundle.
			if (ResourceHelper.getSymbolicNameAttribute(resource).startsWith(RegionContextBundleHelper.SYMBOLICNAME_PREFIX))
				continue;
			try {
				stopResource(resource);
			} 
			catch (Exception e) {
				logger.error("An error occurred while stopping resource " + resource + " of subsystem " + subsystem, e);
			}
		}
		// TODO Can we automatically assume it actually is resolved?
		subsystem.setState(State.RESOLVED);
		try {
			subsystem.setDeploymentManifest(new DeploymentManifest(
					subsystem.getDeploymentManifest(),
					null,
					subsystem.isAutostart(),
					subsystem.getSubsystemId(),
					SubsystemIdentifier.getLastId(),
					subsystem.getLocation(),
					false,
					false));
		}
		catch (Exception e) {
			throw new SubsystemException(e);
		}
		return null;
	}
	
	private void stopBundleResource(Resource resource) throws BundleException {
		if (subsystem.isRoot())
			return;
		((BundleRevision)resource).getBundle().stop();
	}
	
	private void stopResource(Resource resource) throws BundleException, IOException {
		if (Utils.getActiveUseCount(resource) > 0)
			return;
		String type = ResourceHelper.getTypeAttribute(resource);
		if (SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION.equals(type)
				|| SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE.equals(type)
				|| SubsystemConstants.SUBSYSTEM_TYPE_FEATURE.equals(type))
			stopSubsystemResource(resource);
		else if (IdentityNamespace.TYPE_BUNDLE.equals(type))
			stopBundleResource(resource);
		else if (IdentityNamespace.TYPE_FRAGMENT.equals(type))
			return;
		else
			throw new SubsystemException("Unsupported resource type: " + type);
	}
	
	private void stopSubsystemResource(Resource resource) throws IOException {
		new StopAction((AriesSubsystem)resource, !((AriesSubsystem)resource).isRoot(), false).run();
	}
}
