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

package org.apache.aries.cdi.container.internal.model;

import org.xml.sax.Attributes;

public class Model {

	public static boolean getBoolean(String uri, String localName, Attributes attributes, boolean defaultValue) {
		String value = getValue(uri, localName, attributes);

		if (value == null) {
			return defaultValue;
		}

		return Boolean.parseBoolean(value);
	}

	public static String getValue(String uri, String localName, Attributes attributes) {
		return getValue(uri, localName, attributes, "");
	}

	public static String getValue(String uri, String localName, Attributes attributes, String defaultValue) {
		String value = attributes.getValue(uri, localName);

		if (value == null) {
			value = attributes.getValue("", localName);
		}

		if (value != null) {
			value = value.trim();
		}

		if (value == null) {
			return defaultValue;
		}

		return value;
	}

	public static String[] getValues(String uri, String localName, Attributes attributes) {
		return getValues(uri, localName, attributes, new String[0]);
	}

	public static String[] getValues(String uri, String localName, Attributes attributes, String[] defaultValue) {
		String value = getValue(uri, localName, attributes, "");

		if (value.length() == 0) {
			return defaultValue;
		}

		return value.split("\\s+");
	}

	private Model() {
		// TODO Auto-generated constructor stub
	}

}
