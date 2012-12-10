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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.MavenConfiguredJUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

/*
 * Contains a series of tests related to resolution.
 */
@RunWith(MavenConfiguredJUnit4TestRunner.class)
public class ResolutionTest extends SubsystemTest {
	/*
	 * Subsystem-SymbolicName: application.a.esa
	 * Subsystem-Content: bundle.a.jar
	 */
	private static final String APPLICATION_A = "application.a.esa";
	/*
	 * Bundle-SymbolicName: bundle.a.jar
	 * Require-Capability: a
	 */
	private static final String BUNDLE_A = "bundle.a.jar";
	/*
	 * Bundle-SymbolicName: bundle.b.jar
	 * Provide-Capability: a
	 * Require-Capability: b
	 */
	private static final String BUNDLE_B = "bundle.b.jar";
	/*
	 * Bundle-SymbolicName: bundle.c.jar
	 * Provide-Capability: b
	 */
	private static final String BUNDLE_C = "bundle.c.jar";
	
	@Before
	public static void createApplications() throws Exception {
		if (createdApplications) {
			return;
		};
		createBundleA();
		createBundleB();
		createBundleC();
		createApplicationA();
		createdApplications = true;
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
	
	private static void createBundleA() throws IOException {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.REQUIRE_CAPABILITY, "a");
		createBundle(BUNDLE_A, headers);
	}
	
	private static void createBundleB() throws IOException {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.PROVIDE_CAPABILITY, "a");
		headers.put(Constants.REQUIRE_CAPABILITY, "b");
		createBundle(BUNDLE_B, headers);
	}
	
	private static void createBundleC() throws IOException {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.PROVIDE_CAPABILITY, "b");
		createBundle(BUNDLE_C, headers);
	}
	
	/*
	 * Test that the right regions are used when validating capabilities.
	 * 
	 * Application A contains a content bundle requiring capability A. Bundle B
	 * provides capability A and is available as an installable resource from a
	 * repository service. Bundle B also requires capability B. Bundle C is an
	 * already installed resource in the root subsystem providing capability B.
	 * When validating capability A, the subsystem should use the root region as
	 * the from region, and its own region as the to region. When validating 
	 * capability B, the subsystem should use the root region as the from region
	 * as well as for the to region.
	 */
	@Test
	public void testContentWithNonConstituentDependencyWithNonConstituentDependency() throws Exception {
		// Register a repository service containing bundle B requiring
		// capability B and providing capability A.
		registerRepositoryService(BUNDLE_B);
		Subsystem root = getRootSubsystem();
		// Install unmanaged bundle C providing capability B as a constituent 
		// of the root subsystem.
		Bundle bundleC = installBundleFromFile(BUNDLE_C, root);
		try {
			// Install application A with content bundle A requiring
			// capability A.
			Subsystem applicationA = installSubsystemFromFile(APPLICATION_A);
			// Make sure the Require-Capability exists for capability a...
			assertHeaderExists(applicationA, Constants.REQUIRE_CAPABILITY);
			// ...but not for capability b.
			assertEquals("Wrong Require-Capability header", "a;resolution:=mandatory;effective:=resolve", applicationA.getSubsystemHeaders(null).get(Constants.REQUIRE_CAPABILITY));
			try {
				// Make sure the runtime resolution works as well.
				applicationA.start();
			}
			catch (SubsystemException e) {
				fail("Application A should have started");
			}
			finally {
				stopAndUninstallSubsystemSilently(applicationA);
			}
		}
		catch (SubsystemException e) {
			fail("Application A should have installed");
		}
		finally {
			uninstallSilently(bundleC);
		}
	}
}
