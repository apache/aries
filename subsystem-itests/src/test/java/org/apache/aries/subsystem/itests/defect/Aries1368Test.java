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
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.aries.subsystem.itests.Header;
import org.apache.aries.subsystem.itests.SubsystemTest;
import org.apache.aries.subsystem.itests.util.TestCapability;
import org.apache.aries.subsystem.itests.util.TestRepository;
import org.apache.aries.subsystem.itests.util.TestRepositoryContent;
import org.apache.aries.subsystem.itests.util.TestRequirement;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

/*
 * https://issues.apache.org/jira/browse/ARIES-1368
 */
public class Aries1368Test extends SubsystemTest {
    /*
     * Subsystem-SymbolicName: application.a.esa
     * Subsystem-Content: bundle.a.jar;type=osgi.bundle, fragment.a.jar;type=osgi.fragment
     * 
     * Included in archive:
     *      bundle.a.jar
     *      fragment.a.jar
     */
    private static final String APPLICATION_A = "application.a.esa";
    /*
     * Subsystem-SymbolicName: application.b.esa
     * Subsystem-Content: bundle.a.jar;type=osgi.bundle, fragment.a.jar;type=osgi.fragment
     * 
     * Included in archive:
     *      bundle.a.jar
     * 
     * Included in remote repository:
     *      fragment.a.jar
     */
    private static final String APPLICATION_B = "application.b.esa";
	/*
	 * Bundle-SymbolicName: bundle.a.jar
	 */
	private static final String BUNDLE_A = "bundle.a.jar";
	/*
	 * Bundle-SymbolicName: fragment.a.jar
	 * Fragment-Host: bundle.a.jar
	 */
	private static final String FRAGMENT_A = "fragment.a.jar";
	
	private static boolean createdTestFiles;
	
	@Before
	public void createTestFiles() throws Exception {
		if (createdTestFiles)
			return;
		createBundleA();
		createFragmentA();
		createApplicationA();
		createApplicationB();
		createdTestFiles = true;
	}
	
	private void createBundleA() throws IOException {
		createBundle(name(BUNDLE_A));
	}
	
	private void createFragmentA() throws IOException {
		createBundle(name(FRAGMENT_A), new Header(Constants.FRAGMENT_HOST, BUNDLE_A));
	}
	
	private void createApplicationA() throws IOException {
        createApplicationAManifest();
        createSubsystem(APPLICATION_A, BUNDLE_A, FRAGMENT_A);
    }
    
    private void createApplicationAManifest() throws IOException {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_A);
        attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, BUNDLE_A + ";type=osgi.bundle," + FRAGMENT_A + ";type=osgi.fragment");
        createManifest(APPLICATION_A + ".mf", attributes);
    }
    
    private void createApplicationB() throws IOException {
        createApplicationBManifest();
        createSubsystem(APPLICATION_B, BUNDLE_A);
    }
    
    private void createApplicationBManifest() throws IOException {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_B);
        attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, BUNDLE_A + ";type=osgi.bundle," + FRAGMENT_A + ";type=osgi.fragment");
        createManifest(APPLICATION_B + ".mf", attributes);
    }
    
    @Test
    public void testApplicationWithFragmentInArchiveWithSubsystemContentHeaderWithType() throws Exception {
        Subsystem applicationA = installSubsystemFromFile(APPLICATION_A);
        try {
            assertConstituents(3, applicationA);
            startSubsystem(applicationA);
            assertBundleState(Bundle.ACTIVE, BUNDLE_A, applicationA);
            assertBundleState(Bundle.RESOLVED, FRAGMENT_A, applicationA);
            Bundle bundle = context(applicationA).getBundleByName(FRAGMENT_A);
            assertNotNull("Bundle not found: " + FRAGMENT_A, bundle);
            Resource resource = bundle.adapt(BundleRevision.class);
            Capability capability = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).get(0);
            assertEquals(
                    "Wrong type", 
                    IdentityNamespace.TYPE_FRAGMENT, 
                    capability.getAttributes().get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE));
        }
        finally {
            stopAndUninstallSubsystemSilently(applicationA);
        }
    }
    
    @Test
    public void testApplicationWithFragmentInRepositoryWithSubsystemContentHeaderWithType() throws Exception {
        Subsystem applicationB = installSubsystemFromFile(APPLICATION_B);
        try {
            assertConstituents(3, applicationB);
            startSubsystem(applicationB);
            assertBundleState(Bundle.ACTIVE, BUNDLE_A, applicationB);
            assertBundleState(Bundle.RESOLVED, FRAGMENT_A, applicationB);
            Bundle bundle = context(applicationB).getBundleByName(FRAGMENT_A);
            assertNotNull("Bundle not found: " + FRAGMENT_A, bundle);
            Resource resource = bundle.adapt(BundleRevision.class);
            Capability capability = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).get(0);
            String type = String.valueOf(capability.getAttributes().get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE));
            assertEquals("Wrong type", IdentityNamespace.TYPE_FRAGMENT, type);
        }
        finally {
            stopAndUninstallSubsystemSilently(applicationB);
        }
    }
    
    private Repository createTestRepository() throws IOException {
        return new TestRepository.Builder()
        .resource(createTestBundleFragmentResource())
        .build();
    }
    
    private byte[] createTestBundleFragmentContent() throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue(Constants.BUNDLE_SYMBOLICNAME, FRAGMENT_A);
        manifest.getMainAttributes().putValue(Constants.FRAGMENT_HOST, BUNDLE_A);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JarOutputStream jos = new JarOutputStream(baos, manifest);
        jos.close();
        return baos.toByteArray();
    }
    
    private Resource createTestBundleFragmentResource() throws IOException {
        return new TestRepositoryContent.Builder()
        .capability(
                new TestCapability.Builder()
                    .namespace(IdentityNamespace.IDENTITY_NAMESPACE)
                    .attribute(IdentityNamespace.IDENTITY_NAMESPACE, FRAGMENT_A)
                    .attribute(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, IdentityNamespace.TYPE_FRAGMENT)
                    .attribute(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, Version.emptyVersion))
        .requirement(
                new TestRequirement.Builder()
                    .namespace(HostNamespace.HOST_NAMESPACE)
                    .attribute(HostNamespace.HOST_NAMESPACE, BUNDLE_A))
        .content(createTestBundleFragmentContent())
        .build();
    }
    
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
    }
}
