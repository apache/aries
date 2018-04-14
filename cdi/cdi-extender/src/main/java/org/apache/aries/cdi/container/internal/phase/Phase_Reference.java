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

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.jboss.weld.bootstrap.spi.Metadata;

public class Phase_Reference implements Phase {

	public Phase_Reference(
		ContainerState containerState,
		Collection<Metadata<Extension>> extensions) {

/*		_containerState = containerState;
		_extensions = extensions;

		_componentModels = _containerState.beansModel().getComponentModels();
*/	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public void open() {
		// TODO Auto-generated method stub

	}

/*	@Override
	public void close() {
		_lock.lock();

		try {
			if (_nextPhase != null) {
				_nextPhase.close();

				_nextPhase = null;
			}

			_containerState.tracker().close();

			_containerState.referenceCallbacks().clear();
			_containerState.referenceObservers().clear();
		}
		finally {
			_lock.unlock();
		}
	}

	@Override
	public void open() {
		_lock.lock();

		try {
			_containerState.fire(CdiEvent.Type.WAITING_FOR_SERVICES);

			openReferences();

			_containerState.tracker().open();

			if (callbacksResolved()) {
				_nextPhase = new Phase_Publish(_containerState, _extensions);

				_nextPhase.open();
			}
		}
		finally {
			_lock.unlock();
		}
	}

	void openReferences() {
		Consumer<ReferenceCallback> onAdd = r -> {
			if ((_nextPhase == null) && callbacksResolved()) {
				_nextPhase = new Phase_Publish(_containerState, _extensions);
				_nextPhase.open();
			}
		},
		onUpdate = r -> {
			// TODO we may need to handle static greedy references by hard reset of _nextPhase
		},
		onRemove = r -> {
			if ((_nextPhase != null) && !callbacksResolved()) {
				_nextPhase.close();
				_nextPhase = null;
				_containerState.fire(CdiEvent.Type.WAITING_FOR_SERVICES);
			}
		};

		_componentModels.stream().forEach(
			componentModel -> openReferences(componentModel, onAdd, onUpdate, onRemove)
		);
	}

	private void openReferences(
		ComponentModel componentModel,
		Consumer<ReferenceCallback> onAdd,
		Consumer<ReferenceCallback> onUpdate,
		Consumer<ReferenceCallback> onRemove) {

		_lock.lock();

		try {
			componentModel.getReferences().stream().forEach(
				referenceModel -> {
					Map<String, ReferenceCallback> map = _containerState.referenceCallbacks().computeIfAbsent(
						componentModel, k -> new LinkedHashMap<>());

					ReferenceCallback callback = new ReferenceCallback.Builder(
						componentModel, _containerState, _containerState.context()
					).cardinality(
						referenceModel.getCardinality()
					).collectionType(
						referenceModel.getCollectionType()
					).name(
						referenceModel.getName()
					).onAdd(
						onAdd
					).onRemove(
						onRemove
					).onUpdate(
						onUpdate
					).build();

					map.put(referenceModel.getName(), callback);

					try {
						String targetFilter = ReferenceModel.buildFilter(
							referenceModel.getServiceClass(), referenceModel.getTarget(), referenceModel.getScope(), referenceModel.getQualifiers());

						_containerState.tracker().track(targetFilter, callback);
					}
					catch (InvalidSyntaxException ise) {
						if (_log.isErrorEnabled()) {
							_log.error("CDIe - {}", ise.getMessage(), ise);
						}
					}
				}
			);
		}
		finally {
			_lock.unlock();
		}
	}

	boolean referencesExist() {
		return _containerState.referenceCallbacks().values().stream().flatMap(
			entry -> entry.values().stream()
		).findFirst().isPresent();
	}

	boolean callbacksResolved() {
		return !_containerState.referenceCallbacks().values().stream().flatMap(
			valueMap -> valueMap.values().stream()
		).filter(
			c -> !c.resolved()
		).findFirst().isPresent();
	}

	private static final Logger _log = LoggerFactory.getLogger(Phase_Reference.class);

	private final Collection<ComponentModel> _componentModels;
	private final ContainerState _containerState;
	private final Collection<Metadata<Extension>> _extensions;
	private final Lock _lock = new ReentrantLock(true);
	private volatile Phase _nextPhase;
*/
}
