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

import org.osgi.framework.Version;

public class DeployedVersionAttribute extends AbstractAttribute {
	public static final String NAME = "deployed-version";
	
	public static DeployedVersionAttribute newInstance(String value) {
		if (value == null) {
			return new DeployedVersionAttribute();
		}
		return new DeployedVersionAttribute();
	}
	
	private final Version deployedVersion;
	
	public DeployedVersionAttribute() {
		super(NAME, Version.emptyVersion.toString());
		deployedVersion = Version.emptyVersion;
	}
	
	public DeployedVersionAttribute(String value) {
		super(NAME, value);
		deployedVersion = Version.parseVersion(value);
	}

	public Version getDeployedVersion() {
		return deployedVersion;
	}
}
