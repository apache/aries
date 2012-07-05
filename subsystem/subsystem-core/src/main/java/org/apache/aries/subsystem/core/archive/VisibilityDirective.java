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

public class VisibilityDirective extends AbstractDirective {
	public static final String NAME = Constants.VISIBILITY_DIRECTIVE;
	public static final String VALUE_PRIVATE = Constants.VISIBILITY_PRIVATE;
	public static final String VALUE_REEXPORT = Constants.VISIBILITY_REEXPORT;
	
	public static final VisibilityDirective PRIVATE = new VisibilityDirective(VALUE_PRIVATE);
	public static final VisibilityDirective REEXPORT = new VisibilityDirective(VALUE_REEXPORT);
	
	public static VisibilityDirective getInstance(String value) {
		if (VALUE_PRIVATE.equals(value))
			return PRIVATE;
		if (VALUE_REEXPORT.equals(value))
			return REEXPORT;
		return new VisibilityDirective(value);
	}
	
	public VisibilityDirective() {
		this(VALUE_PRIVATE);
	}
	
	public VisibilityDirective(String value) {
		super(NAME, value);
	}
	
	public boolean isPrivate() {
		return PRIVATE == this || VALUE_PRIVATE.equals(getValue());
	}
	
	public boolean isReexport() {
		return REEXPORT == this || VALUE_REEXPORT.equals(getValue());
	}
}
