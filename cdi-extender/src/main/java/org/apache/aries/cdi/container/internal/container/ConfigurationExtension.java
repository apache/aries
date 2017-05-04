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
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.inject.Named;

import org.osgi.framework.BundleContext;
import org.osgi.service.cdi.annotations.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ConfigurationExtension implements Extension {

	public ConfigurationExtension(List<ConfigurationDependency> configurations, BundleContext bundleContext) {
		_configurations = configurations;
		_bundleContext = bundleContext;
	}

	void processInjectionTarget(
		@Observes @SuppressWarnings("rawtypes") ProcessInjectionPoint pip) {

		InjectionPoint injectionPoint = pip.getInjectionPoint();
		Annotated annotated = injectionPoint.getAnnotated();
		Configuration configuration = annotated.getAnnotation(Configuration.class);

		if (configuration == null) {
			return;
		}

		Class<?> beanClass = injectionPoint.getBean().getBeanClass();

		String name = beanClass.getName();

		Named named = annotated.getAnnotation(Named.class);

		if (named != null) {
			name = named.value();
		}

		ConfigurationDependency configurationDependency = new ConfigurationDependency(
			_bundleContext, configuration.value(), configuration.required(), name,
			injectionPoint);

		_configurations.add(configurationDependency);

		if (_log.isDebugEnabled()) {
			_log.debug("CDIe - Found OSGi configuration dependency {}", configurationDependency);
		}
	}

	private static final Logger _log = LoggerFactory.getLogger(ConfigurationExtension.class);

	private final BundleContext _bundleContext;
	private final List<ConfigurationDependency> _configurations;

}
