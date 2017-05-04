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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.service.cdi.CdiEvent;

class ConfigurationResolveAction {

	public ConfigurationResolveAction(BootstrapContainer bc) {
		_bc = bc;
	}

	public void add(ConfigurationManagedService configurationManagedService) {
		try {
			_lock.lock();

			_managedServices.add(configurationManagedService);
		}
		finally {
			_lock.unlock();
		}
	}

	public void close() {
		try {
			_lock.lock();

			_closing = true;

			_nextPhase.close();

			_nextPhase = null;

			for (ConfigurationDependency configurationDependency : _bc.getConfigurations()) {
				configurationDependency.close();
			}
		}
		finally {
			_lock.unlock();
		}
	}

	public void open() {
		try {
			_lock.lock();

			_closing = false;

			for (ConfigurationDependency configurationDependency : _bc.getConfigurations()) {
				configurationDependency.open(this);
			}

			if (configurationsAreResolved()) {
				_nextPhase = new Phase_Reference(_bc);

				_nextPhase.open();
			}
		}
		finally {
			_lock.unlock();
		}
	}

	public void addingConfiguration() {
		try {
			_lock.lock();

			if (_closing) return;

			if (!_resolved && configurationsAreResolved()) {
				_resolved = true;

				if (_nextPhase != null) {
					close();

					_bc = _bc.clone();

					_bc.fire(CdiEvent.Type.WAITING_FOR_CONFIGURATIONS, _bc.getConfigurations().toString());

					open();
				}

				if (_nextPhase == null) {
					_nextPhase = new Phase_Reference(_bc);

					_nextPhase.open();
				}
			}
		}
		catch (Throwable t) {
			_bc.fire(CdiEvent.Type.FAILURE, t);

			// TODO this should close the CDI container!
		}
		finally {
			_lock.unlock();
		}
	}

	public void removeProperties() {
		try {
			_lock.lock();

			if (_closing) return;

			if (_nextPhase != null) {
				_resolved = false;

				close();

				_bc = _bc.clone();

				_bc.fire(CdiEvent.Type.WAITING_FOR_CONFIGURATIONS, _bc.getConfigurations().toString());

				open();
			}
		}
		catch (Throwable t) {
			_bc.fire(CdiEvent.Type.FAILURE, t);

			// TODO this should close the CDI container!
		}
		finally {
			_lock.unlock();
		}
	}

	public void updateProperties() {
	}

	private boolean configurationsAreResolved() {
		for (ConfigurationDependency dependency : _bc.getConfigurations()) {
			if (!dependency.isResolved()) {
				return false;
			}
		}

		return true;
	}

	private volatile BootstrapContainer _bc;
	private volatile boolean _closing = false;
	private volatile Phase _nextPhase;
	private volatile boolean _resolved = false;

	private final Lock _lock = new ReentrantLock();
	private final List<ConfigurationManagedService> _managedServices = new CopyOnWriteArrayList<>();

}