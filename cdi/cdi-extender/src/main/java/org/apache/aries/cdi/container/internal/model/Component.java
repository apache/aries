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

import java.util.Arrays;
import java.util.List;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Phase;
import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.service.cdi.runtime.dto.ComponentInstanceDTO;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;

public abstract class Component extends Phase {

	public abstract static class Builder<T extends Builder<T>> {

		public Builder(ContainerState containerState, InstanceActivator.Builder<?> activatorBuilder) {
			_containerState = containerState;
			_activatorBuilder = activatorBuilder;
		}

		@SuppressWarnings("unchecked")
		public T template(ComponentTemplateDTO templateDTO) {
			_templateDTO = templateDTO;
			return (T)this;
		}

		public abstract Component build();

		protected InstanceActivator.Builder<?> _activatorBuilder;
		protected ContainerState _containerState;
		protected Phase _next;
		protected ComponentTemplateDTO _templateDTO;

	}

	Component(Builder<?> builder) {
		super(builder._containerState, null);
		_activatorBuilder = builder._activatorBuilder;
	}

	public InstanceActivator.Builder<?> activatorBuilder() {
		return _activatorBuilder;
	}

	public abstract List<ConfigurationTemplateDTO> configurationTemplates();

	public abstract List<ComponentInstanceDTO> instances();

	public abstract ComponentDTO snapshot();

	public abstract ComponentTemplateDTO template();

	@Override
	public String toString() {
		return Arrays.asList(getClass().getSimpleName(), template().name).toString();
	}

	private final InstanceActivator.Builder<?> _activatorBuilder;

}
