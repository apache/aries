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

import static org.apache.aries.cdi.container.internal.util.Filters.*;
import static org.osgi.namespace.extender.ExtenderNamespace.*;
import static org.osgi.service.cdi.CDIConstants.*;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.AnnotationLiteral;

import org.apache.aries.cdi.container.internal.ChangeCount;
import org.apache.aries.cdi.container.internal.loader.BundleClassLoader;
import org.apache.aries.cdi.container.internal.loader.BundleResourcesLoader;
import org.apache.aries.cdi.container.internal.model.BeansModel;
import org.apache.aries.cdi.container.internal.model.BeansModelBuilder;
import org.apache.aries.cdi.container.internal.model.ExtendedConfigurationTemplateDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedExtensionTemplateDTO;
import org.apache.aries.cdi.container.internal.util.Logs;
import org.apache.aries.cdi.container.internal.util.Throw;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.cdi.ComponentType;
import org.osgi.service.cdi.ConfigurationPolicy;
import org.osgi.service.cdi.MaximumCardinality;
import org.osgi.service.cdi.runtime.dto.ContainerDTO;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ContainerTemplateDTO;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.Logger;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;
import org.osgi.util.tracker.ServiceTracker;

public class ContainerState {

	public static final AnnotationLiteral<Any> ANY = new AnnotationLiteral<Any>() {
		private static final long serialVersionUID = 1L;
	};

	@SuppressWarnings("unchecked")
	public ContainerState(
		Bundle bundle,
		Bundle extenderBundle,
		ChangeCount ccrChangeCount,
		PromiseFactory promiseFactory,
		ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> caTracker,
		Logs ccrLogs) {

		_bundle = bundle;
		_extenderBundle = extenderBundle;
		_ccrLogs = ccrLogs;
		_log = _ccrLogs.getLogger(getClass());
		_containerLogs = new Logs.Builder(_bundle.getBundleContext()).build();

		_changeCount = new ChangeCount();
		_changeCount.addObserver(ccrChangeCount);

		_promiseFactory = promiseFactory;
		_caTracker = caTracker;

		BundleWiring bundleWiring = _bundle.adapt(BundleWiring.class);

		List<BundleWire> wires = bundleWiring.getRequiredWires(EXTENDER_NAMESPACE);

		Map<String, Object> cdiAttributes = Collections.emptyMap();

		for (BundleWire wire : wires) {
			BundleCapability capability = wire.getCapability();
			Map<String, Object> attributes = capability.getAttributes();
			String extender = (String)attributes.get(EXTENDER_NAMESPACE);

			if (extender.equals(CDI_CAPABILITY_NAME)) {
				BundleRequirement requirement = wire.getRequirement();
				cdiAttributes = requirement.getAttributes();
				break;
			}
		}

		_containerDTO = new ContainerDTO();
		_containerDTO.bundle = _bundle.adapt(BundleDTO.class);
		_containerDTO.changeCount = _changeCount.get();
		_containerDTO.components = new CopyOnWriteArrayList<>();
		_containerDTO.errors = new CopyOnWriteArrayList<>();
		_containerDTO.extensions = new CopyOnWriteArrayList<>();
		_containerDTO.template = new ContainerTemplateDTO();
		_containerDTO.template.components = new CopyOnWriteArrayList<>();
		_containerDTO.template.extensions = new CopyOnWriteArrayList<>();
		_containerDTO.template.id = Optional.ofNullable(
			(String)cdiAttributes.get(CDI_CONTAINER_ID)
		).orElse(
			_bundle.getSymbolicName()
		);

		Optional.ofNullable(
			(List<String>)cdiAttributes.get(REQUIREMENT_EXTENSIONS_ATTRIBUTE)
		).ifPresent(
			list -> list.stream().forEach(
				extensionFilter -> {
					ExtendedExtensionTemplateDTO extensionTemplateDTO = new ExtendedExtensionTemplateDTO();

					try {
						extensionTemplateDTO.filter = asFilter(extensionFilter);
						extensionTemplateDTO.serviceFilter = extensionFilter;

						_containerDTO.template.extensions.add(extensionTemplateDTO);
					}
					catch (Exception e) {
						_containerDTO.errors.add(Throw.asString(e));
					}
				}
			)
		);

		_containerComponentTemplateDTO = new ComponentTemplateDTO();
		_containerComponentTemplateDTO.activations = new CopyOnWriteArrayList<>();
		_containerComponentTemplateDTO.beans = new CopyOnWriteArrayList<>();
		_containerComponentTemplateDTO.configurations = new CopyOnWriteArrayList<>();
		_containerComponentTemplateDTO.name = _containerDTO.template.id;
		_containerComponentTemplateDTO.properties = Collections.emptyMap();
		_containerComponentTemplateDTO.references = new CopyOnWriteArrayList<>();
		_containerComponentTemplateDTO.type = ComponentType.CONTAINER;

		ExtendedConfigurationTemplateDTO configurationTemplate = new ExtendedConfigurationTemplateDTO();
		configurationTemplate.maximumCardinality = MaximumCardinality.ONE;
		configurationTemplate.pid = Optional.ofNullable(
			(String)cdiAttributes.get(CDI_CONTAINER_ID)
		).map(
			s -> s.replaceAll("-", ".")
		).orElse(
			"osgi.cdi." + _bundle.getSymbolicName().replaceAll("-", ".")
		);
		configurationTemplate.policy = ConfigurationPolicy.OPTIONAL;

		_containerComponentTemplateDTO.configurations.add(configurationTemplate);

		_containerDTO.template.components.add(_containerComponentTemplateDTO);

		_aggregateClassLoader = new BundleClassLoader(getBundles(_bundle, _extenderBundle));

		_beansModel = new BeansModelBuilder(this, _aggregateClassLoader, bundleWiring, cdiAttributes).build();

		_bundleClassLoader = bundleWiring.getClassLoader();

		try {
			new ContainerDiscovery(this);
		}
		catch (Exception e) {
			_log.error(l -> l.error("CCR Discovery resulted in errors on {}", bundle, e));

			_containerDTO.errors.add(Throw.asString(e));
		}
	}

