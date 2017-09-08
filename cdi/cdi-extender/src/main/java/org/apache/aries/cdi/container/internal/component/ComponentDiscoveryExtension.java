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

package org.apache.aries.cdi.container.internal.component;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.ProcessObserverMethod;

import org.apache.aries.cdi.container.internal.component.ComponentModel.Builder;
import org.apache.aries.cdi.container.internal.configuration.ConfigurationModel;
import org.apache.aries.cdi.container.internal.model.BeansModel;
import org.apache.aries.cdi.container.internal.model.ObserverMethodAnnotated;
import org.apache.aries.cdi.container.internal.reference.ReferenceModel;
import org.jboss.weld.exceptions.IllegalStateException;
import org.osgi.service.cdi.annotations.Component;
import org.osgi.service.cdi.annotations.Configuration;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.annotations.ReferencePolicy;
import org.osgi.service.cdi.annotations.ServiceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComponentDiscoveryExtension implements Extension {

	public ComponentDiscoveryExtension(BeansModel beansModel) {
		_beansModel = beansModel;
	}

	/*
	 * Process annotated classes to sync them up with the meta-model.
	 */
	<X> void processAnnotatedType(@Observes ProcessAnnotatedType<X> pat, BeanManager beanManager) {
		final AnnotatedType<X> at = pat.getAnnotatedType();

		Class<X> annotatedClass = at.getJavaClass();

		final String className = annotatedClass.getName();

		ComponentModel componentModel = _beansModel.getComponentModel(className);

		if (componentModel == null) {
			return;
		}

		// If the component's class is annotated with @Component, replace the meta-model with one built from the annotation.

		Component component = at.getAnnotation(Component.class);

		if (component != null) {

			// This also means we have to throw away any descriptor configurations/references.

			if (_log.isDebugEnabled()) {
				_log.debug("CDIe - OSGi CDI annotations found. Clearing descriptor infos for {}", className);
			}

			_beansModel.removeComponentModel(className);

			Builder builder = new ComponentModel.Builder(
				annotatedClass
			).name(
				component.name()
			).scope(
				component.scope()
			);

			for (String property : component.property()) {
				builder.property(property);
			}

			for (Class<?> provide : component.service()) {
				builder.provide(provide.getName());
			}

			componentModel = builder.build();

			// Mark the component as "found" so that in the end we can produce an error for missing components.

			componentModel.found(true);

			_beansModel.addComponentModel(className, componentModel);

			return;
		}

		componentModel.found(true);

		// If we didn't find @Component, we still need to check if the class has @Configuration/@Reference annotations.
		// If so, we need to throw away those descriptor infos.

		final List<ConfigurationModel> configurations = componentModel.getConfigurations();
		final List<ReferenceModel> references = componentModel.getReferences();

		Predicate<Annotation> hasAnnotations = annotation ->
			Configuration.class.isInstance(annotation) || Reference.class.isInstance(annotation);

		Consumer<Object> clearInfos = o -> {
			if (_log.isDebugEnabled()) {
				_log.debug("CDIe - OSGi CDI annotations found. Clearing descriptor infos for {}", className);
			}
			configurations.clear();
			references.clear();
		};

		// check constructors

		Arrays.stream(
			annotatedClass.getDeclaredConstructors()
		).filter(
			ctor -> Arrays.stream(
				ctor.getParameterAnnotations()
			).flatMap(
				array -> Arrays.stream(array)
			).filter(
				hasAnnotations
			).findFirst().isPresent()
		).findFirst().ifPresent(
			clearInfos
		);

		// check fields

		Arrays.stream(
			annotatedClass.getDeclaredFields()
		).filter(
			field -> Arrays.stream(
				field.getAnnotations()
			).filter(
				hasAnnotations
			).findFirst().isPresent()
		).findFirst().ifPresent(
			clearInfos
		);

		// check methods

		Arrays.stream(
			annotatedClass.getDeclaredMethods()
		).filter(
			method -> Arrays.stream(
				method.getParameterAnnotations()
			).flatMap(
				array -> Arrays.stream(array)
			).filter(
				hasAnnotations
			).findFirst().isPresent()
		).findFirst().ifPresent(
			clearInfos
		);
	}

	/*
	 * Process every injection point
	 */
	void processInjectionPoint(@Observes ProcessInjectionPoint<?, ?> pip) {
		final InjectionPoint injectionPoint = pip.getInjectionPoint();

		Bean<?> bean = injectionPoint.getBean();

		if (bean == null) {

			// It could be an observer method on an extension! Ignore it!

			if (_log.isDebugEnabled()) {
				_log.debug("CDIe - Ignoring injection point {} on non-component able bean!", injectionPoint);
			}

			return;
		}

		String beanClassName = bean.getBeanClass().getName();

		// Is it a component?

		ComponentModel componentModel = _beansModel.getComponentModel(beanClassName);

		if (componentModel == null) {

			// No it's not!

			return;
		}

		Annotated annotated = injectionPoint.getAnnotated();

		Reference reference = annotated.getAnnotation(Reference.class);
		Configuration configuration = annotated.getAnnotation(Configuration.class);

		// Is it annotated with @Reference?

		if (reference != null) {
			processReference(pip, componentModel, reference, configuration);

			return;
		}

		// Is it annotated with @Configuration?

		else if (configuration != null) {
			processConfiguration(pip, bean, componentModel, configuration);

			return;
		}

		if (matchReference(pip, bean, componentModel, injectionPoint)) {
			return;
		}

		matchConfiguration(pip, bean, componentModel, injectionPoint);
	}

	void processObserverMethod(@Observes ProcessObserverMethod<ServiceEvent<?>, ?> pom) {
		ObserverMethod<ServiceEvent<?>> observerMethod = pom.getObserverMethod();

		if (_log.isDebugEnabled()) {
			_log.debug("CDIe - Processing observer method {}", observerMethod);
		}

		Class<?> beanClass = observerMethod.getBeanClass();

		final String className = beanClass.getName();

		ComponentModel componentModel = _beansModel.getComponentModel(className);

		if (componentModel == null) {
			pom.addDefinitionError(
				new IllegalArgumentException(
					String.format(
						"The observer method {} is using the event type 'ServiceEvent' but is not defined as a bean",
						observerMethod)));

			return;
		}

		Reference reference = getQualifier(observerMethod, Reference.class);
		Configuration configuration = getQualifier(observerMethod, Configuration.class);

		if (reference != null) {
			processReference(pom, componentModel, reference, configuration);

			return;
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends Annotation> T getQualifier(
		ObserverMethod<ServiceEvent<?>> observerMethod, Class<T> clazz) {
		Set<Annotation> qualifiers = observerMethod.getObservedQualifiers();
		for (Annotation annotation : qualifiers) {
			if (clazz.isAssignableFrom(annotation.annotationType())) {
				return (T)annotation;
			}
		}
		return null;
	}

	private boolean matchConfiguration(
		ProcessInjectionPoint<?, ?> pip,
		Bean<?> bean,
		ComponentModel componentModel,
		InjectionPoint injectionPoint) {

		for (ConfigurationModel configurationModel : componentModel.getConfigurations()) {
			if (injectionPoint.getType().equals(configurationModel.getType())) {

				if (configurationModel.found() &&
					injectionPoint.getQualifiers().equals(configurationModel.getQualifiers())) {

					pip.addDefinitionError(
						new IllegalStateException(
							String.format("duplicate injection point match found for configuration %s", configurationModel)));

					return false;
				}

				configurationModel.setQualifiers(injectionPoint.getQualifiers());
				configurationModel.found(true);

				return true;
			}
		}

		return false;
	}

	private boolean matchReference(
		ProcessInjectionPoint<?, ?> pip,
		Bean<?> bean,
		ComponentModel componentModel,
		InjectionPoint injectionPoint) {

		for (ReferenceModel referenceModel : componentModel.getReferences()) {
			if (injectionPoint.getType().equals(referenceModel.getInjectionPointType())) {
				if (referenceModel.found() &&
					injectionPoint.getQualifiers().equals(referenceModel.getQualifiers())) {

					pip.addDefinitionError(
						new IllegalStateException(
							String.format("duplicate injection point match found for reference %s", referenceModel)));

					return false;
				}

				referenceModel.setQualifiers(injectionPoint.getQualifiers());
				referenceModel.found(true);

				return true;
			}
		}

		return false;
	}

	private void processConfiguration(
		ProcessInjectionPoint<?, ?> pip,
		Bean<?> bean,
		ComponentModel componentModel,
		Configuration configuration) {

		InjectionPoint injectionPoint = pip.getInjectionPoint();

		ConfigurationModel configurationModel = new ConfigurationModel.Builder(
			injectionPoint.getType()
		).pid(
			configuration.value()
		).policy(
			configuration.configurationPolicy()
		).qualifiers(
			injectionPoint.getQualifiers()
		).build();

		configurationModel.found(true);

		if (componentModel.getConfigurations().remove(configurationModel)) {
			if (_log.isDebugEnabled()) {
				_log.debug("CDIe - OSGi CDI annotations found. Clearing descriptor configuration for {}", injectionPoint);
			}
		}

		componentModel.getConfigurations().add(configurationModel);
	}

	private void processReference(
		ProcessInjectionPoint<?, ?> pip,
		ComponentModel componentModel,
		Reference reference,
		Configuration configuration) {

		InjectionPoint injectionPoint = pip.getInjectionPoint();

		try {
			if (configuration != null) {
				throw new IllegalArgumentException(
					String.format(
						"Cannot use @Reference and @Configuration on the same injection point {}",
						injectionPoint));
			}

			ReferenceModel referenceModel = new ReferenceModel.Builder(
				injectionPoint.getQualifiers()
			).cardinality(
				reference.cardinality()
			).annotated(
				injectionPoint.getAnnotated()
			).name(
				reference.name()
			).option(
				reference.policyOption()
			).policy(
				reference.policy()
			).scope(
				reference.scope()
			).service(
				reference.service()
			).target(
				reference.target()
			).build();

			if (componentModel.getReferences().remove(referenceModel)) {
				if (_log.isDebugEnabled()) {
					_log.debug("CDIe - OSGi CDI annotations found. Clearing descriptor reference for {}", injectionPoint);
				}
			}

			referenceModel.found(true);

			componentModel.getReferences().add(referenceModel);
		}
		catch (IllegalArgumentException iae) {
			_log.error("CDIe - Component definition error on {}", injectionPoint, iae);

			pip.addDefinitionError(iae);
		}
	}

	private void processReference(
		ProcessObserverMethod<ServiceEvent<?>, ?> pom,
		ComponentModel componentModel,
		Reference reference,
		Configuration configuration) {

		ObserverMethod<ServiceEvent<?>> observerMethod = pom.getObserverMethod();

		try {
			if (configuration != null) {
				throw new IllegalArgumentException(
					String.format(
						"Cannot use @Reference and @Configuration on the same observer method {}",
						observerMethod));
			}

			ReferenceModel referenceModel = new ReferenceModel.Builder(
				observerMethod.getObservedQualifiers()
			).annotated(
				new ObserverMethodAnnotated(observerMethod)
			).policy(
				ReferencePolicy.DYNAMIC
			).build();

			if (componentModel.getReferences().remove(referenceModel)) {
				if (_log.isDebugEnabled()) {
					_log.debug("CDIe - OSGi CDI annotations found. Clearing descriptor reference for {}", observerMethod);
				}
			}

			referenceModel.found(true);

			componentModel.getReferences().add(referenceModel);
		}
		catch (IllegalArgumentException iae) {
			_log.error("CDIe - Component definition error on {}", observerMethod, iae);

			pom.addDefinitionError(iae);
		}
	}

	private static final Logger _log = LoggerFactory.getLogger(ComponentDiscoveryExtension.class);

	private final BeansModel _beansModel;

}
