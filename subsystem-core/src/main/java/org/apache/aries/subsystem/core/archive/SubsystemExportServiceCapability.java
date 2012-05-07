package org.apache.aries.subsystem.core.archive;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.subsystem.core.internal.AbstractCapability;
import org.osgi.framework.Constants;
import org.osgi.resource.Namespace;
import org.osgi.resource.Resource;

public class SubsystemExportServiceCapability extends AbstractCapability {
	public static final String DIRECTIVE_FILTER = Namespace.REQUIREMENT_FILTER_DIRECTIVE;
	// TODO Replace value with ServiceNamspace.SERVICE_NAMESPACE constant when available.
	public static final String NAMESPACE = "osgi.service";
	
	private final Map<String, Object> attributes = new HashMap<String, Object>();
	private final Map<String, String> directives = new HashMap<String, String>();
	private final Resource resource;
	
	public SubsystemExportServiceCapability(SubsystemExportServiceHeader.Clause clause, Resource resource) {
		StringBuilder builder = new StringBuilder("(&(")
				.append(Constants.OBJECTCLASS).append('=')
				.append(clause.getObjectClass()).append(')');
		Directive filter = clause
				.getDirective(SubsystemImportServiceHeader.Clause.DIRECTIVE_FILTER);
		if (filter != null)
			builder.append(filter.getValue());
		directives.put(DIRECTIVE_FILTER, builder.append(')').toString());
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
		return NAMESPACE;
	}

	@Override
	public Resource getResource() {
		return resource;
	}
}
