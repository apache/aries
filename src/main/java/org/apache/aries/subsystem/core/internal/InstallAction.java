package org.apache.aries.subsystem.core.internal;

import java.io.InputStream;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.apache.aries.util.io.IOUtils;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.CoordinationException;
import org.osgi.service.coordinator.Participant;
import org.osgi.service.subsystem.SubsystemException;

public class InstallAction implements PrivilegedAction<AriesSubsystem> {
	private final InputStream content;
	private final AccessControlContext context;
	private final Coordination coordination;
	private final boolean embedded;
	private final String location;
	private final AriesSubsystem parent;
	
	public InstallAction(String location, InputStream content, AriesSubsystem parent, AccessControlContext context) {
		this(location, content, parent, context, null, false);
	}
	
	public InstallAction(String location, InputStream content, AriesSubsystem parent, AccessControlContext context, Coordination coordination, boolean embedded) {
		this.location = location;
		this.content = content;
		this.parent = parent;
		this.context = context;
		this.coordination = coordination;
		this.embedded = embedded;
	}
	
	@Override
	public AriesSubsystem run() {
		AriesSubsystem result = null;
		Coordination coordination = this.coordination;
		if (coordination == null)
			coordination = Activator.getInstance().getCoordinator().create(parent.getSymbolicName() + '-' + parent.getSubsystemId(), 0);
		try {
			TargetRegion region = new TargetRegion(parent);
			final SubsystemResource ssr = new SubsystemResource(location, content, parent);
			coordination.addParticipant(new Participant() {
				@Override
				public void ended(Coordination c) throws Exception {
					// Nothing
				}

				@Override
				public void failed(Coordination c) throws Exception {
					IOUtils.deleteRecursive(ssr.getDirectory());
				}
			});
			result = Activator.getInstance().getSubsystems().getSubsystemByLocation(location);
			if (result != null) {
				checkLifecyclePermission(result);
				if (!region.contains(result))
					throw new SubsystemException("Location already exists but existing subsystem is not part of target region: " + location);
				if (!(result.getSymbolicName().equals(ssr.getSubsystemManifest().getSubsystemSymbolicNameHeader().getSymbolicName())
						&& result.getVersion().equals(ssr.getSubsystemManifest().getSubsystemVersionHeader().getVersion())
						&& result.getType().equals(ssr.getSubsystemManifest().getSubsystemTypeHeader().getType())))
					throw new SubsystemException("Location already exists but symbolic name, version, and type are not the same: " + location);
				parent.installResource(result);
				return result;
			}
			result = (AriesSubsystem)region.find(
					ssr.getSubsystemManifest().getSubsystemSymbolicNameHeader().getSymbolicName(), 
					ssr.getSubsystemManifest().getSubsystemVersionHeader().getVersion());
			if (result != null) {
				checkLifecyclePermission(result);
				if (!result.getType().equals(ssr.getSubsystemManifest().getSubsystemTypeHeader().getType()))
					throw new SubsystemException("Subsystem already exists in target region but has a different type: " + location);
				parent.installResource(result);
				return result;
			}
			result = new AriesSubsystem(ssr, parent);
			checkLifecyclePermission(result);
			parent.installResource(result, coordination, false);
		}
		catch (Throwable t) {
			coordination.fail(t);
		}
		finally {
			try {
				coordination.end();
			}
			catch (CoordinationException e) {
				Throwable t = e.getCause();
				if (t instanceof SubsystemException)
					throw (SubsystemException)t;
				if (t instanceof SecurityException)
					throw (SecurityException)t;
				throw new SubsystemException(t);
			}
		}
		return result;
	}

	private void checkLifecyclePermission(final AriesSubsystem subsystem) {
		if (embedded)
			return;
		AccessController.doPrivileged(new PrivilegedAction<Object>() {
			@Override
			public Object run() {
				SecurityManager.checkLifecyclePermission(subsystem);
				return null;
			}
		},
		context);
	}
}
