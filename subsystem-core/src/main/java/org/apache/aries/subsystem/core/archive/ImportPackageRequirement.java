package org.apache.aries.subsystem.core.archive;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Requirement;

public class ImportPackageRequirement {
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
	
	private static final String REGEX = 
			"\\((" + NAMESPACE + ")(=)([^\\)]+)\\)";
	private static final Pattern PATTERN = Pattern.compile(REGEX);
	
	private final String packageName;
	
	public ImportPackageRequirement(Requirement requirement) {
		if (!NAMESPACE.equals(requirement.getNamespace()))
			throw new IllegalArgumentException("Requirement must be in the '" + NAMESPACE + "' namespace");
		String filter = requirement.getDirectives().get(FILTER);
		String packageName = null;
		Matcher matcher = PATTERN.matcher(filter);
		while (matcher.find()) {
			String name = matcher.group(1);
			String operator = matcher.group(2);
			String value = matcher.group(3);
			if (NAMESPACE.equals(name)) {
				packageName = value;
			}
		}
		if (packageName == null)
			throw new IllegalArgumentException("Missing filter key: " + NAMESPACE);
		this.packageName = packageName;
	}
	
	public ImportPackageHeader.Clause toClause() {
		return new ImportPackageHeader.Clause(packageName);
	}
}
