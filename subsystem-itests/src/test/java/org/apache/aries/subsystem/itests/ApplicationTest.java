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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.aries.subsystem.itests.util.TestCapability;
import org.apache.aries.subsystem.itests.util.TestRepository;
import org.apache.aries.subsystem.itests.util.TestRepositoryContent;
import org.apache.aries.subsystem.itests.util.Utils;
import org.apache.aries.unittest.fixture.ArchiveFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.ZipFixture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.resource.Resource;
import org.osgi.service.repository.Repository;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

@RunWith(JUnit4TestRunner.class)
public class ApplicationTest extends SubsystemTest {
	private static void createApplication(String name, String[] content) throws Exception {
		ZipFixture feature = ArchiveFixture
				.newZip()
				.binary("OSGI-INF/SUBSYSTEM.MF",
						ApplicationTest.class.getClassLoader().getResourceAsStream(
								name + "/OSGI-INF/SUBSYSTEM.MF"));
		for (String s : content) {
			try {
				feature.binary(s,
						ApplicationTest.class.getClassLoader().getResourceAsStream(
								name + '/' + s));
			}
			catch (Exception e) {
				feature.binary(s, new FileInputStream(new File(s)));
			}
		}
		feature.end();
		FileOutputStream fos = new FileOutputStream(name + ".esa");
		try {
			feature.writeOut(fos);
		} finally {
			Utils.closeQuietly(fos);
		}
	}
	
	@Before
	public static void createApplications() throws Exception {
		if (createdApplications) {
			return;
		}
		createApplication("application1", new String[]{"tb1.jar"});
		createdApplications = true;
	}
	
	public void setUp() {
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
    		stopSubsystem(application1);
    		uninstallScopedSubsystem(application1);
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
