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

package org.apache.aries.cdi.container.internal;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.util.DTOs;
import org.apache.aries.cdi.container.internal.util.Logs;
import org.apache.aries.cdi.container.internal.util.Throw;
import org.osgi.framework.Bundle;
import org.osgi.service.cdi.runtime.CDIComponentRuntime;
import org.osgi.service.cdi.runtime.dto.ContainerDTO;
import org.osgi.service.cdi.runtime.dto.template.ContainerTemplateDTO;
import org.osgi.util.promise.PromiseFactory;

public class CCR implements CDIComponentRuntime {

	public CCR(PromiseFactory promiseFactory, Logs logs) {
		_promiseFactory = promiseFactory;
	}

	public void add(Bundle bundle, ContainerState containerState) {
		_states.put(bundle, containerState);
	}

	@Override
	public Collection<ContainerDTO> getContainerDTOs(Bundle... bundles) {
		if ((bundles != null) && (bundles.length > 0)) {
			return call(
				() -> Stream.of(bundles).filter(
					b -> Objects.nonNull(_states.get(b))
				).map(
					b -> _states.get(b)
				).map(
					cs -> DTOs.copy(cs.containerDTO(), true)
				).collect(Collectors.toList())
			);
		}

		return call(
			() -> _states.values().stream().map(
				cs -> DTOs.copy(cs.containerDTO(), true)
			).collect(Collectors.toList())
		);
	}

	@Override
	public ContainerTemplateDTO getContainerTemplateDTO(Bundle bundle) {
		return call(
			() -> Optional.ofNullable(_states.get(bundle)).map(
				cs -> DTOs.copy(cs.containerDTO().template, true)
			).orElse(null)
		);
	}

	public void remove(Bundle bundle) {
		_states.remove(bundle);
	}

	public <R> R call(Callable<R> callable) {
		try {
			return _promiseFactory.submit(callable).getValue();
		}
		catch (Exception e) {
			return Throw.exception(e);
		}
	}

	private final PromiseFactory _promiseFactory;
	private final Map<Bundle, ContainerState> _states = new ConcurrentHashMap<>();

}