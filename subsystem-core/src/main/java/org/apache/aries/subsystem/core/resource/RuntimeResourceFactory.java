/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.subsystem.core.resource;

import org.apache.aries.subsystem.core.internal.AriesSubsystem;
import org.osgi.framework.wiring.Resource;
import org.osgi.service.subsystem.SubsystemException;

/*
 * TODO
 * (1) Perhaps pass a collection of resource contexts? There could be more than one 
 * resource of the same type. Coordinations span multiple resources anyway.
 * (2) Can this be merged with ResourceProcessor in OBR?
 */

/**
 * A ResourceProcessor processes resources from a specific namespace or 
 * namespaces (e.g. bundle). Namespaces not defined by the OSGi Alliance should 
 * use a reverse domain name scheme to avoid collision (e.g. com.acme.config).
 * <p>
 * ResourceProcessors are registered in the OSGi Service Registry. They 
 * advertise the namespaces they support using the service property 
 * osgi.resource.namespace. The type of this property is a String+.
 * <p>
 * A resource processor performs the operation corresponding to those provided 
 * by Subsystem that affect a subsystem's lifecycle (e.g. install and start). 
 * For example, Subsystem.install() would delegate to a resource processor if 
 * there were any resources to install for the namespace that the resource 
 * processor supported.
 * 
 * @ThreadSafe
 */
public interface RuntimeResourceFactory {
	public RuntimeResource create(Resource resource, ResourceListener listener, AriesSubsystem subsystem) throws SubsystemException;
}
