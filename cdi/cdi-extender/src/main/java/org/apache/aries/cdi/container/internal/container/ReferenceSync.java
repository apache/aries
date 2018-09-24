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

import java.util.AbstractMap.SimpleImmutableEntry;

import org.apache.aries.cdi.container.internal.model.CollectionType;
import org.apache.aries.cdi.container.internal.model.ExtendedComponentInstanceDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedReferenceDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedReferenceTemplateDTO;
import org.apache.aries.cdi.container.internal.model.InstanceActivator;
import org.apache.aries.cdi.container.internal.util.Conversions;
import org.apache.aries.cdi.container.internal.util.Maps;
import org.apache.aries.cdi.container.internal.util.SRs;
import org.apache.aries.cdi.container.internal.util.Syncro;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.ReferencePolicy;
import org.osgi.service.cdi.ReferencePolicyOption;
import org.osgi.service.cdi.runtime.dto.template.ReferenceTemplateDTO;
import org.osgi.service.log.Logger;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ReferenceSync implements ServiceTrackerCustomizer<Object, Object> {

	public ReferenceSync(
		ContainerState containerState,
		ExtendedReferenceDTO referenceDTO,
		ExtendedComponentInstanceDTO componentInstanceDTO,
		InstanceActivator.Builder<?> builder) {

		_containerState = containerState;
		_referenceDTO = referenceDTO;
		_componentInstanceDTO = componentInstanceDTO;
		_builder = builder;
		_templateDTO = (ExtendedReferenceTemplateDTO)_referenceDTO.template;
		_log = _containerState.containerLogs().getLogger(getClass());
	}

	@Override
	public Object addingService(final ServiceReference<Object> reference) {
		boolean active = _componentInstanceDTO.active;
		boolean resolved = (_referenceDTO.matches.size() >= _templateDTO.minimumCardinality);
		boolean dynamic = (_templateDTO.policy == ReferencePolicy.DYNAMIC);
		boolean reluctant = (_templateDTO.policyOption == ReferencePolicyOption.RELUCTANT);
		CollectionType collectionType = _templateDTO.collectionType;
		boolean requiresUpdate = true;

		if (resolved && reluctant && active && !dynamic) {
			requiresUpdate = false;
		}

		try (Syncro open = _syncro.open()) {
			_referenceDTO.matches = SRs.from(_referenceDTO.serviceTracker.getServiceReferences(), reference);

			if (collectionType == CollectionType.BINDER_SERVICE ||
				collectionType == CollectionType.BINDER_REFERENCE ||
				collectionType == CollectionType.BINDER_BEAN_SERVICE_OBJECTS) {

				requiresUpdate = false;

				return _referenceDTO.binder.addingService(reference);
			}
			else if (collectionType == CollectionType.PROPERTIES) {
				return Maps.of(reference.getProperties());
			}
			else if (collectionType == CollectionType.REFERENCE) {
				return reference;
			}
			else if (collectionType == CollectionType.SERVICEOBJECTS) {
				return new BeanServiceObjectsImpl<>(
					_containerState.bundleContext().getServiceObjects(reference));
			}
			else if (collectionType == CollectionType.TUPLE) {
				return new SimpleImmutableEntry<>(
					Maps.of(reference.getProperties()),
					_containerState.bundleContext().getService(reference));
			}

			return _containerState.bundleContext().getService(reference);
		}
		finally {
			if (requiresUpdate) {
				InstanceActivator activator = _builder.setInstance(
					_componentInstanceDTO
				).build();

				updateStatically(activator);
			}
		}
	}

	@Override
	public void modifiedService(ServiceReference<Object> reference, Object service) {
		CollectionType collectionType = _templateDTO.collectionType;

		if (collectionType == CollectionType.BINDER_SERVICE ||
			collectionType == CollectionType.BINDER_REFERENCE ||
			collectionType == CollectionType.BINDER_BEAN_SERVICE_OBJECTS) {

			_referenceDTO.binder.modifiedService(reference);
		}
		else if (collectionType == CollectionType.PROPERTIES ||
				collectionType == CollectionType.REFERENCE ||
				collectionType == CollectionType.SERVICEOBJECTS ||
				collectionType == CollectionType.TUPLE) {

			removedService(reference, service);
			addingService(reference);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void removedService(ServiceReference<Object> reference, Object service) {
		boolean active = _componentInstanceDTO.active;
		boolean resolved = (_referenceDTO.matches.size() >= _templateDTO.minimumCardinality);
		boolean dynamic = (_templateDTO.policy == ReferencePolicy.DYNAMIC);
		boolean reluctant = (_templateDTO.policyOption == ReferencePolicyOption.RELUCTANT);
		CollectionType collectionType = _templateDTO.collectionType;
		boolean requiresUpdate = true;

		if (resolved && active && dynamic) {
			requiresUpdate = false;
		}

		try (Syncro open = _syncro.open()) {
			_referenceDTO.matches.removeIf(d -> d.id == SRs.id(reference));

			if (collectionType == CollectionType.BINDER_SERVICE ||
				collectionType == CollectionType.BINDER_REFERENCE ||
				collectionType == CollectionType.BINDER_BEAN_SERVICE_OBJECTS) {

				requiresUpdate = false;

				_referenceDTO.binder.removedService(reference);

				return;
			}
			else if (collectionType == CollectionType.PROPERTIES) {
				return;
			}
			else if (collectionType == CollectionType.REFERENCE) {
				return;
			}
			else if (collectionType == CollectionType.SERVICEOBJECTS) {
				((BeanServiceObjectsImpl<Object>)service).close();

				return;
			}

			_containerState.bundleContext().ungetService(reference);
		}
		finally {
			if (requiresUpdate) {
				InstanceActivator activator = _builder.setInstance(
					_componentInstanceDTO
				).build();

				updateStatically(activator);
			}
		}
	}

	@Override
	public String toString() {
		if (_string == null) {
			_string = Conversions.convert(_referenceDTO.template).to(ReferenceTemplateDTO.class).toString();
		}
		return _string;
	}

	private void updateStatically(InstanceActivator activator) {
		_containerState.submit(
			activator.closeOp(), activator::close
		).then(
			s -> _containerState.submit(
				activator.openOp(), activator::open
			).onFailure(
				f -> {
					_log.error(l -> l.error("CCR Error in OPEN for {} on {}", _componentInstanceDTO.ident(), _containerState.bundle(), f));

					_containerState.error(f);
				}
			),
			f -> {
				_log.error(l -> l.error("CCR Error in CLODE for {} on {}", _componentInstanceDTO.ident(), _containerState.bundle(), f.getFailure()));

				_containerState.error(f.getFailure());
			}
		);
	}

	private final InstanceActivator.Builder<?> _builder;
	private final ExtendedComponentInstanceDTO _componentInstanceDTO;
	private final ContainerState _containerState;
	private final Logger _log;
	private final ExtendedReferenceDTO _referenceDTO;
	private volatile String _string;
	private final Syncro _syncro = new Syncro(true);
	private final ExtendedReferenceTemplateDTO _templateDTO;

}
