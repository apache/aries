package org.apache.aries.subsystem.core.internal;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Resource;
import org.osgi.service.subsystem.SubsystemException;

public class BundleResourceUninstaller extends ResourceUninstaller {
	public BundleResourceUninstaller(Resource resource, AriesSubsystem subsystem) {
		super(resource, subsystem);
	}
	
	public void uninstall() {
		removeReference();
		removeConstituent();
		if (!isResourceUninstallable())
			return;
		if (isBundleUninstallable())
			uninstallBundle();
	}
	
	private Bundle getBundle() {
		return getBundleRevision().getBundle();
	}
	
	private BundleRevision getBundleRevision() {
		return (BundleRevision)resource;
	}
	
	private boolean isBundleUninstallable() {
		return getBundle().getState() != Bundle.UNINSTALLED;
	}
	
	private void uninstallBundle() {
		ThreadLocalSubsystem.set(provisionTo);
		try {
			getBundle().uninstall();
		}
		catch (BundleException e) {
			throw new SubsystemException(e);
		}
	}
}
