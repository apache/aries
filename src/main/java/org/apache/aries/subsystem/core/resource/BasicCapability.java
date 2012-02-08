package org.apache.aries.subsystem.core.resource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.resource.Resource;

public class BasicCapability extends AbstractCapability {
	private final Map<String, Object> attributes;
	private final Map<String, String> directives;
	private final Resource resource;
	private final String namespace;
	
	public BasicCapability(String namespace, Map<String, Object> attributes, Map<String, String> directives, Resource resource) {
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
