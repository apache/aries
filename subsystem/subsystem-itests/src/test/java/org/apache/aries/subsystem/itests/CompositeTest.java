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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

public class CompositeTest extends SubsystemTest {
	private static final String BUNDLE_A = "bundle.a";
	private static final String BUNDLE_B = "bundle.b";
	private static final String BUNDLE_C = "bundle.c";
	private static final String BUNDLE_D = "bundle.d";
	private static final String BUNDLE_E = "bundle.e";
	private static final String COMPOSITE_A = "composite.a";
	private static final String COMPOSITE_B = "composite.b";
	private static final String COMPOSITE_C = "composite.c";
	private static final String COMPOSITE_D = "composite.d";
	private static final String PACKAGE_X = "x";
	
	@Override
	public void createApplications() throws Exception {
		createBundleA();
		createBundleB();
		createBundleC();
		createBundleD();
		createBundleE();
		createCompositeA();
		createCompositeB();
		createCompositeC();
		createCompositeD();
	}
	
	private void createBundleA() throws IOException {
		createBundle(name(BUNDLE_A), version("1.0.0"), exportPackage(PACKAGE_X + ";version=1.0"));
	}
	
	private void createBundleB() throws IOException {
		createBundle(name(BUNDLE_B), version("1.0.0"), 
				new Header(Constants.PROVIDE_CAPABILITY, "y; y=test; version:Version=1.0"));
	}
	
	private void createBundleC() throws IOException {
		createBundle(name(BUNDLE_C), version("1.0.0"), importPackage(PACKAGE_X + ";version=\"[1.0,2.0)\""));
	}
	
	private void createBundleD() throws IOException {
		createBundle(name(BUNDLE_D), requireBundle(BUNDLE_A));
	}
	
	private void createBundleE() throws IOException {
		createBundle(name(BUNDLE_E), new Header(Constants.REQUIRE_CAPABILITY, "y; filter:=(y=test)"));
	}
	
	private static void createCompositeA() throws IOException {
		createCompositeAManifest();
		createSubsystem(COMPOSITE_A);
	}
	
	private static void createCompositeB() throws IOException {
		createCompositeBManifest();
		createSubsystem(COMPOSITE_B);
	}
	
	private static void createCompositeC() throws IOException {
		createCompositeCManifest();
		createSubsystem(COMPOSITE_C);
	}
	
	private static void createCompositeD() throws IOException {
		createCompositeDManifest();
		createSubsystem(COMPOSITE_D);
	}
	
	private static void createCompositeAManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, COMPOSITE_A);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		attributes.put(Constants.EXPORT_PACKAGE, PACKAGE_X + "; version=1.0, does.not.exist; a=b");
		createManifest(COMPOSITE_A + ".mf", attributes);
	}
	
	private static void createCompositeBManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, COMPOSITE_B);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		attributes.put(Constants.REQUIRE_BUNDLE, BUNDLE_A + "; bundle-version=\"[1.0, 2.0)\", does.not.exist; bundle-version=\"[1.0, 2.0)\"");
		createManifest(COMPOSITE_B + ".mf", attributes);
	}
	
	private static void createCompositeCManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, COMPOSITE_C);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		attributes.put(Constants.IMPORT_PACKAGE, PACKAGE_X + ", does.not.exist; a=b");
		createManifest(COMPOSITE_C + ".mf", attributes);
	}
	
	private static void createCompositeDManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, COMPOSITE_D);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		attributes.put(Constants.REQUIRE_CAPABILITY, "y; filter:=\"(y=test)\", does.not.exist; filter:=\"(a=b)\"");
		createManifest(COMPOSITE_D + ".mf", attributes);
	}
	
	@Test
	public void testExportPackage() throws Exception {
		Subsystem composite = installSubsystemFromFile(COMPOSITE_A);
		try {
			startSubsystem(composite);
			Bundle bundleA = installBundleFromFile(BUNDLE_A, composite);
			try {
				Bundle bundleC = installBundleFromFile(BUNDLE_C);
				try {
					startBundle(bundleC);
				}
				finally {
					bundleC.uninstall();
				}
			}
			finally {
				bundleA.uninstall();
			}
		}
		finally {
			stopSubsystemSilently(composite);
			uninstallSubsystemSilently(composite);
		}
	}
	
	@Test
	public void testImportPackage() throws Exception {
		Bundle bundleA = installBundleFromFile(BUNDLE_A);
		try {
			Subsystem compositeC = installSubsystemFromFile(COMPOSITE_C);
			try {
				Bundle bundleC = installBundleFromFile(BUNDLE_C, compositeC);
				try {
					startBundle(bundleC, compositeC);
				}
				finally {
					bundleC.uninstall();
				}
			}
			finally {
				uninstallSubsystemSilently(compositeC);
			}
		}
		finally {
			bundleA.uninstall();
		}
	}
	
	@Test
	public void testRequireBundle() throws Exception {
		Bundle bundleA = installBundleFromFile(BUNDLE_A);
		try {
			Subsystem compositeB = installSubsystemFromFile(COMPOSITE_B);
			try {
				Bundle bundleD = installBundleFromFile(BUNDLE_D, compositeB);
				try {
					startBundle(bundleD, compositeB);
				}
				finally {
					bundleD.uninstall();
				}
			}
			finally {
				uninstallSubsystemSilently(compositeB);
			}
		}
		finally {
			bundleA.uninstall();
		}
	}
	
	@Test
	public void testRequireCapability() throws Exception {
		Bundle bundleB = installBundleFromFile(BUNDLE_B);
		try {
			Subsystem compositeD = installSubsystemFromFile(COMPOSITE_D);
			try {
				Bundle bundleE = installBundleFromFile(BUNDLE_E, compositeD);
				try {
					startBundle(bundleE, compositeD);
				}
				finally {
					bundleE.uninstall();
				}
			}
			finally {
				uninstallSubsystemSilently(compositeD);
			}
		}
		finally {
			bundleB.uninstall();
		}
	}
}
