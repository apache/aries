package org.apache.aries.subsystem.scope.itests;

import java.util.Collection;

import org.osgi.framework.Bundle;

public interface BundleProvider {
//IC see: https://issues.apache.org/jira/browse/ARIES-594
	Bundle getBundle(long id);
	
	Collection<Bundle> getBundles();
}
