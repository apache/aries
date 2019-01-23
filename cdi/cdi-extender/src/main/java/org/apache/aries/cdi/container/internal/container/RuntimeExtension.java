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

import static javax.interceptor.Interceptor.Priority.PLATFORM_AFTER;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.ProcessProducerField;
import javax.enterprise.inject.spi.ProcessProducerMethod;
import javax.enterprise.inject.spi.Producer;
import javax.enterprise.inject.spi.ProducerFactory;

import org.apache.aries.cdi.container.internal.bean.ComponentPropertiesBean;
import org.apache.aries.cdi.container.internal.bean.ReferenceBean;
import org.apache.aries.cdi.container.internal.container.Op.Mode;
import org.apache.aries.cdi.container.internal.container.Op.Type;
import org.apache.aries.cdi.container.internal.model.ComponentPropertiesModel;
import org.apache.aries.cdi.container.internal.model.ExtendedActivationDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedActivationTemplateDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedComponentInstanceDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedComponentTemplateDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedConfigurationTemplateDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedReferenceDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedReferenceTemplateDTO;
import org.apache.aries.cdi.container.internal.model.FactoryComponent;
import org.apache.aries.cdi.container.internal.model.OSGiBean;
import org.apache.aries.cdi.container.internal.model.ReferenceModel;
import org.apache.aries.cdi.container.internal.model.SingleComponent;
import org.apache.aries.cdi.container.internal.util.Annotates;
import org.apache.aries.cdi.container.internal.util.SRs;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cdi.CDIConstants;
import org.osgi.service.cdi.ComponentType;
import org.osgi.service.cdi.ServiceScope;
import org.osgi.service.cdi.annotations.ComponentProperties;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.reference.BindBeanServiceObjects;
import org.osgi.service.cdi.reference.BindService;
import org.osgi.service.cdi.reference.BindServiceReference;
import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.service.cdi.runtime.dto.template.ActivationTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;
import org.osgi.service.log.Logger;
import org.osgi.util.promise.Promise;

public class RuntimeExtension implements Extension {

	public RuntimeExtension(
		ContainerState containerState,
		ConfigurationListener.Builder configurationBuilder,
		SingleComponent.Builder singleBuilder,
		FactoryComponent.Builder factoryBuilder) {

		_containerState = containerState;

		_log = _containerState.containerLogs().getLogger(getClass());
		_log.debug(l -> l.debug("CCR RuntimeExtension {}", containerState.bundle()));

		_configurationBuilder = configurationBuilder;
		_singleBuilder = singleBuilder;
		_factoryBuilder = factoryBuilder;
		_containerTemplate = _containerState.containerDTO().template.components.get(0);
	}

