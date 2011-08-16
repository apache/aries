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
package org.apache.aries.subsystem.core;

import java.util.Collection;
import java.util.List;

import org.osgi.framework.Version;
import org.osgi.framework.wiring.Capability;
import org.osgi.framework.wiring.Requirement;
import org.osgi.framework.wiring.Resource;
import org.osgi.framework.wiring.ResourceConstants;
import org.osgi.service.repository.Repository;

public class ResourceHelper {
	public static String getContentAttribute(Resource resource) {
		// TODO Add to constants.
		return (String)getContentAttribute(resource, "osgi.content");
	}
	
	public static Object getContentAttribute(Resource resource, String name) {
		// TODO Add to constants.
		List<Capability> capabilities = resource.getCapabilities("osgi.content");
		Capability capability = capabilities.get(0);
		return capability.getAttributes().get(name);
	}
	
	public static Object getIdentityAttribute(Resource resource, String name) {
		List<Capability> capabilities = resource.getCapabilities(ResourceConstants.IDENTITY_NAMESPACE);
		Capability capability = capabilities.get(0);
		return capability.getAttributes().get(name);
	}
	
	public static Resource getResource(Requirement requirement, Repository repository) {
		Collection<Capability> capabilities = repository.findProviders(requirement);
		return capabilities == null ? null : capabilities.size() == 0 ? null : capabilities.iterator().next().getResource();
	}
	
	public static String getSymbolicNameAttribute(Resource resource) {
		return (String)getIdentityAttribute(resource, ResourceConstants.IDENTITY_NAMESPACE);
	}
	
	public static String getTypeAttribute(Resource resource) {
		return (String)getIdentityAttribute(resource, ResourceConstants.IDENTITY_TYPE_ATTRIBUTE);
	}
	
	public static Version getVersionAttribute(Resource resource) {
		return (Version)getIdentityAttribute(resource, ResourceConstants.IDENTITY_VERSION_ATTRIBUTE);
	}
}
