package org.apache.aries.subsystem.scope.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import org.apache.aries.subsystem.scope.InstallInfo;
import org.apache.aries.subsystem.scope.Scope;
import org.apache.aries.subsystem.scope.ScopeUpdate;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

/**
 * Bundles may only see other bundles within the same scope. The one exception
 * is the system bundle, which may be seen by all bundles regardless of scope.
 */
public class BundleVisibilityTest extends AbstractTest {
	/**
	 * Install a bundle into the same scope as this one. Both bundles should be
	 * able to see each other.
	 * @throws Exception
	 */
	@Test
	public void test1() throws Exception {
		Scope scope = getScope();
		assertTrue(scope.getBundles().contains(bundleContext.getBundle()));
		ScopeUpdate scopeUpdate = scope.newScopeUpdate();
		String location = getBundleLocation("tb-4.jar");
		assertNull(bundleContext.getBundle(location));
		URL url = new URL(location);
		InstallInfo installInfo = new InstallInfo(location, url.openStream());
		scopeUpdate.getBundlesToInstall().add(installInfo);
		scopeUpdate.commit();
		Bundle bundle = bundleContext.getBundle(location);
		assertTrue(scope.getBundles().contains(bundle));
		bundle.start();
		ServiceReference<BundleProvider> bundleProviderRef = bundleContext.getServiceReference(BundleProvider.class);
		BundleProvider bundleProvider = bundleContext.getService(bundleProviderRef);
		assertTrue(bundleProvider.getBundles().contains(bundleContext.getBundle()));
		assertTrue(Arrays.asList(bundleContext.getBundles()).contains(bundle));
		assertNotNull(bundleContext.getBundle(bundle.getBundleId()));
		assertNotNull(bundleProvider.getBundle(bundle.getBundleId()));
		bundleContext.ungetService(bundleProviderRef);
		bundle.uninstall();
	}
	
	/**
	 * Install a bundle into a different scope than this one. Neither bundle
	 * should be able to see the other.
	 * @throws Exception
	 */
	@Test
	public void test2() throws Exception {
		Scope scope = getScope();
		assertTrue(scope.getBundles().contains(bundleContext.getBundle()));
		ScopeUpdate scopeUpdate = scope.newScopeUpdate();
		ScopeUpdate child = scopeUpdate.newChild("tb4");
		scopeUpdate.getChildren().add(child);
		String location = getBundleLocation("tb-4.jar");
		assertNull(bundleContext.getBundle(location));
		URL url = new URL(location);
		InstallInfo installInfo = new InstallInfo(location, url.openStream());
		child.getBundlesToInstall().add(installInfo);
		addPackageImportPolicy("org.osgi.framework", child);
		addPackageImportPolicy("org.apache.aries.subsystem.scope", child);
		addPackageImportPolicy("org.apache.aries.subsystem.scope.itests", child);
		addServiceExportPolicy(BundleProvider.class, child);
		scopeUpdate.commit();
		Bundle bundle = bundleContext.getBundle(location);
		assertNotNull(bundle);
		Collection<Scope> childScopes = scope.getChildren();
		assertEquals(1, childScopes.size());
		assertTrue(childScopes.iterator().next().getBundles().contains(bundle));
		bundle.start();
		ServiceReference<BundleProvider> bundleProviderRef = bundleContext.getServiceReference(BundleProvider.class);
		BundleProvider bundleProvider = bundleContext.getService(bundleProviderRef);
		assertFalse(Arrays.asList(bundleContext.getBundles()).contains(bundle));
		assertNull(bundleContext.getBundle(bundle.getBundleId()));
		assertFalse(bundleProvider.getBundles().contains(bundleContext.getBundle()));
		assertNull(bundleProvider.getBundle(bundleContext.getBundle().getBundleId()));
		bundleContext.ungetService(bundleProviderRef);
		bundle.uninstall();
	}
}
