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
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

public class FilterDirective extends AbstractDirective {
	public static final String NAME = Constants.FILTER_DIRECTIVE;
	
	public FilterDirective(String value) {
		super(NAME, value);
		try {
			FrameworkUtil.createFilter(value);
		}
		catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException("Invalid filter: " + value, e);
		}
	}

	public String toString() {
		return new StringBuilder()
		.append(getName())
		.append(":=\"")
		.append(getValue())
		.append('\"')
		.toString();
	}
}
