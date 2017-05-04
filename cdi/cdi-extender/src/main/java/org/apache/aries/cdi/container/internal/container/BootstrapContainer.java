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

import java.util.Collection;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.inject.spi.Extension;

import org.apache.aries.cdi.container.internal.literal.ReferenceLiteral;
import org.apache.aries.cdi.container.internal.model.BeansModel;
import org.apache.aries.cdi.container.internal.model.ConfigurationInjectionPoint;
import org.apache.aries.cdi.container.internal.model.ConfigurationModel;
import org.apache.aries.cdi.container.internal.model.ReferenceInjectionPoint;
import org.apache.aries.cdi.container.internal.model.ReferenceModel;
import org.apache.aries.cdi.container.internal.model.ServiceModel;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.util.ServiceLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.cdi.CdiEvent.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BootstrapContainer {

	public BootstrapContainer(
		BundleWiring bundleWiring, Map<ServiceReference<Extension>, Metadata<Extension>> extensions,
		CdiContainerState cdiContainerState) {

		_bundleWiring = bundleWiring;
		_externalExtensions = extensions;
		_cdiContainerState = cdiContainerState;

		_bundle = _bundleWiring.getBundle();

		// Add the internal extensions
		_extensions.add(
			new ExtensionMetadata(
				new ConfigurationExtension(_configurations, _bundle.getBundleContext()), _bundle.toString()));
		_extensions.add(
			new ExtensionMetadata(
				new ReferenceExtension(_references, _bundle.getBundleContext()), _bundle.toString()));
		_extensions.add(new ExtensionMetadata(new ServiceExtension(_services), _bundle.toString()));

		// Add extensions found from the bundle's classloader, such as those in the Bundle-ClassPath
		for (Metadata<Extension> meta : ServiceLoader.load(Extension.class, _bundleWiring.getClassLoader())) {
			_extensions.add(meta);
		}

		// Add external extensions
		for (Metadata<Extension> meta : _externalExtensions.values()) {
			_extensions.add(meta);
		}

		BeansModel beansModel = _cdiContainerState.getBeansModel();

		BeanDeploymentArchive beanDeploymentArchive = new BundleDeploymentArchive(
			bundleWiring, _cdiContainerState.getId(), beansModel.getBeanClassNames(), beansModel.getBeansXml(),
			_cdiContainerState.getExtenderBundle());

		Deployment deployment = new BundleDeployment(_extensions, beanDeploymentArchive);

		_bootstrap = new WeldBootstrap();

		_bootstrap.startContainer(_cdiContainerState.getId(), new SimpleEnvironment(), deployment);

		_beanManagerImpl = _bootstrap.getManager(beanDeploymentArchive);

		_cdiContainerState.setBeanManager(_beanManagerImpl);
		_cdiContainerState.setConfigurationDependencies(_configurations);
		_cdiContainerState.setReferenceDependencies(_references);

		_bootstrap.startInitialization();
		_bootstrap.deployBeans();

		processDescriptorConfigurations();
		processDescriptorReferences();
	}

	@Override
	protected BootstrapContainer clone() {
		BootstrapContainer bootstrapContainer = new BootstrapContainer(
			_bundleWiring, _externalExtensions, _cdiContainerState);

		bootstrapContainer._configurations.clear();
		bootstrapContainer._configurations.addAll(_configurations);

		return bootstrapContainer;
	}

	public void fire(Type state) {
		_cdiContainerState.fire(state);
	}

	public void fire(Type state, String payload) {
		_cdiContainerState.fire(state, payload);
	}

	public void fire(Type state, Throwable cause) {
		_cdiContainerState.fire(state, cause);

	}

	public BeanManagerImpl getBeanManagerImpl() {
		return _beanManagerImpl;
	}

	public WeldBootstrap getBootstrap() {
		return _bootstrap;
	}

	public Bundle getBundle() {
		return _bundle;
	}

	public BundleContext getBundleContext() {
		return _bundle.getBundleContext();
	}

	public CdiContainerState getCdiContainerState() {
		return _cdiContainerState;
	}

	public List<ConfigurationDependency> getConfigurations() {
		return _configurations;
	}

	public Collection<ReferenceModel> getReferenceModels() {
		return _cdiContainerState.getBeansModel().getReferenceModels();
	}

	public List<ReferenceDependency> getReferences() {
		return _references;
	}

	public Collection<ServiceModel> getServiceModels() {
		return _cdiContainerState.getBeansModel().getServiceModels();
	}

	public List<ServiceDeclaration> getServices() {
		return _services;
	}

	public boolean hasConfigurations() {
		return !_configurations.isEmpty();
	}

	public boolean hasReferences() {
		return !_references.isEmpty();
	}

	public Class<?> loadClass(String clazz) throws ClassNotFoundException {
		return _bundle.loadClass(clazz);
	}

	public ServiceRegistration<?> registerService(
		String[] classNames, Object serviceInstance, Dictionary<String, Object> properties) {

		return getBundleContext().registerService(classNames, serviceInstance, properties);
	}

	public void shutdown() {
		_bootstrap.shutdown();
	}

	private void processConfigurationModel(ConfigurationModel configurationModel) {
		try {
			Class<?> beanClass = _bundle.loadClass(configurationModel.beanClass());

			String[] pids = configurationModel.pids();

			ConfigurationDependency configurationDependency = new ConfigurationDependency(
				_bundle.getBundleContext(), pids, configurationModel.required(), beanClass.getName(),
				new ConfigurationInjectionPoint(beanClass, pids));

			_configurations.add(configurationDependency);
		}
		catch (ClassNotFoundException cnfe) {
			_log.error(
				"CDIe - osgi bean descriptor configuration processing cannot load class {}",
				configurationModel.beanClass(), cnfe);
		}
	}

	private void processDescriptorConfigurations() {
		Collection<ConfigurationModel> configurationModels =
			_cdiContainerState.getBeansModel().getConfigurationModels();

		for (ConfigurationModel configurationModel : configurationModels) {
			processConfigurationModel(configurationModel);
		}
	}

	private void processDescriptorReferences() {
		Collection<ReferenceModel> referenceModels = _cdiContainerState.getBeansModel().getReferenceModels();

		for (ReferenceModel referenceModel : referenceModels) {
			processReferenceModel(referenceModel);
		}
	}

	private void processReferenceModel(ReferenceModel referenceModel) {
		try {
			Class<?> beanClass = _bundle.loadClass(referenceModel.getBeanClass());

			ReferenceDependency referenceDependency = new ReferenceDependency(
				_beanManagerImpl, ReferenceLiteral.from(referenceModel.getTarget()),
				new ReferenceInjectionPoint(beanClass, referenceModel.getTarget()));

			_references.add(referenceDependency);
		}
		catch (ClassNotFoundException cnfe) {
			_log.error(
				"CDIe - osgi bean descriptor reference processing cannot load class {}",
				referenceModel.getBeanClass(), cnfe);
		}
		catch (InvalidSyntaxException ise) {
			_log.error("CDIe - osgi bean descriptor reference processing error", ise);
		}
	}

	private static final Logger _log = LoggerFactory.getLogger(BootstrapContainer.class);

	private final BeanManagerImpl _beanManagerImpl;
	private final WeldBootstrap _bootstrap;
	private final Bundle _bundle;
	private final BundleWiring _bundleWiring;
	private final CdiContainerState _cdiContainerState;
	private final List<ConfigurationDependency> _configurations = new CopyOnWriteArrayList<>();
	private final List<Metadata<Extension>> _extensions = new CopyOnWriteArrayList<>();
	private final Map<ServiceReference<Extension>, Metadata<Extension>> _externalExtensions;
	private final List<ReferenceDependency> _references = new CopyOnWriteArrayList<>();
	private final List<ServiceDeclaration> _services = new CopyOnWriteArrayList<>();

}