/**
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

import static org.junit.Assert.assertNull;

import org.apache.aries.itest.RichBundleContext;
import org.junit.Test;
import org.osgi.service.subsystem.Subsystem;

public class ModelledResourceManagerTest extends SubsystemTest {
	
	public ModelledResourceManagerTest() {
		super(false);
	}
	
	@Override
	public void createApplications() throws Exception {
		createApplication("feature3", new String[]{"tb3.jar"});
		createApplication("application1", new String[]{"tb1.jar"});
	}
	
	public void setUp() throws Exception {
		super.setUp();
		RichBundleContext rootContext = context(getRootSubsystem());
		assertNull("Modeller is installed", rootContext.getBundleByName("org.apache.aries.application.modeller"));
		assertNull("Blueprint is installed", rootContext.getBundleByName("org.apache.aries.blueprint.core"));
		assertNull("Proxy is installed", rootContext.getBundleByName("org.apache.aries.proxy"));
	}

	@Test
	public void testNoModelledResourceManagerService() throws Exception {
		Subsystem feature3 = installSubsystemFromFile("feature3.esa");
		try {
			Subsystem application1 = installSubsystemFromFile("application1.esa");
			try {
				startSubsystem(application1);
			}
			finally {
				stopAndUninstallSubsystemSilently(application1);
			}
		}
		finally {
			uninstallSubsystemSilently(feature3);
		}
	}
}
