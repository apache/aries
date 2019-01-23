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

import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessManagedBean;
import javax.enterprise.inject.spi.ProcessProducerField;
import javax.enterprise.inject.spi.ProcessProducerMethod;
import javax.enterprise.inject.spi.ProcessSessionBean;
import javax.enterprise.inject.spi.ProcessSyntheticBean;

import org.jboss.weld.util.Types;
import org.osgi.service.cdi.ServiceScope;
import org.osgi.service.cdi.annotations.Service;
import org.osgi.service.cdi.annotations.ServiceInstance;

public class Annotates {

	private Annotates() {
		// no instances
	}

	public static Map<String, Object> componentProperties(Annotated annotated) {
		return Maps.merge(annotated.getAnnotations());
	}

	@SuppressWarnings("unchecked")
	public static <X> Class<X> declaringClass(Object instance) {
		Class<?> declaringClass = null;

		if (instance instanceof AnnotatedMember) {
			AnnotatedMember<?> af = (AnnotatedMember<?>)instance;

			declaringClass = af.getDeclaringType().getJavaClass();
		}
		else if (instance instanceof AnnotatedParameter) {
			AnnotatedParameter<?> ap = (AnnotatedParameter<?>)instance;

			Parameter javaParameter = ap.getJavaParameter();

			Executable executable = javaParameter.getDeclaringExecutable();

			declaringClass = executable.getDeclaringClass();
		}
		else if (instance instanceof AnnotatedType) {
			AnnotatedType<?> annotatedType = (AnnotatedType<?>)instance;

			declaringClass = annotatedType.getJavaClass();
		}
		else if (instance instanceof Annotated) {
			Annotated annotated = (Annotated)instance;

			declaringClass = Types.getRawTypes(new Type[] {annotated.getBaseType()})[0];
		}
		else if (instance instanceof ProcessManagedBean) {
			ProcessManagedBean<?> bean = (ProcessManagedBean<?>)instance;

			declaringClass = bean.getAnnotatedBeanClass().getJavaClass();
		}
		else if (instance instanceof ProcessSessionBean) {
			ProcessSessionBean<?> bean = (ProcessSessionBean<?>)instance;

			declaringClass = bean.getAnnotatedBeanClass().getJavaClass();
		}
		else if (instance instanceof ProcessProducerMethod) {
			ProcessProducerMethod<?, ?> producer = (ProcessProducerMethod<?, ?>)instance;

			declaringClass = producer.getAnnotatedProducerMethod().getDeclaringType().getJavaClass();
		}
		else if (instance instanceof ProcessProducerField) {
			ProcessProducerField<?, ?> producer = (ProcessProducerField<?, ?>)instance;

			declaringClass = producer.getAnnotatedProducerField().getDeclaringType().getJavaClass();
		}
		else if (instance instanceof ProcessSyntheticBean) {
			ProcessSyntheticBean<?> synthetic = (ProcessSyntheticBean<?>)instance;

			declaringClass = synthetic.getBean().getBeanClass();
		}
		else if (instance instanceof ProcessBean) {
			ProcessBean<?> processBean = (ProcessBean<?>)instance;

			declaringClass = processBean.getBean().getBeanClass();
		}

		return (Class<X>)declaringClass;
	}

	public static List<Class<?>> serviceClasses(Annotated annotated) {
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

	public static List<String> serviceClassNames(Annotated annotated) {
		return serviceClasses(annotated).stream().map(
			st -> st.getName()
		).sorted().collect(Collectors.toList());
	}

	public static ServiceScope serviceScope(Annotated annotated) {
		ServiceInstance serviceInstance = annotated.getAnnotation(ServiceInstance.class);

		if (serviceInstance != null) {
			return serviceInstance.value();
		}

		return ServiceScope.SINGLETON;
	}

}