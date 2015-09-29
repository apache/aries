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
package org.apache.aries.subsystem.core.archive;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.subsystem.core.internal.AbstractRequirement;
import org.osgi.resource.Namespace;
import org.osgi.resource.Resource;

public class RequireCapabilityRequirement extends AbstractRequirement {
	public static final String DIRECTIVE_FILTER = Namespace.REQUIREMENT_FILTER_DIRECTIVE;
	
	private final Map<String, Object> attributes;
	private final Map<String, String> directives;
	private final String namespace;
	private final Resource resource;
	
	public RequireCapabilityRequirement(RequireCapabilityHeader.Clause clause, Resource resource) {
		namespace = clause.getNamespace();
		attributes = new HashMap<String, Object>(clause.getAttributes().size());
		for (Attribute attribute : clause.getAttributes()) {
			attributes.put(attribute.getName(), attribute.getValue());
		}
		directives = new HashMap<String, String>(clause.getDirectives().size());
		for (Directive directive : clause.getDirectives()) {
			directives.put(directive.getName(), directive.getValue());
		}
		this.resource = resource;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return Collections.unmodifiableMap(attributes);
	}

	@Override
	public Map<String, String> getDirectives() {
		return Collections.unmodifiableMap(directives);
	}

	@Override
	public String getNamespace() {
		return namespace;
	}

	@Override
	public Resource getResource() {
		return resource;
	}
}
