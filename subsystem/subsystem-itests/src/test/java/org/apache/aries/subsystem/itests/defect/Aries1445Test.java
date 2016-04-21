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

import java.io.IOException;
import java.io.InputStream;

import org.apache.aries.subsystem.itests.SubsystemTest;
import org.apache.aries.subsystem.itests.util.BundleArchiveBuilder;
import org.apache.aries.subsystem.itests.util.SubsystemArchiveBuilder;
import org.apache.aries.subsystem.itests.util.TestCapability;
import org.apache.aries.subsystem.itests.util.TestRepository;
import org.apache.aries.subsystem.itests.util.TestRepositoryContent;
import org.apache.aries.subsystem.itests.util.TestRequirement;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.service.repository.Repository;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

public class Aries1445Test extends SubsystemTest {
	@Test
	public void testFeatureFeature() throws Exception {
		test(SubsystemConstants.SUBSYSTEM_TYPE_FEATURE, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
	}
	
	@Test
	public void testApplicationApplication() throws Exception {
		test(SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION, SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION);
	}
	
	@Test
	public void testCompositeComposite() throws Exception {
		test(SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
	}
	
	@Test
	public void testFeatureApplication() throws Exception {
		test(SubsystemConstants.SUBSYSTEM_TYPE_FEATURE, SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION);
	}
	
	@Test
	public void testCompositeFeature() throws Exception {
		test(SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
	}
	
    private void test(String type1, String type2) throws Exception {
		serviceRegistrations.add(bundleContext.registerService(
				Repository.class,
				new TestRepository.Builder()
		        		.resource(new TestRepositoryContent.Builder()
		                		.capability(new TestCapability.Builder()
		                        		.namespace(IdentityNamespace.IDENTITY_NAMESPACE)
		                        		.attribute(
		                        				IdentityNamespace.IDENTITY_NAMESPACE, 
		                        				"b")
		                        		.attribute(
		                        				IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, 
		                        				Version.emptyVersion)
		                        		.attribute(
		                        				IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE,
		                        				IdentityNamespace.TYPE_BUNDLE))
		                        .capability(new TestCapability.Builder()
		                        		.namespace(PackageNamespace.PACKAGE_NAMESPACE)
		                        		.attribute(
		                        				PackageNamespace.PACKAGE_NAMESPACE, 
		                        				"b.package")
		                        		.attribute(
		                        				IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, 
		                        				Version.emptyVersion))
		                        .requirement(new TestRequirement.Builder()
		                        		.namespace(PackageNamespace.PACKAGE_NAMESPACE)
		                        		.directive(
		                        				PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE,
		                        				"(osgi.wiring.package=c.package)"))
		                        .requirement(new TestRequirement.Builder()
		                        		.namespace(ServiceNamespace.SERVICE_NAMESPACE)
		                        		.directive(
		                        				ServiceNamespace.REQUIREMENT_FILTER_DIRECTIVE, 
		                        				"(objectClass=foo.Bar)")
		                        		.directive(
		                        				ServiceNamespace.REQUIREMENT_EFFECTIVE_DIRECTIVE, 
		                        				ServiceNamespace.EFFECTIVE_ACTIVE))
		                        .content(new BundleArchiveBuilder()
		                        		.symbolicName("b")
		                        		.exportPackage("b.package")
		                        		.importPackage("c.package")
		                        		.requireCapability("osgi.service;filter:=\"(objectClass=foo.Bar)\";effective:=active")
		                        		.buildAsBytes())
		                        .build())
		                .resource(new TestRepositoryContent.Builder()
		                		.capability(new TestCapability.Builder()
		                        		.namespace(IdentityNamespace.IDENTITY_NAMESPACE)
		                        		.attribute(
		                        				IdentityNamespace.IDENTITY_NAMESPACE, 
		                        				"c")
		                        		.attribute(
		                        				IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, 
		                        				Version.emptyVersion)
		                        		.attribute(
		                        				IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE,
		                        				IdentityNamespace.TYPE_BUNDLE))
		                        .capability(new TestCapability.Builder()
		                        		.namespace(PackageNamespace.PACKAGE_NAMESPACE)
		                        		.attribute(
		                        				PackageNamespace.PACKAGE_NAMESPACE, 
		                        				"c.package")
		                        		.attribute(
		                        				IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, 
		                        				Version.emptyVersion))
		                        .content(new BundleArchiveBuilder()
		                        		.symbolicName("c")
		                        		.exportPackage("c.package")
		                        		.buildAsBytes())
		                        .build())
		                .resource(new TestRepositoryContent.Builder()
		                		.capability(new TestCapability.Builder()
		                        		.namespace(IdentityNamespace.IDENTITY_NAMESPACE)
		                        		.attribute(
		                        				IdentityNamespace.IDENTITY_NAMESPACE, 
		                        				"d")
		                        		.attribute(
		                        				IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, 
		                        				Version.emptyVersion)
		                        		.attribute(
		                        				IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE,
		                        				IdentityNamespace.TYPE_BUNDLE))
		                        .capability(new TestCapability.Builder()
		                        		.namespace(ServiceNamespace.SERVICE_NAMESPACE)
		                        		.attribute(
		                        				Constants.OBJECTCLASS, 
		                        				"foo.Bar")
		                        		.attribute(
		                        				IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, 
		                        				Version.emptyVersion)
		                        		.directive(
		                        				ServiceNamespace.CAPABILITY_EFFECTIVE_DIRECTIVE, 
		                        				ServiceNamespace.EFFECTIVE_ACTIVE))
		                        .content(new BundleArchiveBuilder()
		                        		.symbolicName("d")
		                        		.provideCapability("osgi.service;objectClass=foo.Bar;effective:=active")
		                        		.buildAsBytes())
		                        .build())
		        		.build(),
                null));
		Subsystem root = getRootSubsystem();
		Subsystem s1 = installSubsystem(
				root, 
				"s1", 
				buildSubsystem(root, "s1", type1));
		uninstallableSubsystems.add(s1);
		startSubsystem(s1);
		stoppableSubsystems.add(s1);
		Subsystem s2 = installSubsystem(
				root, 
				"s2", 
				buildSubsystem(root, "s2", type2));
		uninstallableSubsystems.add(s2);
		stopSubsystem(s1);
		stoppableSubsystems.remove(s1);
		uninstallSubsystem(s1);
		uninstallableSubsystems.remove(s1);
		getSystemBundleAsFrameworkWiring().refreshBundles(null, (FrameworkListener)null);
		try {
			s2.start();
			stoppableSubsystems.add(s2);
		}
		catch (SubsystemException e) {
			e.printStackTrace();
			fail("Subsystem should have started");
		}
		// Test the effective:=active service capability and requirement. Bundle
		// D should have had a reference count of 2 and not uninstalled as part
		// of S1. Because effective:=active does not effect runtime resolution,
		// we must ensure it is still a constituent of root.
		assertConstituent(root, "d");
    }
	
	private InputStream buildSubsystem(Subsystem parent, String symbolicName, String type) throws IOException {
		SubsystemArchiveBuilder builder = new SubsystemArchiveBuilder();
		builder.symbolicName(symbolicName);
		builder.type(type);
		if (SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE.equals(type)) {
			builder.importPackage("b.package");
		}
		builder.bundle(
				"a", 
				new BundleArchiveBuilder()
						.symbolicName("a")
						.importPackage("b.package")
						.build());
		return builder.build();
	}
}
