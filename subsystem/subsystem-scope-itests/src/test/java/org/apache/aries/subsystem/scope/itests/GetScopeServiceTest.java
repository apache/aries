package org.apache.aries.subsystem.scope.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.apache.aries.subsystem.scope.InstallInfo;
import org.apache.aries.subsystem.scope.Scope;
import org.apache.aries.subsystem.scope.ScopeUpdate;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

/**
 * Tests that bundles requesting a Scope service receive the correct one. 
 * Bundles should receive the Scope within which they exist. Requesting bundles 
 * are in the root scope by default.
 */
public class GetScopeServiceTest extends AbstractTest {
	/**
	 * The test bundle should be in and receive the root scope by default. The
	 * root scope will always have an ID of '0' and name of 'root'.
	 * @throws Exception
	 */
	@Test
	public void test1() throws Exception {
		Scope scope = getScope();
		assertEquals(0, scope.getId());
		assertEquals("root", scope.getName());
		assertTrue(scope.getBundles().contains(bundleContext.getBundle()));
	}
	
	/**
	 * The tb3 bundle should also be in and receive the root scope by default.
	 * @throws Exception
	 */
	@Test
	public void test2() throws Exception {
		Bundle bundle = installBundle("tb-3.jar");
		bundle.start();
		ServiceReference<ScopeProvider> scopeProviderRef = bundleContext.getServiceReference(ScopeProvider.class);
		ScopeProvider scopeProvider = bundleContext.getService(scopeProviderRef);
		Scope scope = scopeProvider.getScope();
		assertEquals(getScope(), scope);
		assertTrue(scope.getBundles().contains(bundle));
		bundleContext.ungetService(scopeProviderRef);
		bundle.uninstall();
	}
	
	/**
	 * A new scope is created as a child of the root scope and the tb3 bundle
	 * is added to it. The tb3 bundle should receive and be in the new scope.
	 * @throws Exception
	 */
	@Test
	public void test3() throws Exception {
		Scope scope = getScope();
		ScopeUpdate scopeUpdate = scope.newScopeUpdate();
		ScopeUpdate child = scopeUpdate.newChild("tb3");
		scopeUpdate.getChildren().add(child);
		String location = getBundleLocation("tb-3.jar");
		URL url = new URL(location);
		InstallInfo installInfo = new InstallInfo(location, url.openStream());
		child.getBundlesToInstall().add(installInfo);
		addPackageImportPolicy("org.osgi.framework", child);
		addPackageImportPolicy("org.apache.aries.subsystem.scope", child);
		addPackageImportPolicy("org.apache.aries.subsystem.scope.itests", child);
		addServiceImportPolicy(Scope.class, child);
		addServiceExportPolicy(ScopeProvider.class, child);
		scopeUpdate.commit();
		Bundle bundle = bundleContext.getBundle(location);
		bundle.start();
		ServiceReference<ScopeProvider> scopeProviderRef = bundleContext.getServiceReference(ScopeProvider.class);
		ScopeProvider scopeProvider = bundleContext.getService(scopeProviderRef);
		scope = scopeProvider.getScope();
		assertEquals("tb3", scope.getName());
		assertTrue(scope.getBundles().contains(bundle));
		bundleContext.ungetService(scopeProviderRef);
		bundle.uninstall();
	}
	
	/**
	 * A new scope is created as a child of the root scope and the tb3 bundle
	 * is added to it. The tb3 bundle should receive and be in the new scope.
	 * The bundle is added directly as opposed to via an InstallInfo.
	 * @throws Exception
	 */
	@Test
	public void test4() throws Exception {
		Scope scope = getScope();
		Bundle bundle = installBundle("tb-3.jar");
		ScopeUpdate scopeUpdate = scope.newScopeUpdate();
		scopeUpdate.getBundles().remove(bundle);
		ScopeUpdate child = scopeUpdate.newChild("tb3");
		scopeUpdate.getChildren().add(child);
		child.getBundles().add(bundle);
		addPackageImportPolicy("org.osgi.framework", child);
		addPackageImportPolicy("org.apache.aries.subsystem.scope", child);
		addPackageImportPolicy("org.apache.aries.subsystem.scope.itests", child);
		addServiceImportPolicy(Scope.class, child);
		addServiceExportPolicy(ScopeProvider.class, child);
		scopeUpdate.commit();
		bundle.start();
		ServiceReference<ScopeProvider> scopeProviderRef = bundleContext.getServiceReference(ScopeProvider.class);
		ScopeProvider scopeProvider = bundleContext.getService(scopeProviderRef);
		scope = scopeProvider.getScope();
		assertEquals("tb3", scope.getName());
		assertTrue(scope.getBundles().contains(bundle));
		bundleContext.ungetService(scopeProviderRef);
		bundle.uninstall();
	}
}
