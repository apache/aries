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

import org.osgi.resource.Namespace;

public class CardinalityDirective extends AbstractDirective {
	public static final String NAME = Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE;
	public static final String VALUE_MULTIPLE = Namespace.CARDINALITY_MULTIPLE;
	public static final String VALUE_SINGLE = Namespace.CARDINALITY_SINGLE;
	
	public static final CardinalityDirective MULTIPLE = new CardinalityDirective(VALUE_MULTIPLE);
	public static final CardinalityDirective SINGLE = new CardinalityDirective(VALUE_SINGLE);
	
	public static final CardinalityDirective DEFAULT = SINGLE;
	
	public static CardinalityDirective getInstance(String value) {
		if (VALUE_SINGLE.equals(value))
			return SINGLE;
		if (VALUE_MULTIPLE.equals(value))
			return MULTIPLE;
		return new CardinalityDirective(value);
	}
	
	public CardinalityDirective() {
		this(VALUE_SINGLE);
	}
	
	public CardinalityDirective(String value) {
		super(NAME, value);
	}
	
	public boolean isMultiple() {
		return MULTIPLE == this || VALUE_MULTIPLE.equals(getValue());
	}
	
	public boolean isSingle() {
		return SINGLE == this || VALUE_SINGLE.equals(getValue());
	}
}
