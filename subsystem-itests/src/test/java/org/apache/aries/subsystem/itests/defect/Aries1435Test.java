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
package org.apache.aries.subsystem.itests.defect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.aries.subsystem.itests.SubsystemTest;
import org.apache.aries.subsystem.itests.util.TestCapability;
import org.apache.aries.subsystem.itests.util.TestRepository;
import org.apache.aries.subsystem.itests.util.TestRepositoryContent;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph.FilteredRegion;
import org.eclipse.equinox.region.RegionFilter;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.tinybundles.core.InnerClassStrategy;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

/*
 * https://issues.apache.org/jira/browse/ARIES-1435
 * 
 * Sharing policy updates for dynamic imports, if necessary, should proceed up 
 * the subsystem region tree.
 */
public class Aries1435Test extends SubsystemTest {
	private static final String APPLICATION_A = "application.a.esa";
	private static final String APPLICATION_B = "application.b.esa";
	private static final String APPLICATION_C = "application.c.esa";
	private static final String APPLICATION_D = "application.d.esa";
	private static final String APPLICATION_E = "application.e.esa";
	private static final String BUNDLE_A = "bundle.a.jar";
	private static final String BUNDLE_B = "bundle.b.jar";
	private static final String COMPOSITE_A = "composite.a.esa";
	private static final String COMPOSITE_B = "composite.b.esa";
	private static final String COMPOSITE_C = "composite.c.esa";
	private static final String COMPOSITE_D = "composite.d.esa";
	private static final String COMPOSITE_E = "composite.e.esa";
	private static final String COMPOSITE_F = "composite.f.esa";
	private static final String FEATURE_A = "feature.a.esa";
	private static final String FEATURE_B = "feature.b.esa";
	private static final String FEATURE_C = "feature.c.esa";
	private static final String FEATURE_D = "feature.d.esa";
	
	private static boolean createdTestFiles;
	@Before
	public void createTestFiles() throws Exception {
		if (createdTestFiles)
			return;
		createApplicationB();
		createApplicationA();	
		createCompositeA();
		createCompositeB();
		createApplicationC();
		createFeatureA();
		createFeatureB();
		createApplicationD();
		createCompositeC();
		createFeatureC();
		createCompositeD();
		createFeatureD();
		createApplicationE();
		createCompositeE();
		createCompositeF();
		createdTestFiles = true;
	}
	
