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

package org.apache.aries.cdi.container.internal;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.aries.cdi.container.internal.command.CdiCommand;
import org.apache.aries.cdi.container.internal.container.CdiContainerState;
import org.apache.aries.cdi.container.internal.container.Phase_1_Init;
import org.apache.felix.utils.extender.Extension;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.CdiEvent;
import org.osgi.service.cdi.CdiListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CdiBundleExtension implements Extension {

	public CdiBundleExtension(
		Bundle extenderBundle, Bundle bundle, Map<ServiceReference<CdiListener>, CdiListener> listeners,
		CdiCommand command) {

		_extenderBundle = extenderBundle;
		_bundle = bundle;
		_listeners = listeners;
		_command = command;
	}

	@Override
	public void start() throws Exception {
		boolean acquired = false;
		try {
			try {
				acquired = _stateLock.tryLock(DEFAULT_STOP_TIMEOUT, TimeUnit.MILLISECONDS);
			}
			catch ( InterruptedException e ) {
				Thread.currentThread().interrupt();

				_log.warn(
					"The wait for bundle {0}/{1} being destroyed before starting has been interrupted.",
					_bundle.getSymbolicName(), _bundle.getBundleId(), e );
			}

			if (_log.isDebugEnabled()) {
				_log.debug("CDIe - bundle detected {}", _bundle);
			}

			CdiContainerState cdiHelper = new CdiContainerState(_bundle, _extenderBundle, _listeners);

			_command.add(_bundle.getBundleId(), cdiHelper);

			Phase_1_Init initPhase = null;

			try {
				initPhase = new Phase_1_Init(_bundle, cdiHelper);

				initPhase.open();

				_initPhase = initPhase;
			}
			catch (Throwable t) {
				cdiHelper.fire(CdiEvent.State.FAILURE, t);

				if (initPhase != null) {
					initPhase.close();
				}

				_command.remove(_bundle.getBundleId());

				cdiHelper.close();
			}
		}
		finally {
			if (acquired) {
				_stateLock.unlock();
			}
		}
	}

	@Override
	public void destroy() throws Exception {
		boolean acquired = false;
		try {
			try {
				acquired = _stateLock.tryLock(DEFAULT_STOP_TIMEOUT, TimeUnit.MILLISECONDS);

			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				_log.warn(
					"The wait for bundle {0}/{1} being started before destruction has been interrupted.",
					_bundle.getSymbolicName(), _bundle.getBundleId(), e);
			}

			_initPhase.close();

			_initPhase = null;

			_command.remove(_bundle.getBundleId());

			if (_log.isDebugEnabled()) {
				_log.debug("CDIe - bundle removed {}", _bundle);
			}
		}
		finally {
			if (acquired) {
				_stateLock.unlock();
			}
		}
	}

	private static final long DEFAULT_STOP_TIMEOUT = 60000;

	private static final Logger _log = LoggerFactory.getLogger(CdiBundleExtension.class);

	private final Bundle _bundle;
	private final CdiCommand _command;
	private final Bundle _extenderBundle;
	private final Map<ServiceReference<CdiListener>, CdiListener> _listeners;
	private Phase_1_Init _initPhase;
	private final Lock _stateLock = new ReentrantLock();

}
