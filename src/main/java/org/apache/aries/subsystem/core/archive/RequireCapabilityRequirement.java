package org.apache.aries.subsystem.core.archive;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.subsystem.core.resource.AbstractRequirement;
import org.osgi.resource.Namespace;
import org.osgi.resource.Resource;

public class RequireCapabilityRequirement extends AbstractRequirement {
	public static final String DIRECTIVE_FILTER = Namespace.REQUIREMENT_FILTER_DIRECTIVE;
	
	private final Map<String, String> directives = new HashMap<String, String>(1);
	private final String namespace;
	private final Resource resource;
	
	public RequireCapabilityRequirement(RequireCapabilityHeader.Clause clause) {
		this(clause, null);
	}
	
	public RequireCapabilityRequirement(RequireCapabilityHeader.Clause clause, Resource resource) {
		namespace = clause.getNamespace();
		Directive filter = clause.getDirective(RequireCapabilityHeader.Clause.DIRECTIVE_FILTER);
		if (filter != null)
			directives.put(DIRECTIVE_FILTER, filter.getValue());
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
		return namespace;
	}

	@Override
	public Resource getResource() {
		return resource;
	}
}
