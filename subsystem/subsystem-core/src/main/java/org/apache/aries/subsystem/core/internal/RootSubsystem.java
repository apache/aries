package org.apache.aries.subsystem.core.internal;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import org.apache.aries.subsystem.Subsystem;
import org.apache.aries.subsystem.SubsystemException;
import org.eclipse.equinox.region.Region;
import org.osgi.framework.Version;

public class RootSubsystem extends SubsystemImpl {
	public RootSubsystem(Region region) {
		super(region, Collections.EMPTY_MAP, null, "root");
	}

	public Map<String, String> getHeaders() {
		return Collections.emptyMap();
	}

	public Map<String, String> getHeaders(String locale) {
		return Collections.emptyMap();
	}

	public Subsystem getParent() {
		return null;
	}

	public State getState() {
		return Subsystem.State.ACTIVE;
	}

	public long getSubsystemId() {
		return 0;
	}

	public String getSymbolicName() {
		return "org.osgi.service.subsystem.root";
	}

	public Version getVersion() {
		return Version.emptyVersion;
	}

	public void start() throws SubsystemException {
		throw new SubsystemException("The root subsystem may not be started");
	}

	public void stop() throws SubsystemException {
		throw new SubsystemException("The root subsystem may not be stopped");
	}

	public void uninstall() throws SubsystemException {
		throw new SubsystemException("The root subsystem may not be uninstalled");
	}

	public void update() throws SubsystemException {
		update(null);
	}

	public void update(InputStream content) throws SubsystemException {
		throw new SubsystemException("The root subsystem may not be updated");
	}
}
