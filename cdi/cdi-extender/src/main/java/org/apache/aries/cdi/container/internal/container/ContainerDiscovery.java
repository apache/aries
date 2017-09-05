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

import java.util.Collections;
import java.util.List;

import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.Extension;

import org.apache.aries.cdi.container.internal.component.ComponentDiscoveryExtension;
import org.apache.aries.cdi.container.internal.extension.ExtensionMetadata;
import org.apache.aries.cdi.container.internal.model.BeansModel;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.bootstrap.spi.Metadata;

public class ContainerDiscovery {

	public static void discover(ContainerState containerState) {
		String id = containerState.id() + "-discovery";

		BeansModel beansModel = containerState.beansModel();

		BeanDeploymentArchive beanDeploymentArchive = new ContainerDeploymentArchive(
			containerState.loader(), id, beansModel.getBeanClassNames(),
			beansModel.getBeansXml());

		ExtensionMetadata extension = new ExtensionMetadata(
			new ComponentDiscoveryExtension(beansModel), id);

		List<Metadata<Extension>> extensions = Collections.singletonList(extension);

		Deployment deployment = new ContainerDeployment(
			Collections.singletonList(extension), beanDeploymentArchive);

		WeldBootstrap _bootstrap = new WeldBootstrap();

		_bootstrap.startExtensions(extensions);
		_bootstrap.startContainer(id, new ContainerEnvironment(), deployment);
		_bootstrap.startInitialization();
		_bootstrap.deployBeans();
		//_bootstrap.validateBeans();
		//_bootstrap.endInitialization();
		_bootstrap.shutdown();

		validate(containerState);
	}

	private static void validate(ContainerState containerState) {
		containerState.beansModel().getComponentModels().stream().forEach(
			componentModel -> {
				if (!componentModel.found()) {
					throw new DefinitionException(
						String.format(
							"Did not find bean for <component> description %s",
							componentModel));
				}

				componentModel.getReferences().stream().forEach(
					referenceModel -> {
						if (!referenceModel.found()) {
							throw new DefinitionException(
								String.format(
									"Did not find injection point for <reference> description %s",
									referenceModel));
						}
					}
				);

				componentModel.getConfigurations().stream().forEach(
					configurationModel -> {
						if (!configurationModel.found()) {
							throw new DefinitionException(
								String.format(
									"Did not find injection point for <configuration> description %s",
									configurationModel));
						}
					}
				);
			}
		);
	}

}