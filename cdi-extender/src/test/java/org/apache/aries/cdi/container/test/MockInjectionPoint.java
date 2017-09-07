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
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.aries.cdi.container.internal.util.Sets;

public class MockInjectionPoint implements InjectionPoint {

	public MockInjectionPoint(Type type) {
		_type = type;
		_annotated = new MockAnnotated(_type);
	}

	@Override
	public Type getType() {
		return _type;
	}

	@Override
	public Set<Annotation> getQualifiers() {
		return Collections.emptySet();
	}

	@Override
	public Bean<?> getBean() {
		return null;
	}

	@Override
	public Member getMember() {
		return null;
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
		return getClass().getSimpleName() + "[" + _type + "]";
	}

	private final Type _type;
	private final Annotated _annotated;

	private static class MockAnnotated implements Annotated {

		public MockAnnotated(Type service) {
			_service = service;
		}

		@Override
		public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
			return null;
		}

		@Override
		public Set<Annotation> getAnnotations() {
			return Collections.emptySet();
		}

		@Override
		public <T extends Annotation> Set<T> getAnnotations(Class<T> annotationType) {
			return null;
		}

		@Override
		public Type getBaseType() {
			return _service;
		}

		@Override
		public Set<Type> getTypeClosure() {
			return Sets.hashSet(_service);
		}

		@Override
		public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
			return false;
		}

		private final Type _service;

	}

}