	private AtomicBoolean weavingHookCalled;
	@Override
    public void setUp() throws Exception {
        super.setUp();
        try {
            serviceRegistrations.add(
                    bundleContext.registerService(
                            Repository.class, 
                            createTestRepository(), 
                            null));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        weavingHookCalled = new AtomicBoolean();
    }
	
	@Test
    public void testApplicationWithParentApplication() throws Exception {
		testDynamicImport(APPLICATION_B, SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, APPLICATION_A);
    }
	
	@Test
    public void testApplicationWithParentComposite() throws Exception {
		testDynamicImport(APPLICATION_B, SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, COMPOSITE_A);
    }
	
	@Test
    public void testApplicationWithParentFeature() throws Exception {
		testDynamicImport(APPLICATION_B, SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, FEATURE_A);
    }
	
	@Test
    public void testApplicationWithParentRoot() throws Exception {
		testDynamicImport(APPLICATION_B, SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, null);
    }
	
	@Test
	public void testChildExportsPackage() throws Exception {
		registerWeavingHook("b");
		Subsystem applicationB = installSubsystemFromFile(APPLICATION_B);
		try {
			Subsystem compositeE = installSubsystemFromFile(applicationB, COMPOSITE_E);
			try {
				startSubsystem(compositeE);
				try {
					testSharingPolicy(applicationB, "b", true);
					testDynamicImport(applicationB, "b.B");
					testSharingPolicy(applicationB, "b", true);
					testSharingPolicy(compositeE, "b", false);
					testSharingPolicy(getRootSubsystem(), "b", false);
				}
				finally {
					stopSubsystemSilently(compositeE);
				}
			}
			finally {
				uninstallSubsystemSilently(compositeE);
			}
		}
		finally {
			uninstallSubsystemSilently(applicationB);
		}
	}
	
	@Test
    public void testCompositeWithParentApplication() throws Exception {
		testDynamicImport(COMPOSITE_B, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE, APPLICATION_C);
    }
	
	@Test
    public void testCompositeWithParentComposite() throws Exception {
		testDynamicImport(COMPOSITE_B, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE, COMPOSITE_C);
    }
	
	@Test
    public void testCompositeWithParentFeature() throws Exception {
		testDynamicImport(COMPOSITE_B, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE, FEATURE_C);
    }
	
	@Test
	public void testDisconnectedEdgeWithParent() throws Exception {
		registerWeavingHook("b");
		Bundle bundleB = getRootSubsystem().getBundleContext().installBundle(
				BUNDLE_B, new ByteArrayInputStream(createBundleBContent()));
		try {
			Subsystem applicationA = installSubsystemFromFile(APPLICATION_A);
			try {
				Subsystem applicationB = getChild(applicationA, APPLICATION_B);
				uninstallSubsystem(applicationB);
				removeConnectionWithParent(applicationA);
				applicationB = installSubsystemFromFile(applicationA, APPLICATION_B);
				try {
					try {
						testDynamicImport(applicationB, "b.B");
						fail("Dynamic import should have failed");
					}
					catch (AssertionError e) {
						// Okay.
					}
					testSharingPolicy(applicationB, "b", true);
					testSharingPolicy(applicationA, "b", false);
					testSharingPolicy(getRootSubsystem(), "b", false);
				}
				finally {
					uninstallSubsystemSilently(applicationB);
				}
			}
			finally {
				uninstallSubsystemSilently(applicationA);
			}
		}
		finally {
			bundleB.uninstall();
		}
	}
	
	@Test
    public void testFeatureWithParentApplication() throws Exception {
		testDynamicImport(FEATURE_B, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE, APPLICATION_D);
    }
	
	@Test
    public void testFeatureWithParentComposite() throws Exception {
		testDynamicImport(FEATURE_B, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE, COMPOSITE_D);
    }
	
	@Test
    public void testFeatureWithParentFeature() throws Exception {
		testDynamicImport(FEATURE_B, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE, FEATURE_D);
    }
	
	@Test
	public void testNoProviders() throws Exception {
		registerWeavingHook("b");
		Subsystem applicationB = installSubsystemFromFile(APPLICATION_B);
		try {
			Bundle bundleA = getConstituentAsBundle(applicationB, BUNDLE_A, null, null);
			bundleA.loadClass("a.A");
			try {
				bundleA.loadClass("b.B");
				fail("Class should not have loaded");
			}
			catch (ClassNotFoundException e) {
				// Okay.
			}
			testSharingPolicy(applicationB, "b", false);
		}
		finally {
			uninstallSubsystemSilently(applicationB);
		}
	}
	
	@Test
	public void testWildcardEverything() throws Exception {
		registerWeavingHook("b", "*");
		Subsystem compositeB = installSubsystemFromFile(COMPOSITE_B);
		try {
			Bundle bundleB = compositeB.getBundleContext().installBundle(
					BUNDLE_B, 
					new ByteArrayInputStream(createBundleBContent()));
			try {
				Subsystem applicationB = installSubsystemFromFile(compositeB, APPLICATION_B);
				Subsystem featureB = installSubsystemFromFile(applicationB, FEATURE_B);
				Subsystem applicationE = installSubsystemFromFile(featureB, APPLICATION_E);
				
				testSharingPolicy(applicationE, "b", false);
				testSharingPolicy(applicationE, "org.osgi.framework.Constants", false);
				testSharingPolicy(applicationB, "b", false);
				testSharingPolicy(applicationB, "org.osgi.framework.Constants", false);
				testSharingPolicy(compositeB, "b", false);
				testSharingPolicy(compositeB, "org.osgi.framework.Constants", false);
				testSharingPolicy(getRootSubsystem(), "b", false);
				testSharingPolicy(getRootSubsystem(), "org.osgi.framework.Constants", false);
				
				testDynamicImport(applicationE, "b.B");
				testDynamicImport(applicationE, "org.osgi.framework.Constants");
				testDynamicImport(featureB, "b.B");
				testDynamicImport(featureB, "org.osgi.framework.Constants");
				testDynamicImport(applicationB, "b.B");
				testDynamicImport(applicationB, "org.osgi.framework.Constants");
				testDynamicImport(compositeB, "b.B");
				testDynamicImport(compositeB, "org.osgi.framework.Constants");
				
				testSharingPolicy(applicationE, "b", true);
				testSharingPolicy(applicationE, "org.osgi.framework.Constants", true);
				testSharingPolicy(applicationB, "b", true);
				testSharingPolicy(applicationB, "org.osgi.framework.Constants", true);
				testSharingPolicy(compositeB, "b", true);
				testSharingPolicy(compositeB, "org.osgi.framework.Constants", true);
				testSharingPolicy(getRootSubsystem(), "b", false);
				testSharingPolicy(getRootSubsystem(), "org.osgi.framework.Constants", false);
				
				uninstallSubsystemSilently(applicationE);
				uninstallSubsystemSilently(featureB);
				uninstallSubsystemSilently(applicationB);
			}
			finally {
				bundleB.uninstall();
			}
		}
		finally {
			uninstallSubsystemSilently(compositeB);
		}
	}
	
	@Test
	public void testWildcardEverythingInPackage() throws Exception {
		registerWeavingHook("b.*");
		Subsystem applicationA = installSubsystemFromFile(APPLICATION_A);
		try {
			Bundle bundleB = applicationA.getBundleContext().installBundle(
					BUNDLE_B, 
					new ByteArrayInputStream(createBundleBContent()));
			try {
				Subsystem applicationB = getChild(applicationA, APPLICATION_B);
				
				testSharingPolicy(applicationB, "b", false);
				testSharingPolicy(applicationB, "b.a", false);
				testSharingPolicy(applicationA, "b", false);
				testSharingPolicy(applicationA, "b.a", false);
				testSharingPolicy(getRootSubsystem(), "b", false);
				testSharingPolicy(getRootSubsystem(), "b.a", false);
				
				testDynamicImport(applicationB, "b.a.A");
				
				testSharingPolicy(applicationB, "b", false);
				testSharingPolicy(applicationB, "b.a", true);
				testSharingPolicy(applicationA, "b", false);
				testSharingPolicy(applicationA, "b.a", true);
				testSharingPolicy(getRootSubsystem(), "b", false);
				testSharingPolicy(getRootSubsystem(), "b.a", false);
			}
			finally {
				bundleB.uninstall();
			}
		}
		finally {
			uninstallSubsystemSilently(applicationA);
		}
	}
	
	@Test
	public void testWovenSubsystemContainsProvider() throws Exception {
		registerWeavingHook("b");
		Subsystem applicationE = installSubsystemFromFile(APPLICATION_E);
		try {
			assertConstituent(applicationE, BUNDLE_B);
			testDynamicImport(applicationE, "b.B");
			testSharingPolicy(applicationE, "b", false);
		}
		finally {
			uninstallSubsystemSilently(applicationE);
		}
	}
	
	@Test
	public void testWovenSubsystemParentContainsProvider() throws Exception {
		registerWeavingHook("b");
		Subsystem compositeE = installSubsystemFromFile(COMPOSITE_E);
		try {
			assertNotConstituent(getRootSubsystem(), BUNDLE_B);
			assertConstituent(compositeE, BUNDLE_B);
			Subsystem applicationB = getChild(compositeE, APPLICATION_B);
			assertNotConstituent(applicationB, BUNDLE_B);
			testDynamicImport(applicationB, "b.B");
			testSharingPolicy(compositeE, "b", false);
			testSharingPolicy(applicationB, "b", true);
		}
		finally {
			uninstallSubsystemSilently(compositeE);
		}
	}
	
	@Test
	public void testWovenSubsystemParentPolicyAllowsProvider() throws Exception {
		registerWeavingHook("b");
		Subsystem root = getRootSubsystem();
		BundleContext context = root.getBundleContext();
		Bundle bundleB = context.installBundle(BUNDLE_B, new ByteArrayInputStream(createBundleBContent()));
		try {
			Subsystem applicationB1 = installSubsystemFromFile(root, new File(APPLICATION_B), APPLICATION_B + "1");
			try {
				Subsystem compositeF = installSubsystemFromFile(applicationB1, COMPOSITE_F);
				try {
					assertPackageFiltersInParentConnection(compositeF, applicationB1, 2, 1);
					Subsystem applicationB2 = installSubsystemFromFile(compositeF, new File(APPLICATION_B), APPLICATION_B + "2");
					try {
						testDynamicImport(applicationB2, "b.B");
						testSharingPolicy(applicationB2, "b", true);
						testSharingPolicy(compositeF, "b", true);
						assertPackageFiltersInParentConnection(compositeF, applicationB1, 2, 1);
						testSharingPolicy(applicationB1, "b", true);
						testSharingPolicy(root, "b", false);
					}
					finally {
						uninstallSubsystemSilently(applicationB2);
					}
				}
				finally {
					uninstallSubsystemSilently(compositeF);
				}
			}
			finally {
				uninstallSubsystemSilently(applicationB1);
			}
		}
		finally {
			uninstallSilently(bundleB);
		}
	}
	
	private void assertPackageFiltersInParentConnection(Subsystem subsystem, Subsystem parent, int expectedEdges, int expectedFilters) {
		Region parentRegion = getRegion(parent);
		Region region = getRegion(subsystem);
		Set<FilteredRegion> edges = region.getEdges();
		assertEquals("Wrong number of edges", expectedEdges, edges.size());
		for (FilteredRegion edge : region.getEdges()) {
			if (!edge.getRegion().equals(parentRegion)) {
				continue;
			}
			RegionFilter filter = edge.getFilter();
			Map<String, Collection<String>> policy = filter.getSharingPolicy();
			Collection<String> packages = policy.get(PackageNamespace.PACKAGE_NAMESPACE);
			assertNotNull("Wrong number of packages", packages);
			assertEquals("Wrong number of packages", expectedFilters, packages.size());
			return;
		}
		fail("No connection to parent found");
	}
	
	private void createApplicationA() throws IOException {
		createApplicationAManifest();
		createSubsystem(APPLICATION_A, APPLICATION_B);
	}
	
	private void createApplicationAManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_A);
		createManifest(APPLICATION_A + ".mf", attributes);
	}
	
	private void createApplicationB() throws IOException {
		createApplicationBManifest();
		createSubsystem(APPLICATION_B);
	}
	
	private void createApplicationBManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_B);
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, BUNDLE_A);
		createManifest(APPLICATION_B + ".mf", attributes);
	}
	
	private void createApplicationC() throws IOException {
		createApplicationCManifest();
		createSubsystem(APPLICATION_C, COMPOSITE_B);
	}
	
	private void createApplicationCManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_C);
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, COMPOSITE_B + ";type=osgi.subsystem.composite");
		createManifest(APPLICATION_C + ".mf", attributes);
	}
	
	private void createApplicationD() throws IOException {
		createApplicationDManifest();
		createSubsystem(APPLICATION_D, FEATURE_B);
	}
	
	private void createApplicationDManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_D);
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, FEATURE_B + ";type=osgi.subsystem.feature");
		createManifest(APPLICATION_D + ".mf", attributes);
	}
	
	private void createApplicationE() throws IOException {
		createApplicationEManifest();
		createSubsystem(APPLICATION_E);
	}
	
	private void createApplicationEManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_E);
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, BUNDLE_A + ',' + BUNDLE_B);
		createManifest(APPLICATION_E + ".mf", attributes);
	}
	
	private byte[] createBundleAContent() throws Exception {
        InputStream is = TinyBundles
        		.bundle()
        		.add(getClass().getClassLoader().loadClass("a.A"), InnerClassStrategy.NONE)
				.set(Constants.BUNDLE_SYMBOLICNAME, BUNDLE_A)
				.build(TinyBundles.withBnd());
        return createBundleContent(is);
    }
	
	private Resource createBundleAResource() throws Exception {
        return new TestRepositoryContent.Builder()
        .capability(
                new TestCapability.Builder()
                    .namespace(IdentityNamespace.IDENTITY_NAMESPACE)
                    .attribute(IdentityNamespace.IDENTITY_NAMESPACE, BUNDLE_A)
                    .attribute(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, IdentityNamespace.TYPE_BUNDLE)
                    .attribute(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, Version.emptyVersion))
        .content(createBundleAContent())
        .build();
    }
	
	private byte[] createBundleBContent() throws Exception {
        InputStream is = TinyBundles
        		.bundle()
        		.add(getClass().getClassLoader().loadClass("b.B"), InnerClassStrategy.NONE)
        		.add(getClass().getClassLoader().loadClass("b.a.A"), InnerClassStrategy.NONE)
				.set(Constants.BUNDLE_SYMBOLICNAME, BUNDLE_B)
				.set(Constants.EXPORT_PACKAGE, "b,b.a")
				.build(TinyBundles.withBnd());
        return createBundleContent(is);
    }
	
	private byte[] createBundleContent(InputStream is) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] bytes = new byte[1024];
        int length;
        while ((length = is.read(bytes)) != -1) {
        	baos.write(bytes, 0, length);
        }
        is.close();
        baos.close();
        return baos.toByteArray();
	}
	
	private Resource createBundleBResource() throws Exception {
        return new TestRepositoryContent.Builder()
        .capability(
                new TestCapability.Builder()
                    .namespace(IdentityNamespace.IDENTITY_NAMESPACE)
                    .attribute(IdentityNamespace.IDENTITY_NAMESPACE, BUNDLE_B)
                    .attribute(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, IdentityNamespace.TYPE_BUNDLE)
                    .attribute(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, Version.emptyVersion))
        .capability(
        		new TestCapability.Builder()
        			.namespace(PackageNamespace.PACKAGE_NAMESPACE)
        			.attribute(PackageNamespace.PACKAGE_NAMESPACE, "b")
        			.attribute(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, Version.emptyVersion))
        .capability(
        		new TestCapability.Builder()
        			.namespace(PackageNamespace.PACKAGE_NAMESPACE)
        			.attribute(PackageNamespace.PACKAGE_NAMESPACE, "b.a")
        			.attribute(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, Version.emptyVersion))
        .content(createBundleBContent())
        .build();
    }
	
	private void createCompositeA() throws IOException {
		createCompositeAManifest();
		createSubsystem(COMPOSITE_A, APPLICATION_B);
	}
	
	private void createCompositeAManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, COMPOSITE_A);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, APPLICATION_B + 
				";type=osgi.subsystem.application;version=\"[0,0]\"");
		createManifest(COMPOSITE_A + ".mf", attributes);
	}
	
	private void createCompositeB() throws IOException {
		createCompositeBManifest();
		createSubsystem(COMPOSITE_B);
	}
	
	private void createCompositeBManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, COMPOSITE_B);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, BUNDLE_A + ";version=\"[0,0]\"");
		createManifest(COMPOSITE_B + ".mf", attributes);
	}
	
	private void createCompositeC() throws IOException {
		createCompositeCManifest();
		createSubsystem(COMPOSITE_C, COMPOSITE_B);
	}
	
	private void createCompositeCManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, COMPOSITE_C);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, COMPOSITE_B + ";type=osgi.subsystem.composite;version=\"[0,0]\"");
		createManifest(COMPOSITE_C + ".mf", attributes);
	}
	
	private void createCompositeD() throws IOException {
		createCompositeDManifest();
		createSubsystem(COMPOSITE_D, FEATURE_B);
	}
	
	private void createCompositeDManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, COMPOSITE_D);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, FEATURE_B + ";type=osgi.subsystem.feature;version=\"[0,0]\"");
		createManifest(COMPOSITE_D + ".mf", attributes);
	}
	
	private void createCompositeE() throws IOException {
		createCompositeEManifest();
		createSubsystem(COMPOSITE_E, APPLICATION_B);
	}
	
	private void createCompositeEManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, COMPOSITE_E);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, APPLICATION_B + 
				";type=osgi.subsystem.application;version=\"[0,0]\"," +
				BUNDLE_B + ";version=\"[0,0]\"");
		attributes.put(Constants.EXPORT_PACKAGE, "b");
		createManifest(COMPOSITE_E + ".mf", attributes);
	}
	
	private void createCompositeF() throws IOException {
		createCompositeFManifest();
		createSubsystem(COMPOSITE_F);
	}
	
	private void createCompositeFManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, COMPOSITE_F);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		attributes.put(Constants.IMPORT_PACKAGE, "b;resolution:=optional");
		createManifest(COMPOSITE_F + ".mf", attributes);
	}
	
	private void createFeatureA() throws IOException {
		createFeatureAManifest();
		createSubsystem(FEATURE_A, APPLICATION_B);
	}
	
	private void createFeatureAManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, FEATURE_A);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
		createManifest(FEATURE_A + ".mf", attributes);
	}
	
	private void createFeatureB() throws IOException {
		createFeatureBManifest();
		createSubsystem(FEATURE_B);
	}
	
	private void createFeatureBManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, FEATURE_B);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, BUNDLE_A);
		createManifest(FEATURE_B + ".mf", attributes);
	}
	
	private void createFeatureC() throws IOException {
		createFeatureCManifest();
		createSubsystem(FEATURE_C, COMPOSITE_B);
	}
	
	private void createFeatureCManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, FEATURE_C);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
		createManifest(FEATURE_C + ".mf", attributes);
	}
	
	private void createFeatureD() throws IOException {
		createFeatureDManifest();
		createSubsystem(FEATURE_D, FEATURE_B);
	}
	
	private void createFeatureDManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, FEATURE_D);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
		createManifest(FEATURE_D + ".mf", attributes);
	}
	
	private Repository createTestRepository() throws Exception {
        return new TestRepository.Builder()
        .resource(createBundleAResource())
        .resource(createBundleBResource())
        .build();
    }
	
	private void registerWeavingHook(final String...dynamicImport) {
		serviceRegistrations.add(bundleContext.registerService(
    			WeavingHook.class, 
    			new WeavingHook() {
    				@Override
    				public void weave(WovenClass wovenClass) {
    					Bundle bundle = wovenClass.getBundleWiring().getBundle();
    					String symbolicName = bundle.getSymbolicName();
    					if (BUNDLE_A.equals(symbolicName)) {
    						weavingHookCalled.set(true);
    						List<String> dynamicImports = wovenClass.getDynamicImports();
    						dynamicImports.addAll(Arrays.asList(dynamicImport));
    					}
    				}
    			}, 
    			null));
	}
	
	private void testDynamicImport(String child, String type, String parent) throws Exception {
		testDynamicImport(child, type, parent, "org.osgi.framework");
	}
	
	private void testDynamicImport(String child, String type, String parent, String dynamicImport) throws Exception {
		registerWeavingHook(dynamicImport);
		Subsystem p = installSubsystemFromFile(parent == null ? child : parent);
		try {	
			if (parent != null) {
				assertChild(p, child, null, type);
				final Subsystem s = getConstituentAsSubsystem(p, child, null, type);
				testDynamicImport(s);
			}
			else {
				testDynamicImport(p);
			}
		}
		finally {
			uninstallSubsystemSilently(p);
		}
	}
	
	private void testDynamicImport(Subsystem subsystem) throws Exception {
		testDynamicImport(subsystem, "org.osgi.framework.Constants");
	}
	
	private void testDynamicImport(Subsystem subsystem, String clazz) throws Exception {
		assertConstituent(subsystem, BUNDLE_A);
		Bundle bundleA = getConstituentAsBundle(subsystem, BUNDLE_A, null, null);
		bundleA.loadClass("a.A");
		assertTrue("Weaving hook not called", weavingHookCalled.get());
		try {
			bundleA.loadClass(clazz);
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
			fail("Dynamic import not visible");
		}
	}
	
	private void testSharingPolicy(Subsystem subsystem, String dynamicImport, boolean allowed) {
		Region region = getRegion(subsystem);
		Set<FilteredRegion> filteredRegions = region.getEdges();
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(PackageNamespace.PACKAGE_NAMESPACE, dynamicImport);
		map.put(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, Version.emptyVersion);
		boolean wasAllowed = false;
		for (FilteredRegion filteredRegion : filteredRegions) {
			RegionFilter filter = filteredRegion.getFilter();
			if (allowed) {
				if (filter.isAllowed(PackageNamespace.PACKAGE_NAMESPACE, map)) {
					wasAllowed = true;
					break;
				}
			}
			else {
				assertFalse("Sharing policy should not have been updated", 
						filter.isAllowed(PackageNamespace.PACKAGE_NAMESPACE, map));
			}
		}
		if (allowed && !wasAllowed) {
			fail("Sharing policy should have been updated");
		}
	}
}
