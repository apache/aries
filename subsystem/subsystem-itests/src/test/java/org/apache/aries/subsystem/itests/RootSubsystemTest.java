/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.subsystem.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.eclipse.equinox.region.RegionFilter;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.Version;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

public class RootSubsystemTest extends SubsystemTest {
	/*
	 * Subsystem-SymbolicName: application.a.esa
	 * Subsystem-Content: bundle.a.jar
	 */
	private static final String APPLICATION_A = "application.a.esa";
	/*
	 * Bundle-SymbolicName: bundle.a.jar
	 * Import-Package: org.osgi.framework
	 */
	private static final String BUNDLE_A = "bundle.a.jar";
	
	@Override
	public void createApplications() throws Exception {
		createBundleA();
		createApplicationA();
	}
	
	private static void createApplicationA() throws IOException {
		createApplicationAManifest();
		createSubsystem(APPLICATION_A, BUNDLE_A);
	}
	
	private static void createApplicationAManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_A);
		createManifest(APPLICATION_A + ".mf", attributes);
	}
	
	private void createBundleA() throws IOException {
		createBundle(name(BUNDLE_A), importPackage("org.osgi.framework"));
	}
	
	// TODO Test root subsystem headers.
	
	@Test
	public void testDoNotStartExtraneousRootRegionBundles() throws Exception {
		bundleContext.installBundle(new File(BUNDLE_A).toURI().toURL().toString());
		getSubsystemCoreBundle().stop();
		getSubsystemCoreBundle().start();
		Bundle bundleA = context().getBundleByName(BUNDLE_A);
		assertEquals("Extraneous root region bundle should not be started", Bundle.INSTALLED, bundleA.getState());
	}
	
	@Test
	public void testId() {
		assertEquals("Wrong root ID", getRootSubsystem().getSubsystemId(), 0);
	}
	
	@Test
	public void testLocation() {
		assertEquals("Wrong root location", getRootSubsystem().getLocation(), "subsystem://?Subsystem-SymbolicName=org.osgi.service.subsystem.root&Subsystem-Version=1.0.0");
	}
	
	@Test
	public void testRegionContextBundle() throws BundleException {
		assertRegionContextBundle(getRootSubsystem());
		getSubsystemCoreBundle().stop();
		getSubsystemCoreBundle().start();
		assertRegionContextBundle(getRootSubsystem());
	}
	
	@Test
	public void testServiceEvents() throws Exception {
		Subsystem root = getRootSubsystem();
		Bundle core = getSubsystemCoreBundle();
		// TODO Temporary(?) workaround to allow time for any tardy service
		// events to arrive so they can be cleared. So far, this sleep has only
		// been necessary on the IBM 6.0 64-bit JDK.
		Thread.sleep(1000);
		subsystemEvents.clear();
		core.stop();
		assertServiceEventsStop(root);
		core.uninstall();
		core = bundleContext.installBundle(normalizeBundleLocation(core));
		core.start();
		// There should be install events since the persisted root subsystem was
		// deleted when the subsystems implementation bundle was uninstalled.
		assertServiceEventsInstall(root);
		assertServiceEventsResolve(root);
		assertServiceEventsStart(root);
		core.stop();
		assertServiceEventsStop(root);
		core.start();
		// There should be no install events or RESOLVING event since there
		// should be a persisted root subsystem already in the RESOLVED state.
		assertServiceEventResolved(root, ServiceEvent.REGISTERED);
		assertServiceEventsStart(root);
	}
	
	@Test
	public void testSymbolicName() {
		assertEquals("Wrong root symbolic name", getRootSubsystem().getSymbolicName(), "org.osgi.service.subsystem.root");
	}
	
	@Test
	public void testUninstallRootRegionBundleWithNoBundleEventHook() throws Exception {
		// Install an extraneous bundle into the root region. The bundle will
		// be recorded in the root subsystem's persistent memory.
		Bundle bundleA = bundleContext.installBundle(new File(BUNDLE_A).toURI().toURL().toString());
		try {
			Bundle core = getSubsystemCoreBundle();
			// Stop the subsystems bundle in order to unregister the bundle
			// event hook.
			core.stop();
			// Uninstall the bundle so it won't be there on restart.
			bundleA.uninstall();
			try {
				// Start the subsystems bundle and ensure the root subsystem
				// recovers from the uninstalled bundle being in persistent
				// memory.
				core.start();
			}
			catch (BundleException e) {
				fail("Could not start subsystems bundle after uninstalling a root region bundle with no bundle event hook registered");
			}
		}
		finally {
			if (Bundle.UNINSTALLED != bundleA.getState())
				bundleA.uninstall();
		}
	}
	
	@Test
	public void testVersion() {
		assertEquals("Wrong root version", getRootSubsystem().getVersion(), Version.parseVersion("1.0.0"));
	}
	
	/*
	 * The root subsystem should be associated with the region in which the
	 * subsystems implementation bundle is installed.
	 */
	@Test
	public void testRegion() throws Exception {
		RegionDigraph digraph = context().getService(RegionDigraph.class);
		Bundle core = getSubsystemCoreBundle();
		Region kernel = digraph.getRegion(core);
		Subsystem root = getRootSubsystem();
		Bundle rootRegionContext = root.getBundleContext().getBundle();
		// Get the region containing the subsystem's region context bundle, 
		// which is the same thing as getting the region with which the 
		// subsystem is associated.
		Region region = digraph.getRegion(root.getBundleContext().getBundle());
		assertEquals("Wrong region", kernel, region);
		// Uninstall the core bundle to remove the persisted root subsystem.
		core.uninstall();
		// Clean up the lingering region context bundle.
		rootRegionContext.uninstall();
		// Create a new region and install the core bundle into it.
		Region user = digraph.createRegion("user");
		// Allow everything from the kernel region into the user region so the 
		// core bundle will resolve.
		user.connectRegion(
				kernel, 
				digraph.createRegionFilterBuilder().allowAll(RegionFilter.VISIBLE_ALL_NAMESPACE).build());
		// Allow everything from the user region into the kernel region so the
		// root subsystem service can be found.
		kernel.connectRegion(
				user, 
				digraph.createRegionFilterBuilder().allowAll(RegionFilter.VISIBLE_ALL_NAMESPACE).build());
		core = user.installBundle(normalizeBundleLocation(core.getLocation()));
		user = digraph.getRegion(core);
		core.start();
		root = getRootSubsystem();
		region = digraph.getRegion(root.getBundleContext().getBundle());
		// The root subsystem should now be in the new region.
		assertEquals("Wrong region", user, region);
		// Extra test. Install application A into the root region (user) and 
		// make sure it resolves. Although the system bundle is in the kernel 
		// region and not a constituent of the root subsystem, the capability 
		// should still be found and used.
		try {
			Subsystem applicationA = installSubsystemFromFile(root, APPLICATION_A);
			uninstallSubsystemSilently(applicationA);
		}
		catch (Exception e) {
			e.printStackTrace();
			fail("Subsystem should have installed");
		}
	}
}
