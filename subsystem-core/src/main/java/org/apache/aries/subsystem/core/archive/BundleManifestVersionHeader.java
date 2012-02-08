package org.apache.aries.subsystem.core.archive;

import org.osgi.framework.Constants;

public class BundleManifestVersionHeader extends VersionHeader {
	public static final String DEFAULT_VALUE = "2.0";
	public static final String NAME = Constants.BUNDLE_MANIFESTVERSION;
	
	public BundleManifestVersionHeader() {
		this(DEFAULT_VALUE);
	}

	public BundleManifestVersionHeader(String value) {
		super(NAME, value);
	}
}
