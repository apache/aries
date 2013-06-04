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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.subsystem.core.archive.BundleRequiredExecutionEnvironmentHeader.Clause.ExecutionEnvironment;
import org.apache.aries.subsystem.core.internal.AbstractRequirement;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.resource.Resource;

public class OsgiExecutionEnvironmentRequirement extends AbstractRequirement {
	public static final String ATTRIBUTE_VERSION = ExecutionEnvironmentNamespace.CAPABILITY_VERSION_ATTRIBUTE;
	public static final String DIRECTIVE_FILTER = ExecutionEnvironmentNamespace.REQUIREMENT_FILTER_DIRECTIVE;
	public static final String NAMESPACE = ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE;
	
	private final Map<String, String> directives;
	private final Resource resource;
	
	public OsgiExecutionEnvironmentRequirement(BundleRequiredExecutionEnvironmentHeader.Clause clause, Resource resource) {
		this(Collections.singleton(clause), resource);
	}
	
	public OsgiExecutionEnvironmentRequirement(Collection<BundleRequiredExecutionEnvironmentHeader.Clause> clauses, Resource resource) {
		StringBuilder filter = new StringBuilder("(|");
		for (BundleRequiredExecutionEnvironmentHeader.Clause clause : clauses) {
			ExecutionEnvironment ee = clause.getExecutionEnvironment();
			filter.append("(&(").append(NAMESPACE).append('=').append(ee.getName()).append(')');
			Version version = ee.getVersion();
			if (version != null)
				filter.append('(').append(ATTRIBUTE_VERSION).append('=')
						.append(version).append(')');
			filter.append(')');
		}
		directives = new HashMap<String, String>(1);
		directives.put(DIRECTIVE_FILTER, filter.append(')').toString());
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
