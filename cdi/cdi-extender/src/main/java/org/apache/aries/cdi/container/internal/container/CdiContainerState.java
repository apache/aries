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
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.AnnotationLiteral;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cdi.CdiContainer;
import org.osgi.service.cdi.CdiEvent;
import org.osgi.service.cdi.CdiExtenderConstants;
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

		properties.put(CdiExtenderConstants.CDI_EXTENDER_CONTAINER_STATE, CdiEvent.State.CREATING);

		_cdiContainerRegistration = _bundle.getBundleContext().registerService(
			CdiContainer.class, _cdiContainerService, properties);
	}

	public void close() {
		try {
			_lock.lock();

			_cdiContainerRegistration.unregister();
		}
		catch (Exception e) {
			if (_log.isTraceEnabled()) {
				_log.trace("Service already unregistered {}", _cdiContainerRegistration);
			}
		}
		finally {
			_lock.unlock();
		}
	}

	public BeanManager getBeanManager() {
		return _beanManager;
	}

	public Bundle getBundle() {
		return _bundle;
	}

	public Bundle getExtenderBundle() {
		return _extenderBundle;
	}

	public List<ExtensionDependency> getExtensionDependencies() {
		return _extensionDependencies;
	}

	public String getId() {
		return _bundle.getSymbolicName() + ":" + _bundle.getBundleId();
	}

	public CdiEvent.State getLastState() {
		return _lastState.get();
	}

	public List<ReferenceDependency> getReferenceDependencies() {
		return _referenceDependencies;
	}

	public void fire(CdiEvent event) {
		try {
			_lock.lock();

			if ((_lastState.get() == CdiEvent.State.DESTROYING) &&
				((event.getState() == CdiEvent.State.WAITING_FOR_EXTENSIONS) ||
				(event.getState() == CdiEvent.State.WAITING_FOR_SERVICES))) {

				return;
			}

			if (_log.isDebugEnabled()) {
				_log.debug("CDIe - Event {}", event, event.getCause());
			}

			updateState(event);

			if (_beanManager != null) {
				_beanManager.fireEvent(event);
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
		finally {
			_lock.unlock();
		}
	}

	public void fire(CdiEvent.State state) {
		fire(new CdiEvent(state, _bundle, _extenderBundle));
	}

	public void fire(CdiEvent.State state, String payload) {
		fire(new CdiEvent(state, _bundle, _extenderBundle, payload, null));
	}

	public void fire(CdiEvent.State state, Throwable cause) {
		fire(new CdiEvent(state, _bundle, _extenderBundle, null, cause));
	}

	public void setBeanManager(BeanManager beanManager) {
		_beanManager = beanManager;
		_cdiContainerService.setBeanManager(beanManager);
	}

	public void setExtensionDependencies(List<ExtensionDependency> extensionDependencies) {
		_extensionDependencies = extensionDependencies;
	}

	public void setReferenceDependency(List<ReferenceDependency> referenceDependencies) {
		_referenceDependencies = referenceDependencies;
	}

	private void updateState(CdiEvent event) {
		try {
			_lock.lock();

			ServiceReference<CdiContainer> reference = _cdiContainerRegistration.getReference();

			if (event.getState() == reference.getProperty(CdiExtenderConstants.CDI_EXTENDER_CONTAINER_STATE)) {
				return;
			}

			_lastState.set(event.getState());

			Hashtable<String, Object> properties = new Hashtable<>();

			for (String key : reference.getPropertyKeys()) {
				properties.put(key, reference.getProperty(key));
			}

			properties.put(CdiExtenderConstants.CDI_EXTENDER_CONTAINER_STATE, event.getState());

			_cdiContainerRegistration.setProperties(properties);
		}
		finally {
			_lock.unlock();
		}
	}

	private static final Logger _log = LoggerFactory.getLogger(CdiContainerState.class);

	private volatile BeanManager _beanManager;
	private final Bundle _bundle;
	private final ServiceRegistration<CdiContainer> _cdiContainerRegistration;
	private final CdiContainerService _cdiContainerService;
	private final Bundle _extenderBundle;
	private List<ExtensionDependency> _extensionDependencies;
	private AtomicReference<CdiEvent.State> _lastState = new AtomicReference<CdiEvent.State>(CdiEvent.State.CREATING);
	private final Map<ServiceReference<CdiListener>, CdiListener> _listeners;
	private final ReentrantLock _lock = new ReentrantLock();
	private List<ReferenceDependency> _referenceDependencies;

}