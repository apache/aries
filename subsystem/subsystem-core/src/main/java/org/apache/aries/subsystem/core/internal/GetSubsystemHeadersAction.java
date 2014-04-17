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
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import org.apache.aries.subsystem.core.archive.Header;

public class GetSubsystemHeadersAction implements PrivilegedAction<Map<String, String>> {
	private final Locale locale;
	private final BasicSubsystem subsystem;
	
	public GetSubsystemHeadersAction(BasicSubsystem subsystem, Locale locale) {
		this.subsystem = subsystem;
		this.locale = locale;
	}
	
	@Override
	public Map<String, String> run() {
		Map<String, Header<?>> headers = subsystem.getSubsystemManifest().getHeaders();
		Map<String, String> result = new HashMap<String, String>(headers.size());
		for (Entry<String, Header<?>> entry: headers.entrySet()) {
			Header<?> value = entry.getValue();
			result.put(entry.getKey(), translate(value.getValue()));
		}
		return result;
	}

	private String translate(String value) {
		if (locale == null || value == null || !value.startsWith("%"))
			return value;
		String localizationStr = subsystem.getSubsystemManifest().getSubsystemLocalizationHeader().getValue();
		File rootDir;
		File localizationFile;
		try {
			rootDir = subsystem.getDirectory().getCanonicalFile();
			localizationFile = new File(rootDir, localizationStr).getCanonicalFile();
		}
		catch (IOException e) {
			// TODO Log this. Particularly a problem if rootDir throws an
			// exception as corruption has occurred. May want to let that
			// propagate as a runtime exception.
			return value;
		}
		URI rootUri = rootDir.toURI();
		// The last segment of the Subsystem-Localization header value is the
		// base file name. The directory is its parent.
		URI localizationUri = localizationFile.getParentFile().toURI();
		if (rootUri.relativize(localizationUri).equals(localizationUri))
			// TODO Log this. The value of the Subsystem-Localization header
			// is not relative to the subsystem root directory.
			return value;
		URL localizationUrl;
		try {
			localizationUrl = localizationUri.toURL();
		}
		catch (MalformedURLException e) {
			// TODO Should never happen but log it anyway.
			return value;
		}
		URLClassLoader classLoader = new URLClassLoader(new URL[]{localizationUrl});
		try {
			ResourceBundle rb = ResourceBundle.getBundle(localizationFile.getName(), locale, classLoader);
			return rb.getString(value.substring(1));
		}
		catch (Exception e) {
			return value;
		}
	}
}
