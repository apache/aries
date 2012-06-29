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
