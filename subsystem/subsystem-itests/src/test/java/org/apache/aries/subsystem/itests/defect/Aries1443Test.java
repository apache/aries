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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.aries.subsystem.core.archive.AriesProvisionDependenciesDirective;
import org.apache.aries.subsystem.core.internal.Activator;
import org.apache.aries.subsystem.core.internal.SystemRepository;
import org.apache.aries.subsystem.itests.SubsystemTest;
import org.apache.aries.subsystem.itests.util.BundleArchiveBuilder;
import org.apache.aries.subsystem.itests.util.SubsystemArchiveBuilder;
import org.apache.aries.subsystem.itests.util.TestRequirement;
import org.junit.Test;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

public class Aries1443Test extends SubsystemTest {
	@Test
    public void testProvisionDependenciesInstall() throws Exception {
		test(AriesProvisionDependenciesDirective.VALUE_INSTALL);
    }
	
	@Test
    public void testProvisionDependenciesResolve() throws Exception {
		test(AriesProvisionDependenciesDirective.VALUE_RESOLVE);
    }
	
	private void test(String apacheAriesProvisionDependencies) throws Exception {
		Subsystem root = getRootSubsystem();
		Subsystem composite = installSubsystem(
				root,
				"composite", 
				new SubsystemArchiveBuilder()
						.symbolicName("composite")
						.type(SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE 
								+ ';' 
								+ AriesProvisionDependenciesDirective.NAME
								+ ":="
								+ apacheAriesProvisionDependencies)
						.importPackage("org.osgi.framework")
						.build(),
				AriesProvisionDependenciesDirective.VALUE_INSTALL.equals(apacheAriesProvisionDependencies)
		);
		uninstallableSubsystems.add(composite);
		startSubsystem(composite, AriesProvisionDependenciesDirective.VALUE_INSTALL.equals(apacheAriesProvisionDependencies));
		stoppableSubsystems.add(composite);
		installSubsystem(
				composite,
				"feature", 
				new SubsystemArchiveBuilder()
						.symbolicName("feature")
						.type(SubsystemConstants.SUBSYSTEM_TYPE_FEATURE
								+ ';'
								+ AriesProvisionDependenciesDirective.NAME
								+ ":="
								+ apacheAriesProvisionDependencies)
						.bundle(
								"a", 
								new BundleArchiveBuilder()
										.symbolicName("a")
										.exportPackage("a")
								.build())
						.build(),
				AriesProvisionDependenciesDirective.VALUE_INSTALL.equals(apacheAriesProvisionDependencies)
		);
		SystemRepository repository = Activator.getInstance().getSystemRepository();
		Requirement requirement = new TestRequirement.Builder()
				.namespace("osgi.wiring.package")
				.directive("filter", "(osgi.wiring.package=a)")
				.build();
		Map<Requirement, Collection<Capability>> providers = repository.findProviders(
				Collections.singleton(requirement));
		Collection<Capability> capabilities = providers.get(requirement);
		assertEquals("Missing provider", 2, capabilities.size());
	}
}
