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

package org.apache.aries.cdi.container.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.inject.spi.AnnotatedCallable;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;

class AnnotatedCache {

	public static Set<Type> collectTypes(Class<?> c) {
		Set<Type> types = new HashSet<>();
		collectTypes(types, c);
		return types;
	}

	private static void collectTypes(Set<Type> closure, Class<?> c) {
		if (c == null) return;
		closure.add(c);
		collectTypes(closure, c.getSuperclass());
		for (Class<?> i : c.getInterfaces()) {
			collectTypes(closure, i);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <X> AnnotatedConstructor<X> getAnnotatedConstructor(Constructor<X> c) {
		return (AnnotatedConstructor<X>) _ctorCache.computeIfAbsent(c, k -> new MockAnnotatedConstructor(k));
	}

	@SuppressWarnings("unchecked")
	public static <X> AnnotatedCallable<X> getAnnotatedCallable(Executable executable) {
		if (executable instanceof Constructor) {
			return getAnnotatedConstructor((Constructor<X>)executable);
		}
		return getAnnotatedMethod((Method)executable);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <X> AnnotatedField<X> getAnnotatedField(Field f) {
		return (AnnotatedField<X>) _fieldCache.computeIfAbsent(f, k -> new MockAnnotatedField(k));
	}

	@SuppressWarnings("unchecked")
	public static <X> AnnotatedMember<X> getAnnotatedMember(Member member) {
		if (member instanceof Field) {
			return getAnnotatedField((Field)member);
		}
		else if (member instanceof Constructor) {
			return getAnnotatedConstructor((Constructor<X>)member);
		}

		return getAnnotatedMethod((Method)member);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <X> AnnotatedMethod<X> getAnnotatedMethod(Method m) {
		return (AnnotatedMethod<X>) _methodCache.computeIfAbsent(m, k -> new MockAnnotatedMethod(k));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <X> AnnotatedParameter<X> getAnnotatedParameter(Parameter p) {
		return (AnnotatedParameter<X>) _paramCache.computeIfAbsent(p, k -> new MockAnnotatedParameter(k));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <X> AnnotatedType<X> getAnnotatedType(Class<X> type) {
		return (AnnotatedType<X>) _typeCache.computeIfAbsent(type, k -> new MockAnnotatedType(k));
	}

	private static final Map<Constructor<?>, AnnotatedConstructor<?>> _ctorCache = new ConcurrentHashMap<>();
	private static final Map<Field, AnnotatedField<?>> _fieldCache = new ConcurrentHashMap<>();
	private static final Map<Method, AnnotatedMethod<?>> _methodCache = new ConcurrentHashMap<>();
	private static final Map<Parameter, AnnotatedParameter<?>> _paramCache = new ConcurrentHashMap<>();
	private static final Map<Class<?>, AnnotatedType<?>> _typeCache = new ConcurrentHashMap<>();

}