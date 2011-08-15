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
	public static final String NAME = "resolution";
	
	public static final ResolutionDirective MANDATORY = new ResolutionDirective(true);
	public static final ResolutionDirective OPTIONAL = new ResolutionDirective(false);
	
	public static ResolutionDirective getInstance(String value) {
		if (Constants.RESOLUTION_MANDATORY.equals(value)) {
			return MANDATORY;
		}
		else if (Constants.RESOLUTION_OPTIONAL.equals(value)) {
			return OPTIONAL;
		}
		else {
			throw new IllegalArgumentException("Illegal " + Constants.RESOLUTION_DIRECTIVE + " value: " + value);
		}
		
	}
	
	private final boolean mandatory;
	
	private ResolutionDirective(boolean mandatory) {
		super(Constants.RESOLUTION_DIRECTIVE, mandatory ? Constants.RESOLUTION_MANDATORY : Constants.RESOLUTION_OPTIONAL);
		this.mandatory = mandatory;
	}

	public boolean isMandatory() {
		return mandatory;
	}
	
	public boolean isOptional() {
		return !mandatory;
	}
}
