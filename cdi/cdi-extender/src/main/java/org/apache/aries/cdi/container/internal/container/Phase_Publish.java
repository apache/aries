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

import java.util.ArrayList;
import java.util.Collection;
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

import org.apache.aries.cdi.container.internal.bean.ReferenceBean;
import org.apache.aries.cdi.container.internal.literal.AnyLiteral;
import org.apache.aries.cdi.container.internal.literal.ServiceLiteral;
import org.apache.aries.cdi.container.internal.model.ServiceModel;
import org.jboss.weld.bootstrap.api.Bootstrap;
import org.jboss.weld.manager.BeanManagerImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cdi.CdiEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Phase_Publish implements Phase {

	public Phase_Publish(
		List<ReferenceDependency> references, List<ServiceDeclaration> services, CdiContainerState cdiContainerState,
		Bootstrap bootstrap) {

		_references = references;
		_services = services;
		_cdiContainerState = cdiContainerState;
		_bootstrap = bootstrap;
		_bundle = cdiContainerState.getBundle();
		_bundleContext = _bundle.getBundleContext();
	}

	@Override
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
					_log.trace(
						"Service already unregistered {}", _beanManagerRegistration);
				}
			}
		}

		_bootstrap.shutdown();
	}

	@Override
	public void open() {
		_cdiContainerState.fire(CdiEvent.Type.SATISFIED);

		BeanManager beanManager = _cdiContainerState.getBeanManager();

		processReferenceDependencies((BeanManagerImpl)beanManager);

		_bootstrap.validateBeans();
		_bootstrap.endInitialization();

		processRequirementDefinedServices((BeanManagerImpl)beanManager);
		processServiceDeclarations();

		_beanManagerRegistration = _bundleContext.registerService(
			BeanManager.class, beanManager, null);

		_cdiContainerState.fire(CdiEvent.Type.CREATED);
	}

	private void processReferenceDependencies(BeanManagerImpl beanManagerImpl) {
		Map<ServiceReference<?>, Set<ReferenceBean>> beans = new HashMap<>();

		for (ReferenceDependency referenceDependency : _references) {
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
		Collection<ServiceModel> serviceModels = _cdiContainerState.getBeansModel().getServiceModels();

		for (ServiceModel serviceModel : serviceModels) {
			try {
				Class<?> beanClass = _bundle.loadClass(serviceModel.getBeanClass());

				Set<Bean<?>> beans = beanManagerImpl.getBeans(beanClass, AnyLiteral.INSTANCE);

				if (beans.isEmpty()) {
					_log.error(
						"CDIe - MANIFEST service processing cannot find bean for class {}",
						serviceModel.getBeanClass());

					continue;
				}

				Bean<?> bean = beanManagerImpl.resolve(beans);
				CreationalContext<?> creationalContext = beanManagerImpl.createCreationalContext(bean);

				List<String> provides = serviceModel.getProvides();
				List<Class<?>> interfaces = new ArrayList<>();

				for (String provide : provides) {
					try {
						interfaces.add(_bundle.loadClass(provide));
					}
					catch (Exception e) {
						_log.error("CDIe - Failure loading provided interface for service {}", provide);
					}
				}

				ServiceDeclaration serviceDeclaration = new ServiceDeclaration(
					ServiceLiteral.from(interfaces.toArray(new Class<?>[0]), serviceModel.getProperties()), bean,
					creationalContext);

				processServiceDeclaration(serviceDeclaration);
			}
			catch (ClassNotFoundException cnfe) {
				_log.error(
					"CDIe - MANIFEST service processing cannot load class {}", serviceModel.getBeanClass(), cnfe);
			}
		}
	}

	private void processServiceDeclarations() {
		for (ServiceDeclaration serviceDeclaration : _services) {
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

	private static final Logger _log = LoggerFactory.getLogger(Phase_Publish.class);

	private final Bootstrap _bootstrap;
	private final Bundle _bundle;
	private final BundleContext _bundleContext;
	private final CdiContainerState _cdiContainerState;
	private final List<ReferenceDependency> _references;
	private final List<ServiceRegistration<?>> _registrations = new CopyOnWriteArrayList<>();
	private final List<ServiceDeclaration> _services;

	private ServiceRegistration<BeanManager> _beanManagerRegistration;

}