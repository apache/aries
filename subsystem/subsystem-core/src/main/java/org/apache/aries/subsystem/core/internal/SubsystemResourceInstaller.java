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

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.aries.util.filesystem.FileSystem;
import org.osgi.resource.Resource;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Participant;
import org.osgi.service.subsystem.Subsystem.State;

public class SubsystemResourceInstaller extends ResourceInstaller {
	public SubsystemResourceInstaller(Coordination coordination, Resource resource, BasicSubsystem subsystem) {
		super(coordination, resource, subsystem);
	}

	public Resource install() throws Exception {
		if (resource instanceof BasicSubsystem)
			return installAriesSubsystem((BasicSubsystem)resource);
		else if (resource instanceof RawSubsystemResource)
			return installRawSubsystemResource((RawSubsystemResource)resource);
		else if (resource instanceof SubsystemResource)
			return installSubsystemResource((SubsystemResource)resource);
		else {
			return installRepositoryContent(resource);
		}
	}

	private void addChild(final BasicSubsystem child) {
		// provisionTo will be null if the resource is an already installed
		// dependency.
		if (provisionTo == null)
			return;
		// Don't let a resource become a child of itself.
		if (resource.equals(provisionTo))
			return;
		Activator.getInstance().getSubsystems().addChild(provisionTo, child, !isDependency());
	}

	private void addSubsystem(final BasicSubsystem subsystem) {
		Activator.getInstance().getSubsystems().addSubsystem(subsystem);
	}

	private BasicSubsystem installAriesSubsystem(final BasicSubsystem subsystem) throws Exception {
		addChild(subsystem);
		addReference(subsystem);
		addConstituent(subsystem);
		addSubsystem(subsystem);
		installRegionContextBundle(subsystem);
		// This will emit the initial service event for INSTALLING subsystems.
		// The first event for RESOLVED (i.e. persisted) subsystems is emitted later.
		if (State.INSTALLING.equals(subsystem.getState())) {
			Activator.getInstance().getSubsystemServiceRegistrar().register(subsystem, this.subsystem);
			coordination.addParticipant(new Participant() {
				@Override
				public void ended(Coordination coordination) throws Exception {
					// Nothing.
				}

				@Override
				public void failed(Coordination coordination) throws Exception {
					subsystem.setState(State.INSTALL_FAILED);
					subsystem.uninstall();
				}
			});
		}
		Comparator<Resource> comparator = new InstallResourceComparator();
		// Install dependencies first if appropriate...
		if (!subsystem.isRoot() && Utils.isProvisionDependenciesInstall(subsystem)) {
			new InstallDependencies().install(subsystem, this.subsystem, coordination);
		}
		// ...followed by content.
		// Simulate installation of shared content so that necessary relationships are established.
		for (Resource content : subsystem.getResource().getSharedContent()) {
			ResourceInstaller.newInstance(coordination, content, subsystem).install();
		}
		// Now take care of the installable content.
		if (State.INSTALLING.equals(subsystem.getState())) {
			List<Resource> installableContent = new ArrayList<Resource>(subsystem.getResource().getInstallableContent());
			Collections.sort(installableContent, comparator);
			for (Resource content : installableContent)
				ResourceInstaller.newInstance(coordination, content, subsystem).install();
		}
		// Only brand new subsystems should have acquired the INSTALLING state,
		// in which case an INSTALLED event must be propagated.
		if (State.INSTALLING.equals(subsystem.getState()) && 
				Utils.isProvisionDependenciesInstall(subsystem)) {
			subsystem.setState(State.INSTALLED);
		}
		else {
			// This is a persisted subsystem in the RESOLVED state. Emit the first service event.
			Activator.getInstance().getSubsystemServiceRegistrar().register(subsystem, this.subsystem);
		}
		return subsystem;
	}

	private BasicSubsystem installRawSubsystemResource(RawSubsystemResource resource) throws Exception {
		SubsystemResource subsystemResource = new SubsystemResource(resource, provisionTo, coordination);
		return installSubsystemResource(subsystemResource);
	}

	private void installRegionContextBundle(final BasicSubsystem subsystem) throws Exception {
		if (!subsystem.isScoped())
			return;
		RegionContextBundleHelper.installRegionContextBundle(subsystem, coordination);
	}

	private BasicSubsystem installRepositoryContent(Resource resource) throws Exception {
		Method method = resource.getClass().getMethod("getContent");
		InputStream is = (InputStream)method.invoke(resource);
		RawSubsystemResource rawSubsystemResource = new RawSubsystemResource(getLocation(), FileSystem.getFSRoot(is), subsystem);
		return installRawSubsystemResource(rawSubsystemResource);
	}

	private BasicSubsystem installSubsystemResource(SubsystemResource resource) throws Exception {
		BasicSubsystem subsystem = new BasicSubsystem(resource);
		installAriesSubsystem(subsystem);
		return subsystem;
	}
}
