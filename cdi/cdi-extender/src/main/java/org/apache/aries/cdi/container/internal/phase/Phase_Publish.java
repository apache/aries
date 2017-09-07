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

package org.apache.aries.cdi.container.internal.phase;

import java.util.Collection;

import javax.enterprise.inject.spi.Extension;

import org.apache.aries.cdi.container.internal.container.ContainerBootstrap;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.osgi.service.cdi.CdiEvent;
import org.osgi.service.cdi.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Phase_Publish implements Phase {

	public Phase_Publish(
		ContainerState containerState,
		Collection<Metadata<Extension>> extensions) {

		_containerState = containerState;
		_extensions = extensions;
	}

	@Override
	public void close() {
		_containerState.serviceRegistrator().close();
		_containerState.beanManagerRegistrator().close();
		_containerState.serviceComponents().clear();
		_cb.shutdown();
	}

	@Override
	public void open() {
		_containerState.fire(CdiEvent.Type.SATISFIED);

		try {
			_cb = new ContainerBootstrap(_containerState, _extensions);

			WeldBootstrap bootstrap = _cb.getBootstrap();

			bootstrap.validateBeans();
			bootstrap.endInitialization();

			_containerState.referenceCallbacks().values().stream().flatMap(
				map -> map.values().stream()
			).forEach(
				callback -> callback.flush()
			);

			processServices();

			_containerState.beanManagerRegistrator().registerService(_cb.getBeanManager());

			_containerState.fire(CdiEvent.Type.CREATED);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void processServices() {
		_containerState.serviceComponents().values().stream().filter(
			serviceDeclaration ->
				serviceDeclaration.getScope() == ServiceScope.BUNDLE ||
				serviceDeclaration.getScope() == ServiceScope.PROTOTYPE ||
				serviceDeclaration.getScope() == ServiceScope.SINGLETON
		).forEach(
			s -> {
				if (_log.isDebugEnabled()) {
					_log.debug("CDIe - Publishing component {} as '{}' service.", s.getName(), s.getScope());
				}

				_containerState.serviceRegistrator().registerService(
					s.getClassNames(), s.getServiceInstance(), s.getServiceProperties());
			}
		);
	}

	private static final Logger _log = LoggerFactory.getLogger(Phase_Publish.class);

	private volatile ContainerBootstrap _cb;
	private final ContainerState _containerState;
	private final Collection<Metadata<Extension>> _extensions;

}