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

import org.apache.aries.subsystem.itests.SubsystemTest;
import org.apache.aries.subsystem.itests.util.BundleArchiveBuilder;
import org.apache.aries.subsystem.itests.util.SubsystemArchiveBuilder;
import org.junit.Test;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Resource;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

public class Aries1442Test extends SubsystemTest {
	@Test
	public void test() throws Exception {
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
		assertEquals("Wrong number of osgi.identity capabilities",
				1, ((Resource)feature).getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).size());
	}
}
