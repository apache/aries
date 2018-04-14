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
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedType;

class MockAnnotatedField<X> implements AnnotatedField<X> {

	private final AnnotatedType<X> _declaringType;
	private final Field _field;
	private final Set<Type> _types;

	@SuppressWarnings("unchecked")
	public MockAnnotatedField(Field field) {
		_field = field;
		_declaringType = (AnnotatedType<X>) getAnnotatedType(_field.getDeclaringClass());
		_types = collectTypes(_field.getType());
	}

	@Override
	public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
		return _field.getAnnotation(annotationType);
	}

	@Override
	public Set<Annotation> getAnnotations() {
		return Arrays.stream(_field.getAnnotations()).collect(Collectors.toSet());

	}
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Annotation> Set<T> getAnnotations(Class<T> annotationType) {
		return Arrays.stream(_field.getAnnotations()).filter(
			ann -> ann.annotationType().equals(annotationType)
		).map(
			ann -> (T)ann
		).collect(Collectors.toSet());
	}

	@Override
	public Type getBaseType() {
		return _field.getType();
	}

	@Override
	public AnnotatedType<X> getDeclaringType() {
		return _declaringType;
	}

	@Override
	public Field getJavaMember() {
		return _field;
	}

	@Override
	public Set<Type> getTypeClosure() {
		return _types;
	}

	@Override
	public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
		return Arrays.stream(_field.getAnnotations()).filter(
			ann -> ann.annotationType().equals(annotationType)
		).findFirst().isPresent();
	}

	@Override
	public boolean isStatic() {
		return false;
	}

}