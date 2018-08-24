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

package org.apache.aries.cdi.container.internal;

import static org.osgi.namespace.extender.ExtenderNamespace.*;
import static org.osgi.service.cdi.CDIConstants.*;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

import org.apache.aries.cdi.container.internal.command.CDICommand;
import org.apache.aries.cdi.container.internal.container.CDIBundle;
import org.apache.aries.cdi.container.internal.container.ConfigurationListener;
import org.apache.aries.cdi.container.internal.container.ContainerBootstrap;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.ExtensionPhase;
import org.apache.aries.cdi.container.internal.model.ContainerActivator;
import org.apache.aries.cdi.container.internal.model.ContainerComponent;
import org.apache.aries.cdi.container.internal.model.FactoryActivator;
import org.apache.aries.cdi.container.internal.model.FactoryComponent;
import org.apache.aries.cdi.container.internal.model.SingleActivator;
import org.apache.aries.cdi.container.internal.model.SingleComponent;
import org.apache.aries.cdi.container.internal.util.Logs;
import org.apache.felix.utils.extender.AbstractExtender;
import org.apache.felix.utils.extender.Extension;
import org.osgi.annotation.bundle.Header;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.cdi.runtime.CDIComponentRuntime;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.annotations.RequireConfigurationAdmin;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;
import org.osgi.util.promise.PromiseFactory;
import org.osgi.util.tracker.ServiceTracker;

@Header(
	name = Constants.BUNDLE_ACTIVATOR,
	value = "${@class}"
)
@RequireConfigurationAdmin
public class Activator extends AbstractExtender {

	private static final Logs _logs = new Logs.Builder(FrameworkUtil.getBundle(Activator.class).getBundleContext()).build();
	private static final Logger _log = _logs.getLogger(Activator.class);
	private static final PromiseFactory _promiseFactory = new PromiseFactory(Executors.newFixedThreadPool(1));
	public static final CCR ccr = new CCR(_promiseFactory, _logs);

	public Activator() {
		setSynchronous(true);
	}

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		if (_log.isDebugEnabled()) {
			_log.debug("CCR starting {}", bundleContext.getBundle());
		}

		_command = new CDICommand(ccr);

		_bundleContext = bundleContext;

		registerCCR();
		registerCDICommand();

		super.start(bundleContext);

