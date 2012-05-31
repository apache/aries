package org.apache.aries.subsystem.core.internal;

import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.osgi.resource.Resource;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.CoordinationException;
import org.osgi.service.subsystem.SubsystemException;
import org.osgi.service.subsystem.Subsystem.State;

public class StartAction implements PrivilegedAction<Object> {
	private final AriesSubsystem subsystem;
	
	public StartAction(AriesSubsystem subsystem) {
		this.subsystem = subsystem;
	}
	
	@Override
	public Object run() {
		State state = subsystem.getState();
		if (state == State.UNINSTALLING || state == State.UNINSTALLED)
			throw new SubsystemException("Cannot stop from state " + state);
		if (state == State.INSTALLING || state == State.RESOLVING || state == State.STOPPING) {
			subsystem.waitForStateChange();
			subsystem.start();
			return null;
		}
		// TODO Should we wait on STARTING to see if the outcome is ACTIVE?
		if (state == State.STARTING || state == State.ACTIVE)
			return null;
		subsystem.resolve();
		subsystem.setState(State.STARTING);
		subsystem.autostart = true;
		// TODO Need to hold a lock here to guarantee that another start
		// operation can't occur when the state goes to RESOLVED.
		// Start the subsystem.
		Coordination coordination = Activator.getInstance()
				.getCoordinator()
				.create(subsystem.getSymbolicName() + '-' + subsystem.getSubsystemId(), 0);
		try {
			List<Resource> resources = new ArrayList<Resource>(Activator.getInstance().getSubsystems().getResourcesReferencedBy(subsystem));
			if (subsystem.resource != null)
				Collections.sort(resources, new StartResourceComparator(subsystem.resource.getSubsystemManifest().getSubsystemContentHeader()));
			for (Resource resource : resources)
				subsystem.startResource(resource, coordination);
			subsystem.setState(State.ACTIVE);
		} catch (Throwable t) {
			coordination.fail(t);
			// TODO Need to reinstate complete isolation by disconnecting the
			// region and transition to INSTALLED.
		} finally {
			try {
				coordination.end();
			} catch (CoordinationException e) {
				subsystem.setState(State.RESOLVED);
				Throwable t = e.getCause();
				if (t instanceof SubsystemException)
					throw (SubsystemException)t;
				throw new SubsystemException(t);
			}
		}
		return null;
	}
}
