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

import static org.junit.Assert.fail;

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
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

/*
 * https://issues.apache.org/jira/browse/ARIES-1408
 * 
 * The RequireCapabilityHeader currently only supports requirements defined by 
 * the Aries implementation
 */
public class Aries1408Test extends SubsystemTest {
    /*
     * Subsystem-SymbolicName: application.a.esa
     * Subsystem-Content: bundle.a.jar
     */
    private static final String APPLICATION_A = "application.a.esa";
	/*
	 * Bundle-SymbolicName: bundle.a.jar
	 * Require-Capability: foo
	 */
	private static final String BUNDLE_A = "bundle.a.jar";
	/*
	 * Bundle-SymbolicName: bundle.b.jar
	 * Provide-Capability: foo
	 */
	private static final String BUNDLE_B = "bundle.b.jar";
	
	private static boolean createdTestFiles;
	
	@Before
	public void createTestFiles() throws Exception {
		if (createdTestFiles)
			return;
		createBundleB();
		createApplicationA();
		createdTestFiles = true;
	}
	
	private void createBundleB() throws IOException {
		createBundle(name(BUNDLE_B), new Header(Constants.PROVIDE_CAPABILITY, "foo;foo=bar"));
	}
    
    private static void createApplicationA() throws IOException {
        createApplicationAManifest();
        createSubsystem(APPLICATION_A);
    }
    
    private static void createApplicationAManifest() throws IOException {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_A);
        attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, BUNDLE_A);
        createManifest(APPLICATION_A + ".mf", attributes);
    }
    
    @Test
    public void testRequirementFromRemoteRepositoryConvertsToRequireCapability() throws Exception {
        Bundle bundleB = installBundleFromFile(BUNDLE_B);
        try {
            Subsystem applicationA = installSubsystemFromFile(APPLICATION_A);
            uninstallSubsystemSilently(applicationA);
        }
        catch (SubsystemException e) {
            e.printStackTrace();
            fail("Subsystem should have installed");
        }
        finally {
            uninstallSilently(bundleB);
        }
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
    
    private byte[] createBundleAContent() throws IOException {
    	Manifest manifest = new Manifest();
    	manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    	manifest.getMainAttributes().putValue(Constants.BUNDLE_SYMBOLICNAME, BUNDLE_A);
    	manifest.getMainAttributes().putValue(Constants.REQUIRE_CAPABILITY, "foo;filter:=\'(foo=bar)\"");
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	JarOutputStream jos = new JarOutputStream(baos, manifest);
    	jos.close();
    	return baos.toByteArray();
    }
    
    private Resource createBundleAResource() throws IOException {
    	return new TestRepositoryContent.Builder()
    	.capability(
    			new TestCapability.Builder()
    			.namespace(IdentityNamespace.IDENTITY_NAMESPACE)
    			.attribute(IdentityNamespace.IDENTITY_NAMESPACE, BUNDLE_A)
    			.attribute(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, Version.emptyVersion)
    			.attribute(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, IdentityNamespace.TYPE_BUNDLE))
    	.requirement(
    			new TestRequirement.Builder()
    			.namespace("foo")
    			.directive(Constants.FILTER_DIRECTIVE, "(foo=bar)"))
    	.content(createBundleAContent())
    	.build();
    }
    
    private Repository createTestRepository() throws IOException {
    	return new TestRepository.Builder()
    	.resource(createBundleAResource())
    	.build();
    }
}
