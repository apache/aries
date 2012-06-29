package org.apache.aries.subsystem.core.archive;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.subsystem.core.internal.AbstractRequirement;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Resource;

public class SubsystemContentRequirement extends AbstractRequirement {
	public static final String DIRECTIVE_FILTER = IdentityNamespace.REQUIREMENT_FILTER_DIRECTIVE;
	public static final String NAMESPACE = IdentityNamespace.IDENTITY_NAMESPACE;
	
	private final Map<String, String> directives = new HashMap<String, String>();
	private final Resource resource;
	
	public SubsystemContentRequirement(
			SubsystemContentHeader.Clause clause, Resource resource) {
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
}
