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

import static org.apache.aries.cdi.container.test.AnnotatedCache.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

public class MockAnnotatedType<X> implements AnnotatedType<X> {

	private final Class<X> _clazz;
	private final Set<Type> _types;

	public MockAnnotatedType(Class<X> clazz) {
		_clazz = clazz;
		_types = collectTypes(_clazz);
	}

	@Override
	public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
		return _clazz.getAnnotation(annotationType);
	}

	@Override
	public Set<Annotation> getAnnotations() {
		return Arrays.stream(_clazz.getAnnotations()).collect(Collectors.toSet());
	}

	@Override
	public Type getBaseType() {
		return _clazz;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<AnnotatedConstructor<X>> getConstructors() {
		return Arrays.stream(_clazz.getConstructors()).map(
			ctor -> (AnnotatedConstructor<X>)getAnnotatedConstructor(ctor)
		).collect(Collectors.toSet());
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<AnnotatedField<? super X>> getFields() {
		return Arrays.stream(_clazz.getFields()).map(
			field -> (AnnotatedField<X>)getAnnotatedField(field)
		).collect(Collectors.toSet());
	}

	@Override
	public Class<X> getJavaClass() {
		return _clazz;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<AnnotatedMethod<? super X>> getMethods() {
		return Arrays.stream(_clazz.getMethods()).map(
			method -> (AnnotatedMethod<X>)getAnnotatedMethod(method)
		).collect(Collectors.toSet());
	}

	@Override
	public Set<Type> getTypeClosure() {
		return _types;
	}

	@Override
	public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
		return false;
	}

}