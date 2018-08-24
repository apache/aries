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
@Capability(
	namespace = ServiceNamespace.SERVICE_NAMESPACE,
	attribute = "objectClass:List<String>=javax.enterprise.inject.spi.BeanManager",
	uses = {
		javax.el.Expression.class,
		javax.enterprise.context.ApplicationScoped.class,
		javax.enterprise.context.spi.Context.class,
		javax.enterprise.event.Event.class,
		javax.enterprise.inject.Any.class,
		javax.enterprise.inject.spi.Annotated.class,
		javax.enterprise.util.TypeLiteral.class
	}
)
@Capability(
	namespace = ServiceNamespace.SERVICE_NAMESPACE,
	attribute = "objectClass:List<String>=org.osgi.service.cdi.runtime.CDIComponentRuntime",
	uses = {
		org.osgi.service.cdi.runtime.CDIComponentRuntime.class,
		org.osgi.service.cdi.runtime.dto.ContainerDTO.class,
		org.osgi.service.cdi.runtime.dto.template.ContainerTemplateDTO.class
	}
)
@Capability(
	namespace = "osgi.serviceloader",
	name = "javax.enterprise.inject.se.SeContainerInitializer",
	uses = {
		javax.enterprise.inject.se.SeContainerInitializer.class,
		javax.enterprise.inject.spi.CDI.class
	}
)
@Capability(
	namespace = "osgi.serviceloader",
	name = "javax.enterprise.inject.spi.CDIProvider",
	uses = {
		javax.enterprise.inject.Any.class,
		javax.enterprise.inject.spi.Annotated.class,
		javax.enterprise.util.TypeLiteral.class
	}
)
@Requirement(
	namespace = ExtenderNamespace.EXTENDER_NAMESPACE,
	name = "osgi.serviceloader.registrar"
)
package org.apache.aries.cdi.container;

import org.osgi.annotation.bundle.Capability;
import org.osgi.annotation.bundle.Requirement;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.namespace.implementation.ImplementationNamespace;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.service.cdi.CDIConstants;
