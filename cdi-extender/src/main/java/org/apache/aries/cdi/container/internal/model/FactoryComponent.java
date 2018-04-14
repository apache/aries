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
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;
import org.apache.aries.cdi.container.internal.container.Op.Mode;
import org.apache.aries.cdi.container.internal.container.Op.Type;
import org.osgi.service.cdi.MaximumCardinality;
import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.service.cdi.runtime.dto.ComponentInstanceDTO;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;
import org.osgi.service.log.Logger;

public class FactoryComponent extends Component {

	public static class Builder extends Component.Builder<Builder> {

		public Builder(ContainerState containerState, FactoryActivator.Builder activatorBuilder) {
			super(containerState, activatorBuilder);
		}

		@Override
		public FactoryComponent build() {
			return new FactoryComponent(this);
		}

	}

	protected FactoryComponent(Builder builder) {
		super(builder);

		_log = containerState.containerLogs().getLogger(getClass());

		_template = builder._templateDTO;

		_snapshot = new ComponentDTO();
		_snapshot.instances = new CopyOnWriteArrayList<>();
		_snapshot.template = _template;

		containerState.containerDTO().components.add(_snapshot);

		configurationTemplates().stream().filter(
			t -> t.maximumCardinality == MaximumCardinality.MANY
		).forEach(
			t -> {
				containerState.findConfigs(t.pid, true).ifPresent(
					arr -> Arrays.stream(arr).forEach(
						c -> {
							ExtendedComponentInstanceDTO instanceDTO = new ExtendedComponentInstanceDTO(containerState, builder._activatorBuilder);
							instanceDTO.activations = new CopyOnWriteArrayList<>();
							instanceDTO.configurations = new CopyOnWriteArrayList<>();
							instanceDTO.pid = c.getPid();
							instanceDTO.properties = null;
							instanceDTO.references = new CopyOnWriteArrayList<>();
							instanceDTO.template = builder._templateDTO;

							_snapshot.instances.add(instanceDTO);
						}
					)
				);
			}
		);
	}

	@Override
	public boolean close() {
		_snapshot.instances.stream().map(
			instance -> (ExtendedComponentInstanceDTO)instance
		).forEach(
			instance -> {
				submit(instance.closeOp(), instance::close).onFailure(
					f -> {
						_log.error(l -> l.error("CCR Error in factory component close for {} on {}", instance.ident(), containerState.bundle()));
					}
				);
			}
		);

		return true;
	}

	@Override
	public Op closeOp() {
		return Op.of(Mode.CLOSE, Type.FACTORY_COMPONENT, _template.name);
	}

	@Override
	public List<ConfigurationTemplateDTO> configurationTemplates() {
		return _template.configurations;
	}

	@Override
	public List<ComponentInstanceDTO> instances() {
		return _snapshot.instances;
	}

	@Override
	public ComponentDTO snapshot() {
		return _snapshot;
	}

	@Override
	public boolean open() {
		if (!snapshot().enabled || !containerState.containerDTO().components.get(0).enabled) {
			return false;
		}

		_snapshot.instances.stream().map(
			instance -> (ExtendedComponentInstanceDTO)instance
		).forEach(
			instance -> {
				submit(instance.openOp(), instance::open).onFailure(
					f -> {
						_log.error(l -> l.error("CCR Error in factory component open for {} on {}", instance.ident(), containerState.bundle()));
					}
				);
			}
		);

		return true;
	}

	@Override
	public Op openOp() {
		return Op.of(Mode.OPEN, Type.FACTORY_COMPONENT, _template.name);
	}

	@Override
	public ComponentTemplateDTO template() {
		return _template;
	}

	private final Logger _log;
	private final ComponentDTO _snapshot;
	private final ComponentTemplateDTO _template;

}
