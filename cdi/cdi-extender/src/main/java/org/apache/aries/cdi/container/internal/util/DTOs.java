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

package org.apache.aries.cdi.container.internal.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.osgi.service.cdi.runtime.dto.ActivationDTO;
import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.service.cdi.runtime.dto.ComponentInstanceDTO;
import org.osgi.service.cdi.runtime.dto.ConfigurationDTO;
import org.osgi.service.cdi.runtime.dto.ContainerDTO;
import org.osgi.service.cdi.runtime.dto.ExtensionDTO;
import org.osgi.service.cdi.runtime.dto.ReferenceDTO;
import org.osgi.service.cdi.runtime.dto.template.ActivationTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ContainerTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ExtensionTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ReferenceTemplateDTO;

public class DTOs {

	private DTOs() {
		// no instances
	}

	public static ContainerDTO copy(ContainerDTO original, boolean clear) {
		try {
			if (cache.get().containsKey(original)) {
				return (ContainerDTO)cache.get().get(original);
			}
			ContainerDTO copy = copy0(original);
			return (ContainerDTO)cache.get().computeIfAbsent(original, p -> copy);
		}
		finally {
			if (clear)
				cache.remove();
		}
	}

	static ContainerDTO copy0(ContainerDTO original) {
		ContainerDTO copy = new ContainerDTO();
		copy.bundle = original.bundle;
		copy.changeCount = original.changeCount;
		copy.errors = new ArrayList<>(original.errors);
		copy.template = copy(original.template, false);
		copy.extensions = copy(original.extensions);
		copy.components = copy(original.components);
		return copy;
	}

	static ExtensionDTO copy(ExtensionDTO original) {
		if (cache.get().containsKey(original)) {
			return (ExtensionDTO)cache.get().get(original);
		}
		ExtensionDTO copy = copy0(original);
		return (ExtensionDTO)cache.get().computeIfAbsent(original, p -> copy);
	}

	static ExtensionDTO copy0(ExtensionDTO original) {
		ExtensionDTO copy = new ExtensionDTO();
		copy.service = original.service;
		copy.template = copy(original.template);
		return copy;
	}

	static ComponentDTO copy(ComponentDTO original) {
		if (cache.get().containsKey(original)) {
			return (ComponentDTO)cache.get().get(original);
		}
		ComponentDTO copy = copy0(original);
		return (ComponentDTO)cache.get().computeIfAbsent(original, p -> copy);
	}

	static ComponentDTO copy0(ComponentDTO original) {
		ComponentDTO copy = new ComponentDTO();
		copy.enabled = original.enabled;
		copy.instances = copy(original.instances);
		copy.template = copy(original.template);
		return copy;
	}

	static ComponentInstanceDTO copy(ComponentInstanceDTO original) {
		if (cache.get().containsKey(original)) {
			return (ComponentInstanceDTO)cache.get().get(original);
		}
		ComponentInstanceDTO copy = copy0(original);
		return (ComponentInstanceDTO)cache.get().computeIfAbsent(original, p -> copy);
	}

	static ComponentInstanceDTO copy0(ComponentInstanceDTO original) {
		ComponentInstanceDTO copy = new ComponentInstanceDTO();
		copy.activations = copy(original.activations);
		copy.configurations = copy(original.configurations);
		copy.properties = original.properties == null ? null : new HashMap<>(original.properties);
		copy.references = copy(original.references);
		return copy;
	}

	static ActivationDTO copy(ActivationDTO original) {
		if (cache.get().containsKey(original)) {
			return (ActivationDTO)cache.get().get(original);
		}
		ActivationDTO copy = copy0(original);
		return (ActivationDTO)cache.get().computeIfAbsent(original, p -> copy);
	}

	static ActivationDTO copy0(ActivationDTO original) {
		ActivationDTO copy = new ActivationDTO();
		copy.errors = new ArrayList<>(original.errors);
		copy.service = original.service;
		copy.template = copy(original.template);
		return copy;
	}

	static ConfigurationDTO copy(ConfigurationDTO original) {
		if (cache.get().containsKey(original)) {
			return (ConfigurationDTO)cache.get().get(original);
		}
		ConfigurationDTO copy = copy0(original);
		return (ConfigurationDTO)cache.get().computeIfAbsent(original, p -> copy);
	}

	static ConfigurationDTO copy0(ConfigurationDTO original) {
		ConfigurationDTO copy = new ConfigurationDTO();
		copy.properties = original.properties == null ? null : new HashMap<>(original.properties);
		copy.template = copy(original.template);
		return copy;
	}

	static ReferenceDTO copy(ReferenceDTO original) {
		if (cache.get().containsKey(original)) {
			return (ReferenceDTO)cache.get().get(original);
		}
		ReferenceDTO copy = copy0(original);
		return (ReferenceDTO)cache.get().computeIfAbsent(original, p -> copy);
	}

	static ReferenceDTO copy0(ReferenceDTO original) {
		ReferenceDTO copy = new ReferenceDTO();
		copy.matches = new ArrayList<>(original.matches);
		copy.minimumCardinality = original.minimumCardinality;
		copy.targetFilter = original.targetFilter;
		copy.template = copy(original.template);
		return copy;
	}

	public static ContainerTemplateDTO copy(ContainerTemplateDTO original, boolean clear) {
		try {
			if (cache.get().containsKey(original)) {
				return (ContainerTemplateDTO)cache.get().get(original);
			}
			ContainerTemplateDTO copy = copy0(original);
			return (ContainerTemplateDTO)cache.get().computeIfAbsent(original, p -> copy);
		}
		finally {
			if (clear)
				cache.remove();
		}
	}

