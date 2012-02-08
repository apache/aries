package org.apache.aries.subsystem.core.archive;

import org.osgi.framework.Constants;

public class BundleSymbolicNameHeader extends SymbolicNameHeader {
	public static final String NAME = Constants.BUNDLE_SYMBOLICNAME;
	
	public BundleSymbolicNameHeader(String value) {
		super(NAME, value);
	}
}
