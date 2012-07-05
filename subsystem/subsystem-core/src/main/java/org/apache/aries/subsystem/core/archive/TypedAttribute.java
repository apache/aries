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

import org.osgi.framework.Version;

public class TypedAttribute extends AbstractAttribute {
	public static enum Type {
		Double,
		Long,
		String,
		Version
	}
	
	private static Object parseValue(String value, Type type) {
		switch (type) {
			case Double:
				return Double.valueOf(value);
			case Long:
				return Long.valueOf(value);
			case Version:
				return Version.parseVersion(value);
			default:
				return value;
		}
	}
	
	private final Type type;
	
	public TypedAttribute(String name, String value, String type) {
		this(name, value, Type.valueOf(type));
	}
	
	public TypedAttribute(String name, String value, Type type) {
		super(name, parseValue(value, type));
		this.type = type;
	}
	
	public Type getType() {
		return type;
	}
}