	static ContainerTemplateDTO copy0(ContainerTemplateDTO original) {
		ContainerTemplateDTO copy = new ContainerTemplateDTO();
		copy.components = copy(original.components);
		copy.extensions = copy(original.extensions);
		copy.id = original.id;
		return copy;
	}

	static ComponentTemplateDTO copy(ComponentTemplateDTO original) {
		if (cache.get().containsKey(original)) {
			return (ComponentTemplateDTO)cache.get().get(original);
		}
		ComponentTemplateDTO copy = copy0(original);
		return (ComponentTemplateDTO)cache.get().computeIfAbsent(original, p -> copy);
	}

	static ComponentTemplateDTO copy0(ComponentTemplateDTO original) {
		ComponentTemplateDTO copy = new ComponentTemplateDTO();
		copy.activations = copy(original.activations);
		copy.beans = new ArrayList<>(original.beans);
		copy.configurations = copy(original.configurations);
		copy.name = original.name;
		copy.properties = original.properties == null ? null : new HashMap<>(original.properties);
		copy.references = copy(original.references);
		copy.type = original.type;
		return copy;
	}

	static ExtensionTemplateDTO copy(ExtensionTemplateDTO original) {
		if (cache.get().containsKey(original)) {
			return (ExtensionTemplateDTO)cache.get().get(original);
		}
		ExtensionTemplateDTO copy = copy0(original);
		return (ExtensionTemplateDTO)cache.get().computeIfAbsent(original, p -> copy);
	}

	static ExtensionTemplateDTO copy0(ExtensionTemplateDTO original) {
		ExtensionTemplateDTO copy = new ExtensionTemplateDTO();
		copy.serviceFilter = original.serviceFilter;
		return copy;
	}

	static ActivationTemplateDTO copy(ActivationTemplateDTO original) {
		if (cache.get().containsKey(original)) {
			return (ActivationTemplateDTO)cache.get().get(original);
		}
		ActivationTemplateDTO copy = copy0(original);
		return (ActivationTemplateDTO)cache.get().computeIfAbsent(original, p -> copy);
	}

	static ActivationTemplateDTO copy0(ActivationTemplateDTO original) {
		ActivationTemplateDTO copy = new ActivationTemplateDTO();
		copy.properties = original.properties == null ? null : new HashMap<>(original.properties);
		copy.scope = original.scope;
		copy.serviceClasses = new ArrayList<>(original.serviceClasses);
		return copy;
	}

	static ConfigurationTemplateDTO copy(ConfigurationTemplateDTO original) {
		if (cache.get().containsKey(original)) {
			return (ConfigurationTemplateDTO)cache.get().get(original);
		}
		ConfigurationTemplateDTO copy = copy0(original);
		return (ConfigurationTemplateDTO)cache.get().computeIfAbsent(original, p -> copy);
	}

	static ConfigurationTemplateDTO copy0(ConfigurationTemplateDTO original) {
		ConfigurationTemplateDTO copy = new ConfigurationTemplateDTO();
		copy.maximumCardinality = original.maximumCardinality;
		copy.pid = original.pid;
		copy.policy = original.policy;
		return copy;
	}

	static ReferenceTemplateDTO copy(ReferenceTemplateDTO original) {
		if (cache.get().containsKey(original)) {
			return (ReferenceTemplateDTO)cache.get().get(original);
		}
		ReferenceTemplateDTO copy = copy0(original);
		return (ReferenceTemplateDTO)cache.get().computeIfAbsent(original, p -> copy);
	}

	static ReferenceTemplateDTO copy0(ReferenceTemplateDTO original) {
		ReferenceTemplateDTO copy = new ReferenceTemplateDTO();
		copy.maximumCardinality = original.maximumCardinality;
		copy.minimumCardinality = original.minimumCardinality;
		copy.name = original.name;
		copy.policy = original.policy;
		copy.policyOption = original.policyOption;
		copy.serviceType = original.serviceType;
		copy.targetFilter = original.targetFilter;
		return copy;
	}

	@SuppressWarnings("unchecked")
	static <T> T copy(T original) {
		if (original instanceof ActivationDTO) {
			return (T)copy((ActivationDTO)original);
		}
		else if (original instanceof ActivationTemplateDTO) {
			return (T)copy((ActivationTemplateDTO)original);
		}
		else if (original instanceof ComponentDTO) {
			return (T)copy((ComponentDTO)original);
		}
		else if (original instanceof ComponentInstanceDTO) {
			return (T)copy((ComponentInstanceDTO)original);
		}
		else if (original instanceof ComponentTemplateDTO) {
			return (T)copy((ComponentTemplateDTO)original);
		}
		else if (original instanceof ConfigurationDTO) {
			return (T)copy((ConfigurationDTO)original);
		}
		else if (original instanceof ConfigurationTemplateDTO) {
			return (T)copy((ConfigurationTemplateDTO)original);
		}
		else if (original instanceof ContainerDTO) {
			return (T)copy((ContainerDTO)original);
		}
		else if (original instanceof ContainerTemplateDTO) {
			return (T)copy((ContainerTemplateDTO)original);
		}
		else if (original instanceof ExtensionDTO) {
			return (T)copy((ExtensionDTO)original);
		}
		else if (original instanceof ExtensionTemplateDTO) {
			return (T)copy((ExtensionTemplateDTO)original);
		}
		else if (original instanceof ReferenceDTO) {
			return (T)copy((ReferenceDTO)original);
		}
		else if (original instanceof ReferenceTemplateDTO) {
			return (T)copy((ReferenceTemplateDTO)original);
		}

		return null;
	}

	static <T> List<T> copy(List<T> original) {
		return original.stream().map(t -> copy(t)).collect(Collectors.toList());
	}

	private static final ThreadLocal<Map<Object, Object>> cache =
		ThreadLocal.withInitial(() -> new ConcurrentHashMap<>());

}
