package org.apache.aries.cdi.container.internal.v2.component;

import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationPolicy;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.MaximumCardinality;
import org.osgi.service.cdi.runtime.dto.template.ReferenceTemplateDTO;

public class FactoryComponent implements Component {

	public FactoryComponent(String name) {
		_snapshot = new ComponentDTO();
		_snapshot.template = new ComponentTemplateDTO();
		_snapshot.template.activations = new CopyOnWriteArrayList<>();
		_snapshot.template.configurations = new CopyOnWriteArrayList<>();

		ConfigurationTemplateDTO factoryConfig = new ConfigurationTemplateDTO();
		factoryConfig.componentConfiguration = true;
		factoryConfig.maximumCardinality = MaximumCardinality.MANY;
		factoryConfig.pid = name;
		factoryConfig.policy = ConfigurationPolicy.REQUIRED;

		_snapshot.template.configurations.add(factoryConfig);
		_snapshot.template.name = name;
		_snapshot.template.references = new CopyOnWriteArrayList<>();
		_snapshot.template.type = ComponentTemplateDTO.Type.FACTORY;
	}

	@Override
	public void addConfiguration(ConfigurationTemplateDTO dto) {
		if (dto == null) return;
		_snapshot.template.configurations.add(dto);
	}

	@Override
	public void addReference(ReferenceTemplateDTO dto) {
		if (dto == null) return;
		_snapshot.template.references.add(dto);
	}

	@Override
	public ComponentDTO getSnapshot() {
		return _snapshot; // TODO make safe copy using converter
	}

	@Override
	public ComponentTemplateDTO getTemplate() {
		return _snapshot.template; // TODO make safe copy using converter
	}

	private final ComponentDTO _snapshot;

}
