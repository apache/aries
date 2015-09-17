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

public class ResolutionDirective extends AbstractDirective {
	public static final String NAME = Constants.RESOLUTION_DIRECTIVE;
	public static final String VALUE_MANDATORY = Constants.RESOLUTION_MANDATORY;
	public static final String VALUE_OPTIONAL = Constants.RESOLUTION_OPTIONAL;
	
	public static final ResolutionDirective MANDATORY = new ResolutionDirective(VALUE_MANDATORY);
	public static final ResolutionDirective OPTIONAL = new ResolutionDirective(VALUE_OPTIONAL);
	
	public ResolutionDirective() {
		this(VALUE_MANDATORY);
	}
	
	public static ResolutionDirective getInstance(String value) {
		if (VALUE_MANDATORY.equals(value))
			return MANDATORY;
		if (VALUE_OPTIONAL.equals(value))
			return OPTIONAL;
        throw new IllegalArgumentException("Invalid " + Constants.RESOLUTION_DIRECTIVE + " directive: " + value);
	}
	
	private ResolutionDirective(String value) {
		super(NAME, value);
	}

	public boolean isMandatory() {
		return MANDATORY == this || VALUE_MANDATORY.equals(getValue());
	}
	
	public boolean isOptional() {
		return OPTIONAL == this || VALUE_OPTIONAL.equals(getValue());
	}
}
