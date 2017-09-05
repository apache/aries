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

import java.util.Arrays;

import org.osgi.util.converter.Converter;
import org.osgi.util.converter.ConverterBuilder;
import org.osgi.util.converter.Converting;
import org.osgi.util.converter.StandardConverter;
import org.osgi.util.converter.TypeRule;

public class Conversions {

	public static String toString(Object object) {
		return INSTANCE._converter.convert(object).defaultValue("").to(String.class);
	}

	public static Converting convert(Object object) {
		return INSTANCE._converter.convert(object);
	}

	private Conversions() {
		ConverterBuilder builder = new StandardConverter().newConverterBuilder();

		builder
			.rule(new TypeRule<>(String[].class, String.class, i -> Arrays.toString((String[])i)))
			.rule(new TypeRule<>(double[].class, String.class, i -> Arrays.toString((double[])i)))
			.rule(new TypeRule<>(int[].class, String.class, i -> Arrays.toString((int[])i)))
			.rule(new TypeRule<>(long[].class, String.class, i -> Arrays.toString((long[])i)));

		_converter = builder.build();
	}

	public static final Conversions INSTANCE = new Conversions();

	private final Converter _converter;

}
