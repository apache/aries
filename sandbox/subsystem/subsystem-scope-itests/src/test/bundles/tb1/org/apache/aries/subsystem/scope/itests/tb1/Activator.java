package org.apache.aries.subsystem.scope.itests.tb1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.apache.aries.subsystem.scope.Scope;
import org.apache.aries.subsystem.scope.SharePolicy;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class Activator implements BundleActivator {
	public void start(BundleContext bundleContext) throws Exception {
		ServiceReference<Scope> scopeRef = bundleContext.getServiceReference(Scope.class);
		assertNotNull(scopeRef);
		Scope scope = bundleContext.getService(scopeRef);
		assertNotNull(scope);
		assertEquals(bundleContext.getBundles().length, scope.getBundles().size());
		assertEquals(0, scope.getChildren().size());
		assertEquals(0, scope.getId());
		assertNull(scope.getLocation());
		assertEquals("root", scope.getName());
		assertNull(scope.getParent());
		assertEquals(0, scope.getSharePolicies(SharePolicy.TYPE_EXPORT).size());
		assertEquals(0, scope.getSharePolicies(SharePolicy.TYPE_IMPORT).size());
		assertNotNull(scope.newScopeUpdate());
	}

	public void stop(BundleContext arg0) throws Exception {
	}
}
