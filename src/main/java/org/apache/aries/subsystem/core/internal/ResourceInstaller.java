package org.apache.aries.subsystem.core.internal;

import org.osgi.resource.Resource;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Participant;

public abstract class ResourceInstaller {
	protected final Coordination coordination;
	protected final AriesSubsystem provisionTo;
	protected final Resource resource;
	protected final AriesSubsystem subsystem;
	
	public ResourceInstaller(Coordination coordination, Resource resource, AriesSubsystem subsystem, boolean transitive) {
		this.coordination = coordination;
		this.resource = resource;
		this.subsystem = subsystem;
		if (transitive)
			provisionTo = Utils.findFirstSubsystemAcceptingDependenciesStartingFrom(subsystem);
		else
			provisionTo = subsystem;
	}
	
	protected void addConstituent(final Resource resource) {
		Activator.getInstance().getSubsystems().addConstituent(provisionTo, resource);
		coordination.addParticipant(new Participant() {
			@Override
			public void ended(Coordination arg0) throws Exception {
				// Nothing
			}

			@Override
			public void failed(Coordination arg0) throws Exception {
				Activator.getInstance().getSubsystems().removeConstituent(provisionTo, resource);
			}
		});
	}
	
	protected void addReference(final Resource resource) {
		Activator.getInstance().getSubsystems().addReference(subsystem, resource);
		coordination.addParticipant(new Participant() {
			@Override
			public void ended(Coordination arg0) throws Exception {
				// Nothing
			}

			@Override
			public void failed(Coordination arg0) throws Exception {
				Activator.getInstance().getSubsystems().removeReference(subsystem, resource);
			}
		});
	}
	
	protected String getLocation() {
		return provisionTo.getSubsystemId() + "@" + provisionTo.getSymbolicName() + "@" + ResourceHelper.getSymbolicNameAttribute(resource);
	}
	
	protected void removeConstituent() {
		Activator.getInstance().getSubsystems().removeConstituent(provisionTo, resource);
	}
	
	protected void removeReference() {
		Activator.getInstance().getSubsystems().removeReference(subsystem, resource);
	}
}
