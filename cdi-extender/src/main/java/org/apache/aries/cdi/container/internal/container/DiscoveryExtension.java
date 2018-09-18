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

package org.apache.aries.cdi.container.internal.container;

import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.DefinitionException;
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

import org.apache.aries.cdi.container.internal.model.BeansModel;
import org.apache.aries.cdi.container.internal.model.ComponentPropertiesModel;
import org.apache.aries.cdi.container.internal.model.ExtendedActivationTemplateDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedComponentTemplateDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedConfigurationTemplateDTO;
import org.apache.aries.cdi.container.internal.model.OSGiBean;
import org.apache.aries.cdi.container.internal.model.ReferenceModel;
import org.apache.aries.cdi.container.internal.model.ReferenceModel.Builder;
import org.apache.aries.cdi.container.internal.util.Maps;
import org.apache.aries.cdi.container.internal.util.Types;
import org.osgi.service.cdi.ComponentType;
import org.osgi.service.cdi.ConfigurationPolicy;
import org.osgi.service.cdi.MaximumCardinality;
import org.osgi.service.cdi.ServiceScope;
import org.osgi.service.cdi.annotations.ComponentProperties;
import org.osgi.service.cdi.annotations.ComponentScoped;
import org.osgi.service.cdi.annotations.FactoryComponent;
import org.osgi.service.cdi.annotations.PID;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.annotations.ServiceInstance;
import org.osgi.service.cdi.annotations.SingleComponent;
import org.osgi.service.cdi.reference.BindBeanServiceObjects;
import org.osgi.service.cdi.reference.BindService;
import org.osgi.service.cdi.reference.BindServiceReference;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;

public class DiscoveryExtension implements Extension {

	public DiscoveryExtension(ContainerState containerState) {
		_containerState = containerState;
		_beansModel = _containerState.beansModel();
		_containerTemplate = _containerState.containerDTO().template.components.get(0);
	}

	static Entry<Class<?>, Annotated> getBeanClassAndAnnotated(ProcessBean<?> pb) {
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
			annotated = pb.getAnnotated();
			annotatedClass = pb.getBean().getBeanClass();
		}

