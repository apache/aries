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

package org.apache.aries.cdi.container.internal.model;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;
import org.apache.aries.cdi.container.internal.container.Phase;
import org.osgi.framework.ServiceReference;

public abstract class InstanceActivator extends Phase {

	public abstract static class Builder<T extends Builder<T>> {

		public Builder(ContainerState containerState, Phase next) {
			_containerState = containerState;
			_next = next;
		}

		public abstract InstanceActivator build();

		@SuppressWarnings("unchecked")
		public T setInstance(ExtendedComponentInstanceDTO instance) {
			_instance = instance;
			return (T)this;
		}

		@SuppressWarnings("unchecked")
		public T setReference(ServiceReference<Object> reference) {
			_reference = reference;
			return (T)this;
		}

		@SuppressWarnings("unchecked")
		public T setReferenceDTO(ExtendedReferenceDTO referenceDTO) {
			_referenceDTO = referenceDTO;
			return (T)this;
		}

		private ContainerState _containerState;
		private ExtendedComponentInstanceDTO _instance;
		private Phase _next;
		private ServiceReference<Object> _reference;
		private ExtendedReferenceDTO _referenceDTO;
	}

	protected InstanceActivator(Builder<?> builder) {
		super(builder._containerState, builder._next);

		this.instance = builder._instance;
		this.referenceDTO = builder._referenceDTO;
		this.reference = builder._reference;
	}

	@Override
	public abstract Op closeOp();

	@Override
	public abstract Op openOp();

	protected final ExtendedComponentInstanceDTO instance;
	protected final ExtendedReferenceDTO referenceDTO;
	protected final ServiceReference<Object> reference;

}
