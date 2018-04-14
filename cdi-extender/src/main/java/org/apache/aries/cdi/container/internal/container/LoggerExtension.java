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

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.configurator.BeanConfigurator;

import org.osgi.service.log.FormatterLogger;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;

public class LoggerExtension implements Extension {

	public LoggerExtension(ContainerState containerState) {
		_containerState = containerState;
	}

	void afterBeanDiscovery(@Observes AfterBeanDiscovery abd) {
		final LoggerFactory lf = _containerState.containerLogs().getLoggerFactory();

		BeanConfigurator<FormatterLogger> formatterLoggerBean = abd.addBean();
		formatterLoggerBean.addType(FormatterLogger.class);
		formatterLoggerBean.produceWith(
			i -> {
				InjectionPoint ip = i.select(InjectionPoint.class).get();
				return lf.getLogger(
					ip.getMember().getDeclaringClass().getName(),
					FormatterLogger.class);
			}
		);

		BeanConfigurator<Logger> loggerBean = abd.addBean();
		loggerBean.addType(Logger.class);
		loggerBean.produceWith(
			i -> {
				InjectionPoint ip = i.select(InjectionPoint.class).get();
				return lf.getLogger(
					ip.getMember().getDeclaringClass().getName(),
					Logger.class);
			}
		);
	}

	private final ContainerState _containerState;

}
