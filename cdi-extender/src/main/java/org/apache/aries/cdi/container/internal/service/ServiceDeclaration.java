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

package org.apache.aries.cdi.container.internal.service;

import static org.apache.aries.cdi.container.internal.util.Reflection.cast;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;

import org.apache.aries.cdi.container.internal.component.OSGiBean;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.osgi.framework.Bundle;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

public class ServiceDeclaration {

	public ServiceDeclaration(
		ContainerState containerState,
		OSGiBean componentModel,
		Bean<?> bean,
		CreationalContext<?> creationalContext) {

		_containerState = containerState;
		_bean = bean;
		_creationalContext = creationalContext;

		_componentModel = componentModel;

		Object instance = null;

/*		if (_componentModel.getServiceScope() == ServiceScope.SINGLETON) {
			instance = new SingletonScopeWrapper();
		}
		else if (_componentModel.getServiceScope() == ServiceScope.BUNDLE) {
			instance = new BundleScopeWrapper();
		}
		else if (_componentModel.getServiceScope() == ServiceScope.PROTOTYPE) {
			instance = new PrototypeScopeWrapper();
		}
*/
		_instance = instance;
	}

/*	public String[] getClassNames() {
		return Arrays.stream(
			Types.types(_componentModel, _componentModel.getBeanClass(), _containerState.classLoader())
		).map(
			c -> c.getName()
		).collect(
			Collectors.toList()
		).toArray(
			new String[0]
		);
	}

	public String getName() {
		return _componentModel.getName();
	}

	public ServiceScope getScope() {
		return _componentModel.getServiceScope();
	}
*/
	public Object getServiceInstance() {
		return _instance;
	}

/*	public Dictionary<String, ?> getServiceProperties() {
		return new ComponentProperties().bean(
			_bean
		).componentModel(
			_componentModel
		).containerState(
			_containerState
		).build();
	}
*/
	private final Bean<?> _bean;
	private final OSGiBean _componentModel;
	private final ContainerState _containerState;
	private final CreationalContext<?> _creationalContext;
	private final Object _instance;

	@SuppressWarnings({"rawtypes"})
	private class BundleScopeWrapper implements ServiceFactory {

		@Override
		public Object getService(Bundle bundle, ServiceRegistration registration) {
			return _bean.create(cast(_creationalContext));
		}

		@Override
		public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
			_bean.destroy(cast(service), cast(_creationalContext));
		}

	}

	@SuppressWarnings({"rawtypes"})
	private class PrototypeScopeWrapper implements PrototypeServiceFactory {

		@Override
		public Object getService(Bundle bundle, ServiceRegistration registration) {
			return _bean.create(cast(_creationalContext));
		}

		@Override
		public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
			_bean.destroy(cast(service), cast(_creationalContext));
		}

	}

	@SuppressWarnings({"rawtypes"})
	private class SingletonScopeWrapper implements ServiceFactory {

		@Override
		public Object getService(Bundle bundle, ServiceRegistration registration) {
			if (_instance == null) {
				_instance = _bean.create(cast(_creationalContext));
			}
			return _instance;
		}

		@Override
		public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
		}

		private Object _instance;

	}

//	private class ManagedServiceFactoryWrapper implements ManagedServiceFactory {
//
//		@Override
//		public String getName() {
//			return _component.name();
//		}
//
//		@Override
//		public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
//		}
//
//		@Override
//		public void deleted(String pid) {
//		}
//
//		private final Map<String, Object> _instances = new ConcurrentHashMap<>();
//
//	}

}
