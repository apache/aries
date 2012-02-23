package org.apache.aries.subsystem.itests.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.resource.Capability;
import org.osgi.framework.resource.Resource;

public class TestCapability implements Capability {
	public static class Builder {
		private final Map<String, Object> attributes = new HashMap<String, Object>();
		private final Map<String, String> directives = new HashMap<String, String>();
		
		private String namespace;
		private Resource resource;
		
		public Builder attribute(String name, Object value) {
			attributes.put(name,  value);
			return this;
		}
		
		public TestCapability build() {
			return new TestCapability(namespace, attributes, directives, resource);
		}
		
		public Builder directive(String name, String value) {
			directives.put(name, value);
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
	
	public TestCapability(
			String namespace,
			Map<String, Object> attributes,
			Map<String, String> directives,
			Resource resource) {
		this.namespace = namespace;
		this.attributes = new HashMap<String, Object>(attributes);
		this.directives = new HashMap<String, String>(directives);
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
