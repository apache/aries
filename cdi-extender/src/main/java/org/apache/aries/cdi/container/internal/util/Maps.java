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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;

import org.osgi.service.cdi.annotations.ComponentPropertyType;
import org.osgi.util.converter.TypeReference;

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

	public static Map<String, Object> of(Dictionary<String, ?> dict) {
		Map<String, Object> map = new HashMap<>();

		for (Enumeration<String> enu = dict.keys(); enu.hasMoreElements();) {
			String key = enu.nextElement();
			map.put(key, dict.get(key));
		}

		return map;
	}

	public static Dictionary<String, ?> dict(Map<String, Object> map) {
		Dictionary<String, Object> dict = new Hashtable<>();

		if (map != null) {
			for (Entry<String, Object> entry : map.entrySet()) {
				dict.put(entry.getKey(), entry.getValue());
			}
		}

		return dict;
	}

	public static Dictionary<String, ?> dict(Object... args) {
		Dictionary<String, Object> map = new Hashtable<>();

		if ((args.length % 2) != 0) throw new IllegalArgumentException("requires even number of args");

		for (int i = 0; i < args.length; i+=2) {
			map.put(String.valueOf(args[i]), args[i+1]);
		}

		return map;
	}

	@SafeVarargs
	public static <T> Map<String, T> of(T... args) {
		Map<String, T> map = new HashMap<>();

		if ((args.length % 2) != 0) throw new IllegalArgumentException("requires even number of args");

		for (int i = 0; i < args.length; i+=2) {
			map.put(String.valueOf(args[i]), args[i+1]);
		}

		return map;
	}

	public static Map<String, Object> componentProperties(Annotated annotated) {
		if (annotated instanceof AnnotatedType) {
			return merge(Arrays.asList(((AnnotatedType<?>)annotated).getJavaClass().getAnnotations()));
		}
		else if (annotated instanceof AnnotatedParameter) {
			return merge(Arrays.asList(((AnnotatedParameter<?>)annotated).getJavaParameter().getAnnotations()));
		}
		else if (annotated instanceof AnnotatedField) {
			return merge(Arrays.asList(((AnnotatedField<?>)annotated).getJavaMember().getAnnotations()));
		}
		else if (annotated instanceof AnnotatedConstructor) {
			return merge(Arrays.asList(((AnnotatedConstructor<?>)annotated).getJavaMember().getAnnotations()));
		}
		else if (annotated instanceof AnnotatedMethod) {
			return merge(Arrays.asList(((AnnotatedMethod<?>)annotated).getJavaMember().getAnnotations()));
		}
		return merge(new ArrayList<>(annotated.getAnnotations()));
	}

	public static Map<String, Object> merge(List<Annotation> annotations) {
		return annotations.stream().filter(
			ann -> Objects.nonNull(ann.annotationType().getAnnotation(ComponentPropertyType.class))
		).map(
			ann -> Conversions.convert(ann).sourceAs(ann.annotationType()).to(new TypeReference<Map<String, Object>>() {})
		).map(Map::entrySet).flatMap(Collection::stream).collect(
			Collectors.toMap(
				Map.Entry::getKey,
				Map.Entry::getValue,
				Maps::merge
			)
		);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List<?> merge(Object a, Object b) {
		List<?> aList = Conversions.convert(a).to(new TypeReference<List<?>>() {});
		List<?> bList = Conversions.convert(b).to(new TypeReference<List<?>>() {});
		List checkedList = Collections.checkedList(new ArrayList(), aList.get(0).getClass());
		checkedList.addAll(aList);
		checkedList.addAll(bList);
		return checkedList;
	}

}
