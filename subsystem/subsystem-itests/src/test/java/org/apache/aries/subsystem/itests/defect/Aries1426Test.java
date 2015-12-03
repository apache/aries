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
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.aries.subsystem.core.archive.GenericHeader;
import org.apache.aries.subsystem.core.archive.SubsystemContentHeader;
import org.apache.aries.subsystem.itests.SubsystemTest;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

/*
 * https://issues.apache.org/jira/browse/ARIES-1426
 * 
 * Implementation specific subsystem header Application-ImportService should not 
 * affect the sharing policy.
 */
public class Aries1426Test extends SubsystemTest {
	/*
	 * Subsystem-SymbolicName: application.a.esa
	 * Application-ImportService: org.osgi.service.coordinator.Coordinator
	 * 
	 * Included In Archive
	 * 		bundle.a.jar
	 */
	private static final String APPLICATION_A = "application.a.esa";
	/*
	 * Subsystem-SymbolicName: application.b.esa
	 * 
	 * Included In Archive
	 * 		bundle.b.jar
	 */
	private static final String APPLICATION_B = "application.b.esa";
	/*
	 * Bundle-SymbolicName: bundle.a.jar
	 * Require-Capability: osgi.service;filter:="(objectClass=org.osgi.service.coordinator.Coordinator)"
	 */
	private static final String BUNDLE_A = "bundle.a.jar";
	/*
	 * Bundle-SymbolicName: bundle.b.jar
	 * Require-Capability: osgi.service;filter:="(&(objectClass=java.lang.Object)(a=b))";resolution:=optional
	 */
	private static final String BUNDLE_B = "bundle.b.jar";
	
	private void createApplicationA() throws IOException {
		createApplicationAManifest();
		createSubsystem(APPLICATION_A, BUNDLE_A);
	}
	
	private void createApplicationAManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_A);
		attributes.put("Application-ImportService", "org.osgi.service.coordinator.Coordinator");
		createManifest(APPLICATION_A + ".mf", attributes);
	}
	
	private void createApplicationB() throws IOException {
		createApplicationBManifest();
		createSubsystem(APPLICATION_B, BUNDLE_B);
	}
	
	private void createApplicationBManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_B);
		createManifest(APPLICATION_B + ".mf", attributes);
	}
	
	@Override
	public void createApplications() throws Exception {
		createBundleA();
		createBundleB();
		createApplicationA();
		createApplicationB();
	}
	
	private void createBundleA() throws IOException {
		createBundle(name(BUNDLE_A), requireCapability("osgi.service;"
				+ "filter:=\"(objectClass=org.osgi.service.coordinator.Coordinator)\""));
	}
	
	private void createBundleB() throws IOException {
		createBundle(name(BUNDLE_B), requireCapability("osgi.service;"
				+ "filter:=\"(&(objectClass=java.lang.Object)(a=b))\";resolution:=optional"));
	}
	
	@Test
	public void testNoEffectOnSharingPolicy() throws Exception {
		Subsystem applicationA = installSubsystemFromFile(APPLICATION_A);
		try {
			Map<String, String> headers = applicationA.getDeploymentHeaders();
			// There should be no subsystem Require-Capability header because 
			// the Application-ImportService header included the only relevant
			// bundle clause.
			assertNull("Wrong Require-Capability", headers.get(Constants.REQUIRE_CAPABILITY));
			// There should be no subsystem Subsystem-ImportService header 
			// because the Application-ImportService header included the only 
			// relevant bundle clause.
			assertNull("Wrong Subsystem-ImportService", headers.get(SubsystemConstants.SUBSYSTEM_IMPORTSERVICE));
			org.apache.aries.subsystem.core.archive.Header<?> expected = 
					new SubsystemContentHeader("bundle.a.jar;version=\"[0,0]\"");
			org.apache.aries.subsystem.core.archive.Header<?> actual = 
					new SubsystemContentHeader(headers.get(SubsystemConstants.SUBSYSTEM_CONTENT));
			// The Subsystem-Content header should not include any synthesized
			// resources used to process Application-ImportService.
			assertEquals("Wrong Subsystem-Content", expected, actual);
			expected = new GenericHeader("Application-ImportService", "org.osgi.service.coordinator.Coordinator");
			actual = new GenericHeader("Application-ImportService", headers.get("Application-ImportService"));
			// The Application-ImportService header should be included in the
			// deployment manifest.
			assertEquals("Wrong Application-ImportService", expected, actual);
			BundleContext context = applicationA.getBundleContext();
			// The Coordinator service should not be visible to the application
			// region because Application-ImportService does not affect the
			// sharing policy and nothing outside of the subsystems 
			// implementation made it visible.
			assertNull("Coordinator service should not be visible", context.getServiceReference(Coordinator.class));
		}
		finally {
			uninstallSubsystemSilently(applicationA);
		}
	}
	
	@Test
	public void testIncludeUnsatisfiedOptionalServiceDependencyInSharingPolicy() throws Exception {
		Subsystem applicationB = installSubsystemFromFile(APPLICATION_B);
		try {
			BundleContext context = getRootSubsystem().getBundleContext();
			Dictionary<String, String> properties = new Hashtable<String, String>();
			properties.put("a", "b");
			ServiceRegistration<Object> registration = context.registerService(Object.class, new Object(), properties);
			try {
				context = applicationB.getBundleContext();
				Collection<ServiceReference<Object>> references = context.getServiceReferences(Object.class, "(a=b)");
				assertFalse("Service not visible", references.isEmpty());
			}
			finally {
				registration.unregister();
			}
		}
		finally {
			uninstallSubsystemSilently(applicationB);
		}
	}
}
