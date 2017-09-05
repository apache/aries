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

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import javax.enterprise.inject.spi.Extension;

import org.apache.aries.cdi.container.internal.component.ComponentModel;
import org.apache.aries.cdi.container.internal.configuration.ConfigurationCallback;
import org.apache.aries.cdi.container.internal.configuration.ConfigurationManagedService;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.osgi.framework.Constants;
import org.osgi.service.cdi.CdiEvent;
import org.osgi.service.cdi.annotations.Configuration;
import org.osgi.service.cdi.annotations.ConfigurationPolicy;

public class Phase_Configuration implements Phase {

	public Phase_Configuration(
		ContainerState containerState,
		Collection<Metadata<Extension>> extensions) {

		_containerState = containerState;
		_extensions = extensions;

		_componentModels = _containerState.beansModel().getComponentModels();
	}

	@Override
	public void close() {
		_lock.lock();

		try {
			if (_nextPhase != null) {
				_nextPhase.close();

				_nextPhase = null;
			}

			_containerState.managedServiceRegistrator().close();

			_containerState.configurationCallbacks().clear();
		}
		finally {
			_lock.unlock();
		}
	}

	@Override

	public void open() {
		_lock.lock();

		try {
			_containerState.fire(CdiEvent.Type.WAITING_FOR_CONFIGURATIONS);

			openConfigurations();

			callbacksInit();
		}
		finally {
			_lock.unlock();
		}
	}

	boolean callbacksExist() {
		return _containerState.configurationCallbacks().values().stream().flatMap(
			entry -> entry.values().stream()
		).findFirst().isPresent();
	}

	void callbacksInit() {
		_containerState.configurationCallbacks().values().stream().flatMap(
			valueMap -> valueMap.values().stream()
		).forEach(
			cc -> cc.init()
		);
	}

	boolean callbacksResolved() {
		return !_containerState.configurationCallbacks().values().stream().flatMap(
			valueMap -> valueMap.values().stream()
		).filter(
			c -> !c.resolved()
		).findFirst().isPresent();
	}

	void openConfigurations() {
		Consumer<ConfigurationCallback> onAdd = cc -> {
			_lock.lock();
			try {
				if (callbacksResolved()) {
					if ((_nextPhase != null)) {
						_nextPhase.close();
						_nextPhase = null;
					}

					if ((_nextPhase == null)) {
						_nextPhase = new Phase_Reference(_containerState, _extensions);
						_nextPhase.open();
					}
				}
			}
			finally {
				_lock.unlock();
			}
		},
		onUpdate = cc -> {
			_lock.lock();
			try {
				_nextPhase.close();
				_nextPhase = new Phase_Reference(_containerState, _extensions);
				_nextPhase.open();
			}
			finally {
				_lock.unlock();
			}
		},
		onRemove = cc -> {
			_lock.lock();
			try {
				if (_nextPhase != null) {
					_nextPhase.close();
				}
				if (!callbacksResolved()) {
					_nextPhase = null;
					_containerState.fire(CdiEvent.Type.WAITING_FOR_CONFIGURATIONS);
				}
				else {
					_nextPhase = new Phase_Reference(_containerState, _extensions);
					_nextPhase.open();
				}
			}
			finally {
				_lock.unlock();
			}
		};

		_componentModels.stream().forEach(
			componentModel -> openConfigurations(componentModel, onAdd, onUpdate, onRemove)
		);
	}

	void openConfigurations(
		ComponentModel componentModel,
		Consumer<ConfigurationCallback> onAdd,
		Consumer<ConfigurationCallback> onUpdate,
		Consumer<ConfigurationCallback> onRemove) {

		_lock.lock();

		try {
			componentModel.getConfigurations().stream().forEach(
				configurationModel -> Arrays.stream(configurationModel.getPid()).distinct().forEach(
					pid -> {
						if (Configuration.NAME.equals(pid)) {
							pid = componentModel.getName();
						}

						registerConfigurationCallback(
							componentModel, pid, onAdd, onUpdate, onRemove, configurationModel.getConfigurationPolicy());
					}
				)
			);

			if (!_containerState.configurationCallbacks().containsKey(componentModel)) {

				// Add a default callback for every Component using it's name as pid.

				registerConfigurationCallback(
					componentModel, componentModel.getName(), onAdd, onUpdate, onRemove, ConfigurationPolicy.OPTIONAL);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			_lock.unlock();
		}
	}

	void registerConfigurationCallback(
		ComponentModel componentModel,
		String pid,
		Consumer<ConfigurationCallback> onAdd,
		Consumer<ConfigurationCallback> onUpdate,
		Consumer<ConfigurationCallback> onRemove,
		ConfigurationPolicy policy) {

		Map<String, ConfigurationCallback> valueMap = _containerState.configurationCallbacks().computeIfAbsent(
			componentModel, k -> new LinkedHashMap<>());

		ConfigurationCallback callback = valueMap.get(pid);

		if ((callback == null) ||
			(policy == ConfigurationPolicy.REQUIRE)) {

			callback = new ConfigurationCallback.Builder().onAdd(
				onAdd
			).onRemove(
				onRemove
			).onUpdate(
				onUpdate
			).pid(
				pid
			).policy(
				policy
			).build();

			valueMap.put(pid, callback);
		}

		_containerState.managedServiceRegistrator().registerService(
			new ConfigurationManagedService(pid, callback),
			Constants.SERVICE_PID, pid, "component.id", _counter.incrementAndGet());
	}

	private final Collection<ComponentModel> _componentModels;
	private final ContainerState _containerState;
	private final AtomicInteger _counter = new AtomicInteger();
	private final Collection<Metadata<Extension>> _extensions;
	private final Lock _lock = new ReentrantLock();
	private volatile Phase _nextPhase;
}