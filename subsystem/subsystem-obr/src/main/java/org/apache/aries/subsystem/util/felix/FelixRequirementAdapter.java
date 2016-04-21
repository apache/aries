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
package org.apache.aries.subsystem.util.felix;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.subsystem.obr.internal.AbstractRequirement;
import org.apache.aries.subsystem.obr.internal.NamespaceTranslator;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Resource;

public class FelixRequirementAdapter extends AbstractRequirement {
	private final Map<String, String> directives;
	private final org.apache.felix.bundlerepository.Requirement requirement;
	private final Resource resource;
	
	public FelixRequirementAdapter(org.apache.felix.bundlerepository.Requirement requirement, Resource resource) {
		if (requirement == null)
			throw new NullPointerException("Missing required parameter: requirement");
		if (resource == null)
			throw new NullPointerException("Missing required parameter: resource");
		this.requirement = requirement;
		this.resource = resource;
		directives = computeDirectives();
	}

	public Map<String, Object> getAttributes() {
		return Collections.emptyMap();
	}

	public Map<String, String> getDirectives() {
		return directives;
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
	
	private Map<String, String> computeDirectives() {
		Map<String, String> result = new HashMap<String, String>(3);
		/* (1) The Felix OBR specific "mandatory:<*" syntax must be stripped out of the filter.
		 * (2) The namespace must be translated.
		 */
		String namespace = getNamespace();
		String filter = requirement.getFilter()
				.replaceAll("\\(mandatory\\:\\<\\*[^\\)]*\\)", "")
				.replaceAll("\\(service\\=[^\\)]*\\)", "")
				.replaceAll("objectclass", "objectClass")
				.replaceAll(requirement.getName() + '=', namespace + '=');
		if (BundleNamespace.BUNDLE_NAMESPACE.equals(namespace)) {
			filter = filter.replaceAll("symbolicname", namespace)
					.replaceAll("version", BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);
		}
		result.put(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter);
		result.put(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE, requirement.isOptional() ? Namespace.RESOLUTION_OPTIONAL : Namespace.RESOLUTION_MANDATORY);
		result.put(Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE, requirement.isMultiple() ? Namespace.CARDINALITY_MULTIPLE : Namespace.CARDINALITY_SINGLE);
		return Collections.unmodifiableMap(result);
	}
}
