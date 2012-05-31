package org.apache.aries.subsystem.core.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.osgi.framework.Version;

public class Location {
	private final SubsystemUri uri;
	private final String value;
	
	public Location(String location) throws MalformedURLException, URISyntaxException {
		value = location;
		if (location.startsWith("subsystem://"))
			uri = new SubsystemUri(location);
		else
			uri = null;
	}
	
	public String getSymbolicName() {
		return uri == null ? null : uri.getSymbolicName();
	}
	
	public String getValue() {
		return value;
	}
	
	public Version getVersion() {
		return uri == null ? null : uri.getVersion();
	}
	
	public InputStream open() throws IOException {
		return uri == null ? new URL(value).openStream() : uri.getURL().openStream();
	}
}