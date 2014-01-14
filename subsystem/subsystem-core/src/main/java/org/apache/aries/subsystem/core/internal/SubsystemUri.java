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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.aries.subsystem.core.archive.SubsystemSymbolicNameHeader;
import org.apache.aries.subsystem.core.archive.SubsystemVersionHeader;
import org.osgi.framework.Version;
import org.osgi.service.subsystem.SubsystemConstants;

public class SubsystemUri {
	private static final String REGEXP = "([^=]*)=([^&]*)&?";
	private static final Pattern PATTERN = Pattern.compile(REGEXP);

	private final String symbolicName;
	private final URL url;
	private final Version version;

	public SubsystemUri(String location) throws URISyntaxException, MalformedURLException {
		if (!location.startsWith("subsystem://"))
			throw new IllegalArgumentException(location);
		URI uri = new URI(location);
		if (uri.getAuthority() != null)
			url = new URL(uri.getAuthority());
		else
			url = null;
		Matcher matcher = PATTERN.matcher(uri.getQuery());
		String symbolicName = null;
		Version version = Version.emptyVersion;
		while (matcher.find()) {
			String name = matcher.group(1);
			if (SubsystemSymbolicNameHeader.NAME.equals(name)) {
			    int idx = location.indexOf("!/");
                if (idx > 0) {
			        symbolicName = location.substring(idx + 2);
			        int idx2 = symbolicName.indexOf('@');
			        if (idx2 > 0) {
			            symbolicName = symbolicName.substring(0, idx2);
			        }
			    } else {
			        symbolicName = new SubsystemSymbolicNameHeader(matcher.group(2)).getValue();
			    }
			} else if (SubsystemVersionHeader.NAME.equals(name)) {
			    String group = matcher.group(2);
			    if (group.contains("!/") && group.contains("@")) {
			        int idx = group.lastIndexOf('@');
			        version = Version.parseVersion(group.substring(idx + 1));
			    } else {
			        version = Version.parseVersion(group);
			    }
			} else
				throw new IllegalArgumentException("Unsupported subsystem URI parameter: " + name);
		}
		this.symbolicName = symbolicName;
		this.version = version;
	}

	public SubsystemUri(String symbolicName, Version version, URL url) {
		// TODO symbolicName should conform to OSGi grammar.
		if (symbolicName == null || symbolicName.length() == 0)
			throw new IllegalArgumentException(
					"Missing required parameter: symbolicName");
		this.symbolicName = symbolicName;
		this.version = version;
		this.url = url;
	}

	public String getSymbolicName() {
		return symbolicName;
	}

	public URL getURL() {
		return url;
	}

	public Version getVersion() {
		return version;
	}

	public String toString() {
		StringBuilder builder = new StringBuilder("subsystem://");
		if (url != null) {
			try {
				builder.append(URLEncoder.encode(url.toString(), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				builder.append(URLEncoder.encode(url.toString()));
			}
		}
		builder.append('?').append(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME)
				.append('=').append(symbolicName);
		if (version != null)
			builder.append('&').append(SubsystemConstants.SUBSYSTEM_VERSION)
					.append('=').append(version);
		return builder.toString();
	}
}
