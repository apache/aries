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

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Any;
import javax.enterprise.util.AnnotationLiteral;

import org.apache.aries.cdi.container.internal.model.BeansModel;
import org.jboss.weld.manager.BeanManagerImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cdi.CdiConstants;
import org.osgi.service.cdi.CdiContainer;
import org.osgi.service.cdi.CdiEvent;
import org.osgi.service.cdi.CdiEvent.Type;
import org.osgi.service.cdi.CdiListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CdiContainerState {

	public static final AnnotationLiteral<Any> ANY = new AnnotationLiteral<Any>() {
		private static final long serialVersionUID = 1L;
	};

	public CdiContainerState(
		Bundle bundle, Bundle extenderBundle, Map<ServiceReference<CdiListener>, CdiListener> listeners) {

		_bundle = bundle;
		_extenderBundle = extenderBundle;
		_listeners = listeners;

		_cdiContainerService = new CdiContainerService();

		Hashtable<String, Object> properties = new Hashtable<>();

		properties.put(CdiConstants.CDI_CONTAINER_STATE, CdiEvent.Type.CREATING);

		_cdiContainerRegistration = _bundle.getBundleContext().registerService(
			CdiContainer.class, _cdiContainerService, properties);
	}

	public synchronized void close() {
		try {
			_cdiContainerRegistration.unregister();
		}
		catch (Exception e) {
			if (_log.isTraceEnabled()) {
				_log.trace("Service already unregistered {}", _cdiContainerRegistration);
			}
		}
	}

	public BeansModel getBeansModel() {
		return _beansModel;
	}

	public Bundle getBundle() {
		return _bundle;
	}

	public Bundle getExtenderBundle() {
		return _extenderBundle;
	}

	public List<ConfigurationDependency> getConfigurationDependencies() {
		return _configurations;
	}

	public List<ExtensionDependency> getExtensionDependencies() {
		return _extensionDependencies;
	}

	public List<ReferenceDependency> getReferenceDependencies() {
		return _references;
	}

	public String getId() {
		return _bundle.getSymbolicName() + ":" + _bundle.getBundleId();
	}

	public CdiEvent.Type getLastState() {
		return _lastState;
	}

	public synchronized void fire(CdiEvent event) {
		Type type = event.getType();

		if ((_lastState == CdiEvent.Type.DESTROYING) &&
			((type == CdiEvent.Type.WAITING_FOR_CONFIGURATIONS) ||
			(type == CdiEvent.Type.WAITING_FOR_EXTENSIONS) ||
			(type == CdiEvent.Type.WAITING_FOR_SERVICES))) {

			return;
		}

		if (_log.isErrorEnabled() && (event.getCause() != null)) {
			_log.error("CDIe - Event {}", event, event.getCause());
		}
		else if (_log.isDebugEnabled()) {
			_log.debug("CDIe - Event {}", event);
		}

		updateState(event);

		if (_beanManagerImpl != null) {
			_beanManagerImpl.fireEvent(event);
		}

		for (CdiListener listener : _listeners.values()) {
			try {
				listener.cdiEvent(event);
			}
			catch (Throwable t) {
				if (_log.isErrorEnabled()) {
					_log.error("CDIe - CdiListener failed", t);
				}
			}
		}
	}

	public void fire(CdiEvent.Type state) {
		fire(new CdiEvent(state, _bundle, _extenderBundle));
	}

	public void fire(CdiEvent.Type state, String payload) {
		fire(new CdiEvent(state, _bundle, _extenderBundle, payload, null));
	}

	public void fire(CdiEvent.Type state, Throwable cause) {
		fire(new CdiEvent(state, _bundle, _extenderBundle, null, cause));
	}

	public void setBeanManager(BeanManagerImpl beanManagerImpl) {
		_beanManagerImpl = beanManagerImpl;
		_cdiContainerService.setBeanManager(beanManagerImpl);
	}

	public void setBeansModel(BeansModel beansModel) {
		_beansModel = beansModel;
	}

	public void setConfigurationDependencies(List<ConfigurationDependency> configurations) {
		_configurations = configurations;
	}

	public void setExtensionDependencies(List<ExtensionDependency> extensionDependencies) {
		_extensionDependencies = extensionDependencies;
	}

	public void setReferenceDependencies(List<ReferenceDependency> references) {
		_references = references;
	}

	private synchronized void updateState(CdiEvent event) {
		Type type = event.getType();

		ServiceReference<CdiContainer> reference = _cdiContainerRegistration.getReference();

		if (type == reference.getProperty(CdiConstants.CDI_CONTAINER_STATE)) {
			return;
		}

		_lastState = type;

		Hashtable<String, Object> properties = new Hashtable<>();

		for (String key : reference.getPropertyKeys()) {
			properties.put(key, reference.getProperty(key));
		}

		properties.put(CdiConstants.CDI_CONTAINER_STATE, type);

		_cdiContainerRegistration.setProperties(properties);
	}

	private static final Logger _log = LoggerFactory.getLogger(CdiContainerState.class);

	private volatile BeanManagerImpl _beanManagerImpl;
	private BeansModel _beansModel;
	private final Bundle _bundle;
	private final ServiceRegistration<CdiContainer> _cdiContainerRegistration;
	private final CdiContainerService _cdiContainerService;
	private List<ConfigurationDependency> _configurations;
	private final Bundle _extenderBundle;
	private List<ExtensionDependency> _extensionDependencies;
	private CdiEvent.Type _lastState = CdiEvent.Type.CREATING;
	private final Map<ServiceReference<CdiListener>, CdiListener> _listeners;
	private List<ReferenceDependency> _references;

}