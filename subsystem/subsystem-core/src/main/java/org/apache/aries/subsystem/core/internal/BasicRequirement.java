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
package org.apache.aries.subsystem.core.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class BasicRequirement extends AbstractRequirement {
	public static class Builder {
		private final Map<String, Object> attributes = new HashMap<String, Object>();
		private final Map<String, String> directives = new HashMap<String, String>();
		private Resource resource;
		private String namespace;
		
		public Builder attribute(String key, Object value) {
			attributes.put(key, value);
			return this;
		}
		
		public Builder attributes(Map<String, Object> values) {
			attributes.putAll(values);
			return this;
		}
		
		public BasicRequirement build() {
			return new BasicRequirement(namespace, attributes, directives, resource);
		}
		
		public Builder directive(String key, String value) {
			directives.put(key, value);
			return this;
		}
		
		public Builder directives(Map<String, String> values) {
			directives.putAll(values);
			return this;
		}
		
		public Builder namespace(String value) {
			namespace = value;
			return this;
		}
		
		public Builder resource(Resource value) {
			resource = value;
			return this;
		}
	}
	
	private final Map<String, Object> attributes;
	private final Map<String, String> directives;
	private final String namespace;
	private final Resource resource;
	
	public BasicRequirement(Requirement requirement, Resource resource) {
		attributes = requirement.getAttributes();
		directives = requirement.getDirectives();
		namespace = requirement.getNamespace();
		this.resource = resource;
	}
	
	public BasicRequirement(String namespace, String filter) throws InvalidSyntaxException {
		this(namespace, FrameworkUtil.createFilter(filter));
	}
	
	public BasicRequirement(String namespace, Filter filter) {
		if (namespace == null)
			throw new NullPointerException("Missing required parameter: namespace");
		attributes = Collections.emptyMap();
		Map<String, String> directives = new HashMap<String, String>(1);
		directives.put(Constants.FILTER_DIRECTIVE, filter.toString());
		this.directives = Collections.unmodifiableMap(directives);
		this.namespace = namespace;
		resource = null;
	}
	
	private BasicRequirement(String namespace, Map<String, Object> attributes, Map<String, String> directives, Resource resource) {
		if (namespace == null)
			throw new NullPointerException();
		this.namespace = namespace;
		if (attributes == null)
			this.attributes = Collections.emptyMap();
		else
			this.attributes = Collections.unmodifiableMap(new HashMap<String, Object>(attributes));
		if (directives == null)
			this.directives = Collections.emptyMap();
		else
			this.directives = Collections.unmodifiableMap(new HashMap<String, String>(directives));
		if (resource == null)
			throw new NullPointerException();
		this.resource = resource;
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
		return namespace;
	}

	@Override
	public Resource getResource() {
		return resource;
	}

}
