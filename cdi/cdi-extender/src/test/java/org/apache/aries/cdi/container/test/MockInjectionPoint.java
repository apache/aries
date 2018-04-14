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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Qualifier;

import org.jboss.weld.exceptions.IllegalArgumentException;

public class MockInjectionPoint implements InjectionPoint {

	public MockInjectionPoint(AnnotatedElement annotatedElement) {
		_annotatedElement = annotatedElement;
		if (annotatedElement instanceof Parameter) {
			_annotated = AnnotatedCache.getAnnotatedParameter((Parameter)annotatedElement);
			_type = ((Parameter)annotatedElement).getParameterizedType();
			_member = ((Parameter)annotatedElement).getDeclaringExecutable();
		}
		else if (annotatedElement instanceof Field) {
			_annotated = AnnotatedCache.getAnnotatedField((Field)annotatedElement);
			_type = ((Field)annotatedElement).getGenericType();
			_member = (Field)_annotatedElement;
		}
		else {
			throw new IllegalArgumentException("InjectionPoints are parameters or fields");
		}
	}

	protected String getFoo() {
		return foo;
	}

	@Override
	public Type getType() {
		return _type;
	}

	@Override
	public Set<Annotation> getQualifiers() {
		return _annotated.getAnnotations().stream().filter(
			ann -> ann.annotationType().isAnnotationPresent(Qualifier.class)
		).collect(Collectors.toSet());
	}

	@Override
	public Bean<?> getBean() {
		return null;
	}

	@Override
	public Member getMember() {
		return _member;
	}

	@Override
	public Annotated getAnnotated() {
		return _annotated;
	}

	@Override
	public boolean isDelegate() {
		return false;
	}

	@Override
	public boolean isTransient() {
		return false;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + _annotated.getBaseType() + "]";
	}

	private final Annotated _annotated;
	private final AnnotatedElement _annotatedElement;
	private final String foo = "bar";
	private final Member _member;
	private final Type _type;

}
