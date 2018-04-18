@Capability(
	name = CDIConstants.CDI_CAPABILITY_NAME,
	namespace = ExtenderNamespace.EXTENDER_NAMESPACE,
	uses = {
		org.osgi.service.cdi.ServiceScope.class,
		org.osgi.service.cdi.annotations.Reference.class,
		org.osgi.service.cdi.reference.BindObject.class,
		org.osgi.service.cdi.runtime.CDIComponentRuntime.class,
		org.osgi.service.cdi.runtime.dto.ActivationDTO.class,
		org.osgi.service.cdi.runtime.dto.template.ActivationTemplateDTO.class
	},
	version = CDIConstants.CDI_SPECIFICATION_VERSION
)
@Capability(
	name = CDIConstants.CDI_CAPABILITY_NAME,
	namespace = ImplementationNamespace.IMPLEMENTATION_NAMESPACE,
	uses = {
		org.osgi.service.cdi.ServiceScope.class,
		org.osgi.service.cdi.annotations.Reference.class,
		org.osgi.service.cdi.reference.BindObject.class,
		org.osgi.service.cdi.runtime.CDIComponentRuntime.class,
		org.osgi.service.cdi.runtime.dto.ActivationDTO.class,
		org.osgi.service.cdi.runtime.dto.template.ActivationTemplateDTO.class
	},
	version = CDIConstants.CDI_SPECIFICATION_VERSION
)
@Export
@Version("1.0.0")
package org.apache.aries.cdi.provider;

import org.osgi.annotation.bundle.Capability;
import org.osgi.annotation.bundle.Export;
import org.osgi.annotation.versioning.Version;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.namespace.implementation.ImplementationNamespace;
import org.osgi.service.cdi.CDIConstants;
