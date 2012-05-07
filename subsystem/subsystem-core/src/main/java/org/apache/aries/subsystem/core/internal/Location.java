package org.apache.aries.subsystem.core.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.osgi.framework.Version;

public class Location {
	private final String symbolicName;
	private final URL url;
	private final String value;
	private final Version version;
	
	public Location(String location) throws MalformedURLException, URISyntaxException {
		value = location;
		SubsystemUri uri = null;
		if (location.startsWith("subsystem://"))
			uri = new SubsystemUri(location);
		symbolicName = uri == null ? null : uri.getSymbolicName();
		url = uri == null ? null : uri.getURL();
		version = uri == null ? null : uri.getVersion();
	}
	
	public String getSymbolicName() {
		return symbolicName;
	}
	
	public String getValue() {
		return value;
	}
	
	public Version getVersion() {
		return version;
	}
	
	public InputStream open() throws IOException {
		return url == null ? new URL(value).openStream() : url.openStream();
	}
}