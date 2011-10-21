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

import org.apache.aries.util.VersionRange;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class VersionRangeAttribute extends AbstractAttribute {
	public static String toFilterString(VersionRange range) {
		String version = Constants.VERSION_ATTRIBUTE;
		Version min = range.getMinimumVersion();
		Version max = range.getMaximumVersion();
		StringBuilder sb = new StringBuilder();
		if (max != null)
			sb.append("(&");
		if (range.isMinimumExclusive())
			sb.append("(!(").append(version).append("<=").append(min).append("))");
		else
			sb.append('(').append(version).append(">=").append(min).append(')');
		if (max != null) {
			if (range.isMaximumExclusive())
				sb.append("(!(").append(version).append(">=").append(range.getMaximumVersion()).append("))");
			else
				sb.append('(').append(version).append("<=").append(max).append(')');
			sb.append(')');
		}
		return sb.toString();
	}
	
	private final VersionRange range;
	
	public VersionRangeAttribute() {
		super(Constants.VERSION_ATTRIBUTE, Version.emptyVersion.toString());
		range = new VersionRange(getValue());
	}
			
	public VersionRangeAttribute(String value) {
		super(Constants.VERSION_ATTRIBUTE, value);
		range = new VersionRange(getValue());
	}
	
	public StringBuilder appendToFilter(StringBuilder builder) {
		return builder.append(toFilterString(range));
	}

	public VersionRange getVersionRange() {
		return range;
	}
}
