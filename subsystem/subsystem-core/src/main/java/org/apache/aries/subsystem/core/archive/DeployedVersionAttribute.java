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
import org.osgi.framework.VersionRange;
import org.osgi.service.subsystem.SubsystemConstants;

public class DeployedVersionAttribute extends AbstractAttribute {
	public static final String NAME = SubsystemConstants.DEPLOYED_VERSION_ATTRIBUTE;
	
	private final Version deployedVersion;
	
	public DeployedVersionAttribute(String value) {
		super(NAME, value);
		deployedVersion = Version.parseVersion(value);
	}
	
	@Override
	public StringBuilder appendToFilter(StringBuilder builder) {
		VersionRange versionRange = new VersionRange(VersionRange.LEFT_CLOSED, getVersion(), getVersion(), VersionRange.RIGHT_CLOSED);
		return builder.append(versionRange.toFilterString(VersionRangeAttribute.NAME));
	}

	public Version getDeployedVersion() {
		return deployedVersion;
	}
	
	public Version getVersion() {
		return deployedVersion;
	}

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((deployedVersion == null) ? 0 : deployedVersion.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        DeployedVersionAttribute other = (DeployedVersionAttribute) obj;
        if (deployedVersion == null) {
            if (other.deployedVersion != null)
                return false;
        } else
            if (!deployedVersion.equals(other.deployedVersion))
                return false;
        return true;
    }
	
}
