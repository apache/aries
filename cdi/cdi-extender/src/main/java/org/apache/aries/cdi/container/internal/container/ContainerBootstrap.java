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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.inject.spi.Extension;

import org.apache.aries.cdi.container.internal.component.ComponentRuntimeExtension;
import org.apache.aries.cdi.container.internal.context.BundleContextExtension;
import org.apache.aries.cdi.container.internal.extension.ExtensionMetadata;
import org.apache.aries.cdi.container.internal.model.BeansModel;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.util.ServiceLoader;

public class ContainerBootstrap {

	public ContainerBootstrap(
		ContainerState containerState,
		Collection<Metadata<Extension>> externalExtensions) {

		_containerState = containerState;
		_externalExtensions = externalExtensions;

		BeansModel beansModel = _containerState.beansModel();

		List<Metadata<Extension>> extensions = new CopyOnWriteArrayList<>();

		// Add the internal extensions
		extensions.add(
			new ExtensionMetadata(
				new BundleContextExtension(_containerState.bundleContext()),
				_containerState.id()));
		extensions.add(
			new ExtensionMetadata(
				new ComponentRuntimeExtension(_containerState),
				_containerState.id()));

		// Add extensions found from the bundle's classloader, such as those in the Bundle-ClassPath
		for (Metadata<Extension> meta : ServiceLoader.load(Extension.class, _containerState.classLoader())) {
			extensions.add(meta);
		}

		// Add external extensions
		for (Metadata<Extension> meta : _externalExtensions) {
			extensions.add(meta);
		}

		BeanDeploymentArchive beanDeploymentArchive = new ContainerDeploymentArchive(
			_containerState.loader(), _containerState.id(), beansModel.getBeanClassNames(),
			beansModel.getBeansXml());

		Deployment deployment = new ContainerDeployment(extensions, beanDeploymentArchive);

		_bootstrap = new WeldBootstrap();

		_bootstrap.startExtensions(extensions);
		_bootstrap.startContainer(_containerState.id(), new ContainerEnvironment(), deployment);

		_beanManagerImpl = _bootstrap.getManager(beanDeploymentArchive);
		_containerState.setBeanManager(_beanManagerImpl);

		_bootstrap.startInitialization();
		_bootstrap.deployBeans();
	}

	public BeanManagerImpl getBeanManagerImpl() {
		return _beanManagerImpl;
	}

	public WeldBootstrap getBootstrap() {
		return _bootstrap;
	}

	public void shutdown() {
		_bootstrap.shutdown();
	}

	private final BeanManagerImpl _beanManagerImpl;
	private final WeldBootstrap _bootstrap;
	private final ContainerState _containerState;
	private final Collection<Metadata<Extension>> _externalExtensions;

}