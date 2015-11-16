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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.aries.subsystem.itests.SubsystemTest;
import org.apache.aries.subsystem.itests.util.TestCapability;
import org.apache.aries.subsystem.itests.util.TestRepository;
import org.apache.aries.subsystem.itests.util.TestRequirement;
import org.apache.aries.subsystem.itests.util.TestResource;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;
import org.osgi.service.repository.RepositoryContent;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

public class Aries1451Test extends SubsystemTest {

    private static final String APPLICATION_A = "application.a.esa";
    private static final String BUNDLE_A = "bundle.a.jar";
    private static final String BUNDLE_B = "bundle.b.jar";
    private static final String PACKAGE_REQUIREMENT = "org.apache.aries.test.bundlebrequirement";

    private static boolean createdTestFiles;

    @Before
    public void createTestFiles() throws Exception {
        if (createdTestFiles)
            return;
        createBundleA();
        createApplicationA();
        createdTestFiles = true;

        //set up repository to satisfy BUNDLE_A's package requirement for a package in BUNDLE_B use
        // a RepositoryContent test with implementation that is private. 
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

    private void createBundleA() throws IOException {
        createBundle(name(BUNDLE_A), importPackage(PACKAGE_REQUIREMENT));
    }

    private void createApplicationA() throws IOException {
        createApplicationAManifest();
        createSubsystem(APPLICATION_A, BUNDLE_A);
    }

    private void createApplicationAManifest() throws IOException {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_A);
        attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, BUNDLE_A + ";type=osgi.bundle");
        createManifest(APPLICATION_A + ".mf", attributes);
    }


    @Test
    public void testInstallWithAccessProtectedRepository() throws Exception {
        Subsystem applicationA = installSubsystemFromFile(APPLICATION_A);
        try {
            startSubsystem(applicationA);
        }
        finally {
            stopAndUninstallSubsystemSilently(applicationA);
        }
    }


    private Repository createTestRepository() throws IOException {
        return new TestRepository.Builder()
        .resource(createTestBundleResource())
        .build();
    }

    private byte[] createTestBundleContent() throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue(Constants.BUNDLE_SYMBOLICNAME, BUNDLE_B);
        manifest.getMainAttributes().putValue(Constants.EXPORT_PACKAGE,  PACKAGE_REQUIREMENT);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JarOutputStream jos = new JarOutputStream(baos, manifest);
        jos.close();
        return baos.toByteArray();
    }


    //This resource must have private visibility for the test 
    private Resource createTestBundleResource() throws IOException {

        List<TestCapability.Builder> capabilities = new ArrayList<TestCapability.Builder>() {
            private static final long serialVersionUID = 1L;

            {
                add(new TestCapability.Builder()
                .namespace(IdentityNamespace.IDENTITY_NAMESPACE)
                .attribute(IdentityNamespace.IDENTITY_NAMESPACE, BUNDLE_B)
                .attribute(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, IdentityNamespace.TYPE_BUNDLE));
                add(new TestCapability.Builder()
                .namespace(PackageNamespace.PACKAGE_NAMESPACE)
                .attribute(PackageNamespace.PACKAGE_NAMESPACE, PACKAGE_REQUIREMENT)
                .attribute(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, "0.0.0"));
            }
        };

        return new PrivateRepositoryContent(capabilities, Collections.<TestRequirement.Builder>emptyList(), 
                createTestBundleContent());
    }

    private class PrivateRepositoryContent extends TestResource implements RepositoryContent {

        private final byte[] content;

        public PrivateRepositoryContent(List<TestCapability.Builder> capabilities, 
                List<TestRequirement.Builder> requirements, byte[] content) {
            super(capabilities, requirements);
            this.content = content;
        }

        @Override
        public InputStream getContent() {
            try {
                return new ByteArrayInputStream(content);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
