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
import java.util.List;
import java.util.Map;

import org.apache.aries.subsystem.core.internal.Activator;
import org.apache.aries.subsystem.core.internal.SystemRepository;
import org.apache.aries.subsystem.itests.SubsystemTest;
import org.apache.aries.subsystem.itests.util.BundleArchiveBuilder;
import org.apache.aries.subsystem.itests.util.SubsystemArchiveBuilder;
import org.apache.aries.subsystem.itests.util.TestRequirement;
import org.junit.Test;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

@ExamReactorStrategy(PerMethod.class)
public class Aries1442Test extends SubsystemTest {
	@Test
	public void testNewlyInstalledFeature() throws Exception {
		assertFeature(createFeature());
	}
	
	@Test
	public void testPersistedFeature() throws Exception {
		createFeature();
		restartSubsystemsImplBundle();
		Subsystem root = getRootSubsystem();
		Subsystem feature = getChild(root, "feature", null, SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
		assertFeature(feature);
	}
	
	private Subsystem createFeature() throws Exception {
		Subsystem root = getRootSubsystem();
		Subsystem feature = installSubsystem(
				root,
				"feature", 
				new SubsystemArchiveBuilder()
						.symbolicName("feature")
						.type(SubsystemConstants.SUBSYSTEM_TYPE_FEATURE)
						.bundle(
								"a", 
								new BundleArchiveBuilder()
										.symbolicName("a")
										.exportPackage("a")
								.build())
						.build(),
				true
		);
		uninstallableSubsystems.add(feature);
		startSubsystem(feature, true);
		stoppableSubsystems.add(feature);
		return feature;
	}
	
	private void assertFeature(Subsystem feature) {
		Resource resource = (Resource)feature;
		List<Capability> identityCapabilities = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
		String message = "Wrong number of osgi.identity capabilities";
		assertEquals(message, 1, identityCapabilities.size());
		Collection<Capability> capabilities = resource.getCapabilities(null);
		int count = 0;
		for (Capability capability : capabilities) {
			if (IdentityNamespace.IDENTITY_NAMESPACE.equals(capability.getNamespace())) {
					count++;
			}
		}
		assertEquals(message, 1, count);
		SystemRepository repository = Activator.getInstance().getSystemRepository();
		Requirement requirement = new TestRequirement.Builder()
				.namespace("osgi.identity")
				.directive("filter", "(osgi.identity=a)")
				.build();
		Map<Requirement, Collection<Capability>> providers = repository.findProviders(
				Collections.singleton(requirement));
		capabilities = providers.get(requirement);
		assertEquals(message, 1, capabilities.size());
	}
}
