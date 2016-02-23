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

import org.apache.aries.subsystem.itests.SubsystemTest;
import org.apache.aries.subsystem.itests.util.BundleArchiveBuilder;
import org.apache.aries.subsystem.itests.util.SubsystemArchiveBuilder;
import org.junit.Test;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.Bundle;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

@ExamReactorStrategy(PerMethod.class)
public class Aries1441Test extends SubsystemTest {
	@Test
	public void testApplication() throws Exception {
		test(SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION);
	}
	
	@Test
	public void testComposite() throws Exception {
		test(SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
	}
	
	@Test
	public void testFeature() throws Exception {
		test(SubsystemConstants.SUBSYSTEM_TYPE_FEATURE);
	}
	
	private void test(String type) throws Exception {
		SubsystemArchiveBuilder builder = new SubsystemArchiveBuilder()
				.symbolicName("subsystem")
				.type(type)
				.content("a;version=\"[0,0]\"")
				.bundle(
						"a",
						new BundleArchiveBuilder()
								.symbolicName("a")
									.importPackage("org.osgi.framework")
									.importPackage("b")
									.build()
				)
				.bundle(
						"b",
						new BundleArchiveBuilder()
								.symbolicName("b")
									.exportPackage("b")
									.build()
				);
		if (SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE.equals(type)) {
			builder.importPackage("org.osgi.framework,b");
		}
		Subsystem root = getRootSubsystem();
		Subsystem subsystem = installSubsystem(root, "subsystem", builder.build());
		uninstallableSubsystems.add(subsystem);
		startSubsystem(subsystem);
		stoppableSubsystems.add(subsystem);
		Bundle core = getSubsystemCoreBundle();
		core.stop();
		stoppableSubsystems.remove(subsystem);
		uninstallableSubsystems.remove(subsystem);
		assertBundleState(getSystemBundle(), org.osgi.framework.Bundle.ACTIVE);
		core.start();
		root = getRootSubsystem();
		subsystem = getChild(root, "subsystem", null, type);
		stopSubsystem(subsystem);
		assertBundleState(Bundle.RESOLVED, "b", root);
		uninstallSubsystem(subsystem);
		assertNotConstituent(root, "b");
	}
}
