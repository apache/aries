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

import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

public class VersionRangeAttribute extends AbstractAttribute {
	public static final String NAME_BUNDLEVERSION = Constants.BUNDLE_VERSION_ATTRIBUTE;
	public static final String NAME_VERSION = Constants.VERSION_ATTRIBUTE;
	
	public static final VersionRangeAttribute DEFAULT_BUNDLEVERSION = new VersionRangeAttribute(
			NAME_BUNDLEVERSION, new VersionRange(Version.emptyVersion.toString()));
	public static final VersionRangeAttribute DEFAULT_VERSION = new VersionRangeAttribute();
	
	private final VersionRange range;
	
	public VersionRangeAttribute() {
		this(Version.emptyVersion.toString());
	}
			
	public VersionRangeAttribute(String value) {
		this(new VersionRange(value));
	}
	
	public VersionRangeAttribute(VersionRange range) {
		this(Constants.VERSION_ATTRIBUTE, range);
	}
	
	public VersionRangeAttribute(String name, VersionRange range) {
		super(name, range.toString());
		this.range = range;
	}
	
	@Override
	public StringBuilder appendToFilter(StringBuilder builder) {
		return builder.append(range.toFilterString(name));
	}
	
	@Override
	public Object getValue() {
		return new StringBuilder().append('"').append(range.toString()).append('"').toString();
	}

	public VersionRange getVersionRange() {
		return range;
	}
}
