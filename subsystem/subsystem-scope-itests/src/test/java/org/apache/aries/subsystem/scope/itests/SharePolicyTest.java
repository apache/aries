package org.apache.aries.subsystem.scope.itests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Arrays;

import org.apache.aries.subsystem.scope.InstallInfo;
import org.apache.aries.subsystem.scope.ScopeUpdate;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.FrameworkWiring;

public class SharePolicyTest extends AbstractTest {
	/**
	 * Bundle tb5
	 * Bundle tb6
	 * tb5 imports package exported by tb6
	 * tb5 and tb6 in same scope
	 * tb5 should resolve
	 * 
	 * Share policies have no effect within the same scope.
	 * 
	 * @throws Exception
	 */
	@Test
	public void test1() throws Exception {
		Bundle tb5 = null;
		Bundle tb6 = null;
		try {
			String tb5Location = getBundleLocation("tb-5.jar");
			String tb6Location = getBundleLocation("tb-6.jar");
			InstallInfo tb5Info = new InstallInfo(tb5Location, new URL(tb5Location));
			InstallInfo tb6Info = new InstallInfo(tb6Location, new URL(tb6Location));
			ScopeUpdate scopeUpdate = getScope().newScopeUpdate();
			scopeUpdate.getBundlesToInstall().add(tb5Info);
			scopeUpdate.commit();
			tb5 = findBundleInRootScope("org.apache.aries.subsystem.scope.itests.tb5");
			assertNotNull(tb5);
			FrameworkWiring frameworkWiring = bundleContext.getBundle(0).adapt(FrameworkWiring.class);
			assertFalse(frameworkWiring.resolveBundles(Arrays.asList(new Bundle[]{tb5})));
			scopeUpdate = getScope().newScopeUpdate();
			scopeUpdate.getBundlesToInstall().add(tb6Info);
			scopeUpdate.commit();
			tb6 = findBundleInRootScope("org.apache.aries.subsystem.scope.itests.tb6");
			assertNotNull(tb6);
			assertTrue(frameworkWiring.resolveBundles(Arrays.asList(new Bundle[]{tb5,tb6})));
		}
		finally {
			uninstallQuietly(tb6);
			uninstallQuietly(tb5);
		}
	}
	
	/**
	 * Bundle tb5
	 * Bundle tb6
	 * tb5 imports package exported by tb6
	 * tb5 in root scope
	 * tb6 in child scope of root
	 * tb6 scope does not export tb6 package
	 * tb5 should not resolve
	 * @throws Exception
	 */
	@Test
	public void test2() throws Exception {
		Bundle tb5 = null;
		Bundle tb6 = null;
		try {
			String tb5Location = getBundleLocation("tb-5.jar");
			String tb6Location = getBundleLocation("tb-6.jar");
			InstallInfo tb5Info = new InstallInfo(tb5Location, new URL(tb5Location));
			InstallInfo tb6Info = new InstallInfo(tb6Location, new URL(tb6Location));
			ScopeUpdate scopeUpdate = getScope().newScopeUpdate();
			scopeUpdate.getBundlesToInstall().add(tb5Info);
			scopeUpdate.commit();
			tb5 = findBundleInRootScope("org.apache.aries.subsystem.scope.itests.tb5");
			assertNotNull(tb5);
			FrameworkWiring frameworkWiring = bundleContext.getBundle(0).adapt(FrameworkWiring.class);
			assertFalse(frameworkWiring.resolveBundles(Arrays.asList(new Bundle[]{tb5})));
			scopeUpdate = getScope().newScopeUpdate();
			ScopeUpdate tb6ScopeUpdate = scopeUpdate.newChild("tb6");
			scopeUpdate.getChildren().add(tb6ScopeUpdate);
			tb6ScopeUpdate.getBundlesToInstall().add(tb6Info);
			scopeUpdate.commit();
			tb6 = findBundle("org.apache.aries.subsystem.scope.itests.tb6", tb6ScopeUpdate.getScope());
			assertNotNull(tb6);
			assertFalse(frameworkWiring.resolveBundles(Arrays.asList(new Bundle[]{tb5,tb6})));
		}
		finally {
			uninstallQuietly(tb6);
			uninstallQuietly(tb5);
		}
	}
	
