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

import org.osgi.service.subsystem.SubsystemConstants;

public class SubsystemLocalizationHeader extends AbstractHeader {
	public static final String DEFAULT_VALUE = "OSGI-INF/l10n/subsystem";
	public static final String NAME = SubsystemConstants.SUBSYSTEM_LOCALIZATION;
	
	public static final SubsystemLocalizationHeader DEFAULT = new SubsystemLocalizationHeader();
	
	private final String baseFileName;
	private final String directoryName;
	
	public SubsystemLocalizationHeader() {
		this(DEFAULT_VALUE);
	}

	public SubsystemLocalizationHeader(String localization) {
		super(NAME, localization);
		int index = value.lastIndexOf('/');
		baseFileName = index == -1 ? value : value.substring(index + 1);
		directoryName = index == -1 ? null : value.substring(0, index + 1);
	}
	
	public String getBaseFileName() {
		return baseFileName;
	}
	
	public String getDirectoryName() {
		return directoryName;
	}

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        SubsystemLocalizationHeader other = (SubsystemLocalizationHeader) obj;
        if (baseFileName == null) {
            if (other.baseFileName != null)
                return false;
        } else
            if (!baseFileName.equals(other.baseFileName))
                return false;
        if (directoryName == null) {
            if (other.directoryName != null)
                return false;
        } else
            if (!directoryName.equals(other.directoryName))
                return false;
        return true;
    }
	
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((baseFileName == null) ? 0 : baseFileName.hashCode());
        result = prime * result
                + ((directoryName == null) ? 0 : directoryName.hashCode());
        return result;
    }
	
}
