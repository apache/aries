/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.cdi.container.internal.util;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Maps {

	private Maps() {
		// no instances
	}

	public static void appendFilter(StringBuilder sb, Map<String, String> map) {
		if (map.isEmpty()) {
			return;
		}

		for (Map.Entry<String, String> entry : map.entrySet()) {
			sb.append("(");
			sb.append(entry.getKey());
			sb.append("=");
			sb.append(entry.getValue());
			sb.append(")");
		}

	}

	public static Map<String, Object> map(String[] properties) {
		Map<String,Object> map = new HashMap<>();

		for (String property : properties) {
			map(map, property);
		}

		return map;
	}

	static void map(Map<String, Object> map, String property) {
		int eq = property.indexOf('=');

		String key = property.substring(0, eq);
		String type = "String";
		String value = property.substring(eq + 1, property.length());

		int colon = key.indexOf(':');

		if (colon != -1) {
			property = key;
			key = property.substring(0, colon);
			type = property.substring(colon + 1, property.length());
		}

		map(map, key, type, value);
	}

	static void map(Map<String, Object> map, String key, String type, String value) {
		PropertyType propertyType = PropertyType.find(type);

		Object object = map.get(key);

		if (object == null) {
			Object valueObject = Conversions.c().convert(value).to(propertyType.getType());

			map.put(key, valueObject);

			return;
		}

		Object valueObject = Conversions.c().convert(value).to(propertyType.componentType());

		if (propertyType.isRaw()) {
			if (!object.getClass().isArray()) {
				Object array = Array.newInstance(propertyType.componentType(), 2);
				Array.set(array, 0, object);
				Array.set(array, 1, valueObject);
				map.put(key, array);
			}
			else {
				int length = Array.getLength(object);
				Object array = Array.newInstance(propertyType.componentType(), length + 1);
				System.arraycopy(object, 0, array, 0, length);
				Array.set(array, length, valueObject);
				map.put(key, array);
			}
		}
		else if (propertyType.isList()) {
			List list = Collections.checkedList((List)object, propertyType.componentType());
			list.add(valueObject);
		}
		else if (propertyType.isSet()) {
			Set set = Collections.checkedSet((Set)object, propertyType.componentType());
			set.add(valueObject);
		}
	}

}
