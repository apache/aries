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

import org.apache.aries.subsystem.core.internal.AbstractCapability;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.resource.Resource;

public class ProvideBundleCapability extends AbstractCapability {
	public static final String ATTRIBUTE_BUNDLE_VERSION = BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE;
	public static final String DIRECTIVE_EFFECTIVE = BundleNamespace.CAPABILITY_EFFECTIVE_DIRECTIVE;
	public static final String DIRECTIVE_FRAGMENT_ATTACHMENT = BundleNamespace.CAPABILITY_FRAGMENT_ATTACHMENT_DIRECTIVE;
	public static final String DIRECTIVE_MANDATORY = BundleNamespace.CAPABILITY_MANDATORY_DIRECTIVE;
	public static final String DIRECTIVE_SINGLETON = BundleNamespace.CAPABILITY_SINGLETON_DIRECTIVE;
	public static final String DIRECTIVE_USES = BundleNamespace.CAPABILITY_USES_DIRECTIVE;
	public static final String NAMESPACE = BundleNamespace.BUNDLE_NAMESPACE;
	
	private static Map<String, Object> initializeAttributes(BundleSymbolicNameHeader bsn, BundleVersionHeader version) {
		if (version == null) {
			version = new BundleVersionHeader();
		}
		Clause clause = bsn.getClauses().get(0);
		Collection<Attribute> attributes = clause.getAttributes();
		Map<String, Object> result = new HashMap<String, Object>(attributes.size() + 2);
		result.put(NAMESPACE, clause.getPath());
		result.put(ATTRIBUTE_BUNDLE_VERSION, version.getValue());
		for (Attribute attribute : attributes) {
			result.put(attribute.getName(), attribute.getValue());
		}
		return Collections.unmodifiableMap(result);
	}
	
	private static Map<String, String> initializeDirectives(Collection<Directive> directives) {
		if (directives.isEmpty())
			return Collections.emptyMap();
		Map<String, String> result = new HashMap<String, String>(directives.size());
		for (Directive directive : directives) {
			result.put(directive.getName(), directive.getValue());
		}
		return Collections.unmodifiableMap(result);
	}
	
	private final Map<String, Object> attributes;
	private final Map<String, String> directives;
	private final Resource resource;
	
	public ProvideBundleCapability(BundleSymbolicNameHeader bsn, BundleVersionHeader version, Resource resource) {
		if (resource == null)
			throw new NullPointerException("Missing required parameter: resource");
		this.resource = resource;
		attributes = initializeAttributes(bsn, version);
		directives = initializeDirectives(bsn.getClauses().get(0).getDirectives());
	}

	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	@Override
	public Map<String, String> getDirectives() {
		return directives;
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
                + ((attributes == null) ? 0 : attributes.hashCode());
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
        ProvideBundleCapability other = (ProvideBundleCapability) obj;
        if (attributes == null) {
            if (other.attributes != null)
                return false;
        } else
            if (!attributes.equals(other.attributes))
                return false;
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
