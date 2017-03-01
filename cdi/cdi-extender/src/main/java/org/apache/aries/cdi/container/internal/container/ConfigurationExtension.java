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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionPoint;

import org.osgi.service.cdi.annotations.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ConfigurationExtension implements Extension {

	public ConfigurationExtension(List<ConfigurationDependency> configurations) {
		_configurations = configurations;
	}

	void processInjectionTarget(
		@Observes @SuppressWarnings("rawtypes") ProcessInjectionPoint pip, BeanManager manager) {

		InjectionPoint injectionPoint = pip.getInjectionPoint();
		Annotated annotated = injectionPoint.getAnnotated();
		Configuration configuration = annotated.getAnnotation(Configuration.class);

		if (configuration == null) {
			return;
		}

		Class<?> beanClass = injectionPoint.getBean().getBeanClass();

		ConfigurationDependency configurationDependency = new ConfigurationDependency(
			configuration.value(), beanClass.getName());

		_configurations.add(configurationDependency);

		if (_log.isDebugEnabled()) {
			_log.debug("CDIe - Found OSGi configuration dependency {}", configurationDependency);
		}
	}

	private static final Logger _log = LoggerFactory.getLogger(ReferenceExtension.class);

	private final List<ConfigurationDependency> _configurations;

}
