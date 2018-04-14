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
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.ProcessManagedBean;
import javax.enterprise.inject.spi.ProcessProducerField;
import javax.enterprise.inject.spi.ProcessProducerMethod;
import javax.enterprise.inject.spi.ProcessSessionBean;
import javax.enterprise.inject.spi.ProcessSyntheticBean;

import org.apache.aries.cdi.container.internal.configuration.ConfigurationModel;
import org.apache.aries.cdi.container.internal.model.BeansModel;
import org.apache.aries.cdi.container.internal.reference.ReferenceModel;
import org.apache.aries.cdi.container.internal.v2.component.ContainerComponent;
import org.osgi.service.cdi.annotations.ComponentScoped;
import org.osgi.service.cdi.annotations.Configuration;
import org.osgi.service.cdi.annotations.FactoryComponent;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.annotations.Service;
import org.osgi.service.cdi.annotations.SingleComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscoveryExtension implements Extension {

	public DiscoveryExtension(BeansModel beansModel, ContainerComponent containerComponent) {
		_beansModel = beansModel;
		_containerComponent = containerComponent;
	}

	void afterBeanDiscovery(@Observes AfterBeanDiscovery abd) {
		_beansModel.getErrors().stream().forEach(err ->
			abd.addDefinitionError(err)
		);
	}

	/*
	 * Process annotated classes to sync them up with the meta-model.
	 */
	<X> void processAnnotatedType(@Observes ProcessAnnotatedType<X> pat, BeanManager beanManager) {
		final AnnotatedType<X> at = pat.getAnnotatedType();

		Class<X> annotatedClass = at.getJavaClass();

		final String className = annotatedClass.getName();

		OSGiBean osgiBean = _beansModel.getOSGiBean(className);

		if (osgiBean == null) {
			return;
		}

		osgiBean.found(true);

//		if (checkIfBeanClassIsOSGiAnnotated(annotatedClass)) {
//			if (_log.isDebugEnabled()) {
//				_log.debug("CDIe - OSGi CDI annotations found. Clearing descriptor infos for {}", className);
//			}
//		}
	}

	void processInjectionPoint(@Observes ProcessInjectionPoint<?, ?> pip) {
		final InjectionPoint injectionPoint = pip.getInjectionPoint();

		Annotated annotated = injectionPoint.getAnnotated();

		Class<?> injectionPointClass = null;

		if (annotated instanceof AnnotatedParameter) {
			AnnotatedParameter<?> ap = (AnnotatedParameter<?>)annotated;

			Parameter javaParameter = ap.getJavaParameter();

			Executable declaringExecutable = javaParameter.getDeclaringExecutable();

			injectionPointClass = declaringExecutable.getDeclaringClass(); //TODO verify
		}
		else {
			AnnotatedField<?> af = (AnnotatedField<?>)annotated;

			injectionPointClass = af.getDeclaringType().getJavaClass();
		}

		String injectionPointClassName = injectionPointClass.getName();

		OSGiBean osgiBean = _beansModel.getOSGiBean(injectionPointClassName);

		if (osgiBean == null) {
			return;
		}

		Reference reference = annotated.getAnnotation(Reference.class);
		Configuration configuration = annotated.getAnnotation(Configuration.class);

		// Is it annotated with @Reference?

		if (reference != null) {
			processReference(pip, osgiBean, reference, configuration);

			return;
		}

		// Is it annotated with @Configuration?

		else if (configuration != null) {
			processConfiguration(pip, osgiBean);

			return;
		}
	}

	void proc(@Observes ProcessBean<?> pb) {
		Annotated annotated = null;
		Class<?> annotatedClass = null;

		if (pb instanceof ProcessManagedBean) {
			ProcessManagedBean<?> bean = (ProcessManagedBean<?>)pb;

			annotated = bean.getAnnotated();
			annotatedClass = bean.getAnnotatedBeanClass().getJavaClass();
		}
		else if (pb instanceof ProcessSessionBean) {
			ProcessSessionBean<?> bean = (ProcessSessionBean<?>)pb;

			annotated = bean.getAnnotated();
			annotatedClass = bean.getAnnotatedBeanClass().getJavaClass();
		}
		else if (pb instanceof ProcessProducerMethod) {
			ProcessProducerMethod<?, ?> producer = (ProcessProducerMethod<?, ?>)pb;

			annotated = producer.getAnnotated();
			annotatedClass = producer.getAnnotatedProducerMethod().getDeclaringType().getJavaClass();
		}
		else if (pb instanceof ProcessProducerField) {
			ProcessProducerField<?, ?> producer = (ProcessProducerField<?, ?>)pb;

			annotated = producer.getAnnotated();
			annotatedClass = producer.getAnnotatedProducerField().getDeclaringType().getJavaClass();
		}
		else if (pb instanceof ProcessSyntheticBean) {
			ProcessSyntheticBean<?> synthetic = (ProcessSyntheticBean<?>)pb;

			annotated = synthetic.getAnnotated();
			annotatedClass = synthetic.getBean().getBeanClass();
		}
		else {
			return;
		}

		String className = annotatedClass.getName();

		OSGiBean osgiBean = _beansModel.getOSGiBean(className);

		if (osgiBean == null) {
			return;
		}

		if (Optional.ofNullable(
				annotated.getAnnotation(SingleComponent.class)).isPresent()) {

			osgiBean.setComponent(new org.apache.aries.cdi.container.internal.v2.component.SingleComponent(className));
		}
		else if (Optional.ofNullable(
				annotated.getAnnotation(FactoryComponent.class)).isPresent()) {

			osgiBean.setComponent(new org.apache.aries.cdi.container.internal.v2.component.FactoryComponent(className));
		}
		else {
			osgiBean.setComponent(_containerComponent);
		}
	}

	/*
	void processObserverMethod(@Observes ProcessObserverMethod<ReferenceEvent<?>, ?> pom) {
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
*/
	private void processConfiguration(
		ProcessInjectionPoint<?, ?> pip,
		OSGiBean osgiBean) {

		InjectionPoint injectionPoint = pip.getInjectionPoint();

		ConfigurationModel configurationModel = new ConfigurationModel.Builder(
			injectionPoint.getType()
		).injectionPoint(
			injectionPoint
		).build();

		osgiBean.addConfiguration(configurationModel.toDTO());
	}

	private void processReference(
		ProcessInjectionPoint<?, ?> pip,
		OSGiBean osgiBean,
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
			).injectionPoint(
				injectionPoint
			).build();

			osgiBean.addReference(referenceModel.toDTO());
		}
		catch (IllegalArgumentException iae) {
			_log.error("CDIe - Component definition error on {}", injectionPoint, iae);

			pip.addDefinitionError(iae);
		}
	}

	/*
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
	 */

	static boolean checkIfBeanClassIsOSGiAnnotated(Class<?> annotatedClass) {
		// check for @SingleComponent

		if (Optional.ofNullable(
				annotatedClass.getAnnotation(SingleComponent.class)).isPresent()) {

			return true;
		}

		// check for @FactoryComponent

		if (Optional.ofNullable(
				annotatedClass.getAnnotation(FactoryComponent.class)).isPresent()) {

			return true;
		}

		// check for @ComponentScoped

		if (Optional.ofNullable(
				annotatedClass.getAnnotation(ComponentScoped.class)).isPresent()) {

			return true;
		}

		// check for @ComponentScoped on (producer) fields

		if (Arrays.stream(
				annotatedClass.getDeclaredFields()
			).filter(
				field -> Arrays.stream(
					field.getAnnotations()
				).filter(
					annotation -> ComponentScoped.class.equals(annotation.annotationType())
				).findFirst().isPresent()
			).findAny().isPresent()) {

			return true;
		}

		// check for @ComponentScoped on (producer) methods

		if (Arrays.stream(
				annotatedClass.getDeclaredMethods()
			).filter(
				method -> Arrays.stream(
					method.getAnnotations()
				).filter(
					annotation -> ComponentScoped.class.equals(annotation.annotationType())
				).findFirst().isPresent()
			).findAny().isPresent()) {

			return true;
		}

		// check for @Service

		if (Optional.ofNullable(
				annotatedClass.getAnnotation(Service.class)).isPresent()) {

			return true;
		}

		// check for @Service on implements

		if (Arrays.stream(
				annotatedClass.getAnnotatedInterfaces()
			).filter(
				annotatedType -> Objects.nonNull(annotatedType.getAnnotation(Service.class))
			).findAny().isPresent()) {

			return true;
		}

		// check for @Service on extends

		if (Stream.of(
				annotatedClass.getAnnotatedSuperclass()
			).filter(
				annotatedType -> Objects.nonNull(annotatedType.getAnnotation(Service.class))
			).findAny().isPresent()) {

			return true;
		}

		// check for @Service on (producer) fields

		if (Arrays.stream(
				annotatedClass.getDeclaredFields()
			).filter(
				field -> Arrays.stream(
					field.getAnnotations()
				).filter(
					annotation -> Service.class.equals(annotation.annotationType())
				).findFirst().isPresent()
			).findAny().isPresent()) {

			return true;
		}

		// check for @Service on (producer) methods

		if (Arrays.stream(
				annotatedClass.getDeclaredMethods()
			).filter(
				method -> Arrays.stream(
					method.getAnnotations()
				).filter(
					annotation -> Service.class.equals(annotation.annotationType())
				).findFirst().isPresent()
			).findAny().isPresent()) {

			return true;
		}

		Predicate<Annotation> hasAnnotations = annotation ->
			Configuration.class.isInstance(annotation) || Reference.class.isInstance(annotation);

		// check for @Configuration/@Reference on constructors

		if (Arrays.stream(
				annotatedClass.getDeclaredConstructors()
			).filter(
				ctor -> Arrays.stream(
					ctor.getParameterAnnotations()
				).flatMap(
					array -> Arrays.stream(array)
				).filter(
					hasAnnotations
				).findFirst().isPresent()
			).findAny().isPresent()) {

			return true;
		}

		// check for @Configuration/@Reference on fields

		if (Arrays.stream(
				annotatedClass.getDeclaredFields()
			).filter(
				field -> Arrays.stream(
					field.getAnnotations()
				).filter(
					hasAnnotations
				).findFirst().isPresent()
			).findAny().isPresent()) {

			return true;
		}

		// check for @Configuration/@Reference on methods

		if (Arrays.stream(
				annotatedClass.getDeclaredMethods()
			).filter(
				method -> Arrays.stream(
					method.getParameterAnnotations()
				).flatMap(
					array -> Arrays.stream(array)
				).filter(
					hasAnnotations
				).findFirst().isPresent()
			).findAny().isPresent()) {

			return true;
		}

		return false;
	}

	private static final Logger _log = LoggerFactory.getLogger(DiscoveryExtension.class);

	private final BeansModel _beansModel;
	private final ContainerComponent _containerComponent;

}
