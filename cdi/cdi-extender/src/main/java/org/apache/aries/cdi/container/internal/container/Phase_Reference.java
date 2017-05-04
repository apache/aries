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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.CdiEvent;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Phase_Reference implements Phase {

	public Phase_Reference(BootstrapContainer bc) {
		_bc = bc;
	}

	@Override
	public void close() {
		if (_serviceTracker != null) {
			_serviceTracker.close();

			_serviceTracker = null;
		}
		else {
			_lock.lock();

			try {
				if (_nextPhase != null) {
					_nextPhase.close();

					_nextPhase = null;
				}
			}
			finally {
				_lock.unlock();
			}
		}
	}

	@Override
	public void open() {
		if (_bc.hasReferences()) {
			Filter filter = FilterBuilder.createReferenceFilter(_bc.getReferences());

			_bc.fire(CdiEvent.Type.WAITING_FOR_SERVICES, filter.toString());

			_serviceTracker = new ServiceTracker<>(_bc.getBundleContext(), filter, new ReferencePhaseCustomizer());

			_serviceTracker.open();
		}

		_lock.lock();

		try {
			if ((_nextPhase == null) && dependenciesAreEmptyOrAllOptional()) {
				_nextPhase = new Phase_Publish(_bc);

				_nextPhase.open();
			}
		}
		finally {
			_lock.unlock();
		}
	}

	private boolean dependenciesAreEmptyOrAllOptional() {
		if (!_bc.hasReferences()) {
			return true;
		}

		for (ReferenceDependency referenceDependency : _bc.getReferences()) {
			if (referenceDependency.getMinCardinality() > 0) {
				return false;
			}
		}

		return true;
	}

	private final BootstrapContainer _bc;
	private final Lock _lock = new ReentrantLock(true);
	private Phase _nextPhase;

	ServiceTracker<?, ?> _serviceTracker;

	private class ReferencePhaseCustomizer implements ServiceTrackerCustomizer<Object, Object> {

		@Override
		public Object addingService(ServiceReference<Object> reference) {
			_lock.lock();

			try {
				if (_nextPhase != null) {
					return null;
				}

				boolean matches = false;
				boolean resolved = true;

				for (ReferenceDependency referenceDependency : _bc.getReferences()) {
					if (referenceDependency.matches(reference)) {
						referenceDependency.resolve(reference);
						matches = true;
					}
					if (!referenceDependency.isResolved()) {
						resolved = false;
					}
				}

				if (!matches) {
					return null;
				}

				if (resolved) {
					_nextPhase = new Phase_Publish(_bc);

					_nextPhase.open();
				}

				return new Object();
			}
			finally {
				_lock.unlock();
			}
		}

		@Override
		public void modifiedService(ServiceReference<Object> reference, Object object) {
		}

		@Override
		public void removedService(ServiceReference<Object> reference, Object object) {
			_lock.lock();

			try {
				if (_nextPhase != null) {
					_nextPhase.close();

					_nextPhase = null;

					_bc.fire(CdiEvent.Type.WAITING_FOR_SERVICES);
				}

				for (ReferenceDependency referenceDependency : _bc.getReferences()) {
					if (referenceDependency.matches(reference)) {
						referenceDependency.unresolve(reference);
					}
				}
			}
			finally {
				_lock.unlock();
			}
		}

	}

}
