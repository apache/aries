package org.apache.aries.subsystem.scope.itests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.apache.aries.subsystem.scope.InstallInfo;
import org.apache.aries.subsystem.scope.ScopeUpdate;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

/**
 * Bundles may only see other services registered by other bundles within the 
 * same scope. The one exception is the system bundle, whose services may be 
 * seen by all bundles regardless of scope.
 */
public class ServiceVisibilityTest extends AbstractTest {
	/**
	 * Install a bundle registering a service into the same scope as this one. 
	 * This bundle should be able to see the service.
	 * @throws Exception
	 */
	@Test
	public void test1() throws Exception {
		assertTrue(getScope().getBundles().contains(bundleContext.getBundle()));
		ScopeUpdate scopeUpdate = getScope().newScopeUpdate();
		String location = getBundleLocation("tb-7.jar");
		assertNull(bundleContext.getBundle(location));
		URL url = new URL(location);
		InstallInfo installInfo = new InstallInfo(location, url.openStream());
		scopeUpdate.getBundlesToInstall().add(installInfo);
		scopeUpdate.commit();
		Bundle bundle = bundleContext.getBundle(location);
		assertNotNull(bundle);
		assertTrue(getScope().getBundles().contains(bundle));
		bundle.start();
		ServiceReference<Service> serviceRef = bundleContext.getServiceReference(Service.class);
		assertNotNull(serviceRef);
		Service service = bundleContext.getService(serviceRef);
		assertNotNull(service);
		bundleContext.ungetService(serviceRef);
		bundle.uninstall();
	}
	
	/**
	 * Install a bundle registering a service into a different scope than this 
	 * one. This bundle should not be able to see the service.
	 * @throws Exception
	 */
	@Test
	public void test2() throws Exception {
		assertTrue(getScope().getBundles().contains(bundleContext.getBundle()));
		String location = getBundleLocation("tb-7.jar");
		assertNull(bundleContext.getBundle(location));
		URL url = new URL(location);
		InstallInfo installInfo = new InstallInfo(location, url.openStream());
		ScopeUpdate scopeUpdate = getScope().newScopeUpdate();
		ScopeUpdate child = scopeUpdate.newChild("tb7");
		scopeUpdate.getChildren().add(child);
		child.getBundlesToInstall().add(installInfo);
		addPackageImportPolicy("org.osgi.framework", child);
		addPackageImportPolicy("org.apache.aries.subsystem.scope.itests", child);
		scopeUpdate.commit();
		Bundle bundle = bundleContext.getBundle(location);
		assertNotNull(bundle);
		assertTrue(child.getScope().getBundles().contains(bundle));
		bundle.start();
		ServiceReference<Service> serviceRef = bundleContext.getServiceReference(Service.class);
		assertNull(serviceRef);
		bundle.uninstall();
	}
}
