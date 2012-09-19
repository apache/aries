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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.filesystem.IDirectory;
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
	
	public IDirectory open() throws IOException, URISyntaxException {
		URL url = uri == null ? new URL(value) : uri.getURL();
		if ("file".equals(url.getProtocol()))
			return FileSystem.getFSRoot(new File(url.toURI()));
		return FileSystem.getFSRoot(url.openStream());
	}
}