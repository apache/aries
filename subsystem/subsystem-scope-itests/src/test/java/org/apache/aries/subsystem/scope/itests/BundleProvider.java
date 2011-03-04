package org.apache.aries.subsystem.scope.itests;

import java.util.Collection;

import org.osgi.framework.Bundle;

public interface BundleProvider {
	Bundle getBundle(long id);
	
	Collection<Bundle> getBundles();
}
