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
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.resource.Resource;

public class RequireBundleRequirement extends AbstractRequirement {
	public static final String DIRECTIVE_FILTER = BundleNamespace.REQUIREMENT_FILTER_DIRECTIVE;
	public static final String NAMESPACE = BundleNamespace.BUNDLE_NAMESPACE;
	
	private final Map<String, String> directives;
	private final Resource resource;
	
	public RequireBundleRequirement(
			RequireBundleHeader.Clause clause, Resource resource) {
		directives = new HashMap<String, String>(clause.getDirectives().size() + 1);
		for (Directive directive : clause.getDirectives())
			directives.put(directive.getName(), directive.getValue());
		StringBuilder builder = new StringBuilder("(&(")
				.append(NAMESPACE).append('=')
				.append(clause.getSymbolicName()).append(')');
		for (Attribute attribute : clause.getAttributes())
			attribute.appendToFilter(builder);
		directives.put(DIRECTIVE_FILTER, builder.append(')').toString());
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((directives == null) ? 0 : directives.hashCode());
        result = prime * result
                + ((resource == null) ? 0 : resource.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        RequireBundleRequirement other = (RequireBundleRequirement) obj;
        if (directives == null) {
            if (other.directives != null)
                return false;
        } else
            if (!directives.equals(other.directives))
                return false;
        if (resource == null) {
            if (other.resource != null)
                return false;
        } else
            if (!resource.equals(other.resource))
                return false;
        return true;
    }
	
}
