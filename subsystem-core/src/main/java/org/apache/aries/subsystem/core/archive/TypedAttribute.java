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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import org.osgi.framework.Version;

public class TypedAttribute extends AbstractAttribute {
	private static final String DOUBLE = "Double";
	private static final String LIST = "List";
	private static final String LIST_DOUBLE = "List<Double>";
	private static final String LIST_LONG = "List<Long>";
	private static final String LIST_STRING = "List<String>";
	private static final String LIST_VERSION = "List<Version>";
	private static final String LONG = "Long";
	private static final String STRING = "String";
	private static final String VERSION = "Version";
	
	private static Object parseScalar(String value, String type) {
		if (STRING.equals(type)) {
			return value;
		}
		if (VERSION.equals(type)) {
			return Version.parseVersion(value);
		}
		if (LONG.equals(type)) {
			return Long.valueOf(value);
		}
		if (DOUBLE.equals(type)) {
			return Double.valueOf(value);
		}
		return null;
	}
	
	private static Object parseList(String value, String type) {
		if (!type.startsWith(LIST)) {
			return null;
		}
		String scalar;
		if (type.length() == LIST.length()) {
			scalar = STRING;
		}
		else {
			Matcher matcher = Patterns.SCALAR_LIST.matcher(type);
			if (!matcher.matches()) {
				return null;
			}
			scalar = matcher.group(1);
		}
		String[] values = value.split(",");
		List<Object> result = new ArrayList<Object>(values.length);
		for (String s : values) {
			result.add(parseScalar(s, scalar));
		}
		return result;
	}
	
	private static Object parseValue(String value, String type) {
		if (type == null) {
			return value;
		}
		Object result = parseScalar(value, type);
		if (result == null) {
			result = parseList(value, type);
		}
		return result;
	}
	
	private final String type;
	
	public TypedAttribute(String name, String value, String type) {
		super(name, parseValue(value, type));
		this.type = type;
	}
	
	public TypedAttribute(String name, Object value) {
		super(name, value);
		if (value instanceof String) {
			type = STRING;
		}
		else if (value instanceof List) {
			@SuppressWarnings("rawtypes")
			List list = (List)value;
			if (list.isEmpty()) {
				type = LIST;
			}
			else {
				Object o = list.get(0);
				if (o instanceof String) {
					type = LIST_STRING;
				}
				else if (o instanceof Version) {
					type = LIST_VERSION;
				}
				else if (o instanceof Long) {
					type = LIST_LONG;
				}
				else if (o instanceof Double) {
					type = LIST_DOUBLE;
				}
				else {
					throw new IllegalArgumentException(name + '=' + value);
				}
			}
		}
		else if (value instanceof Version) {
			type = VERSION;
		}
		else if (value instanceof Long) {
			type = LONG;
		}
		else if (value instanceof Double) {
			type = DOUBLE;
		}
		else {
			throw new IllegalArgumentException(name + '=' + value);
		}
	}
	
	@Override
    public String toString() {
		StringBuilder builder = new StringBuilder()
				.append(getName())
				.append(':')
				.append(type)
				.append("=\"");
		if (type.startsWith(LIST)) {
			@SuppressWarnings("rawtypes")
			List list = (List)getValue();
			if (!list.isEmpty()) {
				builder.append(list.get(0));
			}
			for (int i = 1; i < list.size(); i++) {
				builder.append(',').append(list.get(i));
			}
		}
		else {
			builder.append(getValue());
		}
		return builder.append('"').toString();
	}
}