	void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd) {
		bbd.addQualifier(org.osgi.service.cdi.annotations.ComponentProperties.class);
		bbd.addQualifier(org.osgi.service.cdi.annotations.MinimumCardinality.class);
		bbd.addQualifier(org.osgi.service.cdi.annotations.PID.class);
		bbd.addQualifier(org.osgi.service.cdi.annotations.PrototypeRequired.class);
		bbd.addQualifier(org.osgi.service.cdi.annotations.Reference.class);
		bbd.addQualifier(org.osgi.service.cdi.annotations.Reluctant.class);
		bbd.addQualifier(org.osgi.service.cdi.annotations.Service.class);
		bbd.addQualifier(org.osgi.service.cdi.annotations.ServiceInstance.class);
		bbd.addScope(org.osgi.service.cdi.annotations.ComponentScoped.class, false, false);
		bbd.addStereotype(org.osgi.service.cdi.annotations.FactoryComponent.class);
		bbd.addStereotype(org.osgi.service.cdi.annotations.SingleComponent.class);
	}

	void processBindObject(@Observes ProcessInjectionPoint<?, BindService<?>> pip) {
		processInjectionPoint0(pip, true);
	}

	void processBindServiceObjects(@Observes ProcessInjectionPoint<?, BindBeanServiceObjects<?>> pip) {
		processInjectionPoint0(pip, true);
	}

	void processBindServiceReference(@Observes ProcessInjectionPoint<?, BindServiceReference<?>> pip) {
		processInjectionPoint0(pip, true);
	}

	void processInjectionPoint(@Observes ProcessInjectionPoint<?, ?> pip) {
		processInjectionPoint0(pip, false);
	}

	<X> void processBean(@Observes ProcessBean<X> pb) {
		final Class<X> declaringClass = Annotates.declaringClass(pb);

		String className = declaringClass.getName();

		OSGiBean osgiBean = _containerState.beansModel().getOSGiBean(className);

		if (osgiBean == null) {
			return;
		}

		final Annotated annotated = pb.getAnnotated();

		try {
			List<String> serviceTypes = Annotates.serviceClassNames(annotated);
			Map<String, Object> componentProperties = Annotates.componentProperties(annotated);
			ServiceScope serviceScope = Annotates.serviceScope(annotated);

			if (annotated.isAnnotationPresent(org.osgi.service.cdi.annotations.SingleComponent.class) ||
				annotated.isAnnotationPresent(org.osgi.service.cdi.annotations.FactoryComponent.class)) {

				ActivationTemplateDTO activationTemplate = osgiBean.getComponent().activations.get(0);
				activationTemplate.scope = serviceScope;
				activationTemplate.serviceClasses = serviceTypes;
				osgiBean.getComponent().properties = componentProperties;
			}
			else if (annotated.isAnnotationPresent(org.osgi.service.cdi.annotations.ComponentScoped.class)) {
				// Explicitly ignore this case
			}
			else if (!serviceTypes.isEmpty()) {
				ExtendedActivationTemplateDTO activationTemplate = _containerTemplate.activations.stream().map(
					ExtendedActivationTemplateDTO.class::cast
				).filter(
					at -> at.declaringClass.equals(declaringClass)
				).findFirst().orElseGet(
					() -> {
						ExtendedActivationTemplateDTO at = new ExtendedActivationTemplateDTO();
						at.cdiScope = pb.getBean().getScope();
						at.declaringClass = declaringClass;
						if (pb instanceof ProcessProducerField) {
							at.producer = ((ProcessProducerField<?, ?>) pb).getAnnotatedProducerField();
						}
						else if (pb instanceof ProcessProducerMethod) {
							at.producer = ((ProcessProducerMethod<?, ?>) pb).getAnnotatedProducerMethod();
						}
						_containerTemplate.activations.add(at);
						return at;
					}
				);

				activationTemplate.properties = componentProperties;
				activationTemplate.scope = serviceScope;
				activationTemplate.serviceClasses = serviceTypes;
			}
		}
		catch (Exception e) {
			pb.addDefinitionError(e);
		}
	}

	void afterBeanDiscovery(@Observes AfterBeanDiscovery abd, BeanManager bm) {
		abd.addContext(_containerState.componentContext());

		_containerState.containerDTO().template.components.forEach(
			ct -> addBeans(ct, abd, bm)
		);
	}

	void afterDeploymentValidation(
		@Observes @Priority(PLATFORM_AFTER + 100) AfterDeploymentValidation adv,
		BeanManager bm) {

		_log.debug(l -> l.debug("CCR AfterDeploymentValidation on {}", _containerState.bundle()));

		_containerState.beanManager(bm);

		ComponentDTO componentDTO = _containerState.containerDTO().components.get(0);

		_containerState.submit(
			Op.of(Mode.OPEN, Type.CONTAINER_PUBLISH_SERVICES, _containerState.id()),
			() -> registerServices(componentDTO, bm)
		).then(
			s -> initComponents()
		).then(s -> {
				Dictionary<String, Object> properties = new Hashtable<>();
				properties.put(CDIConstants.CDI_CONTAINER_ID, _containerState.id());
				properties.put(Constants.SERVICE_DESCRIPTION, "Aries CDI - BeanManager for " + _containerState.bundle());
				properties.put(Constants.SERVICE_VENDOR, "Apache Software Foundation");

				registerService(
					new String[] {BeanManager.class.getName()}, bm,
					properties);

				return s;
			}
		);
	}

	void beforeShutdown(@Observes BeforeShutdown bs) {
		_log.debug(l -> l.debug("CCR BeforeShutdown on {}", _containerState.bundle()));

		_containerState.beanManager(null);

		_configurationListeners.removeIf(
			cl -> {
				_containerState.submit(cl.closeOp(), cl::close).onFailure(
					f -> {
						_log.error(l -> l.error("CCR Error while closing configuration listener {} on {}", cl, _containerState.bundle(), f));
					}
				);

				return true;
			}
		);

		_registrations.removeIf(
			r -> {
				try {
					r.unregister();
				}
				catch (Exception e) {
					_log.error(l -> l.error("CCR Error while unregistring {} on {}", r, _containerState.bundle(), e));
				}
				return true;
			}
		);
	}

	private void addBeans(ComponentTemplateDTO componentTemplate, AfterBeanDiscovery abd, BeanManager bm) {
		ComponentDTO componentDTO = _containerState.containerDTO().components.get(0);

		componentTemplate.references.stream().map(ExtendedReferenceTemplateDTO.class::cast).forEach(
			t -> {
				ReferenceBean bean = t.bean;
				bean.setBeanManager(bm);
				if (componentTemplate.type == ComponentType.CONTAINER) {
					componentDTO.instances.get(0).references.stream().filter(
						r -> r.template == t
					).findFirst().map(
						ExtendedReferenceDTO.class::cast
					).ifPresent(
						r -> bean.setReferenceDTO(r)
					);
				}

				_log.debug(l -> l.debug("CCR Adding synthetic bean {} on {}", bean, _containerState.bundle()));

				abd.addBean(bean);
			}
		);

		componentTemplate.configurations.stream().map(ExtendedConfigurationTemplateDTO.class::cast).filter(
			t -> Objects.nonNull(t.injectionPointType)
		).forEach(
			t -> {
				ComponentPropertiesBean bean = t.bean;

				_log.debug(l -> l.debug("CCR Adding synthetic bean {} on {}", bean, _containerState.bundle()));

				abd.addBean(bean);
			}
		);
	}

	@SuppressWarnings("unchecked")
	private Producer<Object> createProducer(Object producerObject, Bean<Object> bean, BeanManager bm) {
		ProducerFactory<Object> producerFactory = null;
		if (producerObject instanceof AnnotatedField)
			producerFactory = bm.getProducerFactory((AnnotatedField<Object>)producerObject, bean);
		else if (producerObject instanceof AnnotatedMethod)
			producerFactory = bm.getProducerFactory((AnnotatedMethod<Object>)producerObject, bean);

		if (producerFactory == null)
			return null;

		return producerFactory.createProducer(bean);
	}

	private Promise<Boolean> initComponents() {
		_containerState.containerDTO().template.components.stream().filter(
			t -> t.type != ComponentType.CONTAINER
		).map(ExtendedComponentTemplateDTO.class::cast).forEach(
			this::initComponent
		);

		return null;
	}

	private void initComponent(ExtendedComponentTemplateDTO componentTemplateDTO) {
		if (componentTemplateDTO.type == ComponentType.FACTORY) {
			initFactoryComponent(componentTemplateDTO);
		}
		else {
			initSingleComponent(componentTemplateDTO);
		}
	}

	private Promise<Boolean> initFactoryComponent(ExtendedComponentTemplateDTO componentTemplateDTO) {
		ConfigurationListener cl = _configurationBuilder.component(
			_factoryBuilder.template(componentTemplateDTO).build()
		).build();

		_configurationListeners.add(cl);

		return _containerState.submit(cl.openOp(), cl::open);
	}

	private Promise<Boolean> initSingleComponent(ExtendedComponentTemplateDTO componentTemplateDTO) {
		ConfigurationListener cl = _configurationBuilder.component(
			_singleBuilder.template(componentTemplateDTO).build()
		).build();

		_configurationListeners.add(cl);

		return _containerState.submit(cl.openOp(), cl::open);
	}

	private boolean matchConfiguration(OSGiBean osgiBean, ProcessInjectionPoint<?, ?> pip) {
		InjectionPoint injectionPoint = pip.getInjectionPoint();

		Class<?> declaringClass = Annotates.declaringClass(injectionPoint.getAnnotated());

		ConfigurationTemplateDTO current = new ComponentPropertiesModel.Builder(injectionPoint.getType()).declaringClass(
			declaringClass
		).injectionPoint(
			injectionPoint
		).build().toDTO();

		return osgiBean.getComponent().configurations.stream().map(
			t -> (ExtendedConfigurationTemplateDTO)t
		).filter(
			t -> current.equals(t)
		).findFirst().map(
			t -> {
				MarkedInjectionPoint markedInjectionPoint = new MarkedInjectionPoint(injectionPoint);

				pip.setInjectionPoint(markedInjectionPoint);

				t.bean.setInjectionPoint(injectionPoint);
				t.bean.setMark(markedInjectionPoint.getMark());

				return true;
			}
		).orElse(false);
	}

	private boolean matchReference(OSGiBean osgiBean, ProcessInjectionPoint<?, ?> pip) {
		InjectionPoint injectionPoint = pip.getInjectionPoint();

		Annotated annotated = injectionPoint.getAnnotated();

		ReferenceModel.Builder builder = null;

		if (annotated instanceof AnnotatedField) {
			builder = new ReferenceModel.Builder((AnnotatedField<?>)annotated);
		}
		else if (annotated instanceof AnnotatedMethod) {
			builder = new ReferenceModel.Builder((AnnotatedMethod<?>)annotated);
		}
		else {
			builder = new ReferenceModel.Builder((AnnotatedParameter<?>)annotated);
		}

		ReferenceModel referenceModel = builder.injectionPoint(injectionPoint).build();

		ExtendedReferenceTemplateDTO current = referenceModel.toDTO();

		return osgiBean.getComponent().references.stream().map(
			t -> (ExtendedReferenceTemplateDTO)t
		).filter(
			t -> current.equals(t)
		).findFirst().map(
			t -> {
				MarkedInjectionPoint markedInjectionPoint = new MarkedInjectionPoint(injectionPoint);

				pip.setInjectionPoint(markedInjectionPoint);

				t.bean.setMark(markedInjectionPoint.getMark());

				_log.debug(l -> l.debug("CCR maping InjectionPoint {} to reference template {}", injectionPoint, t));

				return true;
			}
		).orElse(false);
	}

	private void processInjectionPoint0(ProcessInjectionPoint<?, ?> pip, boolean special) {
		InjectionPoint injectionPoint = pip.getInjectionPoint();

		Class<?> declaringClass = Annotates.declaringClass(injectionPoint.getAnnotated());

		String className = declaringClass.getName();

		OSGiBean osgiBean = _containerState.beansModel().getOSGiBean(className);

		if (osgiBean == null) {
			return;
		}

		Annotated annotated = injectionPoint.getAnnotated();
		ComponentProperties componentProperties = annotated.getAnnotation(ComponentProperties.class);
		Reference reference = annotated.getAnnotation(Reference.class);

		if (((reference != null) || special) && matchReference(osgiBean, pip)) {
			return;
		}

		if (componentProperties != null) {
			matchConfiguration(osgiBean, pip);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void registerService(
		ExtendedComponentInstanceDTO componentInstance,
		ExtendedActivationTemplateDTO activationTemplate,
		BeanManager bm) {

		ServiceScope scope = activationTemplate.scope;

		if (activationTemplate.cdiScope == ApplicationScoped.class) {
			scope = ServiceScope.SINGLETON;
		}

		final Context context = bm.getContext(activationTemplate.cdiScope);
		final Bean<Object> bean = (Bean<Object>)bm.resolve(
			bm.getBeans(activationTemplate.declaringClass, Any.Literal.INSTANCE));
		final Producer producer = createProducer(activationTemplate.producer, bean, bm);

		Object serviceObject;

		if (scope == ServiceScope.PROTOTYPE) {
			serviceObject = new PrototypeServiceFactory<Object>() {
				@Override
				public Object getService(Bundle bundle, ServiceRegistration<Object> registration) {
					CreationalContext<Object> cc = bm.createCreationalContext(bean);
					if (producer != null) {
						return producer.produce(cc);
					}
					return context.get(bean, cc);
				}

				@Override
				public void ungetService(Bundle bundle, ServiceRegistration<Object> registration, Object service) {
				}
			};
		}
		else if (scope == ServiceScope.BUNDLE) {
			serviceObject = new ServiceFactory<Object>() {
				@Override
				public Object getService(Bundle bundle, ServiceRegistration<Object> registration) {
					CreationalContext<Object> cc = bm.createCreationalContext(bean);
					if (producer != null) {
						return producer.produce(cc);
					}
					return context.get(bean, cc);
				}

				@Override
				public void ungetService(Bundle bundle, ServiceRegistration<Object> registration, Object service) {
				}
			};
		}
		else {
			CreationalContext<Object> cc = bm.createCreationalContext(bean);
			if (producer != null) {
				serviceObject = producer.produce(cc);
			}
			else {
				serviceObject = context.get(bean, cc);
			}
		}

		Objects.requireNonNull(serviceObject, "The service object is somehow null on " + this);

		Dictionary<String, Object> properties = new Hashtable<>(
			componentInstance.componentProperties(activationTemplate.properties));

		ServiceRegistration<?> serviceRegistration = registerService(
			activationTemplate.serviceClasses.toArray(new String[0]),
			serviceObject, properties);

		ExtendedActivationDTO activationDTO = new ExtendedActivationDTO();
		activationDTO.errors = new CopyOnWriteArrayList<>();
		activationDTO.service = SRs.from(serviceRegistration.getReference());
		activationDTO.template = activationTemplate;
		componentInstance.activations.add(activationDTO);
	}

	private ServiceRegistration<?> registerService(String[] serviceTypes, Object serviceObject, Dictionary<String, Object> properties) {
		ServiceRegistration<?> serviceRegistration = _containerState.bundleContext().registerService(
			serviceTypes, serviceObject, properties);

		_registrations.add(serviceRegistration);

		return serviceRegistration;
	}

	private boolean registerServices(ComponentDTO componentDTO, BeanManager bm) {
		componentDTO.template.activations.stream().map(
			ExtendedActivationTemplateDTO.class::cast
		).forEach(
			a -> registerService((ExtendedComponentInstanceDTO)componentDTO.instances.get(0), a, bm)
		);

		return true;
	}

	private final ConfigurationListener.Builder _configurationBuilder;
	private final List<ConfigurationListener> _configurationListeners = new CopyOnWriteArrayList<>();
	private final ContainerState _containerState;
	private final ComponentTemplateDTO _containerTemplate;
	private final FactoryComponent.Builder _factoryBuilder;
	private final Logger _log;
	private final List<ServiceRegistration<?>> _registrations = new CopyOnWriteArrayList<>();
	private final SingleComponent.Builder _singleBuilder;

}
