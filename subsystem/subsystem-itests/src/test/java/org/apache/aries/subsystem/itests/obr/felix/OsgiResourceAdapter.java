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
package org.apache.aries.subsystem.itests.obr.felix;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.aries.subsystem.core.internal.ResourceHelper;
import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resource;
import org.osgi.framework.Version;

// TODO Need to distinguish between resources that have already been deployed (local) and those that have not.
public class OsgiResourceAdapter implements Resource {
	private final org.osgi.resource.Resource resource;
	
	public OsgiResourceAdapter(org.osgi.resource.Resource resource) {
		if (resource == null)
			throw new NullPointerException("Missing required parameter: resource");
		this.resource = resource;
	}

	public Capability[] getCapabilities() {
		Collection<org.osgi.resource.Capability> capabilities = resource.getCapabilities(null);
		Collection<Capability> result = new ArrayList<Capability>(capabilities.size());
		for (org.osgi.resource.Capability capability : capabilities)
			result.add(new OsgiCapabilityAdapter(capability));
		return result.toArray(new Capability[result.size()]);
	}

	public String[] getCategories() {
		return new String[0];
	}

	public String getId() {
		String symbolicName = ResourceHelper.getSymbolicNameAttribute(resource);
		Version version = ResourceHelper.getVersionAttribute(resource);
		return symbolicName + ";version=" + version;
	}

	public String getPresentationName() {
		return ResourceHelper.getSymbolicNameAttribute(resource);
	}

	@SuppressWarnings("rawtypes")
	public Map getProperties() {
		return Collections.emptyMap();
	}

	public Requirement[] getRequirements() {
		Collection<org.osgi.resource.Requirement> requirements = resource.getRequirements(null);
		Collection<Requirement> result = new ArrayList<Requirement>(requirements.size());
		for (org.osgi.resource.Requirement requirement : requirements)
			result.add(new OsgiRequirementAdapter(requirement));
		return result.toArray(new Requirement[result.size()]);
	}

	public Long getSize() {
		return -1L;
	}

	public String getSymbolicName() {
		return ResourceHelper.getSymbolicNameAttribute(resource);
	}

	public String getURI() {
		return ResourceHelper.getContentAttribute(resource);
	}

	public Version getVersion() {
		return ResourceHelper.getVersionAttribute(resource);
	}

	public boolean isLocal() {
		return false;
	}
}
