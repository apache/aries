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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.aries.cdi.container.internal.command.CdiCommand;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.phase.Init;
import org.apache.aries.cdi.container.internal.phase.Phase;
import org.apache.felix.utils.extender.Extension;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CdiBundle implements Extension {

	public CdiBundle(Bundle extenderBundle, Bundle bundle, CdiCommand command) {
		_extenderBundle = extenderBundle;
		_bundle = bundle;
		_command = command;
	}

	@Override
	public void start() throws Exception {
		boolean acquired = false;

		try {
			try {
				acquired = _lock.tryLock(DEFAULT_STOP_TIMEOUT, TimeUnit.MILLISECONDS);
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

			_containerState = new ContainerState(_bundle, _extenderBundle);

			_command.add(_bundle, _containerState);

			try {
				_nextPhase = new Init(_bundle, _containerState);

				_nextPhase.open();
			}
			catch (Throwable t) {
				if (_nextPhase != null) {
					_nextPhase.close();
				}

				_command.remove(_bundle);

				_containerState = null;
			}
		}
		finally {
			if (acquired) {
				_lock.unlock();
			}
		}
	}

	@Override
	public void destroy() throws Exception {
		boolean acquired = false;

		try {
			try {
				acquired = _lock.tryLock(DEFAULT_STOP_TIMEOUT, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();

				_log.warn(
					"The wait for bundle {0}/{1} being started before destruction has been interrupted.",
					_bundle.getSymbolicName(), _bundle.getBundleId(), e);
			}

			_command.remove(_bundle);

			_nextPhase.close();

			_nextPhase = null;

			_containerState = null;

			if (_log.isDebugEnabled()) {
				_log.debug("CDIe - bundle removed {}", _bundle);
			}
		}
		finally {
			if (acquired) {
				_lock.unlock();
			}
		}
	}

	private static final long DEFAULT_STOP_TIMEOUT = 60000; // TODO make this configurable

	private static final Logger _log = LoggerFactory.getLogger(CdiBundle.class);

	private final Bundle _bundle;
	private final CdiCommand _command;
	private volatile ContainerState _containerState;
	private final Bundle _extenderBundle;
	private final Lock _lock = new ReentrantLock();
	private volatile Phase _nextPhase;

}
