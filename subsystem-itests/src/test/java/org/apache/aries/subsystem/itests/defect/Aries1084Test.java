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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.aries.subsystem.core.archive.DeploymentManifest;
import org.apache.aries.subsystem.itests.SubsystemTest;
import org.apache.aries.subsystem.itests.util.TestCapability;
import org.apache.aries.subsystem.itests.util.TestRepository;
import org.apache.aries.subsystem.itests.util.TestRepositoryContent;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

/*
 * https://issues.apache.org/jira/browse/ARIES-1084
 * 
 * Subsystem : Failure on restart after framework crash
 */
public class Aries1084Test extends SubsystemTest {
    /*
     * Subsystem-SymbolicName: feature.a.esa
     * Subsystem-Content: bundle.a.jar
     * 
     * Included in archive:
     *      bundle.a.jar
     */
    private static final String FEATURE_A = "feature.a.esa";
	/*
	 * Bundle-SymbolicName: bundle.a.jar
	 */
	private static final String BUNDLE_A = "bundle.a.jar";
	
	private static boolean createdTestFiles;
	
	@Before
	public void createTestFiles() throws Exception {
		if (createdTestFiles)
			return;
		createBundleA();
		createFeatureA();
		createdTestFiles = true;
	}
	
	private void createBundleA() throws IOException {
		createBundle(name(BUNDLE_A));
	}
	
	private void createFeatureA() throws IOException {
        createFeatureAManifest();
        createSubsystem(FEATURE_A, BUNDLE_A);
    }
    
    private void createFeatureAManifest() throws IOException {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, FEATURE_A);
        attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, BUNDLE_A);
        createManifest(FEATURE_A + ".mf", attributes);
    }
    
    @Test
    public void testBundleStartsWhenSubsystemLeftInInvalidState() throws Exception {
        Subsystem featureA = installSubsystemFromFile(FEATURE_A);
        try {
	        startSubsystem(featureA);
	        assertBundleState(Bundle.ACTIVE, BUNDLE_A, featureA);
	        Bundle core = getSubsystemCoreBundle();
	        File file = core.getBundleContext().getDataFile(
	        		featureA.getSubsystemId() + "/OSGI-INF/DEPLOYMENT.MF");
	        core.stop();
	        DeploymentManifest manifest = new DeploymentManifest(file);
	        FileOutputStream fos = new FileOutputStream(file);
	        try {
		        new DeploymentManifest.Builder()
			        	.manifest(manifest)
			        	.state(Subsystem.State.ACTIVE)
			        	.build()
			        	.write(fos);
	        }
	        finally {
	        	fos.close();
	        }
	        core.start();
	        featureA = getChild(getRootSubsystem(), FEATURE_A);
	        assertBundleState(Bundle.ACTIVE, BUNDLE_A, featureA);
        }
        finally {
            stopAndUninstallSubsystemSilently(featureA);
        }
    }
    
    private Repository createTestRepository() throws IOException {
        return new TestRepository.Builder()
        .resource(createBundleAResource())
        .build();
    }
    
    private byte[] createBundleAContent() throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue(Constants.BUNDLE_SYMBOLICNAME, BUNDLE_A);
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
                    .attribute(IdentityNamespace.IDENTITY_NAMESPACE, BUNDLE_A))
        .content(createBundleAContent())
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