	/**
	 * Bundle tb5
	 * Bundle tb6
	 * tb5 imports package exported by tb6
	 * tb5 in root scope
	 * tb6 in child scope of root
	 * tb6 scope exports tb6 package
	 * tb5 should resolve
	 * 
	 * There is an implicit import between parent and child. In other words,
	 * anything exported by a child is automatically available without the
	 * parent explicitly importing it.
	 * 
	 * @throws Exception
	 */
	@Test
	public void test3() throws Exception {
		Bundle tb5 = null;
		Bundle tb6 = null;
		try {
			String tb5Location = getBundleLocation("tb-5.jar");
			String tb6Location = getBundleLocation("tb-6.jar");
			InstallInfo tb5Info = new InstallInfo(tb5Location, new URL(tb5Location));
			InstallInfo tb6Info = new InstallInfo(tb6Location, new URL(tb6Location));
			ScopeUpdate scopeUpdate = getScope().newScopeUpdate();
			scopeUpdate.getBundlesToInstall().add(tb5Info);
			scopeUpdate.commit();
			tb5 = findBundleInRootScope("org.apache.aries.subsystem.scope.itests.tb5");
			assertNotNull(tb5);
			FrameworkWiring frameworkWiring = bundleContext.getBundle(0).adapt(FrameworkWiring.class);
			assertFalse(frameworkWiring.resolveBundles(Arrays.asList(new Bundle[]{tb5})));
			scopeUpdate = getScope().newScopeUpdate();
			ScopeUpdate tb6ScopeUpdate = scopeUpdate.newChild("tb6");
			scopeUpdate.getChildren().add(tb6ScopeUpdate);
			tb6ScopeUpdate.getBundlesToInstall().add(tb6Info);
			addPackageExportPolicy("org.apache.aries.subsystem.scope.itests.tb6", tb6ScopeUpdate);
			scopeUpdate.commit();
			tb6 = findBundle("org.apache.aries.subsystem.scope.itests.tb6", tb6ScopeUpdate.getScope());
			assertNotNull(tb6);
			tb5.start();
			assertTrue(frameworkWiring.resolveBundles(Arrays.asList(new Bundle[]{tb5,tb6})));
		}
		finally {
			uninstallQuietly(tb6);
			uninstallQuietly(tb5);
		}
	}
	
	/**
	 * Bundle tb5
	 * Bundle tb6
	 * tb5 imports package exported by tb6
	 * tb5 in child scope of root
	 * tb6 in different child scope of root
	 * tb6 scope exports tb6 package
	 * root scope exports tb6 package
	 * tb5 scope imports tb6 package
	 * tb5 should resolve
	 * 
	 * @throws Exception
	 */
	@Test
	public void test4() throws Exception {
		Bundle tb5 = null;
		Bundle tb6 = null;
		try {
			String tb5Location = getBundleLocation("tb-5.jar");
			String tb6Location = getBundleLocation("tb-6.jar");
			InstallInfo tb5Info = new InstallInfo(tb5Location, new URL(tb5Location));
			InstallInfo tb6Info = new InstallInfo(tb6Location, new URL(tb6Location));
			ScopeUpdate rootUpdate = getScope().newScopeUpdate();
			addPackageExportPolicy("org.apache.aries.subsystem.scope.itests.tb6", rootUpdate);
			ScopeUpdate tb5Update = rootUpdate.newChild("tb5");
			rootUpdate.getChildren().add(tb5Update);
			tb5Update.getBundlesToInstall().add(tb5Info);
			addPackageImportPolicy("org.apache.aries.subsystem.scope.itests.tb6", tb5Update);
			rootUpdate.commit();
			tb5 = findBundle("org.apache.aries.subsystem.scope.itests.tb5", tb5Update.getScope());
			assertNotNull(tb5);
			FrameworkWiring frameworkWiring = bundleContext.getBundle(0).adapt(FrameworkWiring.class);
			assertFalse(frameworkWiring.resolveBundles(Arrays.asList(new Bundle[]{tb5})));
			rootUpdate = getScope().newScopeUpdate();
			ScopeUpdate tb6Update = rootUpdate.newChild("tb6");
			rootUpdate.getChildren().add(tb6Update);
			tb6Update.getBundlesToInstall().add(tb6Info);
			addPackageExportPolicy("org.apache.aries.subsystem.scope.itests.tb6", tb6Update);
			rootUpdate.commit();
			tb6 = findBundle("org.apache.aries.subsystem.scope.itests.tb6", tb6Update.getScope());
			assertNotNull(tb6);
			assertTrue(frameworkWiring.resolveBundles(Arrays.asList(new Bundle[]{tb5,tb6})));
		}
		finally {
			uninstallQuietly(tb6);
			uninstallQuietly(tb5);
		}
	}
}
