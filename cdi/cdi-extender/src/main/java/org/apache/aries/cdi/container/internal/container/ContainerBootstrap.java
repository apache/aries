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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.inject.spi.Extension;

import org.apache.aries.cdi.container.internal.container.Op.Mode;
import org.apache.aries.cdi.container.internal.container.Op.Type;
import org.apache.aries.cdi.container.internal.model.ExtendedExtensionDTO;
import org.apache.aries.cdi.container.internal.model.FactoryComponent;
import org.apache.aries.cdi.container.internal.model.SingleComponent;
import org.apache.aries.cdi.container.internal.util.Syncro;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.util.ServiceLoader;
import org.osgi.service.log.Logger;

public class ContainerBootstrap extends Phase {

	public ContainerBootstrap(
		ContainerState containerState,
		ConfigurationListener.Builder configurationBuilder,
		SingleComponent.Builder singleBuilder,
		FactoryComponent.Builder factoryBuilder) {

		super(containerState, null);

		_configurationBuilder = configurationBuilder;
		_singleBuilder = singleBuilder;
		_factoryBuilder = factoryBuilder;
		_log = containerState.containerLogs().getLogger(getClass());
	}

	@Override
	public boolean close() {
		try (Syncro syncro = _lock.open()) {
			if (_bootstrap != null) {
				_log.debug(l -> l.debug("CCR container bootstrap shutdown on {}", _bootstrap));
				_bootstrap.shutdown();
				_bootstrap = null;
			}

			return true;
		}
		catch (Throwable t) {
			_log.error(l -> l.error("CCR Failure in container bootstrap shutdown on {}", _bootstrap, t));

			return false;
		}
	}

	@Override
	public Op closeOp() {
		return Op.of(Mode.CLOSE, Type.CONTAINER_BOOTSTRAP, containerState.id());
	}

	@Override
	public boolean open() {
		try (Syncro syncro = _lock.open()) {
			if (_bootstrap != null) {
				return true;
			}

			List<Metadata<Extension>> extensions = new CopyOnWriteArrayList<>();

			// Add the internal extensions
			extensions.add(
					new ExtensionMetadata(
							new BundleContextExtension(containerState.bundleContext()),
							containerState.id()));
			extensions.add(
					new ExtensionMetadata(
							new RuntimeExtension(containerState, _configurationBuilder, _singleBuilder, _factoryBuilder),
							containerState.id()));
			extensions.add(
					new ExtensionMetadata(
							new LoggerExtension(containerState),
							containerState.id()));

			// Add extensions found from the bundle's class loader, such as those in the Bundle-ClassPath
			ServiceLoader.load(Extension.class, containerState.classLoader()).forEach(extensions::add);

			// Add external extensions
			containerState.containerDTO().extensions.stream().map(
					ExtendedExtensionDTO.class::cast
					).map(
							e -> new ExtensionMetadata(e.extension.getService(), e.template.serviceFilter)
							).forEach(extensions::add);

			_bootstrap = new WeldBootstrap();

			BeanDeploymentArchive beanDeploymentArchive = new ContainerDeploymentArchive(
					containerState.loader(),
					containerState.id(),
					containerState.beansModel().getBeanClassNames(),
					containerState.beansModel().getBeansXml());

			Deployment deployment = new ContainerDeployment(extensions, beanDeploymentArchive);

			_bootstrap.startExtensions(extensions);
			_bootstrap.startContainer(containerState.id(), new ContainerEnvironment(), deployment);
			_bootstrap.startInitialization();
			_bootstrap.deployBeans();
			_bootstrap.validateBeans();
			_bootstrap.endInitialization();

			return true;
		}
	}

	@Override
	public Op openOp() {
		return Op.of(Mode.OPEN, Type.CONTAINER_BOOTSTRAP, containerState.id());
	}


	private volatile WeldBootstrap _bootstrap;
	private final ConfigurationListener.Builder _configurationBuilder;
	private final FactoryComponent.Builder _factoryBuilder;
	private final SingleComponent.Builder _singleBuilder;
	private final Syncro _lock = new Syncro(true);
	private final Logger _log;

}