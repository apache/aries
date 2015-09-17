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

public class ReferenceDirective extends AbstractDirective {
	public static final String NAME = "reference";
	public static final String VALUE_FALSE = Boolean.FALSE.toString();
	public static final String VALUE_TRUE = Boolean.TRUE.toString();
	
	public static final ReferenceDirective FALSE = new ReferenceDirective(VALUE_FALSE);
	public static final ReferenceDirective TRUE = new ReferenceDirective(VALUE_TRUE);
	
	
	public ReferenceDirective() {
		this(VALUE_TRUE);
	}
	
	public static ReferenceDirective getInstance(String value) {
		if (VALUE_TRUE.equals(value))
			return TRUE;
		if (VALUE_FALSE.equals(value))
			return FALSE;
        throw new IllegalArgumentException("Invalid " + NAME + " directive: " + value);
	}
	
	private ReferenceDirective(String value) {
		super(NAME, value);
	}

	public boolean isReferenced() {
		return TRUE == this || VALUE_TRUE.equals(getValue());
	}
}
