/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.subsystem.itests;

import java.io.FileInputStream;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.MavenConfiguredJUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.service.subsystem.Subsystem;

/*
 * Contains a series of tests for unmanaged bundles. An unmanaged bundle is a
 * bundle that was installed outside of the Subsystems API.
 */
@RunWith(MavenConfiguredJUnit4TestRunner.class)
public class UnmanagedBundleTest extends SubsystemTest {
	/*
	 * Bundle-SymbolicName: bundle.a.jar
	 */
	private static final String BUNDLE_A = "bundle.a.jar";
	
	@Before
	public static void createApplications() throws Exception {
		if (createdApplications) {
			return;
		};
		createBundleA();
		createdApplications = true;
	}
	
	private static void createBundleA() throws IOException {
		createBundle(BUNDLE_A);
	}
	
	/*
	 * Test that an unmanaged bundle is detected as a constituent in the root
	 * subsystem when the subsystems core bundle is active.
	 */
	@Test
	public void testInstallWhileImplBundleActive() throws Exception {
		Bundle a = bundleContext.installBundle(BUNDLE_A, new FileInputStream(BUNDLE_A));
		try {
			assertConstituent(getRootSubsystemInState(Subsystem.State.ACTIVE, 5000L), BUNDLE_A);
		}
		finally {
			uninstallSilently(a);
		}
	}
	
	/*
	 * Test that an unmanaged bundle is detected as a constituent in the root
	 * subsystem when the subsystems core bundle is stopped. This ensures that
	 * persistence isn't interfering with detection.
	 */
	@Test
	public void testInstallWhileImplBundleStopped() throws Exception {
		Bundle core = getSubsystemCoreBundle();
		core.stop();
		try {
			Bundle a = bundleContext.installBundle(BUNDLE_A, new FileInputStream(BUNDLE_A));
			try {
				core.start();
				assertConstituent(getRootSubsystem(), BUNDLE_A);
			}
			finally {
				uninstallSilently(a);
			}
		}
		finally {
			core.start();
		}
	}
	
	/*
	 * Test that an unmanaged bundle is detected as a constituent in the root
	 * subsystem when the subsystems core bundle is uninstalled.
	 */
	@Test
	public void testInstallWhileImplBundleUninstalled() throws Exception {
		Bundle core = getSubsystemCoreBundle();
		core.uninstall();
		try {
			Bundle a = bundleContext.installBundle(BUNDLE_A, new FileInputStream(BUNDLE_A));
			try {
				core = bundleContext.installBundle(core.getLocation());
				core.start();
				assertConstituent(getRootSubsystem(), BUNDLE_A);
			}
			finally {
				uninstallSilently(a);
			}
		}
		finally {
			if (core.getState() == Bundle.UNINSTALLED) {
				core = bundleContext.installBundle(core.getLocation());
				core.start();
			}
		}
	}
	
	/*
	 * Test that bundles installed when the bundle event hook is unavailable
	 * (i.e. when the subsystems core bundle is stopped) are handled properly
	 * by the hook when uninstalled.
	 * 
	 * See https://issues.apache.org/jira/browse/ARIES-967.
	 */
	@Test
	public void testBundleEventHook() throws Exception {
		Bundle core = getSubsystemCoreBundle();
		// Stop the subsystems core bundle so the bundle event hook is not registered.
		core.stop();
		try {
			// Install an unmanaged bundle that will not be seen by the bundle event hook.
			Bundle a = bundleContext.installBundle(BUNDLE_A, new FileInputStream(BUNDLE_A));
			try {
				// Restart the subsystems core bundle.
				core.start();
				// Bundle A should be detected as a constituent of the root subsystem.
				assertConstituent(getRootSubsystem(), BUNDLE_A);
				// Uninstall bundle A so that it is seen by the bundle event hook.
				a.uninstall();
				// Bundle A should no longer be a constituent of the root subsystem.
				assertNotConstituent(getRootSubsystem(), BUNDLE_A);
			}
			finally {
				uninstallSilently(a);
			}
		}
		finally {
			try {
				core.start();
			}
			catch (Exception e) {}
		}
	}
}
