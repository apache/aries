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

import static org.osgi.namespace.extender.ExtenderNamespace.EXTENDER_NAMESPACE;
import static org.osgi.service.cdi.CdiExtenderConstants.CDI_EXTENDER;
import static org.osgi.service.cdi.CdiExtenderConstants.REQUIREMENT_SERVICES_DIRECTIVE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.naming.spi.ObjectFactory;

import org.apache.aries.cdi.container.internal.bean.ReferenceBean;
import org.apache.aries.cdi.container.internal.literal.AnyLiteral;
import org.apache.aries.cdi.container.internal.literal.ServiceLiteral;
import org.jboss.weld.bootstrap.api.Bootstrap;
import org.jboss.weld.manager.BeanManagerImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.cdi.CdiEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Phase_4_Publish {

	public Phase_4_Publish(
		Phase_3_Reference referencePhase, Bootstrap bootstrap) {

		_referencePhase = referencePhase;
		_bootstrap = bootstrap;
		_bundle = _referencePhase._bundle;
		_bundleContext = _referencePhase._bundle.getBundleContext();
	}

	public void close() {
		for (ServiceRegistration<?> registration : _registrations) {
			try {
				registration.unregister();
			}
			catch (IllegalStateException ise) {
				if (_log.isTraceEnabled()) {
					_log.trace("Service already unregistered {}", registration);
				}
			}
		}

		_registrations.clear();

		if (_beanManagerRegistration != null) {
			try {
				_beanManagerRegistration.unregister();
			}
			catch (Exception e) {
				if (_log.isTraceEnabled()) {
					_log.trace("Service already unregistered {}", _beanManagerRegistration);
				}
			}
		}

		if (_objectFactoryRegistration != null) {
			try {
				_objectFactoryRegistration.unregister();
			}
			catch (Exception e) {
				if (_log.isTraceEnabled()) {
					_log.trace("Service already unregistered {}", _objectFactoryRegistration);
				}
			}
		}

		_bootstrap.shutdown();
	}

	public void open() {
		_referencePhase._cdiContainerState.fire(CdiEvent.State.SATISFIED);

		BeanManager beanManager = _referencePhase._cdiContainerState.getBeanManager();

		processReferenceDependencies((BeanManagerImpl)beanManager);

		_bootstrap.validateBeans();
		_bootstrap.endInitialization();

		processRequirementDefinedServices((BeanManagerImpl)beanManager);
		processServiceDeclarations();

		_beanManagerRegistration = _bundleContext.registerService(
			BeanManager.class, beanManager, null);

		_referencePhase._cdiContainerState.fire(CdiEvent.State.CREATED);
	}

	private List<String> getServiceClassNames() {
		List<String> serviceClassNames = new ArrayList<>();

		BundleWiring bundleWiring = _bundle.adapt(BundleWiring.class);
		List<BundleWire> requiredBundleWires = bundleWiring.getRequiredWires(EXTENDER_NAMESPACE);

		for (BundleWire bundleWire : requiredBundleWires) {
			Map<String, Object> attributes = bundleWire.getCapability().getAttributes();

			if (attributes.containsKey(EXTENDER_NAMESPACE) &&
				attributes.get(EXTENDER_NAMESPACE).equals(CDI_EXTENDER)) {

				BundleRequirement requirement = bundleWire.getRequirement();

				Map<String, String> directives = requirement.getDirectives();

				if (directives.containsKey(REQUIREMENT_SERVICES_DIRECTIVE)) {
					String string = directives.get(REQUIREMENT_SERVICES_DIRECTIVE);

					List<String> services = Arrays.asList(string.split("\\s*,\\s*"));

					if (!services.isEmpty()) {
						serviceClassNames.addAll(services);
					}
				}
			}
		}

		return serviceClassNames;
	}

	private void processReferenceDependencies(BeanManagerImpl beanManagerImpl) {
		Map<ServiceReference<?>, Set<ReferenceBean>> beans = new HashMap<>();

		for (ReferenceDependency referenceDependency : _referencePhase._referenceDependencies) {
			for (ServiceReference<?> matchingReference : referenceDependency.getMatchingReferences()) {
				Set<ReferenceBean> set = beans.get(matchingReference);

				if (set == null) {
					set = new HashSet<>();

					beans.put(matchingReference, set);
				}

				ReferenceBean existingBean = null;

				for (ReferenceBean bean : set) {
					if (bean.getBindType() == referenceDependency.getBindType() &&
						bean.getBeanClass().equals(referenceDependency.getBeanClass()) &&
						bean.getTypes().contains(referenceDependency.getInjectionPointType())) {

						existingBean = bean;
					}
				}

				if (existingBean != null) {
					existingBean.addQualifier(referenceDependency.getReference());
					existingBean.addQualifiers(referenceDependency.getInjectionPoint().getQualifiers());
				}
				else {
					ReferenceBean bean = new ReferenceBean(
						beanManagerImpl, _bundleContext, referenceDependency.getInjectionPointType(),
						referenceDependency.getBeanClass(), referenceDependency.getBindType(), matchingReference);

					bean.addQualifier(referenceDependency.getReference());
					bean.addQualifiers(referenceDependency.getInjectionPoint().getQualifiers());

					set.add(bean);

					beanManagerImpl.addBean(bean);
				}
			}
		}
	}

	private void processRequirementDefinedServices(BeanManagerImpl beanManagerImpl) {
		List<String> serviceClassNames = getServiceClassNames();

		for (String serviceClassName : serviceClassNames) {
			try {
				Class<?> beanClass = _bundle.loadClass(serviceClassName);

				Set<Bean<?>> beans = beanManagerImpl.getBeans(beanClass, AnyLiteral.INSTANCE);

				if (beans.isEmpty()) {
					_log.error(
						"CDIe - MANIFEST service processing cannot find bean for class {}", serviceClassName);

					continue;
				}

				Bean<?> bean = beanManagerImpl.resolve(beans);
				CreationalContext<?> creationalContext = beanManagerImpl.createCreationalContext(bean);
				ServiceDeclaration serviceDeclaration = new ServiceDeclaration(
					ServiceLiteral.INSTANCE, bean, creationalContext);

				processServiceDeclaration(serviceDeclaration);
			}
			catch (ClassNotFoundException cnfe) {
				_log.error("CDIe - MANIFEST service processing cannot load class {}", serviceClassName, cnfe);
			}
		}
	}

	private void processServiceDeclarations() {
		for (ServiceDeclaration serviceDeclaration : _referencePhase._services) {
			processServiceDeclaration(serviceDeclaration);
		}
	}

	private void processServiceDeclaration(ServiceDeclaration serviceDeclaration) {
		if (_log.isDebugEnabled()) {
			_log.debug("CDIe - Publishing bean {} as service.", serviceDeclaration);
		}

		String[] classNames = serviceDeclaration.getClassNames();
		Object serviceInstance = serviceDeclaration.getServiceInstance();
		Dictionary<String,Object> properties = serviceDeclaration.getProperties();

		_registrations.add(
			_bundleContext.registerService(classNames, serviceInstance, properties));
	}

	private static final Logger _log = LoggerFactory.getLogger(Phase_4_Publish.class);

	private final Bootstrap _bootstrap;
	private final Bundle _bundle;
	private final BundleContext _bundleContext;
	private final Phase_3_Reference _referencePhase;
	private final List<ServiceRegistration<?>> _registrations = new CopyOnWriteArrayList<>();

	private ServiceRegistration<BeanManager> _beanManagerRegistration;
	private ServiceRegistration<ObjectFactory> _objectFactoryRegistration;

}