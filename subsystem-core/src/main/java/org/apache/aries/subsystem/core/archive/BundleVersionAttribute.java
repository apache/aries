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

public class BundleVersionAttribute extends AbstractAttribute {
	public static final String NAME = Constants.BUNDLE_VERSION_ATTRIBUTE;
	
	private final VersionRange range;
	
	public BundleVersionAttribute() {
		this(Version.emptyVersion.toString());
	}
			
	public BundleVersionAttribute(String value) {
		this(new VersionRange(value));
	}
	
	public BundleVersionAttribute(VersionRange range) {
		super(NAME, range.toString());
		this.range = range;
	}
	
	public StringBuilder appendToFilter(StringBuilder builder) {
		return builder.append(range.toFilterString(NAME));
	}

	public VersionRange getVersionRange() {
		return range;
	}
	
	@Override
	public Object getValue() {
		return new StringBuilder().append('"').append(range.toString()).append('"').toString();
	}
}
