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

import org.apache.aries.subsystem.core.internal.BundleResourceInstaller.BundleConstituent;
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
		// Always remove the bundle as a constituent of the subsystem being
		// acted upon. The bundle may or may not actually be a constituent.
		// This covers the case of unscoped subsystems with shared content
		// where the resource may not be uninstallable.
		removeConstituent(subsystem, new BundleConstituent(null, (BundleRevision)resource));
		if (!isResourceUninstallable())
			return;
		// If the resource is uninstallable, remove it from the "provisioned to"
		// subsystem in case it was a dependency. The "provisioned to" subsystem
		// may be the same subsystem as the one being acted upon. This covers
		// the case where a dependency of the subsystem being acted upon was 
		// provisioned to another subsystem but is not content of the other
		// subsystem.
		removeConstituent(provisionTo, new BundleConstituent(null, (BundleRevision)resource));
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
