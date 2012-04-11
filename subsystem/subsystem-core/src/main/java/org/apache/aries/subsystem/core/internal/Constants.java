package org.apache.aries.subsystem.core.internal;

import org.osgi.framework.namespace.IdentityNamespace;

public class Constants {
	public static final String BundleSymbolicName = org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME;
	public static final String BundleVersion = org.osgi.framework.Constants.BUNDLE_VERSION;
	public static final String RegionContextBundleSymbolicNamePrefix = "org.osgi.service.subsystem.region.context.";
	public static final String ResourceTypeBundle = IdentityNamespace.TYPE_BUNDLE;
	public static final String ResourceTypeFragment = IdentityNamespace.TYPE_FRAGMENT;
	public static final String SubsystemServicePropertyRegions = "org.apache.aries.subsystem.service.regions";
	
	private Constants() {}
}