	public <T, R> Promise<R> addCallback(CheckedCallback<T, R> checkedCallback) {
		Deferred<R> deferred = _promiseFactory.deferred();
		_callbacks.put(checkedCallback, deferred);
		return deferred.getPromise();
	}

	public BeanManager beanManager() {
		return _beanManager;
	}

	public void beanManager(BeanManager beanManager) {
		_beanManager = beanManager;
	}

	public BeansModel beansModel() {
		return _beansModel;
	}

	public Bundle bundle() {
		return _bundle;
	}

	public ClassLoader bundleClassLoader() {
		return _bundleClassLoader;
	}

	public BundleContext bundleContext() {
		return _bundle.getBundleContext();
	}

	public ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> caTracker() {
		return _caTracker;
	}

	public Logs ccrLogs() {
		return _ccrLogs;
	}

	public ClassLoader classLoader() {
		return _aggregateClassLoader;
	}

	public void closing() {
		try {
			_closing.set(_promiseFactory.submit(() -> Boolean.TRUE).getValue());
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	public ComponentContext componentContext() {
		return _componentContext;
	}

	public ComponentTemplateDTO containerComponentTemplateDTO() {
		return _containerComponentTemplateDTO;
	}

	public ContainerDTO containerDTO() {
		_containerDTO.changeCount = _changeCount.get();
		return _containerDTO;
	}

	public Logs containerLogs() {
		return _containerLogs;
	}

	public void error(Throwable t) {
		containerDTO().errors.add(Throw.asString(t));
	}

	public Bundle extenderBundle() {
		return _extenderBundle;
	}

	public Optional<Configuration> findConfig(String pid) {
		return findConfigs(pid, false).map(arr -> arr[0]);
	}

	public Optional<Configuration[]> findConfigs(String pid, boolean factory) {
		try {
			String query = "(service.pid=".concat(pid).concat(")");

			if (factory) {
				query = "(service.factoryPid=".concat(pid).concat(")");
			}

			return Optional.ofNullable(
				_caTracker.getService().listConfigurations(query)
			);
		}
		catch (Exception e) {
			_log.error(l -> l.error("CCR unexpected failure fetching configuration for {}", pid, e));

			return Throw.exception(e);
		}
	}

	public String id() {
		return _containerDTO.template.id;
	}

	public void incrementChangeCount() {
		_changeCount.incrementAndGet();
	}

	@SuppressWarnings("unchecked")
	public <T extends ResourceLoader & ProxyServices> T loader() {
		return (T)new BundleResourcesLoader(_bundle, _extenderBundle);
	}

	public PromiseFactory promiseFactory() {
		return _promiseFactory;
	}

	@SuppressWarnings("unchecked")
	public <T, R> Promise<T> submit(Op op, Callable<T> task) {
		try {
			switch (op.mode) {
				case CLOSE: {
					// always perform close synchronously
					_log.debug(l -> l.debug("CCR submit {}", op));
					return _promiseFactory.resolved(task.call());
				}
				case OPEN:
					// when closing don't do perform any opens
					// also, don't log it since it's just going to be noise
					if (_closing.get()) {
						return _promiseFactory.resolved((T)new Object());
					}
			}
		}
		catch (Exception e) {
			return _promiseFactory.failed(e);
		}

		_log.debug(l -> l.debug("CCR submit {}", op));

		Promise<T> promise = _promiseFactory.submit(task);

		for (Entry<CheckedCallback<?, ?>, Deferred<?>> entry : _callbacks.entrySet()) {
			CheckedCallback<T, R> cc = (CheckedCallback<T, R>)entry.getKey();
			if (cc.test(op)) {
				((Deferred<R>)entry.getValue()).resolveWith(promise.then(cc, cc)).then(
					s -> {
						_callbacks.remove(cc);
						return s;
					},
					f -> _callbacks.remove(cc)
				);
			}
		}

		return promise;
	}

	private static Bundle[] getBundles(Bundle bundle, Bundle extenderBundle) {
		List<Bundle> bundles = new ArrayList<>();

		bundles.add(bundle);
		bundles.add(extenderBundle);

		BundleWiring extenderWiring = extenderBundle.adapt(BundleWiring.class);

		List<BundleWire> requiredWires = extenderWiring.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE);

		for (BundleWire bundleWire : requiredWires) {
			BundleCapability capability = bundleWire.getCapability();
			Map<String, Object> attributes = capability.getAttributes();
			String packageName = (String)attributes.get(PackageNamespace.PACKAGE_NAMESPACE);
			if (!packageName.startsWith("org.jboss.weld.")) {
				continue;
			}

			Bundle wireBundle = bundleWire.getProvider().getBundle();
			if (!bundles.contains(wireBundle)) {
				bundles.add(wireBundle);
			}
		}

		return bundles.toArray(new Bundle[0]);
	}

	private final ClassLoader _aggregateClassLoader;
	private volatile BeanManager _beanManager;
	private final BeansModel _beansModel;
	private final Bundle _bundle;
	private final ClassLoader _bundleClassLoader;
	private final Map<CheckedCallback<?, ?>, Deferred<?>> _callbacks = new ConcurrentHashMap<>();
	private final ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> _caTracker;
	private final Logger _log;
	private final Logs _ccrLogs;
	private final ChangeCount _changeCount;
	private final AtomicBoolean _closing = new AtomicBoolean(false);
	private final ComponentContext _componentContext = new ComponentContext();
	private final ContainerDTO _containerDTO;
	private final Logs _containerLogs;
	private final ComponentTemplateDTO _containerComponentTemplateDTO;
	private final Bundle _extenderBundle;
	private final PromiseFactory _promiseFactory;

}