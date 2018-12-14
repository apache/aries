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
	attribute = "objectClass:List<String>=javax.enterprise.inject.spi.Extension",
	namespace = SERVICE_NAMESPACE
)
@Capability(
	namespace = CDI_EXTENSION_PROPERTY,
	name = "aries.cdi.jndi",
	uses= {
		javax.annotation.Priority.class,
		javax.enterprise.event.Observes.class,
		javax.enterprise.inject.spi.Extension.class,
		javax.naming.Context.class,
		javax.naming.spi.ObjectFactory.class
	},
	version = "1.0.0"
)
@RequireCDIImplementation
package org.apache.aries.cdi.extension.jndi;

import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;
import static org.osgi.service.cdi.CDIConstants.CDI_EXTENSION_PROPERTY;

import org.osgi.annotation.bundle.Capability;
import org.osgi.service.cdi.annotations.RequireCDIImplementation;
