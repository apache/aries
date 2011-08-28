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
package org.apache.aries.subsystem.core.archive;

import java.util.jar.Attributes;

public class ManifestVersionHeader extends VersionHeader {
	public static final String DEFAULT_VALUE = "1.0";
	public static final String NAME = Attributes.Name.MANIFEST_VERSION.toString();
	
	public static final ManifestVersionHeader DEFAULT = new ManifestVersionHeader(DEFAULT_VALUE);
	
	public ManifestVersionHeader() {
		this(DEFAULT_VALUE);
	}

	public ManifestVersionHeader(String value) {
		super(NAME, value);
	}
}
