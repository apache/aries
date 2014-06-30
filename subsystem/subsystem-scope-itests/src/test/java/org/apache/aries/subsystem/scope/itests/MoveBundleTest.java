package org.apache.aries.subsystem.scope.itests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.aries.subsystem.scope.Scope;
import org.apache.aries.subsystem.scope.ScopeUpdate;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Bundle;

/**
 * Bundles may be moved from one scope to another.
 */
public class MoveBundleTest extends AbstractTest {
	/**
	 * Create two scopes off of the root scope with the following structure.
	 *    R
	 *   / \
	 * S1   S2
	 * Install a bundle using the test bundle's bundle context. This should add
	 * the bundle to R since the test bundle is in R. Next, move the bundle 
	 * into S1. Finally, move the bundle into S2.
	 * @throws Exception
	 */
	@Test
	@Ignore
	public void test1() throws Exception {
		Bundle tb2 = installBundle("tb-2.jar");
		Scope root = getScope();
		ScopeUpdate rootUpdate = root.newScopeUpdate();
		ScopeUpdate s1Update = rootUpdate.newChild("S1");
		ScopeUpdate s2Update = rootUpdate.newChild("S2");
		rootUpdate.getChildren().add(s1Update);
		rootUpdate.getChildren().add(s2Update);
		rootUpdate.commit();
		Scope s1 = s1Update.getScope();
		Scope s2 = s2Update.getScope();
		assertTrue(root.getBundles().contains(tb2));
		assertFalse(s1.getBundles().contains(tb2));
		assertFalse(s2.getBundles().contains(tb2));
		
		rootUpdate = root.newScopeUpdate();
		rootUpdate.getBundles().remove(tb2);
		s1Update = findChildUpdate("S1", rootUpdate);
		s1Update.getBundles().add(tb2);
		rootUpdate.commit();
		assertFalse(root.getBundles().contains(tb2));
		assertTrue(s1.getBundles().contains(tb2));
		assertFalse(s2.getBundles().contains(tb2));
		
		rootUpdate = root.newScopeUpdate();
		s1Update = findChildUpdate("S1", rootUpdate);
		s1Update.getBundles().remove(tb2);
		s2Update = findChildUpdate("S2", rootUpdate);
		s2Update.getBundles().add(tb2);
		rootUpdate.commit();
		assertFalse(root.getBundles().contains(tb2));
		assertFalse(s1.getBundles().contains(tb2));
		assertTrue(s2.getBundles().contains(tb2));
		
		tb2.uninstall();
	}
	
	/**
	 * Create one scope off of the root scope with the following structure. 
	 * R
	 * |
	 * S
	 * Install a bundle using the test bundle's bundle context. This should add 
	 * the bundle to R since the test bundle is in R. Next, move the bundle into
	 * S without removing it from R. This should result in an 
	 * IllegalStateException. Finally, correct the error using the same 
	 * ScopeUpdate objects and commit again. This should succeed, and the bundle
	 * should now be in S.
	 * @throws Exception
	 */
	@Test
	public void test2() throws Exception {
		Bundle tb2 = installBundle("tb-2.jar");
		Scope root = getScope();
		ScopeUpdate rootUpdate = root.newScopeUpdate();
		ScopeUpdate sUpdate = rootUpdate.newChild("S");
		rootUpdate.getChildren().add(sUpdate);
		rootUpdate.commit();
		Scope s = sUpdate.getScope();
		assertTrue(root.getBundles().contains(tb2));
		assertFalse(s.getBundles().contains(tb2));
		
		rootUpdate = root.newScopeUpdate();
		sUpdate = findChildUpdate("S", rootUpdate);
		sUpdate.getBundles().add(tb2);
		try {
			rootUpdate.commit();
			fail();
		}
		catch (IllegalStateException e) {
			// Okay.
		}
		assertTrue(root.getBundles().contains(tb2));
		assertFalse(s.getBundles().contains(tb2));
		
		rootUpdate.getBundles().remove(tb2);
		rootUpdate.commit();
		assertFalse(root.getBundles().contains(tb2));
		assertTrue(s.getBundles().contains(tb2));
		
		tb2.uninstall();
	}
}
