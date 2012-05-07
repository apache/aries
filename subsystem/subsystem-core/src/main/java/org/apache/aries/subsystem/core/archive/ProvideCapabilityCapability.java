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
}
