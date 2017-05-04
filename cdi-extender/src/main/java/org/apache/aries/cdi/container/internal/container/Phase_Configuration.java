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

import java.util.Map;

import javax.enterprise.inject.spi.Extension;

import org.jboss.weld.bootstrap.spi.Metadata;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.cdi.CdiEvent;

public class Phase_Configuration implements Phase {

	public Phase_Configuration(
		CdiContainerState cdiContainerState,
		Map<ServiceReference<Extension>, Metadata<Extension>> extensions) {

		_cdiContainerState = cdiContainerState;
		_extensions = extensions;
		_bundleWiring = _cdiContainerState.getBundle().adapt(BundleWiring.class);
	}

	@Override
	public void close() {
		if (_resolveAction != null) {
			_resolveAction.close();

			_resolveAction = null;
		}
		else if (_nextPhase != null) {
			_nextPhase.close();

			_nextPhase = null;
		}
	}

	@Override
	public void open() {
		BootstrapContainer bc = new BootstrapContainer(_bundleWiring, _extensions, _cdiContainerState);

		if (bc.hasConfigurations()) {
			_cdiContainerState.fire(CdiEvent.Type.WAITING_FOR_CONFIGURATIONS, bc.getConfigurations().toString());

			_resolveAction = new ConfigurationResolveAction(bc);

			_resolveAction.open();
		}
		else {
			_nextPhase = new Phase_Reference(bc);

			_nextPhase.open();
		}
	}

	private final BundleWiring _bundleWiring;
	private final CdiContainerState _cdiContainerState;
	private final Map<ServiceReference<Extension>, Metadata<Extension>> _extensions;
	private Phase _nextPhase;
	private ConfigurationResolveAction _resolveAction;

}