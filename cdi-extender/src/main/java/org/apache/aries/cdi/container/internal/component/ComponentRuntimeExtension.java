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

import javax.enterprise.inject.spi.Extension;

public class ComponentRuntimeExtension implements Extension {

/*	public ComponentRuntimeExtension(ContainerState containerState) {
		_containerState = containerState;
	}

	void afterBeanDiscovery(@Observes AfterBeanDiscovery abd) {
		_beans.stream().forEach(bean -> abd.addBean(bean));
	}

	ConfigurationModel matchConfiguration(ComponentModel componentModel, ProcessInjectionPoint<?, ?> pip) {
		InjectionPoint injectionPoint = pip.getInjectionPoint();

		Configuration configuration = injectionPoint.getAnnotated().getAnnotation(Configuration.class);

		for (ConfigurationModel configurationModel : componentModel.getConfigurations()) {
			ConfigurationModel tempModel = new ConfigurationModel.Builder(
				injectionPoint.getType()
			).injectionPoint(
				injectionPoint
			).build();

			if (configurationModel.equals(tempModel)) {
				if (configuration == null) {
					configuration = new ConfigurationLiteral(
						configurationModel.getPid(), configurationModel.getConfigurationPolicy());
				}

				MarkedInjectionPoint markedInjectionPoint = new MarkedInjectionPoint(
					injectionPoint,
					configuration,
					_mark.incrementAndGet());

				pip.setInjectionPoint(markedInjectionPoint);

				return configurationModel;
			}
		}

		return null;
	}

	ReferenceModel matchReference(ComponentModel componentModel, ProcessInjectionPoint<?, ?> pip) {
		InjectionPoint injectionPoint = pip.getInjectionPoint();

		Reference reference = injectionPoint.getAnnotated().getAnnotation(Reference.class);

		for (ReferenceModel referenceModel : componentModel.getReferences()) {
			ReferenceModel tempModel = new ReferenceModel.Builder(
				injectionPoint.getQualifiers()
			).scope(
				referenceModel.getScope()
			).target(
				referenceModel.getTarget()
			).annotated(
				injectionPoint.getAnnotated()
			).build();

			if (referenceModel.equals(tempModel)) {
				if (reference == null) {
					reference = new ReferenceLiteral(
						referenceModel.getName(),
						referenceModel.getServiceClass(),
						referenceModel.getCardinality(),
						referenceModel.getPolicy(),
						referenceModel.getPolicyOption(),
						referenceModel.getScope(),
						referenceModel.getTarget());
				}

				MarkedInjectionPoint markedInjectionPoint = new MarkedInjectionPoint(
					injectionPoint,
					reference,
					_mark.incrementAndGet());

				pip.setInjectionPoint(markedInjectionPoint);

				return referenceModel;
			}
		}

		return null;
	}

	ReferenceModel matchReference(ComponentModel componentModel, ProcessObserverMethod<ServiceEvent<?>, ?> pom) {
		ObserverMethod<ServiceEvent<?>> observerMethod = pom.getObserverMethod();

		Annotated annotated = new ObserverMethodAnnotated(observerMethod);

		for (ReferenceModel referenceModel : componentModel.getReferences()) {
			ReferenceModel tempModel = new ReferenceModel.Builder(
				observerMethod.getObservedQualifiers()
			).annotated(
				annotated
			).policy(
				ReferencePolicy.DYNAMIC
			).build();

			if (referenceModel.equals(tempModel)) {
				return referenceModel;
			}
		}

		return null;
	}


	 * discover if an annotated class is a component

	<X> void processAnnotatedType(@Observes ProcessAnnotatedType<X> pat, BeanManager beanManager) {
		final AnnotatedType<X> at = pat.getAnnotatedType();

		Class<X> annotatedClass = at.getJavaClass();

		ComponentModel componentModel = _containerState.beansModel().getComponentModel(annotatedClass.getName());

		// Is it one of the CDI Bundle's defined beans?

		if (componentModel == null) {

			// No it's not!

			return;
		}

		// If the class is already annotated with @Component, skip it!

		if (at.isAnnotationPresent(Component.class)) {
			return;
		}

		// Since it's not, add @Component to the metadata for completeness.

		AnnotatedType<X> wrapped = new AnnotatedType<X>() {

			// Create an impl of @Component

			private final ComponentLiteral componentLiteral = new ComponentLiteral(
				componentModel.getName(),
				Types.types(componentModel, annotatedClass, _containerState.classLoader()),
				componentModel.getProperties(),
				componentModel.getServiceScope());

			@Override
			public Set<AnnotatedConstructor<X>> getConstructors() {
				return at.getConstructors();
			}

			@Override
			public Set<AnnotatedField<? super X>> getFields() {
				return at.getFields();
			}

			@Override
			public Class<X> getJavaClass() {
				return at.getJavaClass();
			}

			@Override
			public Set<AnnotatedMethod<? super X>> getMethods() {
				return at.getMethods();
			}

			@Override
			@SuppressWarnings("unchecked")
			public <T extends Annotation> T getAnnotation(final Class<T> annType) {
				if (Component.class.equals(annType)) {
					return (T)componentLiteral;
				}
				return at.getAnnotation(annType);
			}

			@Override
			public Set<Annotation> getAnnotations() {
				return Sets.hashSet(at.getAnnotations(), componentLiteral);
			}

			@Override
			public Type getBaseType() {
				return at.getBaseType();
			}

			@Override
			public Set<Type> getTypeClosure() {
				return at.getTypeClosure();
			}

			@Override
			public boolean isAnnotationPresent(Class<? extends Annotation> annType) {
				if (Component.class.equals(annType)) {
					return true;
				}
				return at.isAnnotationPresent(annType);
			}

		};

		pat.setAnnotatedType(wrapped);
	}

	void processBean(@Observes ProcessBean<?> processBean, BeanManager beanManager) {
		Bean<?> bean = processBean.getBean();

		Class<?> beanClass = bean.getBeanClass();

		ComponentModel componentModel = _containerState.beansModel().getComponentModel(beanClass.getName());

		if (componentModel == null) {
			return;
		}

		Annotated annotated = processBean.getAnnotated();

		if (annotated instanceof AnnotatedMethod) {
			AnnotatedMethod<?> annotatedMethod = (AnnotatedMethod<?>)annotated;

			annotated = annotatedMethod.getDeclaringType();
		}

		if (!annotated.isAnnotationPresent(Component.class)) {
			processBean.addDefinitionError(
				new IllegalStateException(
					String.format(
						"Bean %s is missing the @Component annotation. This should have been added synthetically if missing...",
						bean)));

			return;
		}

		Component component = annotated.getAnnotation(Component.class);

		// Only Dependent CDI scope is allowed for components who's service scope is singleton, bundle, prototype.

		if ((component.scope() == ServiceScope.BUNDLE) ||
			(component.scope() == ServiceScope.PROTOTYPE) ||
			(component.scope() == ServiceScope.SINGLETON)) {

			Class<?> scope = bean.getScope();

			if (!Dependent.class.isAssignableFrom(scope)) {
				processBean.addDefinitionError(
					new IllegalArgumentException(
						String.format(
							"An illegal scope (%s) was used for component service (%s)",
							scope, bean)));

				return;
			}
		}

		_containerState.serviceComponents().put(
			componentModel,
			new ServiceDeclaration(
				_containerState, componentModel, bean, beanManager.createCreationalContext(bean)));
	}

	void processInjectionPoint(@Observes ProcessInjectionPoint<?, ?> pip, BeanManager beanManager) {
		InjectionPoint injectionPoint = pip.getInjectionPoint();

		Bean<?> bean = injectionPoint.getBean();

		if (bean == null) {
			return;
		}

		Class<?> beanClass = bean.getBeanClass();

		ComponentModel componentModel = _containerState.beansModel().getComponentModel(beanClass.getName());

		if (componentModel == null) {
			return;
		}

		ReferenceModel matchingReference = matchReference(componentModel, pip);

		if (matchingReference != null) {
			ReferenceBean referenceBean = new ReferenceBean(
				_containerState,
				matchingReference,
				componentModel,
				pip.getInjectionPoint(),
				beanManager);

			_beans.add(referenceBean);

			return;
		}

		ConfigurationModel matchingConfiguration = matchConfiguration(componentModel, pip);

		if (matchingConfiguration != null) {
			ConfigurationBean configurationBean = new ConfigurationBean(
				_containerState,
				matchingConfiguration,
				componentModel,
				pip.getInjectionPoint(),
				beanManager);

			_beans.add(configurationBean);
		}
	}

	void processObserverMethod(@Observes ProcessObserverMethod<ServiceEvent<?>, ?> pom) {
		ObserverMethod<ServiceEvent<?>> observerMethod = pom.getObserverMethod();

		if (_log.isDebugEnabled()) {
			_log.debug("CDIe - Processing observer method {}", observerMethod);
		}

		Class<?> beanClass = observerMethod.getBeanClass();

		final String className = beanClass.getName();

		ComponentModel componentModel = _containerState.beansModel().getComponentModel(className);

		if (componentModel == null) {
			return;
		}

		ReferenceModel matchingReference = matchReference(componentModel, pom);

		if (matchingReference != null) {
			Map<String, ObserverMethod<ServiceEvent<?>>> map = _containerState.referenceObservers().computeIfAbsent(
				componentModel, k -> new LinkedHashMap<>());

			map.put(matchingReference.getName(), observerMethod);
		}
	}

	private static final Logger _log = LoggerFactory.getLogger(ComponentRuntimeExtension.class);

	private final AtomicInteger _mark = new AtomicInteger();
	private final ContainerState _containerState;
	private final List<Bean<?>> _beans = new CopyOnWriteArrayList<>();
*/
}
