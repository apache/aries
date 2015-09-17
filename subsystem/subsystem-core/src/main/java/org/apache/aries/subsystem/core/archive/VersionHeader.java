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

public abstract class VersionHeader extends AbstractHeader {
    

    protected final Version version;
	
	public VersionHeader(String name, String value) {
		this(name, Version.parseVersion(value));
	}
	
	public VersionHeader(String name, Version value) {
		super(name, value.toString());
		version = value;
	}
	
	public Version getVersion() {
		return version;
	}

	@Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((version == null) ? 0 : version.hashCode());
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
        VersionHeader other = (VersionHeader) obj;
        if (version == null) {
            if (other.version != null)
                return false;
        } else
            if (!version.equals(other.version))
                return false;
        return true;
    }
}
