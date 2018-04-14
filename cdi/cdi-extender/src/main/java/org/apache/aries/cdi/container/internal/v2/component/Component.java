package org.apache.aries.cdi.container.internal.v2.component;

import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ReferenceTemplateDTO;

public interface Component {

	void addConfiguration(ConfigurationTemplateDTO dto);

	void addReference(ReferenceTemplateDTO dto);

	ComponentDTO getSnapshot();

	ComponentTemplateDTO getTemplate();


}
