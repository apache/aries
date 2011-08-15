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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.aries.subsystem.core.archive.Grammar;
import org.osgi.framework.Version;

public class SubsystemUri {
	private final String symbolicName;
	private final String type;
	private final URL url;
	private final Version version;
	
	public SubsystemUri(String location) throws URISyntaxException, MalformedURLException {
		URI uri = new URI(location);
		String scheme = uri.getScheme();
		// TODO Add to constants.
		if (!scheme.matches("subsystem(?:.(?:eba|cba|fba))?"))
			throw new IllegalArgumentException(location);
		int i = scheme.indexOf('.');
		if (i != -1)
			type = scheme.substring(i);
		else
			// TODO Add to constants.
			type = "eba";
		symbolicName = uri.getQuery();
		if (!symbolicName.matches(Grammar.SYMBOLICNAME))
			throw new IllegalArgumentException(location);
		url = new URL(uri.getAuthority());
		String fragment = uri.getFragment();
		if (fragment != null)
			version = Version.parseVersion(uri.getFragment());
		else
			version = null;
	}
	
	public String getSymbolicName() {
		return symbolicName;
	}
	
	public String getType() {
		return type;
	}
	
	public URL getURL() {
		return url;
	}
	
	public Version getVersion() {
		return version;
	}
}
