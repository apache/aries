package org.apache.aries.subsystem.core.archive;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.subsystem.core.resource.AbstractRequirement;
import org.osgi.framework.Constants;
import org.osgi.resource.Namespace;
import org.osgi.resource.Resource;

public class SubsystemImportServiceRequirement extends AbstractRequirement {
	public static final String DIRECTIVE_FILTER = Namespace.REQUIREMENT_FILTER_DIRECTIVE;
	// TODO Replace value with ServiceNamspace.SERVICE_NAMESPACE constant when available.
	public static final String NAMESPACE = "osgi.service";
	
	private final Map<String, String> directives = new HashMap<String, String>(1);
	private final Resource resource;
	
	public SubsystemImportServiceRequirement(SubsystemImportServiceHeader.Clause clause) {
		this(clause, null);
	}
	
	public SubsystemImportServiceRequirement(
			SubsystemImportServiceHeader.Clause clause, Resource resource) {
		StringBuilder builder = new StringBuilder("(&(")
				.append(Constants.OBJECTCLASS).append('=')
				.append(clause.getServiceName()).append(')');
		Directive filter = clause
				.getDirective(SubsystemImportServiceHeader.Clause.DIRECTIVE_FILTER);
		if (filter != null)
			builder.append(filter.getValue());
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
