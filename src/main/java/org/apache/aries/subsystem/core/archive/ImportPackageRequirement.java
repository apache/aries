package org.apache.aries.subsystem.core.archive;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.subsystem.core.resource.AbstractRequirement;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Resource;

public class ImportPackageRequirement extends AbstractRequirement {
	private static final String BUNDLE_SYMBOLICNAME = PackageNamespace.CAPABILITY_BUNDLE_SYMBOLICNAME_ATTRIBUTE;
	private static final String BUNDLE_VERSION = PackageNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE;
	private static final String EQUAL = "=";
	private static final String FILTER = PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE;
	private static final String GREATER_THAN = ">";
	private static final String GREATER_THAN_OR_EQUAL = GREATER_THAN + EQUAL;
	private static final String LESS_THAN = "<";
	private static final String LESS_THAN_OR_EQUAL = LESS_THAN + EQUAL;
	private static final String NAMESPACE = PackageNamespace.PACKAGE_NAMESPACE;
	private static final String RESOLUTION = PackageNamespace.REQUIREMENT_RESOLUTION_DIRECTIVE;
	private static final String RESOLUTION_MANDATORY = PackageNamespace.RESOLUTION_MANDATORY;
	private static final String RESOLUTION_OPTIONAL = PackageNamespace.RESOLUTION_OPTIONAL;
	private static final String VERSION = PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE;
	
	private final Map<String, String> directives = new HashMap<String, String>(1);
	
	public ImportPackageRequirement(ImportPackageHeader.Clause clause) {
		Collection<String> packageNames = clause.getPackageNames();
		if (packageNames.isEmpty() || packageNames.size() > 1)
			throw new IllegalArgumentException("Only one package name per requirement allowed");
		StringBuilder filter = new StringBuilder("(&(").append(NAMESPACE)
				.append('=').append(packageNames.iterator().next()).append(')');
		VersionRangeAttribute versionRange = clause.getVersionRangeAttribute();
		if (versionRange != null) {
			versionRange.appendToFilter(filter);
		}
		directives.put(FILTER, filter.append(')').toString());
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
		return PackageNamespace.PACKAGE_NAMESPACE;
	}

	@Override
	public Resource getResource() {
		return null;
	}
}
