package org.apache.aries.subsystem.scope.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Arrays;

import org.apache.aries.subsystem.scope.InstallInfo;
import org.apache.aries.subsystem.scope.Scope;
import org.apache.aries.subsystem.scope.ScopeUpdate;
import org.apache.aries.subsystem.scope.SharePolicy;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Bundle;

public class PersistenceTest extends AbstractTest {
	/**
	 * When starting from a clean slate (i.e. nothing was persisted), only the 
	 * root scope with its default configuration should exist.
	 * 
	 * @throws Exception
	 */
	@Test
	public void test1() throws Exception {
		Scope scope = getScope();
		assertEquals(0, scope.getId());
		assertEquals("root", scope.getName());
		assertEquals(null, scope.getLocation());
		assertEquals(null, scope.getParent());
		assertEquals(0, scope.getChildren().size());
		assertCollectionEquals(Arrays.asList(bundleContext.getBundles()), scope.getBundles());
		assertEquals(0, scope.getSharePolicies(SharePolicy.TYPE_EXPORT).size());
		assertEquals(0, scope.getSharePolicies(SharePolicy.TYPE_IMPORT).size());
	}
	
	/**
	 * Stopping and starting the Scope Admin bundle should cause it to pull
	 * from the persistent storage. If nothing changed after the original
	 * bundle start, the persisted root bundle should look exactly the same
	 * as before.
	 * 
	 * @throws Exception
	 */
	@Test
	public void test2() throws Exception {
		Scope scope = getScope();
		Bundle bundle = findBundle("org.apache.aries.subsystem.scope.impl");
		assertNotNull(bundle);
		bundle.stop();
		bundle.start();
		assertEquals(0, scope.getId());
		assertEquals("root", scope.getName());
		assertEquals(null, scope.getLocation());
		assertEquals(null, scope.getParent());
		assertEquals(0, scope.getChildren().size());
		assertCollectionEquals(Arrays.asList(bundleContext.getBundles()), scope.getBundles());
		assertEquals(0, scope.getSharePolicies(SharePolicy.TYPE_EXPORT).size());
		assertEquals(0, scope.getSharePolicies(SharePolicy.TYPE_IMPORT).size());
	}
	
	/**
	 * A scope's persisted bundle data will become stale if bundles are 
	 * installed or uninstalled while Scope Admin is not connected to the 
	 * environment. This should be detected and dealt with.
	 * 
	 * @throws Exception
	 */
	@Test
	@Ignore
	public void test3() throws Exception {
		Scope scope = getScope();
		Bundle tb1 = findBundle("org.apache.aries.subsystem.scope.itests.tb1", scope);
		assertNull(tb1);
		tb1 = installBundle("tb-1.jar");
		assertTrue(scope.getBundles().contains(tb1));
		Bundle scopeAdmin = findBundle("org.apache.aries.subsystem.scope.impl");
		assertNotNull(scopeAdmin);
		scopeAdmin.stop();
		scopeAdmin.start();
		scope = getScope();
		assertTrue(scope.getBundles().contains(tb1));
		scopeAdmin.stop();
		tb1.uninstall();
		Bundle tb2 = findBundle("org.apache.aries.subsystem.scope.itests.tb2", scope);
		assertNull(tb2);
		tb2 = installBundle("tb-2.jar");
		scopeAdmin.start();
		scope = getScope();
		assertFalse(scope.getBundles().contains(tb1));
		assertTrue(scope.getBundles().contains(tb2));
		tb2.uninstall();
		assertFalse(scope.getBundles().contains(tb2));
	}
	
	/**
	 * Create two scopes off of the root scope with the following structure.
	 * 
	 *    R
	 *   / \
	 * S1   S2
	 * 
	 * S1 contains bundle tb1, one import policy, and one export policy.
	 * S2 contains bundle tb2 and two import policies.
	 * 
	 * This configuration should persist between restarts of the Scope Admin
	 * bundle.
	 * 
	 * @throws Exception
	 */
	@Test
	public void test4() throws Exception {
		Scope root = getScope();
		ScopeUpdate rootUpdate = root.newScopeUpdate();
		ScopeUpdate s1Update = rootUpdate.newChild("S1");
		rootUpdate.getChildren().add(s1Update);
		ScopeUpdate s2Update = rootUpdate.newChild("S2");
		rootUpdate.getChildren().add(s2Update);
		s1Update.getBundlesToInstall().add(
				new InstallInfo(
						null,
						new URL(getBundleLocation("tb-1.jar"))));
		s2Update.getBundlesToInstall().add(
				new InstallInfo(
						null,
						new URL(getBundleLocation("tb-2.jar"))));
		addPackageImportPolicy("org.osgi.framework", s1Update);
		addPackageExportPolicy("org.apache.aries.subsystem.scope.itests.tb1", s1Update);
		addPackageImportPolicy("org.osgi.framework", s2Update);
		addPackageImportPolicy("org.apache.aries.subsystem.scope.itests.tb1", s2Update);
		assertTrue(rootUpdate.commit());
		root = getScope();
		assertEquals(2, root.getChildren().size());
		Scope s1 = findChildScope("S1", root);
		Bundle tb1 = findBundle("org.apache.aries.subsystem.scope.itests.tb1", s1);
		assertNotNull(tb1);
		assertTrue(s1.getBundles().contains(tb1));
		assertEquals(1, s1.getSharePolicies(SharePolicy.TYPE_IMPORT).get("osgi.wiring.package").size());
		assertEquals(1, s1.getSharePolicies(SharePolicy.TYPE_EXPORT).get("osgi.wiring.package").size());
		Scope s2 = findChildScope("S2", root);
		Bundle tb2 = findBundle("org.apache.aries.subsystem.scope.itests.tb2", s2);
		assertNotNull(tb2);
		assertTrue(s2.getBundles().contains(tb2));
		assertEquals(2, s2.getSharePolicies(SharePolicy.TYPE_IMPORT).get("osgi.wiring.package").size());
		Bundle scopeAdmin = findBundle("org.apache.aries.subsystem.scope.impl");
		assertNotNull(scopeAdmin);
		scopeAdmin.stop();
		scopeAdmin.start();
		root = getScope();
		assertEquals(2, root.getChildren().size());
		s1 = findChildScope("S1", root);
		assertTrue(s1.getBundles().contains(tb1));
		assertEquals(1, s1.getSharePolicies(SharePolicy.TYPE_IMPORT).get("osgi.wiring.package").size());
		assertEquals(1, s1.getSharePolicies(SharePolicy.TYPE_EXPORT).get("osgi.wiring.package").size());
		s2 = findChildScope("S2", root);
		assertTrue(s2.getBundles().contains(tb2));
		assertEquals(2, s2.getSharePolicies(SharePolicy.TYPE_IMPORT).get("osgi.wiring.package").size());
	}
}
