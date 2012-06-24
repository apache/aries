package org.apache.aries.subsystem.core.archive;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.subsystem.core.internal.AbstractRequirement;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Resource;

public class ImportPackageRequirement extends AbstractRequirement {
	public static final String DIRECTIVE_FILTER = PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE;
	public static final String NAMESPACE = PackageNamespace.PACKAGE_NAMESPACE;
	
	private final Map<String, String> directives;
	private final Resource resource;
	
	public ImportPackageRequirement(ImportPackageHeader.Clause clause, Resource resource) {
		Collection<Directive> clauseDirectives = clause.getDirectives();
		directives = new HashMap<String, String>(clauseDirectives.size() + 1);
		for (Directive directive : clauseDirectives)
			directives.put(directive.getName(), directive.getValue());
		Collection<String> packageNames = clause.getPackageNames();
		if (packageNames.isEmpty() || packageNames.size() > 1)
			throw new IllegalArgumentException("Only one package name per requirement allowed");
		StringBuilder filter = new StringBuilder("(&(").append(NAMESPACE)
				.append('=').append(packageNames.iterator().next()).append(')');
		VersionRangeAttribute versionRange = clause.getVersionRangeAttribute();
		if (versionRange != null) {
			versionRange.appendToFilter(filter);
		}
		directives.put(DIRECTIVE_FILTER, filter.append(')').toString());
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
