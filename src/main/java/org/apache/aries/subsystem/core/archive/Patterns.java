package org.apache.aries.subsystem.core.archive;

import java.util.regex.Pattern;

import org.osgi.framework.namespace.PackageNamespace;

public class Patterns {
	public static final Pattern NAMESPACE = Pattern.compile('(' + Grammar.NAMESPACE + ")(?=;|\\z)");
	public static final Pattern OBJECTCLASS_OR_STAR = Pattern.compile("((" + Grammar.OBJECTCLASS + ")|[*])(?=;|\\z)");
	public static final Pattern PACKAGE_NAMES = Pattern.compile('(' + Grammar.PACKAGENAMES + ")(?=;|\\z)");
	public static final Pattern PACKAGE_NAMESPACE = Pattern.compile("\\((" + PackageNamespace.PACKAGE_NAMESPACE + ")(=)([^\\)]+)\\)");
	public static final Pattern PARAMETER = Pattern.compile('(' + Grammar.PARAMETER + ")(?=;|\\z)");
	public static final Pattern PATHS = Pattern.compile('(' + Grammar.PATH + "\\s*(?:\\;\\s*" + Grammar.PATH + ")*)(?=;|\\z)");
	public static final Pattern SUBSYSTEM_TYPE = Pattern.compile('(' + SubsystemTypeHeader.TYPE_APPLICATION + '|' + SubsystemTypeHeader.TYPE_COMPOSITE + '|' + SubsystemTypeHeader.TYPE_FEATURE + ")(?=;|\\z)");
	public static final Pattern SYMBOLIC_NAME = Pattern.compile('(' + Grammar.SYMBOLICNAME + ")(?=;|\\z)");
	public static final Pattern WILDCARD_NAMES = Pattern.compile('(' + Grammar.WILDCARD_NAMES + ")(?=;|\\z)");
	
	private static final String DIRECTIVE = '(' + Grammar.EXTENDED + ")(:=)(" + Grammar.ARGUMENT + ')';
	private static final String TYPED_ATTR = '(' + Grammar.EXTENDED + ")(?:(\\:)(" + Grammar.TYPE + "))?=(" + Grammar.ARGUMENT + ')';
	public static final Pattern TYPED_PARAMETER = Pattern.compile("(?:(?:" + DIRECTIVE + ")|(?:" + TYPED_ATTR + "))(?=;|\\z)");
	
	public static final Pattern SCALAR_LIST = Pattern.compile("List(?:<(String|Long|Double|Version)>)?");
}
