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
package org.apache.aries.subsystem.obr.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Constants;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

public class FelixRequirementAdapter extends AbstractRequirement {
	private final org.apache.felix.bundlerepository.Requirement requirement;
	private final Resource resource;
	
	public FelixRequirementAdapter(org.apache.felix.bundlerepository.Requirement requirement, Resource resource) {
		if (requirement == null)
			throw new NullPointerException("Missing required parameter: requirement");
		if (resource == null)
			throw new NullPointerException("Missing required parameter: resource");
		this.requirement = requirement;
		this.resource = resource;
	}

	public Map<String, Object> getAttributes() {
		return Collections.emptyMap();
	}

	public Map<String, String> getDirectives() {
		Map<String, String> result = new HashMap<String, String>(1);
		/* (1) The Felix OBR specific "mandatory:<*" syntax must be stripped out of the filter.
		 * (2) The namespace must be translated.
		 */
		result.put(Constants.FILTER_DIRECTIVE, requirement.getFilter()
				.replaceAll("\\(mandatory\\:\\<\\*[^\\)]*\\)", "")
				.replaceAll("\\(service\\=[^\\)]*\\)", "")
				.replaceAll("objectclass", "objectClass")
				.replaceAll(requirement.getName() + '=', getNamespace() + '='));
		return result;
	}

	public String getNamespace() {
		return NamespaceTranslator.translate(requirement.getName());
	}

	public Resource getResource() {
		return resource;
	}

	public boolean matches(Capability capability) {
		return requirement.isSatisfied(new OsgiCapabilityAdapter(capability));
	}
}
