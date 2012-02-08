package org.apache.aries.subsystem.core.internal;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.framework.resource.Resource;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemException;

public class RootSubsystem implements Subsystem {

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
	public Map<String, String> getSubsystemHeaders(Locale locale) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getLocation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Subsystem> getParents() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Resource> getConstituents() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public State getState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getSubsystemId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getSymbolicName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Version getVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Subsystem install(String location) throws SubsystemException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Subsystem install(String location, InputStream content)
			throws SubsystemException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void start() throws SubsystemException {
		// TODO Auto-generated method stub

	}

	@Override
	public void stop() throws SubsystemException {
		// TODO Auto-generated method stub

	}

	@Override
	public void uninstall() throws SubsystemException {
		// TODO Auto-generated method stub

	}

}
