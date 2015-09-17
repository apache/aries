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

import org.apache.aries.subsystem.core.internal.AbstractCapability;
import org.osgi.resource.Resource;

public class ProvideCapabilityCapability extends AbstractCapability {
    
	private final Map<String, Object> attributes = new HashMap<String, Object>();
	private final Map<String, String> directives = new HashMap<String, String>();
	private final String namespace;
	private final Resource resource;
	
	public ProvideCapabilityCapability(ProvideCapabilityHeader.Clause clause, Resource resource) {
		namespace = clause.getNamespace();
		for (Parameter parameter : clause.getParameters()) {
			if (parameter instanceof Attribute)
				attributes.put(parameter.getName(), parameter.getValue());
			else
				directives.put(parameter.getName(), ((Directive)parameter).getValue());
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((attributes == null) ? 0 : attributes.hashCode());
        result = prime * result
                + ((directives == null) ? 0 : directives.hashCode());
        result = prime * result
                + ((namespace == null) ? 0 : namespace.hashCode());
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
        ProvideCapabilityCapability other = (ProvideCapabilityCapability) obj;
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
        if (namespace == null) {
            if (other.namespace != null)
                return false;
        } else
            if (!namespace.equals(other.namespace))
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
