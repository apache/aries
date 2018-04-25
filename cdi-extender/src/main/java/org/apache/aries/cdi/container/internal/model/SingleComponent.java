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

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;
import org.apache.aries.cdi.container.internal.container.Op.Mode;
import org.apache.aries.cdi.container.internal.util.Throw;
import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.service.cdi.runtime.dto.ComponentInstanceDTO;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;
import org.osgi.service.log.Logger;
import org.osgi.util.promise.Promise;

public class SingleComponent extends Component {

	public static class Builder extends Component.Builder<Builder> {

		public Builder(ContainerState containerState, SingleActivator.Builder activatorBuilder) {
			super(containerState, activatorBuilder);
		}

		@Override
		public SingleComponent build() {
			return new SingleComponent(this);
		}

	}

	protected SingleComponent(Builder builder) {
		super(builder);

		_template = builder._templateDTO;
		_activatorBuilder = builder._activatorBuilder;
		_log = containerState.containerLogs().getLogger(getClass());
	}

	@Override
	public boolean close() {
		if (_snapshot == null) {
			return true;
		}

		_snapshot.instances.removeIf(
			instance -> {
				ExtendedComponentInstanceDTO einstance = (ExtendedComponentInstanceDTO)instance;

				Promise<Boolean> result = submit(einstance.closeOp(), einstance::close).onFailure(
					f -> {
						_log.error(l -> l.error("CCR Error in single component close for {} on {}", einstance.ident(), bundle(), f));
					}
				);

				try {
					return result.getValue();
				}
				catch (InvocationTargetException | InterruptedException e) {
					return Throw.exception(e);
				}
			}
		);

		containerState.containerDTO().components.remove(_snapshot);

		_snapshot = null;

		return true;
	}

	@Override
	public Op closeOp() {
		return Op.of(Mode.CLOSE, Op.Type.SINGLE_COMPONENT, _template.name);
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
		_snapshot = new ComponentDTO();
		_snapshot.instances = new CopyOnWriteArrayList<>();
		_snapshot.template = _template;

		containerState.containerDTO().components.add(_snapshot);

		ExtendedComponentInstanceDTO instanceDTO = new ExtendedComponentInstanceDTO(
			containerState, _activatorBuilder);
		instanceDTO.activations = new CopyOnWriteArrayList<>();
		instanceDTO.configurations = new CopyOnWriteArrayList<>();
		instanceDTO.references = new CopyOnWriteArrayList<>();
		instanceDTO.template = _template;

		_snapshot.instances.add(instanceDTO);

		submit(instanceDTO.openOp(), instanceDTO::open).onFailure(
			f -> {
				_log.error(l -> l.error("CCR Error in single component open for {} on {}", instanceDTO.ident(), containerState.bundle()));
			}
		);

		return true;
	}

	@Override
	public Op openOp() {
		return Op.of(Mode.OPEN, Op.Type.SINGLE_COMPONENT, _template.name);
	}

	@Override
	public ComponentTemplateDTO template() {
		return _template;
	}

	private final InstanceActivator.Builder<?> _activatorBuilder;
	private final Logger _log;
	private volatile ComponentDTO _snapshot;
	private final ComponentTemplateDTO _template;

}
