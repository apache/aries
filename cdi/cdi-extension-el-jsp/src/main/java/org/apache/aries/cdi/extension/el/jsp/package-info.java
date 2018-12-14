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

@org.osgi.annotation.bundle.Capability(
	attribute = "objectClass:List<String>=javax.enterprise.inject.spi.Extension",
	namespace = SERVICE_NAMESPACE
)
@org.osgi.annotation.bundle.Capability(
	name = "aries.cdi.el.jsp",
	namespace = CDI_EXTENSION_PROPERTY,
	uses= {
		javax.annotation.Priority.class,
		javax.el.ELContext.class,
		javax.enterprise.event.Observes.class,
		javax.enterprise.inject.spi.Extension.class,
		javax.servlet.ServletContextListener.class,
		javax.servlet.jsp.JspFactory.class
	},
	version = "1.0.0"
)
// Deliberately depend on Http Whiteboard version 1.0.0 (the spec annotation starts at 1.1.0)
@org.osgi.annotation.bundle.Requirement(
	name = "osgi.http",
	namespace = IMPLEMENTATION_NAMESPACE,
	version = "1.0.0"
)
@org.osgi.annotation.bundle.Requirement(
	attribute = "osgi.contract=JavaJSP",
	filter = "(&(osgi.contract=JavaJSP)(version=${jsp.version}))",
	namespace = "osgi.contract"
)
@org.osgi.service.cdi.annotations.RequireCDIImplementation
package org.apache.aries.cdi.extension.el.jsp;

import static org.osgi.namespace.implementation.ImplementationNamespace.IMPLEMENTATION_NAMESPACE;
import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;
import static org.osgi.service.cdi.CDIConstants.CDI_EXTENSION_PROPERTY;
