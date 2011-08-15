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

public class TypeAttribute extends AbstractAttribute {
	public static final TypeAttribute DEFAULT = new TypeAttribute();
	// TODO Add to constants.
	public static final String DEFAULT_VALUE = "osgi.bundle";
	// TODO Add to constants.
	public static final String NAME = "type";
	
	public TypeAttribute() {
		this(DEFAULT_VALUE);
	}
	
	public TypeAttribute(String value) {
		super(NAME, value);
	}

	public String getType() {
		return getValue();
	}
}
