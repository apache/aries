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

@Capability(
	name = CDIConstants.CDI_CAPABILITY_NAME,
	namespace = ExtenderNamespace.EXTENDER_NAMESPACE,
	uses = {
		org.osgi.service.cdi.ServiceScope.class,
		org.osgi.service.cdi.annotations.Reference.class,
		org.osgi.service.cdi.reference.BindService.class,
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
		org.osgi.service.cdi.reference.BindService.class,
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
