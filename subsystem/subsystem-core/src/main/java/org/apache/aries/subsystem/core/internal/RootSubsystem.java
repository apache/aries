package org.apache.aries.subsystem.core.internal;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.resource.Resource;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

public class RootSubsystem implements Subsystem {
	private static final String SYMBOLIC_NAME = "org.osgi.service.subsystem.root";
	private static final String TYPE = SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION;
	private static final Version VERSION = Version.parseVersion("1.0.0");
	
	private static final String LOCATION = "subsystem://?"
			+ SubsystemConstants.SUBSYSTEM_SYMBOLICNAME + '='
			+ SYMBOLIC_NAME + '&' + SubsystemConstants.SUBSYSTEM_VERSION
			+ '=' + VERSION;
	
	@Override
	public BundleContext getBundleContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Subsystem> getChildren() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Resource> getConstituents() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getLocation() {
		return LOCATION;
	}

	@Override
	public Collection<Subsystem> getParents() {
		return Collections.emptyList();
	}

	@Override
	public State getState() {
		return Subsystem.State.ACTIVE;
	}

	@Override
	public Map<String, String> getSubsystemHeaders(Locale locale) {
		return Collections.emptyMap();
	}

	@Override
	public long getSubsystemId() {
		return 0;
	}

	@Override
	public String getSymbolicName() {
		return SYMBOLIC_NAME;
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public Version getVersion() {
		return VERSION;
	}
	
	public void install() throws SubsystemException {
		
	}

	@Override
	public Subsystem install(String location) throws SubsystemException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Subsystem install(String location, InputStream content) throws SubsystemException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void start() throws SubsystemException {
		// TODO Auto-generated method stub

	}

	@Override
	public void stop() throws SubsystemException {
		
	}

	@Override
	public void uninstall() throws SubsystemException {
		// TODO Auto-generated method stub

	}
}
