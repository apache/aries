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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.aries.subsystem.itests.util.TestCapability;
import org.apache.aries.subsystem.itests.util.TestRepository;
import org.apache.aries.subsystem.itests.util.TestRepositoryContent;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

public class ApplicationTest extends SubsystemTest {
	/*
	 * Subsystem-SymbolicName: application.a.esa
	 * Subsystem-Content: bundle.a.jar
	 */
	private static final String APPLICATION_A = "application.a.esa";
	/*
	 * Subsystem-SymbolicName: application.b.esa
	 * Subsystem-Content: bundle.c.jar
	 */
	private static final String APPLICATION_B = "application.b.esa";
	/*
	 * Bundle-SymbolicName: bundle.a.jar
	 * Require-Capability: foo; filter:="(foo=bar)"
	 */
	private static final String BUNDLE_A = "bundle.a.jar";
	/*
	 * Bundle-SymbolicName: bundle.b.jar
	 * Provide-Capability: foo; foo=bar
	 */
	private static final String BUNDLE_B = "bundle.b.jar";
	/*
	 * Bundle-SymbolicName: bundle.c.jar
	 * Require-Bundle: bundle.b.jar
	 */
	private static final String BUNDLE_C = "bundle.c.jar";
	
	private static boolean createdTestFiles;
	
	@Before
	public void createTestFiles() throws Exception {
		if (createdTestFiles)
			return;
		createBundleA();
		createBundleB();
		createBundleC();
		createApplicationA();
		createApplicationB();
		createdTestFiles = true;
	}
	
	private void createBundleA() throws IOException {
		createBundle(name(BUNDLE_A), version("1.0.0"),
				new Header(Constants.REQUIRE_CAPABILITY, "foo; filter:=\"(foo=bar)\""));
	}
	
	private void createBundleB() throws IOException {
		createBundle(name(BUNDLE_B), version("1.0.0"), 
				new Header(Constants.PROVIDE_CAPABILITY, "foo; foo=bar"));
	}
	
	private void createBundleC() throws IOException {
		createBundle(name(BUNDLE_C), version("1.0.0"), requireBundle(BUNDLE_B));
	}
	
	private void createApplicationA() throws IOException {
		createApplicationAManifest();
		createSubsystem(APPLICATION_A, BUNDLE_A);
	}
	
	private void createApplicationB() throws IOException {
		createApplicationBManifest();
		createSubsystem(APPLICATION_B, BUNDLE_C);
	}
	
	private void createApplicationAManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_A);
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, BUNDLE_A);
		createManifest(APPLICATION_A + ".mf", attributes);
	}
	
	private void createApplicationBManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_B);
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, BUNDLE_C);
		createManifest(APPLICATION_B + ".mf", attributes);
	}
	
	@Override
	public void createApplications() throws Exception {
		createApplication("application1", "tb1.jar");
	}
	
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
    
	/*
	 * Subsystem application1 has content bundle tb1.jar.
	 * Bundle tb1.jar has an import package dependency on org.apache.aries.subsystem.itests.tb3.
	 */
    @Test
    public void testApplication1() throws Exception {
    	Subsystem application1 = installSubsystemFromFile("application1.esa");
    	try {
	    	assertSymbolicName("org.apache.aries.subsystem.application1", application1);
			assertVersion("0.0.0", application1);
			assertType(SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, application1);
			assertChildren(0, application1);
			assertConstituents(2, application1);
			startSubsystem(application1);
			assertBundleState(Bundle.RESOLVED|Bundle.ACTIVE, "org.apache.aries.subsystem.itests.tb1", application1);
			assertBundleState(Bundle.RESOLVED|Bundle.ACTIVE, "org.apache.aries.subsystem.itests.tb3", getRootSubsystem());
    	}
    	finally {
    		stopSubsystemSilently(application1);
    		uninstallSubsystemSilently(application1);
    	}
    }
    
    @Test
    public void testRequireBundle() throws Exception {
    	File file = new File(BUNDLE_B);
    	// The following input stream is closed by the bundle context.
    	Bundle b = getRootSubsystem().getBundleContext().installBundle(file.toURI().toString(), new FileInputStream(file));
    	try {
	    	Subsystem application = installSubsystemFromFile(APPLICATION_B);
	    	try {
	    		startSubsystem(application);
	    	}
	    	finally {
	    		stopSubsystemSilently(application);
	    		uninstallSubsystemSilently(application);
	    	}
    	}
    	finally {
    		uninstallSilently(b);
    	}
    }
    
    @Test
    public void testRequireCapability() throws Exception {
    	File file = new File(BUNDLE_B);
    	// The following input stream is closed by the bundle context.
    	Bundle b = getRootSubsystem().getBundleContext().installBundle(file.toURI().toString(), new FileInputStream(file));
    	try {
	    	Subsystem application = installSubsystemFromFile(APPLICATION_A);
	    	try {
	    		startSubsystem(application);
	    	}
	    	finally {
	    		stopSubsystemSilently(application);
	    		uninstallSubsystemSilently(application);
	    	}
    	}
    	finally {
    		uninstallSilently(b);
    	}
    }
    
    private byte[] createTestBundle3Content() throws IOException {
    	Manifest manifest = new Manifest();
    	manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    	manifest.getMainAttributes().putValue(Constants.BUNDLE_SYMBOLICNAME, "org.apache.aries.subsystem.itests.tb3");
    	manifest.getMainAttributes().putValue(Constants.EXPORT_PACKAGE, "org.apache.aries.subsystem.itests.tb3");
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	JarOutputStream jos = new JarOutputStream(baos, manifest);
    	jos.close();
    	return baos.toByteArray();
    }
    
    private Resource createTestBundle3Resource() throws IOException {
    	return new TestRepositoryContent.Builder()
    	.capability(
    			new TestCapability.Builder()
    			.namespace(IdentityNamespace.IDENTITY_NAMESPACE)
    			.attribute(IdentityNamespace.IDENTITY_NAMESPACE, "org.apache.aries.subsystem.itests.tb3")
    			.attribute(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, Version.emptyVersion)
    			.attribute(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, IdentityNamespace.TYPE_BUNDLE))
    	.capability(
    			new TestCapability.Builder()
    			.namespace(PackageNamespace.PACKAGE_NAMESPACE)
    			.attribute(PackageNamespace.PACKAGE_NAMESPACE, "org.apache.aries.subsystem.itests.tb3")
    			.attribute(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, "0.0.0"))
    	.content(createTestBundle3Content())
    	.build();
    }
    
    private Repository createTestRepository() throws IOException {
    	return new TestRepository.Builder()
    	.resource(createTestBundle3Resource())
    	.build();
    }
}
