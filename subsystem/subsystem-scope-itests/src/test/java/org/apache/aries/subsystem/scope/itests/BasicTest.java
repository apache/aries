package org.apache.aries.subsystem.scope.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import org.apache.aries.subsystem.scope.InstallInfo;
import org.apache.aries.subsystem.scope.Scope;
import org.apache.aries.subsystem.scope.ScopeUpdate;
import org.apache.aries.subsystem.scope.SharePolicy;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public class BasicTest extends AbstractTest {
	/**
	 * Basic test of the initial state of the root scope.
	 */
	@Test
	public void testRootScopeInitialState() {
		Scope scope = getScope();
		Collection<Bundle> bundles = Arrays.asList(bundleContext.getBundles());
		assertCollectionEquals(bundles, scope.getBundles());
		assertEmpty(scope.getChildren());
		assertEquals(0, scope.getId());
		assertNull(scope.getLocation());
		assertEquals("root", scope.getName());
		assertNull(scope.getParent());
		assertEmpty(scope.getSharePolicies(SharePolicy.TYPE_EXPORT));
		assertEmpty(scope.getSharePolicies(SharePolicy.TYPE_IMPORT));
		assertNotNull(scope.newScopeUpdate());
	}
	
	/**
	 * Basic test of the initial state of the root scope from another bundle.
	 * The root scope instance should be the same as in the previous test.
	 * @throws Exception
	 */
	@Test
	public void testRootScopeInitialStateFromOtherBundle() throws Exception {
		Bundle tb1 = installBundle("tb-1.jar");
		try {
			tb1.start();
		}
		catch (BundleException e) {
			if (e.getCause() instanceof AssertionError) {
				throw (AssertionError)e.getCause();
			}
			throw e;
		}
		finally {
			tb1.uninstall();
		}
	}
	
	@Test
	public void testInstallBundleIntoRootScope() throws Exception {
		Scope scope = getScope();
		int previousSize = scope.getBundles().size();
		String location = getBundleLocation("tb-2.jar");
		URL url = new URL(location);
		InstallInfo tb2Info = new InstallInfo(location, url.openStream());
		ScopeUpdate scopeUpdate = scope.newScopeUpdate();
		scopeUpdate.getBundlesToInstall().add(tb2Info);
		assertTrue(scopeUpdate.commit());
		Bundle b = bundleContext.getBundle(location);
		assertNotNull(b);
		Collection<Bundle> bundles = scope.getBundles();
		assertEquals(previousSize + 1, bundles.size());
		assertTrue(bundles.contains(b));
	}
	
	@Test
	public void testCreateChildScope() throws Exception {
		Scope scope = getScope();
		String name = "scope1";
		ScopeUpdate parent = scope.newScopeUpdate();
		ScopeUpdate child = parent.newChild(name);
		parent.getChildren().add(child);
		assertTrue(parent.commit());
		Collection<Scope> children = scope.getChildren();
		assertEquals(1, children.size());
		Scope feature1 = null;
		for (Scope s : children) {
			if (name.equals(s.getName())) {
				feature1 = s;
				break;
			}
		}
		assertNotNull(feature1);
		assertEmpty(feature1.getBundles());
		assertEmpty(feature1.getChildren());
		assertEquals(1, feature1.getId());
		assertNull(feature1.getLocation());
		assertEquals(name, feature1.getName());
		assertEquals(scope, feature1.getParent());
		assertEmpty(feature1.getSharePolicies(SharePolicy.TYPE_EXPORT));
		assertEmpty(feature1.getSharePolicies(SharePolicy.TYPE_IMPORT));
	}
}
