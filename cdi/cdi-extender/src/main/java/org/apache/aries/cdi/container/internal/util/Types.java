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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

import org.osgi.service.cdi.annotations.Service;

public class Types {

	private Types() {
		// no instances
	}

	public static List<Class<?>> collectServiceTypes(Annotated annotated) {
		List<Class<?>> serviceTypes = new ArrayList<>();

		List<java.lang.reflect.AnnotatedType> ats = new ArrayList<>();

		if (annotated instanceof AnnotatedType) {
			Class<?> annotatedClass = ((AnnotatedType<?>)annotated).getJavaClass();
			Optional.ofNullable(annotatedClass.getAnnotatedSuperclass()).ifPresent(at -> ats.add(at));
			ats.addAll(Arrays.asList(annotatedClass.getAnnotatedInterfaces()));

			for (java.lang.reflect.AnnotatedType at : ats) {
				Optional.ofNullable(at.getAnnotation(Service.class)).ifPresent(
					service -> {
						if (service.value().length > 0) {
							throw new IllegalArgumentException(
								String.format(
									"@Service on type_use must not specify a value: %s",
									annotatedClass));
						}

						Type type = at.getType();

						if (!(type instanceof Class)) {
							throw new IllegalArgumentException(
								String.format(
									"@Service on type_use must only be specified on non-generic types: %s",
									annotatedClass));
						}

						serviceTypes.add((Class<?>)type);
					}
				);
			}

			Service service = annotated.getAnnotation(Service.class);

			if (service == null) {
				return serviceTypes;
			}

			if (!serviceTypes.isEmpty()) {
				throw new IllegalArgumentException(
					String.format(
						"@Service must not be applied to type and type_use: %s",
						annotated));
			}

			if (service.value().length > 0) {
				serviceTypes.addAll(Arrays.asList(service.value()));
			}
			else if (annotatedClass.getInterfaces().length > 0) {
				serviceTypes.addAll(Arrays.asList(annotatedClass.getInterfaces()));
			}
			else {
				serviceTypes.add(annotatedClass);
			}
		}
		else if (annotated instanceof AnnotatedMethod) {
			Service service = annotated.getAnnotation(Service.class);

			if (service == null) {
				return serviceTypes;
			}

			Class<?> returnType = ((AnnotatedMethod<?>)annotated).getJavaMember().getReturnType();

			if (service.value().length > 0) {
				serviceTypes.addAll(Arrays.asList(service.value()));
			}
			else if (returnType.getInterfaces().length > 0) {
				serviceTypes.addAll(Arrays.asList(returnType.getInterfaces()));
			}
			else {
				serviceTypes.add(returnType);
			}
		}
		else if (annotated instanceof AnnotatedField) {
			Service service = annotated.getAnnotation(Service.class);

			if (service == null) {
				return serviceTypes;
			}

			Class<?> fieldType = ((AnnotatedField<?>)annotated).getJavaMember().getType();

			if (service.value().length > 0) {
				serviceTypes.addAll(Arrays.asList(service.value()));
			}
			else if (fieldType.getInterfaces().length > 0) {
				serviceTypes.addAll(Arrays.asList(fieldType.getInterfaces()));
			}
			else {
				serviceTypes.add(fieldType);
			}
		}

		return serviceTypes;
	}

}