		return new SimpleEntry<>(annotatedClass, annotated);
	}

	static Class<?> getDeclaringClass(InjectionPoint injectionPoint) {
		Annotated annotated = injectionPoint.getAnnotated();

		Class<?> declaringClass = null;

		if (annotated instanceof AnnotatedParameter) {
			AnnotatedParameter<?> ap = (AnnotatedParameter<?>)annotated;

			Parameter javaParameter = ap.getJavaParameter();

			Executable executable = javaParameter.getDeclaringExecutable();

			declaringClass = executable.getDeclaringClass();
		}
		else {
			AnnotatedField<?> af = (AnnotatedField<?>)annotated;

			declaringClass = af.getDeclaringType().getJavaClass();
		}

		return declaringClass;
	}

	void afterBeanDiscovery(@Observes AfterBeanDiscovery abd, BeanManager beanManager) {
		_containerState.containerDTO().template.components.stream().filter(
			template -> template.type != ComponentType.CONTAINER
		).map(
			template -> (ExtendedComponentTemplateDTO)template
		).forEach(
			template -> {
				Set<Bean<?>> visited = new HashSet<>();
				scanComponentBean(template, template.bean, beanManager, visited);
			}
		);

		_beansModel.getOSGiBeans().stream().forEach(
			osgiBean -> {
				if (!osgiBean.found()) {
					abd.addDefinitionError(
						new DefinitionException(
							String.format(
								"Did not find bean for <cdi:bean class=\"%s\">",
								osgiBean.getBeanClass())));
				}
			}
		);
	}

	<X> void processAnnotatedType(@Observes ProcessAnnotatedType<X> pat, BeanManager beanManager) {
		final AnnotatedType<X> at = pat.getAnnotatedType();

		Class<X> annotatedClass = at.getJavaClass();

		final String className = annotatedClass.getName();

		OSGiBean osgiBean = _beansModel.getOSGiBean(className);

		if (osgiBean == null) {
			return;
		}

		osgiBean.found(true);
	}

	@SuppressWarnings("rawtypes")
	void processBean(@Observes ProcessBean<?> pb) {
		Entry<Class<?>, Annotated> beanClassAndAnnotated = getBeanClassAndAnnotated(pb);

		final Class<?> annotatedClass = beanClassAndAnnotated.getKey();

		String className = annotatedClass.getName();

		OSGiBean osgiBean = _beansModel.getOSGiBean(className);

		if (osgiBean == null) {
			return;
		}

		osgiBean.found(true);

		final Annotated annotated = beanClassAndAnnotated.getValue();

		try {
			List<Class<?>> serviceTypes = Types.collectServiceTypes(annotated);

			if ((annotated instanceof AnnotatedType) &&
				Optional.ofNullable(
					annotated.getAnnotation(SingleComponent.class)).isPresent()) {

				ExtendedComponentTemplateDTO componentTemplate = new ExtendedComponentTemplateDTO();
				componentTemplate.activations = new CopyOnWriteArrayList<>();

				ExtendedActivationTemplateDTO activationTemplate = new ExtendedActivationTemplateDTO();
				activationTemplate.declaringClass = annotatedClass;
				activationTemplate.properties = Collections.emptyMap();
				activationTemplate.scope = getScope(annotated);
				activationTemplate.serviceClasses = serviceTypes.stream().map(
					st -> st.getName()
				).collect(Collectors.toList());

				componentTemplate.activations.add(activationTemplate);

				componentTemplate.bean = pb.getBean();
				componentTemplate.beans = new CopyOnWriteArrayList<>();
				componentTemplate.configurations = new CopyOnWriteArrayList<>();
				componentTemplate.name = pb.getBean().getName();
				componentTemplate.properties = Maps.componentProperties(annotated);
				componentTemplate.references = new CopyOnWriteArrayList<>();
				componentTemplate.type = ComponentType.SINGLE;

				annotated.getAnnotations(PID.class).stream().forEach(
					PID -> {
						ExtendedConfigurationTemplateDTO configurationTemplate = new ExtendedConfigurationTemplateDTO();

						configurationTemplate.declaringClass = annotatedClass;
						configurationTemplate.maximumCardinality = MaximumCardinality.ONE;
						configurationTemplate.pid = Optional.of(PID.value()).map(
							s -> {
								if (s.equals("$") || s.equals("")) {
									return componentTemplate.name;
								}
								return s;
							}
						).orElse(componentTemplate.name);

						if (PID.value().equals("$") || PID.value().equals("")) {
							configurationTemplate.pid = componentTemplate.name;
						}
						else {
							configurationTemplate.pid = PID.value();
						}

						configurationTemplate.policy = PID.policy();

						componentTemplate.configurations.add(configurationTemplate);
					}
				);

				if (componentTemplate.configurations.isEmpty()) {
					ExtendedConfigurationTemplateDTO configurationTemplate = new ExtendedConfigurationTemplateDTO();

					configurationTemplate.declaringClass = annotatedClass;
					configurationTemplate.maximumCardinality = MaximumCardinality.ONE;
					configurationTemplate.pid = componentTemplate.name;
					configurationTemplate.policy = ConfigurationPolicy.OPTIONAL;

					componentTemplate.configurations.add(configurationTemplate);
				}

				componentTemplate.beans.add(className);

				_containerState.containerDTO().template.components.add(componentTemplate);

				osgiBean.setComponent(componentTemplate);
			}
			else if ((annotated instanceof AnnotatedType) &&
					Optional.ofNullable(
					annotated.getAnnotation(FactoryComponent.class)).isPresent()) {

				ExtendedComponentTemplateDTO componentTemplate = new ExtendedComponentTemplateDTO();
				componentTemplate.activations = new CopyOnWriteArrayList<>();

				ExtendedActivationTemplateDTO activationTemplate = new ExtendedActivationTemplateDTO();
				activationTemplate.declaringClass = annotatedClass;
				activationTemplate.properties = Collections.emptyMap();
				activationTemplate.scope = getScope(annotated);
				activationTemplate.serviceClasses = serviceTypes.stream().map(
					st -> st.getName()
				).collect(Collectors.toList());

				componentTemplate.activations.add(activationTemplate);

				componentTemplate.bean = pb.getBean();
				componentTemplate.beans = new CopyOnWriteArrayList<>();
				componentTemplate.configurations = new CopyOnWriteArrayList<>();
				componentTemplate.name = pb.getBean().getName();
				componentTemplate.properties = Maps.componentProperties(annotated);
				componentTemplate.references = new CopyOnWriteArrayList<>();
				componentTemplate.type = ComponentType.FACTORY;

				annotated.getAnnotations(PID.class).stream().forEach(
					PID -> {
						ExtendedConfigurationTemplateDTO configurationTemplate = new ExtendedConfigurationTemplateDTO();

						configurationTemplate.declaringClass = annotatedClass;
						configurationTemplate.maximumCardinality = MaximumCardinality.ONE;
						configurationTemplate.pid = Optional.of(PID.value()).map(
							s -> {
								if (s.equals("$") || s.equals("")) {
									return componentTemplate.name;
								}
								return s;
							}
						).orElse(componentTemplate.name);

						configurationTemplate.policy = PID.policy();

						componentTemplate.configurations.add(configurationTemplate);
					}
				);

				ExtendedConfigurationTemplateDTO configurationTemplate = new ExtendedConfigurationTemplateDTO();

				configurationTemplate.declaringClass = annotatedClass;
				configurationTemplate.maximumCardinality = MaximumCardinality.MANY;
				configurationTemplate.pid = Optional.ofNullable(
					annotated.getAnnotation(FactoryComponent.class)
				).map(fc -> {
					if (fc.value().equals("$") || fc.value().equals("")) {
						return componentTemplate.name;
					}
					return fc.value();
				}).orElse(componentTemplate.name);
				configurationTemplate.policy = ConfigurationPolicy.REQUIRED;

				componentTemplate.configurations.add(configurationTemplate);
				componentTemplate.beans.add(className);

				_containerState.containerDTO().template.components.add(componentTemplate);

				osgiBean.setComponent(componentTemplate);
			}
			else if ((annotated instanceof AnnotatedType) &&
					Optional.ofNullable(
					annotated.getAnnotation(ComponentScoped.class)).isPresent()) {

				// Explicitly ignore this case
			}
			else {
				if (!_containerTemplate.beans.contains(className)) {
					_containerTemplate.beans.add(className);
				}

				if (!serviceTypes.isEmpty()) {
					Class<? extends Annotation> scope = pb.getBean().getScope();
					if (!scope.equals(ApplicationScoped.class) &&
						!scope.equals(Dependent.class)) {

						pb.addDefinitionError(
							new IllegalStateException(
								String.format(
									"@Service can only be used on @ApplicationScoped, @Dependent, @SingleComponent, and @FactoryComponent: %s",
									pb.getBean())));
						return;
					}

					ExtendedActivationTemplateDTO activationTemplate = new ExtendedActivationTemplateDTO();
					activationTemplate.cdiScope = scope;
					activationTemplate.declaringClass = annotatedClass;
					if (pb instanceof ProcessProducerField) {
						activationTemplate.producer = ((ProcessProducerField) pb).getAnnotatedProducerField();
					}
					else if (pb instanceof ProcessProducerMethod) {
						activationTemplate.producer = ((ProcessProducerMethod) pb).getAnnotatedProducerMethod();
					}
					activationTemplate.properties = Maps.componentProperties(annotated);
					activationTemplate.scope = getScope(annotated);
					activationTemplate.serviceClasses = serviceTypes.stream().map(
						st -> st.getName()
					).collect(Collectors.toList());

					_containerTemplate.activations.add(activationTemplate);
				}

				osgiBean.setComponent(_containerTemplate);
			}
		}
		catch (Exception e) {
			pb.addDefinitionError(e);
		}
	}

	void processBindObject(@Observes ProcessInjectionPoint<?, BindService<?>> pip) {
		InjectionPoint injectionPoint = pip.getInjectionPoint();

		Class<?> declaringClass = getDeclaringClass(injectionPoint);

		String className = declaringClass.getName();

		OSGiBean osgiBean = _beansModel.getOSGiBean(className);

		if (osgiBean == null) {
			return;
		}

		Annotated annotated = injectionPoint.getAnnotated();

		Builder builder = null;

		if (annotated instanceof AnnotatedParameter) {
			builder = new ReferenceModel.Builder((AnnotatedParameter<?>)annotated);
		}
		else {
			builder = new ReferenceModel.Builder((AnnotatedField<?>)annotated);
		}

		try {
			ReferenceModel referenceModel = builder.type(injectionPoint.getType()).build();

			osgiBean.addReference(referenceModel.toDTO());
		}
		catch (Exception e) {
			_containerState.error(e);
		}
	}

	void processBindServiceObjects(@Observes ProcessInjectionPoint<?, BindBeanServiceObjects<?>> pip) {
		InjectionPoint injectionPoint = pip.getInjectionPoint();

		Class<?> declaringClass = getDeclaringClass(injectionPoint);

		String className = declaringClass.getName();

		OSGiBean osgiBean = _beansModel.getOSGiBean(className);

		if (osgiBean == null) {
			return;
		}

		Annotated annotated = injectionPoint.getAnnotated();

		Builder builder = null;

		if (annotated instanceof AnnotatedParameter) {
			builder = new ReferenceModel.Builder((AnnotatedParameter<?>)annotated);
		}
		else {
			builder = new ReferenceModel.Builder((AnnotatedField<?>)annotated);
		}

		try {
			ReferenceModel referenceModel = builder.type(injectionPoint.getType()).build();

			osgiBean.addReference(referenceModel.toDTO());
		}
		catch (Exception e) {
			_containerState.error(e);
		}
	}

	void processBindServiceReference(@Observes ProcessInjectionPoint<?, BindServiceReference<?>> pip) {
		InjectionPoint injectionPoint = pip.getInjectionPoint();

		Class<?> declaringClass = getDeclaringClass(injectionPoint);

		String className = declaringClass.getName();

		OSGiBean osgiBean = _beansModel.getOSGiBean(className);

		if (osgiBean == null) {
			return;
		}

		Annotated annotated = injectionPoint.getAnnotated();

		Builder builder = null;

		if (annotated instanceof AnnotatedParameter) {
			builder = new ReferenceModel.Builder((AnnotatedParameter<?>)annotated);
		}
		else {
			builder = new ReferenceModel.Builder((AnnotatedField<?>)annotated);
		}

		try {
			ReferenceModel referenceModel = builder.type(injectionPoint.getType()).build();

			osgiBean.addReference(referenceModel.toDTO());
		}
		catch (Exception e) {
			_containerState.error(e);
		}
	}

	void processInjectionPoint(@Observes ProcessInjectionPoint<?, ?> pip) {
		InjectionPoint injectionPoint = pip.getInjectionPoint();

		Class<?> declaringClass = getDeclaringClass(injectionPoint);

		String className = declaringClass.getName();

		OSGiBean osgiBean = _beansModel.getOSGiBean(className);

		if (osgiBean == null) {
			return;
		}

		Annotated annotated = injectionPoint.getAnnotated();
		Reference reference = annotated.getAnnotation(Reference.class);
		ComponentProperties componentProperties = annotated.getAnnotation(ComponentProperties.class);

		if (reference != null) {
			if (componentProperties != null) {
				_containerState.error(
					new IllegalArgumentException(
						String.format(
							"Cannot use @Reference and @Configuration on the same injection point {}",
							injectionPoint))
				);

				return;
			}

			Builder builder = null;

			if (annotated instanceof AnnotatedParameter) {
				builder = new ReferenceModel.Builder((AnnotatedParameter<?>)annotated);
			}
			else {
				builder = new ReferenceModel.Builder((AnnotatedField<?>)annotated);
			}

			try {
				ReferenceModel referenceModel = builder.type(injectionPoint.getType()).build();

				osgiBean.addReference(referenceModel.toDTO());
			}
			catch (Exception e) {
				_containerState.error(e);
			}
		}
		else if (componentProperties != null) {
			try {
				ComponentPropertiesModel configurationModel = new ComponentPropertiesModel.Builder(
					injectionPoint.getType()
				).declaringClass(
					declaringClass
				).injectionPoint(
					injectionPoint
				).build();

				osgiBean.addConfiguration(configurationModel.toDTO());
			}
			catch (Exception e) {
				_containerState.error(e);
			}
		}
	}

	ServiceScope getScope(Annotated annotated) {
		ServiceInstance serviceInstance = annotated.getAnnotation(ServiceInstance.class);

		if (serviceInstance != null) {
			return serviceInstance.value();
		}

		return ServiceScope.SINGLETON;
	}

	void scanComponentBean(
		ExtendedComponentTemplateDTO template,
		Bean<?> bean,
		BeanManager beanManager,
		Set<Bean<?>> visited) {

		if (visited.contains(bean)) {
			return;
		}

		visited.add(bean);

		Class<?> beanClass = bean.getBeanClass();

		String className = beanClass.getName();

		OSGiBean osgiBean = _beansModel.getOSGiBean(className);

		ComponentTemplateDTO currentTemplate = osgiBean.getComponent();

		if (currentTemplate == null) {
			osgiBean.setComponent(template);
		}
		else if (!currentTemplate.equals(template)) {
			throw new IllegalStateException("Something is wrong here");
		}

		if (!template.beans.contains(className)) {
			template.beans.add(className);
		}

		for (InjectionPoint injectionPoint : bean.getInjectionPoints()) {
			if ((injectionPoint.getAnnotated().getAnnotation(ComponentProperties.class) != null) ||
				(injectionPoint.getAnnotated().getAnnotation(Reference.class) != null)) {

				continue;
			}

			Set<Bean<?>> beans = beanManager.getBeans(
				injectionPoint.getType(),
				injectionPoint.getQualifiers().toArray(new Annotation[0]));

			Bean<?> next = beanManager.resolve(beans);

			if ((next == null) || next.getScope() != ComponentScoped.class) {
				continue;
			}

			scanComponentBean(template, next, beanManager, visited);
		}
	}

	private final BeansModel _beansModel;
	private final ComponentTemplateDTO _containerTemplate;
	private final ContainerState _containerState;

}
