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

import org.apache.aries.cdi.container.internal.container.ContainerBootstrap;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;
import org.apache.aries.cdi.container.internal.container.Op.Mode;
import org.apache.aries.cdi.container.internal.container.Op.Type;
import org.apache.aries.cdi.container.internal.util.Syncro;
import org.osgi.service.log.Logger;

public class ContainerActivator extends InstanceActivator {

	public static class Builder extends InstanceActivator.Builder<Builder> {

		public Builder(ContainerState containerState, ContainerBootstrap next) {
			super(containerState, next);
		}

		@Override
		public ContainerActivator build() {
			return new ContainerActivator(this);
		}

	}

	private final Syncro syncro = new Syncro(true);

	private ContainerActivator(Builder builder) {
		super(builder);
		_log = containerState.containerLogs().getLogger(getClass());
	}

	@Override
	public boolean close() {
		try (Syncro open = syncro.open()) {
			boolean result = next.map(
					next -> {
						submit(next.closeOp(), next::close).onFailure(
								f -> {
									_log.error(l -> l.error("CCR Failure in container activator close on {}", next, f));
								}
								);

						return true;
					}
					).orElse(true);

			_instance.active = false;

			return result;
		}
	}

	@Override
	public Op closeOp() {
		return Op.of(Mode.CLOSE, Type.CONTAINER_ACTIVATOR, _instance.template.name);
	}

	@Override
	public boolean open() {
		try (Syncro open = syncro.open()) {
			if (!_instance.referencesResolved()) {
				return false;
			}

			boolean result = next.map(
					next -> {
						submit(next.openOp(), next::open).onFailure(
								f -> {
									_log.error(l -> l.error("CCR Failure in container activator open on {}", next, f));

									containerState.error(f);
								}
								);

						return true;
					}
					).orElse(true);

			if (result) {
				_instance.active = true;
			}

			return result;
		}
	}

	@Override
	public Op openOp() {
		return Op.of(Mode.OPEN, Type.CONTAINER_ACTIVATOR, _instance.template.name);
	}

	private final Logger _log;

}
