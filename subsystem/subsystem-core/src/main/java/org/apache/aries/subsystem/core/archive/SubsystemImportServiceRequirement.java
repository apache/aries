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
import org.osgi.framework.Constants;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Namespace;
import org.osgi.resource.Resource;

public class SubsystemImportServiceRequirement extends AbstractRequirement {
	public static final String DIRECTIVE_FILTER = Namespace.REQUIREMENT_FILTER_DIRECTIVE;
	public static final String NAMESPACE = ServiceNamespace.SERVICE_NAMESPACE;
	
	private final Map<String, String> directives = new HashMap<String, String>(1);
	private final Resource resource;
	
	public SubsystemImportServiceRequirement(
			SubsystemImportServiceHeader.Clause clause, Resource resource) {
		boolean appendObjectClass = !ServiceNamespace.SERVICE_NAMESPACE.equals(clause.getPath());
		StringBuilder builder = new StringBuilder();
		if (appendObjectClass) {
			builder.append("(&(").append(Constants.OBJECTCLASS).append('=').append(clause.getPath()).append(')');
		}
		Directive filter = clause.getDirective(SubsystemExportServiceHeader.Clause.DIRECTIVE_FILTER);
		if (filter != null) {
			builder.append(filter.getValue());
		}
		if (appendObjectClass) {
			builder.append(')');
		}
		String filterStr = builder.toString();
		if (!filterStr.isEmpty()) {
			directives.put(DIRECTIVE_FILTER, filterStr);
		}
		this.resource = resource;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return Collections.emptyMap();
	}

	@Override
	public Map<String, String> getDirectives() {
		return Collections.unmodifiableMap(directives);
	}

	@Override
	public String getNamespace() {
		return NAMESPACE;
	}

	@Override
	public Resource getResource() {
		return resource;
	}
}
