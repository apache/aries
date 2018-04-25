package org.apache.aries.cdi.container.internal.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
			return (ContainerDTO)cache.get().computeIfAbsent(original, o -> {
				return copy0(original);
			});
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
		return (ExtensionDTO)cache.get().computeIfAbsent(original, o -> {
			return copy0(original);
		});
	}

	static ExtensionDTO copy0(ExtensionDTO original) {
		ExtensionDTO copy = new ExtensionDTO();
		copy.service = original.service;
		copy.template = copy(original.template);
		return copy;
	}

	static ComponentDTO copy(ComponentDTO original) {
		return (ComponentDTO)cache.get().computeIfAbsent(original, o -> {
			return copy0(original);
		});
	}

	static ComponentDTO copy0(ComponentDTO original) {
		ComponentDTO copy = new ComponentDTO();
		copy.enabled = original.enabled;
		copy.instances = copy(original.instances);
		copy.template = copy(original.template);
		return copy;
	}

	static ComponentInstanceDTO copy(ComponentInstanceDTO original) {
		return (ComponentInstanceDTO)cache.get().computeIfAbsent(original, o -> {
			return copy0(original);
		});
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
		return (ActivationDTO)cache.get().computeIfAbsent(original, o -> {
			return copy0(original);
		});
	}

	static ActivationDTO copy0(ActivationDTO original) {
		ActivationDTO copy = new ActivationDTO();
		copy.errors = new ArrayList<>(original.errors);
		copy.service = original.service;
		copy.template = copy(original.template);
		return copy;
	}

	static ConfigurationDTO copy(ConfigurationDTO original) {
		return (ConfigurationDTO)cache.get().computeIfAbsent(original, o -> {
			return copy0(original);
		});
	}

	static ConfigurationDTO copy0(ConfigurationDTO original) {
		ConfigurationDTO copy = new ConfigurationDTO();
		copy.properties = original.properties == null ? null : new HashMap<>(original.properties);
		copy.template = copy(original.template);
		return copy;
	}

	static ReferenceDTO copy(ReferenceDTO original) {
		return (ReferenceDTO)cache.get().computeIfAbsent(original, o -> {
			return copy0(original);
		});
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
			return (ContainerTemplateDTO)cache.get().computeIfAbsent(original, o -> {
				return copy0(original);
			});
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
		return (ComponentTemplateDTO)cache.get().computeIfAbsent(original, o -> {
			return copy0(original);
		});
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
		return (ExtensionTemplateDTO)cache.get().computeIfAbsent(original, o -> {
			return copy0(original);
		});
	}

	static ExtensionTemplateDTO copy0(ExtensionTemplateDTO original) {
		ExtensionTemplateDTO copy = new ExtensionTemplateDTO();
		copy.serviceFilter = original.serviceFilter;
		return copy;
	}

	static ActivationTemplateDTO copy(ActivationTemplateDTO original) {
		return (ActivationTemplateDTO)cache.get().computeIfAbsent(original, o -> {
			return copy0(original);
		});
	}

	static ActivationTemplateDTO copy0(ActivationTemplateDTO original) {
		ActivationTemplateDTO copy = new ActivationTemplateDTO();
		copy.properties = original.properties == null ? null : new HashMap<>(original.properties);
		copy.scope = original.scope;
		copy.serviceClasses = new ArrayList<>(original.serviceClasses);
		return copy;
	}

	static ConfigurationTemplateDTO copy(ConfigurationTemplateDTO original) {
		return (ConfigurationTemplateDTO)cache.get().computeIfAbsent(original, o -> {
			return copy0(original);
		});
	}

	static ConfigurationTemplateDTO copy0(ConfigurationTemplateDTO original) {
		ConfigurationTemplateDTO copy = new ConfigurationTemplateDTO();
		copy.componentConfiguration = original.componentConfiguration;
		copy.maximumCardinality = original.maximumCardinality;
		copy.pid = original.pid;
		copy.policy = original.policy;
		return copy;
	}

	static ReferenceTemplateDTO copy(ReferenceTemplateDTO original) {
		return (ReferenceTemplateDTO)cache.get().computeIfAbsent(original, o -> {
			return copy0(original);
		});
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
		ThreadLocal.withInitial(() -> new HashMap<>());

}
