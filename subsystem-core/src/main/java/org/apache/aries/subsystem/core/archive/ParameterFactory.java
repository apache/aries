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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParameterFactory {
	public static final String QUOTED_STRING = "\"((?:[^\"\r\n\u0000]|\\\\\"|\\\\\\\\)*)\"";
	public static final String ARGUMENT = '(' + Grammar.EXTENDED + ")|" + QUOTED_STRING;
	private static final String REGEX = '(' + Grammar.EXTENDED + ")(\\:?=)(?:" + ARGUMENT + ')';
	private static final Pattern PATTERN = Pattern.compile(REGEX);
	
	public static Parameter create(String parameter) {
		Matcher matcher = PATTERN.matcher(parameter);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Invalid parameter: " + parameter);
		}
		String name = matcher.group(1);
		String symbol = matcher.group(2);
		String value = matcher.group(3);
		if (value == null)
			value = matcher.group(4);
		if (symbol.equals("=")) {
			return AttributeFactory.createAttribute(name, value);
		}
		return DirectiveFactory.createDirective(name, value);
	}
}
