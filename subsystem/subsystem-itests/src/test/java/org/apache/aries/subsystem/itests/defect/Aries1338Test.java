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

import org.apache.aries.subsystem.itests.SubsystemTest;
import org.apache.aries.subsystem.itests.util.BundleArchiveBuilder;
import org.apache.aries.subsystem.itests.util.SubsystemArchiveBuilder;
import org.junit.Test;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemException;

public class Aries1338Test extends SubsystemTest {
	@Test
	public void test() throws Exception {
		test("x;y;z;version=1", "x;version=1,y;version=1,z;version=1", false);
	}
	
	@Test
	public void testMissingExportPackageX() throws Exception {
		test("x;y;z;version=1", "y;version=1,z;version=1", true);
	}
	
	@Test
	public void testMissingExportPackageY() throws Exception {
		test("x;y;z;version=1", "x;version=1,z;version=1", true);
	}
	
	@Test
	public void testMissingExportPackageZ() throws Exception {
		test("x;y;z;version=1", "x;version=1,y;version=1", true);
	}
	
	@Test
	public void testWrongVersionExportPackageX() throws Exception {
		test("x;y;z;version=\"[1,2)\"", "x;version=0,y;version=1,z;version=1.1", true);
	}
	
	@Test
	public void testWrongVersionExportPackageY() throws Exception {
		test("x;y;z;version=\"[1,2)\"", "x;version=1.9,y;version=2,z;version=1.1", true);
	}
	
	@Test
	public void testWrongVersionExportPackageZ() throws Exception {
		test("x;y;z;version=\"[1,2)\"", "x;version=1.9,y;version=1.0.1,z", true);
	}
	
	private void test(String importPackage, String exportPackage, boolean shouldFail) throws Exception {
		Subsystem root = getRootSubsystem();
		try {
			Subsystem subsystem = installSubsystem(
					root,
					"subsystem",
					new SubsystemArchiveBuilder()
							.symbolicName("subsystem")
							.bundle(
									"a", 
									new BundleArchiveBuilder()
											.symbolicName("a")
											.importPackage(importPackage)
											.build())
							.bundle(
									"b", 
									new BundleArchiveBuilder()
											.symbolicName("b")
											.exportPackage(exportPackage)
											.build())
							.build());
			uninstallableSubsystems.add(subsystem);
			if (shouldFail) {
				fail("Subsystem should not have installed");
			}
		}
		catch (SubsystemException e) {
			e.printStackTrace();
			if (!shouldFail) {
				fail("Subsystem should have installed");
			}
		}
	}
}
