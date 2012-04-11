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

import org.osgi.framework.namespace.IdentityNamespace;

public class TypeAttribute extends AbstractAttribute {
	public static final TypeAttribute DEFAULT = new TypeAttribute();
	// TODO Add to constants.
	public static final String DEFAULT_VALUE = IdentityNamespace.TYPE_BUNDLE;
	// TODO Add to constants.
	public static final String NAME = "type";
	
	public static TypeAttribute newInstance(String value) {
		if (value == null || value.length() == 0)
			return DEFAULT;
		return new TypeAttribute(value);
	}
	
	public TypeAttribute() {
		this(DEFAULT_VALUE);
	}
	
	public TypeAttribute(String value) {
		super(NAME, value);
	}

	public String getType() {
		return (String)getValue();
	}
}
