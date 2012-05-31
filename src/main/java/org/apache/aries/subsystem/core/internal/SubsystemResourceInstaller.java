package org.apache.aries.subsystem.core.internal;

import java.security.AccessController;

import org.osgi.resource.Resource;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Participant;
import org.osgi.service.repository.RepositoryContent;

public class SubsystemResourceInstaller extends ResourceInstaller {
	public SubsystemResourceInstaller(Coordination coordination, Resource resource, AriesSubsystem subsystem, boolean transitive) {
		super(coordination, resource, subsystem, transitive);
	}
	
	public void install() throws Exception {
		final AriesSubsystem subsystem;
		if (resource instanceof RepositoryContent) {
			AccessController.doPrivileged(new InstallAction(getLocation(), ((RepositoryContent)resource).getContent(), provisionTo, null, coordination, true));
			return;
		}
		else if (resource instanceof AriesSubsystem)
			subsystem = (AriesSubsystem)resource;
		else if (resource instanceof RawSubsystemResource)
			subsystem = new AriesSubsystem(new SubsystemResource((RawSubsystemResource)resource, provisionTo), provisionTo);
		else
			subsystem = new AriesSubsystem((SubsystemResource)resource, provisionTo);
		addChild(subsystem);
		addConstituent(subsystem);
		addReference(subsystem);
		subsystem.install(coordination, provisionTo);
	}
	
	private void addChild(final AriesSubsystem child) {
		Activator.getInstance().getSubsystems().addChild(subsystem, child);
		coordination.addParticipant(new Participant() {
			@Override
			public void ended(Coordination arg0) throws Exception {
				// Nothing
			}

			@Override
			public void failed(Coordination arg0) throws Exception {
				Activator.getInstance().getSubsystems().removeChild(subsystem, child);
			}
		});
	}
}
