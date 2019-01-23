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
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.ProcessProducerField;
import javax.enterprise.inject.spi.ProcessProducerMethod;

import org.apache.aries.cdi.container.internal.model.BeansModel;
import org.apache.aries.cdi.container.internal.model.ComponentPropertiesModel;
import org.apache.aries.cdi.container.internal.model.ExtendedActivationTemplateDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedComponentTemplateDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedConfigurationTemplateDTO;
import org.apache.aries.cdi.container.internal.model.OSGiBean;
import org.apache.aries.cdi.container.internal.model.ReferenceModel;
import org.apache.aries.cdi.container.internal.model.ReferenceModel.Builder;
import org.apache.aries.cdi.container.internal.util.Annotates;
import org.osgi.service.cdi.ComponentType;
import org.osgi.service.cdi.ConfigurationPolicy;
import org.osgi.service.cdi.MaximumCardinality;
import org.osgi.service.cdi.ServiceScope;
import org.osgi.service.cdi.annotations.ComponentProperties;
import org.osgi.service.cdi.annotations.ComponentScoped;
import org.osgi.service.cdi.annotations.FactoryComponent;
import org.osgi.service.cdi.annotations.PID;
import org.osgi.service.cdi.annotations.Reference;
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

	<X> void processAnnotatedType(@Observes ProcessAnnotatedType<X> pat) {
		Class<X> declaringClass = Annotates.declaringClass(pat.getAnnotatedType());

		final String className = declaringClass.getName();

		OSGiBean osgiBean = _beansModel.getOSGiBean(className);

		if (osgiBean == null) {
			return;
		}

		osgiBean.found(true);
	}

	<X> void processBindObject(@Observes ProcessInjectionPoint<X, BindService<?>> pip) {
		processInjectionPoint0(pip, true);
	}

	<X> void processBindServiceObjects(@Observes ProcessInjectionPoint<X, BindBeanServiceObjects<?>> pip) {
		processInjectionPoint0(pip, true);
	}

	<X> void processBindServiceReference(@Observes ProcessInjectionPoint<X, BindServiceReference<?>> pip) {
		processInjectionPoint0(pip, true);
	}

	<X, T> void processInjectionPoint(@Observes ProcessInjectionPoint<X, T> pip) {
		processInjectionPoint0(pip, false);
	}

	<X, T> void processInjectionPoint0(ProcessInjectionPoint<X, T> pip, boolean special) {
		InjectionPoint injectionPoint = pip.getInjectionPoint();

		Annotated annotated = injectionPoint.getAnnotated();

		Class<X> declaringClass = Annotates.declaringClass(annotated);

		String className = declaringClass.getName();

		OSGiBean osgiBean = _beansModel.getOSGiBean(className);

		if (osgiBean == null) {
			return;
		}

		if (special) {
			doSpecial(osgiBean, annotated, injectionPoint.getType());
		}
		else {
			doOther(osgiBean, declaringClass, annotated, injectionPoint);
		}
	}

	<X> void processBean(@Observes ProcessBean<X> pb) {
		final Class<X> declaringClass = Annotates.declaringClass(pb);

		String className = declaringClass.getName();

		OSGiBean osgiBean = _beansModel.getOSGiBean(className);

		if (osgiBean == null) {
			return;
		}

		osgiBean.found(true);

		final Annotated annotated = pb.getAnnotated();

		try {
			List<String> serviceTypes = Annotates.serviceClassNames(annotated);
			Map<String, Object> componentProperties = Annotates.componentProperties(annotated);
			ServiceScope serviceScope = Annotates.serviceScope(annotated);

			if (annotated.isAnnotationPresent(SingleComponent.class)) {
				doSingleComponent(osgiBean, declaringClass, annotated, pb.getBean(), serviceTypes, serviceScope, componentProperties);
			}
			else if (annotated.isAnnotationPresent(FactoryComponent.class)) {
				doFactoryComponent(osgiBean, declaringClass, annotated, pb.getBean(), serviceTypes, serviceScope, componentProperties);
			}
			else if (annotated.isAnnotationPresent(ComponentScoped.class)) {
				// Explicitly ignore this case
			}
			else {
				doContainerBean(osgiBean, declaringClass, annotated, pb, pb.getBean().getScope(), serviceTypes, serviceScope, componentProperties);
			}
		}
		catch (Exception e) {
			pb.addDefinitionError(e);
		}
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
								"Did not find bean for %s",
								osgiBean.getBeanClass())));
				}
			}
		);
	}

	void doComponentProperties(OSGiBean osgiBean, Class<?> declaringClass, InjectionPoint injectionPoint) {
		try {
			ComponentPropertiesModel configurationModel = new ComponentPropertiesModel.Builder(
				injectionPoint.getType()
			).declaringClass(
				declaringClass
			).injectionPoint(
				injectionPoint
			).build();

			osgiBean.addConfiguration(_containerState, configurationModel.toDTO());
		}
		catch (Exception e) {
			_containerState.error(e);
		}
	}

	void doContainerBean(OSGiBean osgiBean, Class<?> declaringClass, Annotated annotated, ProcessBean<?> pb, Class<? extends Annotation> scope, List<String> serviceTypeNames, ServiceScope serviceScope, Map<String, Object> componentProperties) {
		String className = declaringClass.getName();

		if (!_containerTemplate.beans.contains(className)) {
			_containerTemplate.beans.add(className);
		}

		if (!serviceTypeNames.isEmpty()) {
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
			activationTemplate.declaringClass = declaringClass;
			if (pb instanceof ProcessProducerField) {
				activationTemplate.producer = ((ProcessProducerField<?, ?>) pb).getAnnotatedProducerField();
			}
			else if (pb instanceof ProcessProducerMethod) {
				activationTemplate.producer = ((ProcessProducerMethod<?, ?>) pb).getAnnotatedProducerMethod();
			}
			activationTemplate.properties = componentProperties;
			activationTemplate.scope = serviceScope;
			activationTemplate.serviceClasses = serviceTypeNames;

			_containerTemplate.activations.add(activationTemplate);
		}

		osgiBean.setComponent(_containerState, _containerTemplate);
	}

	void doFactoryComponent(OSGiBean osgiBean, Class<?> declaringClass, Annotated annotated, Bean<?> bean, List<String> serviceTypeNames, ServiceScope serviceScope, Map<String, Object> componentProperties) {
		ExtendedComponentTemplateDTO componentTemplate = new ExtendedComponentTemplateDTO();
		componentTemplate.activations = new CopyOnWriteArrayList<>();

		ExtendedActivationTemplateDTO activationTemplate = new ExtendedActivationTemplateDTO();
		activationTemplate.declaringClass = declaringClass;
		activationTemplate.properties = Collections.emptyMap();
		activationTemplate.scope = serviceScope;
		activationTemplate.serviceClasses = serviceTypeNames;

		componentTemplate.activations.add(activationTemplate);

		componentTemplate.bean = bean;
		componentTemplate.beans = new CopyOnWriteArrayList<>();
		componentTemplate.configurations = new CopyOnWriteArrayList<>();
		componentTemplate.name = bean.getName();
		componentTemplate.properties = componentProperties;
		componentTemplate.references = new CopyOnWriteArrayList<>();
		componentTemplate.type = ComponentType.FACTORY;

		annotated.getAnnotations(PID.class).stream().forEach(
			PID -> {
				ExtendedConfigurationTemplateDTO configurationTemplate = new ExtendedConfigurationTemplateDTO();

				configurationTemplate.declaringClass = declaringClass;
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

		configurationTemplate.declaringClass = declaringClass;
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
		componentTemplate.beans.add(declaringClass.getName());

		_containerState.containerDTO().template.components.add(componentTemplate);

		osgiBean.setComponent(_containerState, componentTemplate);
	}

	void doOther(OSGiBean osgiBean, Class<?> declaringClass, Annotated annotated, InjectionPoint injectionPoint) {
		Reference reference = annotated.getAnnotation(Reference.class);
		ComponentProperties componentProperties = annotated.getAnnotation(ComponentProperties.class);

		if (reference != null) {
			doReference(osgiBean, annotated, injectionPoint, reference, componentProperties);
		}
		else if (componentProperties != null) {
			doComponentProperties(osgiBean, declaringClass, injectionPoint);
		}
	}

	void doReference(OSGiBean osgiBean, Annotated annotated, InjectionPoint injectionPoint, Reference reference, ComponentProperties componentProperties) {
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

	void doSingleComponent(OSGiBean osgiBean, Class<?> declaringClass, Annotated annotated, Bean<?> bean, List<String> serviceTypes, ServiceScope serviceScope, Map<String, Object> componentProperties) {
		ExtendedComponentTemplateDTO componentTemplate = new ExtendedComponentTemplateDTO();
		componentTemplate.activations = new CopyOnWriteArrayList<>();

		ExtendedActivationTemplateDTO activationTemplate = new ExtendedActivationTemplateDTO();
		activationTemplate.declaringClass = declaringClass;
		activationTemplate.properties = Collections.emptyMap();
		activationTemplate.scope = serviceScope;
		activationTemplate.serviceClasses = serviceTypes;

		componentTemplate.activations.add(activationTemplate);

		componentTemplate.bean = bean;
		componentTemplate.beans = new CopyOnWriteArrayList<>();
		componentTemplate.configurations = new CopyOnWriteArrayList<>();
		componentTemplate.name = bean.getName();
		componentTemplate.properties = componentProperties;
		componentTemplate.references = new CopyOnWriteArrayList<>();
		componentTemplate.type = ComponentType.SINGLE;

		annotated.getAnnotations(PID.class).stream().forEach(
			PID -> {
				ExtendedConfigurationTemplateDTO configurationTemplate = new ExtendedConfigurationTemplateDTO();

				configurationTemplate.declaringClass = declaringClass;
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

			configurationTemplate.declaringClass = declaringClass;
			configurationTemplate.maximumCardinality = MaximumCardinality.ONE;
			configurationTemplate.pid = componentTemplate.name;
			configurationTemplate.policy = ConfigurationPolicy.OPTIONAL;

			componentTemplate.configurations.add(configurationTemplate);
		}

		componentTemplate.beans.add(declaringClass.getName());

		_containerState.containerDTO().template.components.add(componentTemplate);

		osgiBean.setComponent(_containerState, componentTemplate);
	}

	void doSpecial(OSGiBean osgiBean, Annotated annotated, Type injectionPointType) {
		Builder builder = null;

		if (annotated instanceof AnnotatedParameter) {
			builder = new ReferenceModel.Builder((AnnotatedParameter<?>)annotated);
		}
		else {
			builder = new ReferenceModel.Builder((AnnotatedField<?>)annotated);
		}

		try {
			ReferenceModel referenceModel = builder.type(injectionPointType).build();

			osgiBean.addReference(referenceModel.toDTO());
		}
		catch (Exception e) {
			_containerState.error(e);
		}
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
			osgiBean.setComponent(_containerState, template);
		}
		else if (!currentTemplate.equals(template)) {
			throw new IllegalStateException("Something is wrong here");
		}

		if (!template.beans.contains(className)) {
			template.beans.add(className);
		}

		for (InjectionPoint injectionPoint : bean.getInjectionPoints()) {
			if (injectionPoint.getAnnotated().isAnnotationPresent(ComponentProperties.class) ||
				injectionPoint.getAnnotated().isAnnotationPresent(Reference.class)) {

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
