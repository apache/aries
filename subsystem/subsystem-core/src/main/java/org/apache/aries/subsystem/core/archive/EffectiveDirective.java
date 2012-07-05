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

public class EffectiveDirective extends AbstractDirective {
	public static final String NAME = Constants.EFFECTIVE_DIRECTIVE;
	public static final String VALUE_ACTIVE = Constants.EFFECTIVE_ACTIVE;
	public static final String VALUE_RESOLVE = Constants.EFFECTIVE_RESOLVE;
	
	public static final EffectiveDirective ACTIVE = new EffectiveDirective(VALUE_ACTIVE);
	public static final EffectiveDirective RESOLVE = new EffectiveDirective(VALUE_RESOLVE);
	
	public static final EffectiveDirective DEFAULT = RESOLVE;
	
	public static EffectiveDirective getInstance(String value) {
		if (VALUE_ACTIVE.equals(value))
			return ACTIVE;
		if (VALUE_RESOLVE.equals(value))
			return RESOLVE;
		return new EffectiveDirective(value);
	}
	
	public EffectiveDirective() {
		this(Constants.EFFECTIVE_RESOLVE);
	}
	
	public EffectiveDirective(String value) {
		super(NAME, value);
	}
	
	public boolean isActive() {
		return ACTIVE == this || Constants.EFFECTIVE_ACTIVE.equals(getValue());
	}
	
	public boolean isResolve() {
		return RESOLVE == this || Constants.EFFECTIVE_RESOLVE.equals(getValue());
	}
}
