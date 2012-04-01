package org.apache.aries.subsystem.core.archive;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.subsystem.core.resource.AbstractRequirement;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.resource.Resource;

public class RequireBundleRequirement extends AbstractRequirement {
	public static final String DIRECTIVE_FILTER = BundleNamespace.REQUIREMENT_FILTER_DIRECTIVE;
	public static final String NAMESPACE = BundleNamespace.BUNDLE_NAMESPACE;
	
	private final Map<String, String> directives = new HashMap<String, String>(1);
	private final Resource resource;
	
	public RequireBundleRequirement(
			RequireBundleHeader.Clause clause, Resource resource) {
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
