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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.aries.cdi.container.internal.container.ContainerDiscovery;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.model.BeansModel;
import org.apache.aries.cdi.container.internal.model.BeansModelBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Init implements Phase {

	public Init(Bundle bundle, ContainerState containerState) {
		_containerState = containerState;

		BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);

		BeansModel beansModel = new BeansModelBuilder(_containerState.classLoader(), bundleWiring).build();

		_containerState.setBeansModel(beansModel);
	}

	@Override
	public void close() {
		_lock.lock();

		try {
			_extensionPhase.close();

			_extensionPhase = null;
		}
		finally {
			_lock.unlock();
		}
	}

	@Override
	public void open() {
		_lock.lock();

		try {
			if (_log.isDebugEnabled()) {
				_log.debug("CDIe - Starting discovery phase on {}", _containerState);
			}

			ContainerDiscovery.discover(_containerState);

			if (_log.isDebugEnabled()) {
				_log.debug("CDIe - Finished discovery phase on {}", _containerState);
			}

			if (_log.isDebugEnabled()) {
				_log.debug("CDIe - Begin extension phase on {}", _containerState);
			}

			_extensionPhase = new Phase_Extension(_containerState);

			_extensionPhase.open();
		}
		finally {
			_lock.unlock();
		}
	}

	private static final Logger _log = LoggerFactory.getLogger(Init.class);

	private final ContainerState _containerState;
	private volatile Phase _extensionPhase;
	private final Lock _lock = new ReentrantLock();

}