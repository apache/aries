package org.apache.aries.subsystem.core.archive;

import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class BundleVersionHeader extends VersionHeader {
	public static final String DEFAULT_VALUE = Version.emptyVersion.toString();
	public static final String NAME = Constants.BUNDLE_VERSION;
	
	public BundleVersionHeader() {
		this(DEFAULT_VALUE);
	}

	public BundleVersionHeader(String value) {
		super(NAME, value);
	}
}