		if (_log.isDebugEnabled()) {
			_log.debug("CCR started {}", bundleContext.getBundle());
		}
	}

	private void registerCCR() {
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(Constants.SERVICE_CHANGECOUNT, _ccrChangeCount.get());
		properties.put(Constants.SERVICE_DESCRIPTION, "CDI Component Runtime");
		properties.put(Constants.SERVICE_VENDOR, "Apache Aries");

		ChangeObserverFactory changeObserverFactory = new ChangeObserverFactory();

		_ccrChangeCount.addObserver(changeObserverFactory);

		_ccrRegistration = _bundleContext.registerService(
			CDIComponentRuntime.class, changeObserverFactory, properties);
	}

	private void registerCDICommand() {
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put("osgi.command.scope", "cdi");
		properties.put("osgi.command.function", new String[] {"list", "info"});

		_commandRegistration = _bundleContext.registerService(Object.class, _command, properties);
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		if (_log.isDebugEnabled()) {
			_log.debug("CCR stoping {}", bundleContext.getBundle());
		}

		super.stop(bundleContext);

		_commandRegistration.unregister();
		_ccrRegistration.unregister();

		if (_log.isDebugEnabled()) {
			_log.debug("CCR stoped {}", bundleContext.getBundle());
		}
	}

	@Override
	protected Extension doCreateExtension(Bundle bundle) throws Exception {
		if (!requiresCDIExtender(bundle)) {
			return null;
		}

		ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> caTracker = new ServiceTracker<>(
			bundle.getBundleContext(), ConfigurationAdmin.class, null);

		caTracker.open();

		ServiceTracker<LoggerFactory, LoggerFactory> loggerTracker = new ServiceTracker<>(
			bundle.getBundleContext(), LoggerFactory.class, null);

		loggerTracker.open();

		ContainerState containerState = new ContainerState(
			bundle, _bundleContext.getBundle(), _ccrChangeCount, _promiseFactory, caTracker, _logs);

		// the CDI bundle
		return new CDIBundle(ccr, containerState,
			// handle extensions
			new ExtensionPhase(containerState,
				// listen for configurations of the container component
				new ConfigurationListener.Builder(containerState).component(
					// the container component
					new ContainerComponent.Builder(containerState,
						// when dependencies are satisfied activate the container
						new ContainerActivator.Builder(containerState,
							// when the active container bootstraps CDI
							new ContainerBootstrap(
								containerState,
								// when CDI is bootstrapping is complete and is up and running
								// activate the configuration listeners for single and factory components
								new ConfigurationListener.Builder(containerState),
								new SingleComponent.Builder(containerState,
									new SingleActivator.Builder(containerState)),
								new FactoryComponent.Builder(containerState,
									new FactoryActivator.Builder(containerState))
							)
						)
					).build()
				).build()
			)
		);
	}

	@Override
	protected void debug(Bundle bundle, String msg) {
		if (_log.isDebugEnabled()) {
			_log.debug(msg, bundle);
		}
	}

	@Override
	protected void warn(Bundle bundle, String msg, Throwable t) {
		if (_log.isWarnEnabled()) {
			_log.warn(msg, bundle, t);
		}
	}

	@Override
	protected void error(String msg, Throwable t) {
		if (_log.isErrorEnabled()) {
			_log.error(msg, t);
		}
	}

	private boolean requiresCDIExtender(Bundle bundle) {
		BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
		List<BundleWire> requiredBundleWires = bundleWiring.getRequiredWires(EXTENDER_NAMESPACE);

		for (BundleWire bundleWire : requiredBundleWires) {
			Map<String, Object> attributes = bundleWire.getCapability().getAttributes();

			if (attributes.containsKey(EXTENDER_NAMESPACE) &&
				attributes.get(EXTENDER_NAMESPACE).equals(CDI_CAPABILITY_NAME)) {

				Bundle providerWiringBundle = bundleWire.getProviderWiring().getBundle();

				if (providerWiringBundle.equals(_bundleContext.getBundle())) {
					BundleRequirement requirement = bundleWire.getRequirement();
					Map<String, Object> requirementAttributes = requirement.getAttributes();

					@SuppressWarnings("unchecked")
					List<String> beans = (List<String>)requirementAttributes.get(REQUIREMENT_BEANS_ATTRIBUTE);

					if (beans != null && !beans.isEmpty()) {
						return true;
					}
				}
			}
		}

		return false;
	}

	private BundleContext _bundleContext;
	private final ChangeCount _ccrChangeCount = new ChangeCount();
	private ServiceRegistration<CDIComponentRuntime> _ccrRegistration;
	private volatile CDICommand _command;
	private ServiceRegistration<?> _commandRegistration;

	private class ChangeObserverFactory implements Observer, ServiceFactory<CDIComponentRuntime> {

		@Override
		public CDIComponentRuntime getService(
			Bundle bundle,
			ServiceRegistration<CDIComponentRuntime> registration) {

			_registrations.add(registration);

			return ccr;
		}

		@Override
		public void ungetService(
			Bundle bundle, ServiceRegistration<CDIComponentRuntime> registration,
			CDIComponentRuntime service) {

			_registrations.remove(registration);
		}

		@Override
		public void update(Observable o, Object arg) {
			if (!(o instanceof ChangeCount)) {
				return;
			}

			ChangeCount changeCount = (ChangeCount)o;

			for (ServiceRegistration<CDIComponentRuntime> registration : _registrations) {
				Dictionary<String, Object> properties = registration.getReference().getProperties();
				properties.put(Constants.SERVICE_CHANGECOUNT, changeCount.get());
				registration.setProperties(properties);
			}
		}

		private final List<ServiceRegistration<CDIComponentRuntime>> _registrations = new CopyOnWriteArrayList<>();

	}

}