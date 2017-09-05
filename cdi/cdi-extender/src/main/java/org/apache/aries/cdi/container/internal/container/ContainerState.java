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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.util.AnnotationLiteral;

import org.apache.aries.cdi.container.internal.component.ComponentModel;
import org.apache.aries.cdi.container.internal.configuration.ConfigurationCallback;
import org.apache.aries.cdi.container.internal.extension.ExtensionDependency;
import org.apache.aries.cdi.container.internal.loader.BundleClassLoader;
import org.apache.aries.cdi.container.internal.loader.BundleResourcesLoader;
import org.apache.aries.cdi.container.internal.model.BeansModel;
import org.apache.aries.cdi.container.internal.model.Context;
import org.apache.aries.cdi.container.internal.model.Registrator;
import org.apache.aries.cdi.container.internal.model.Tracker;
import org.apache.aries.cdi.container.internal.reference.ReferenceCallback;
import org.apache.aries.cdi.container.internal.service.ServiceDeclaration;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.cdi.CdiConstants;
import org.osgi.service.cdi.CdiContainer;
import org.osgi.service.cdi.CdiEvent;
import org.osgi.service.cdi.CdiEvent.Type;
import org.osgi.service.cdi.annotations.ServiceEvent;
import org.osgi.service.cm.ManagedService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContainerState implements CdiContainer {

	public static final AnnotationLiteral<Any> ANY = new AnnotationLiteral<Any>() {
		private static final long serialVersionUID = 1L;
	};

	public ContainerState(
		Bundle bundle, Bundle extenderBundle) {

		_bundle = Optional.ofNullable(bundle);
		_extenderBundle = extenderBundle;

		Hashtable<String, Object> properties = new Hashtable<>();

		properties.put(CdiConstants.CDI_CONTAINER_STATE, CdiEvent.Type.CREATING);

		_bundle.ifPresent(
			b -> {
				_classLoader = new BundleClassLoader(
					BundleResourcesLoader.getBundles(bundle, extenderBundle));
				_registration = b.getBundleContext().registerService(CdiContainer.class, this, properties);
			}
		);

		_context = new Context() {

			@Override
			public <T> T getService(ServiceReference<T> reference) {
				return bundleContext().getService(reference);
			}

			@Override
			public <T> ServiceObjects<T> getServiceObjects(ServiceReference<T> reference) {
				return bundleContext().getServiceObjects(reference);
			}

			@Override
			public <T> boolean ungetService(ServiceReference<T> reference) {
				return bundleContext().ungetService(reference);
			}

		};

		_msRegistrator = new Registrator<ManagedService>() {

			public void registerService(String[] classNames, ManagedService service, Dictionary<String, ?> properties) {
				registrations.add(bundleContext().registerService(ManagedService.class, service, properties));
			}

		};

		_bmRegistrator = new Registrator<BeanManager>() {

			@Override
			public void registerService(String[] classNames, BeanManager service, Dictionary<String, ?> properties) {
				registrations.add(bundleContext().registerService(BeanManager.class, service, properties));
			}

		};

		_serviceRegistrator = new Registrator<Object>() {

			@Override
			public void registerService(String[] classNames, Object service, Dictionary<String, ?> properties) {
				registrations.add(bundleContext().registerService(classNames, service, properties));
			}

		};

		_tracker = new Tracker() {

			@Override
			public <T> void track(String targetFilter, ReferenceCallback callback) {
				try {
					Filter filter = bundleContext().createFilter(targetFilter);

					trackers.add(new ServiceTracker<>(bundleContext(), filter, callback));
				}
				catch (InvalidSyntaxException ise) {
					if (_log.isErrorEnabled()) {
						_log.error("CDIe - Invalid filter syntax in {}", targetFilter, ise);
					}
				}
			}

		};
	}

	public Registrator<BeanManager> beanManagerRegistrator() {
		return _bmRegistrator;
	}

	public BeansModel beansModel() {
		return _beansModel;
	}

	public Bundle bundle() {
		return _bundle.orElse(null);
	}

	public ClassLoader bundleClassLoader() {
		return _bundle.map(b -> b.adapt(BundleWiring.class).getClassLoader()).orElse(getClass().getClassLoader());
	}

	public BundleContext bundleContext() {
		return _bundle.map(b -> b.getBundleContext()).orElse(null);
	}

	public ClassLoader classLoader() {
		return _bundle.map(b -> _classLoader).orElse(getClass().getClassLoader());
	}

	public synchronized void close() {
		try {
			if (_registration != null) {
				_registration.unregister();
			}
		}
		catch (Exception e) {
			if (_log.isTraceEnabled()) {
				_log.trace("Service already unregistered {}", _registration);
			}
		}
	}

	public Map<ComponentModel, Map<String, ConfigurationCallback>> configurationCallbacks() {
		return _configurationCallbacksMap;
	}

	public Context context() {
		return _context;
	}

	public Bundle extenderBundle() {
		return _extenderBundle;
	}

	public List<ExtensionDependency> extensionDependencies() {
		return _extensionDependencies;
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
	}

	public void fire(CdiEvent.Type state) {
		fire(new CdiEvent(state, _bundle.orElse(null), _extenderBundle));
	}

	public void fire(CdiEvent.Type state, String payload) {
		fire(new CdiEvent(state, _bundle.orElse(null), _extenderBundle, payload, null));
	}

	public void fire(CdiEvent.Type state, Throwable cause) {
		fire(new CdiEvent(state, _bundle.orElse(null), _extenderBundle, null, cause));
	}

	@Override
	public BeanManagerImpl getBeanManager() {
		return _beanManagerImpl;
	}

	public String id() {
		return _bundle.map(b -> b.getSymbolicName() + ":" + b.getBundleId()).orElse("null");
	}

	public CdiEvent.Type lastState() {
		return _lastState;
	}

	@SuppressWarnings("unchecked")
	public <T extends ResourceLoader & ProxyServices> T loader() {
		return (T)_bundle.map(b -> new BundleResourcesLoader(b, _extenderBundle)).orElse(null);
	}

	public Registrator<ManagedService> managedServiceRegistrator() {
		return _msRegistrator;
	}

	public Map<ComponentModel, Map<String, ReferenceCallback>> referenceCallbacks() {
		return _referenceCallbacksMap;
	}

	public Map<ComponentModel, Map<String, ObserverMethod<ServiceEvent<?>>>> referenceObservers() {
		return _referenceObserversMap;
	}

	public Map<ComponentModel, ServiceDeclaration> serviceComponents() {
		return _serviceComponents;
	}

	public Registrator<Object> serviceRegistrator() {
		return _serviceRegistrator;
	}

	public void setBeanManager(BeanManagerImpl beanManagerImpl) {
		_beanManagerImpl = beanManagerImpl;
	}

	public void setBeansModel(BeansModel beansModel) {
		_beansModel = beansModel;
	}

	public void setExtensionDependencies(List<ExtensionDependency> extensionDependencies) {
		_extensionDependencies = extensionDependencies;
	}

	public Tracker tracker() {
		return _tracker;
	}

	private synchronized void updateState(CdiEvent event) {
		Type type = event.getType();

		_lastState = type;

		if (_registration == null) {
			return;
		}

		ServiceReference<CdiContainer> reference = _registration.getReference();

		if (type == reference.getProperty(CdiConstants.CDI_CONTAINER_STATE)) {
			return;
		}

		Hashtable<String, Object> properties = new Hashtable<>();

		for (String key : reference.getPropertyKeys()) {
			properties.put(key, reference.getProperty(key));
		}

		properties.put(CdiConstants.CDI_CONTAINER_STATE, type);

		_registration.setProperties(properties);
	}

	private static final Logger _log = LoggerFactory.getLogger(ContainerState.class);

	private volatile BeanManagerImpl _beanManagerImpl;
	private BeansModel _beansModel;
	private final Registrator<BeanManager> _bmRegistrator;
	private final Optional<Bundle> _bundle;
	private ClassLoader _classLoader;
	private final Map<ComponentModel, Map<String, ConfigurationCallback>> _configurationCallbacksMap = new ConcurrentHashMap<>();
	private final Context _context;
	private final Bundle _extenderBundle;
	private List<ExtensionDependency> _extensionDependencies;
	private CdiEvent.Type _lastState = CdiEvent.Type.CREATING;
	private final Registrator<ManagedService> _msRegistrator;
	private final Map<ComponentModel, Map<String, ReferenceCallback>> _referenceCallbacksMap = new ConcurrentHashMap<>();
	private final Map<ComponentModel, Map<String, ObserverMethod<ServiceEvent<?>>>> _referenceObserversMap = new ConcurrentHashMap<>();
	private ServiceRegistration<CdiContainer> _registration;
	private final Map<ComponentModel, ServiceDeclaration> _serviceComponents = new ConcurrentHashMap<>();
	private final Registrator<Object> _serviceRegistrator;
	private final Tracker _tracker;

}