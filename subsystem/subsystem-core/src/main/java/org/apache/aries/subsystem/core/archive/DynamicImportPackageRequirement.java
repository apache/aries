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

import org.apache.aries.subsystem.core.internal.AbstractRequirement;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Resource;

public class DynamicImportPackageRequirement extends AbstractRequirement {
	public static final String DIRECTIVE_FILTER = PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE;
	public static final String NAMESPACE = PackageNamespace.PACKAGE_NAMESPACE;
	
	private final Map<String, String> directives;
	private final String packageName;
	private final Resource resource;
	
	public DynamicImportPackageRequirement(String pkg, DynamicImportPackageHeader.Clause clause, Resource resource) {
		packageName = pkg;
		Collection<Directive> clauseDirectives = clause.getDirectives();
		directives = new HashMap<String, String>(clauseDirectives.size() + 1);
		for (Directive directive : clauseDirectives)
			directives.put(directive.getName(), directive.getValue());
		StringBuilder filter = new StringBuilder("(&(").append(NAMESPACE)
				.append('=').append(pkg).append(')');
		VersionRangeAttribute versionRange = clause.getVersionRangeAttribute();
		if (versionRange != null) {
			versionRange.appendToFilter(filter);
		}
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
	
	public String getPackageName() {
		return packageName;
	}

	@Override
	public Resource getResource() {
		return resource;
	}
}
