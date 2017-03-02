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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;

import org.apache.aries.cdi.container.internal.model.BeansModel;
import org.apache.aries.cdi.container.internal.model.ConfigurationModel;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.manager.BeanManagerImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.cdi.CdiEvent;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Phase_Configuration implements Phase {

	public Phase_Configuration(
		Bundle bundle, CdiContainerState cdiContainerState, Map<ServiceReference<Extension>,
		Metadata<Extension>> extensions) {

		_bundle = bundle;
		_cdiContainerState = cdiContainerState;
		_extensions = extensions;
		_bundleContext = _bundle.getBundleContext();
		_bundleWiring = _bundle.adapt(BundleWiring.class);

		_configAdminTracker = new ServiceTracker<>(
			_cdiContainerState.getExtenderBundle().getBundleContext(), ConfigurationAdmin.class, null);

		_configAdminTracker.open();

		_cdiContainerState.setConfigurationDependencies(_configurations);
		_cdiContainerState.setReferenceDependencies(_references);
	}

	@Override
	public void close() {
//		if (_serviceTracker != null) {
//			_serviceTracker.close();
//
//			_serviceTracker = null;
//		}
//		else {
			if (_nextPhase != null) {
				_nextPhase.close();

				_nextPhase = null;
			}
//		}

		_configAdminTracker.close();
	}

	@Override
	public void open() {
		BeansModel beansModel = _cdiContainerState.getBeansModel();

		BeanDeploymentArchive beanDeploymentArchive = new BundleDeploymentArchive(
			_bundleWiring, _cdiContainerState.getId(), beansModel.getBeanClassNames(), beansModel.getBeansXml(),
			_cdiContainerState.getExtenderBundle());

		WeldBootstrap bootstrap = new WeldBootstrap();

		List<Metadata<Extension>> extensions = new ArrayList<>();

		// Add the internal extensions
		extensions.add(
			new ExtensionMetadata(new ConfigurationExtension(_configurations), _bundle.toString()));
		extensions.add(
			new ExtensionMetadata(new ReferenceExtension(_references, _bundleContext), _bundle.toString()));
		extensions.add(new ExtensionMetadata(new ServiceExtension(_services), _bundle.toString()));

		// Add extensions found from the bundle's classloader, such as those in the Bundle-ClassPath
		for (Metadata<Extension> meta : bootstrap.loadExtensions(_bundleWiring.getClassLoader())) {
			extensions.add(meta);
		}

		// Add external extensions
		for (Metadata<Extension> meta : _extensions.values()) {
			extensions.add(meta);
		}

		Deployment deployment = new BundleDeployment(extensions, beanDeploymentArchive);

		bootstrap.startContainer(_cdiContainerState.getId(), new SimpleEnvironment(), deployment);

		BeanManager beanManager = bootstrap.getManager(beanDeploymentArchive);

		_cdiContainerState.setBeanManager(beanManager);

		bootstrap.startInitialization();
		bootstrap.deployBeans();

		processDescriptorConfigurations((BeanManagerImpl)beanManager);

		if (!_configurations.isEmpty()) {
			_cdiContainerState.fire(CdiEvent.Type.WAITING_FOR_CONFIGURATIONS, _configurations.toString());

			// TODO configuration listener
			temporary: {
				_nextPhase = new Phase_Reference(_references, _services, _cdiContainerState, bootstrap);

				_nextPhase.open();
			}
		}
		else {
			_nextPhase = new Phase_Reference(_references, _services, _cdiContainerState, bootstrap);

			_nextPhase.open();
		}
	}

	private void processConfigurationModel(ConfigurationModel configurationModel, BeanManagerImpl beanManagerImpl) {
		_configurations.add(
			new ConfigurationDependency(new String[] {configurationModel.getPid()}, configurationModel.getPid()));
	}

	private void processDescriptorConfigurations(BeanManagerImpl beanManagerImpl) {
		Collection<ConfigurationModel> configurationModels =
			_cdiContainerState.getBeansModel().getConfigurationModels();

		for (ConfigurationModel configurationModel : configurationModels) {
			processConfigurationModel(configurationModel, beanManagerImpl);
		}
	}

	private static final Logger _log = LoggerFactory.getLogger(Phase_Configuration.class);

	private final Bundle _bundle;
	private final BundleContext _bundleContext;
	private final BundleWiring _bundleWiring;
	private final CdiContainerState _cdiContainerState;
	private final ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> _configAdminTracker;
	private final List<ConfigurationDependency> _configurations = new CopyOnWriteArrayList<>();
	private final Map<ServiceReference<Extension>, Metadata<Extension>> _extensions;
	private Phase _nextPhase;
	private final List<ReferenceDependency> _references = new CopyOnWriteArrayList<>();
	private final List<ServiceDeclaration> _services = new CopyOnWriteArrayList<>();

	class ConfigurationDependencyListener implements ConfigurationListener {

		@Override
		public void configurationEvent(ConfigurationEvent event) {
			_log.info("CDIe - configuration event {}", event);
		}

	}

}