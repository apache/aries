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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.io.File;

import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.filesystem.IDirectory;
import org.apache.aries.util.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.MavenConfiguredJUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.service.subsystem.Subsystem;

@RunWith(MavenConfiguredJUnit4TestRunner.class)
public class InstallTest extends SubsystemTest {
	@Before
	public static void createApplications() throws Exception {
		if (createdApplications) {
			return;
		}
		createApplication("emptySubsystem", new String[0]);
		createApplication("feature3", new String[]{"tb3.jar"});
		createApplication("feature2", new String[]{"tb3.jar", "tb2.jar"});
		createdApplications = true;
	}
	
	public void setUp() throws Exception {
		super.setUp();
		File userDir = new File(System.getProperty("user.dir"));
    	IDirectory idir = FileSystem.getFSRoot(userDir);
    	File emptySubsystem = new File(userDir, "emptySubsystem");
    	emptySubsystem.mkdir();
    	IOUtils.unpackZip(idir.getFile("emptySubsystem.esa"), emptySubsystem);
	}

	@Test
	public void testReturnExistingSubsystemWithSameLocation() throws Exception {
		Subsystem subsystem1 = installSubsystemFromFile("feature3.esa");
		try {
			Subsystem subsystem2 = subsystem1.install(subsystem1.getLocation());
			assertSame(subsystem1, subsystem2);
		}
		finally {
			uninstallSubsystemSilently(subsystem1);
		}
	}
	
	/*
     * Install a subsystem using a location string and a null input stream. The
     * location string is a file URL pointing to a directory.
     */
    @Test
    public void testLocationAsDirectoryUrl() throws Exception {
    	File file = new File("emptySubsystem");
    	try {
    		Subsystem subsystem = installSubsystem(getRootSubsystem(), file.toURI().toString(), null);
    		try {
    			assertEmptySubsystem(subsystem);
    		}
    		finally {
    			uninstallSubsystemSilently(subsystem);
    		}
    		
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    		fail("Subsystem installation using directory URL as location failed");
    	}
    }
    
    @Test
    public void testManagedBundleStartLevel() throws Exception {
    	bundleContext.getBundle(0).adapt(FrameworkStartLevel.class).setInitialBundleStartLevel(5);
    	Bundle tb1 = bundleContext.installBundle("tb1.jar", SubsystemTest.class.getClassLoader().getResourceAsStream("feature1/tb1.jar"));
    	try {
    		Subsystem feature2 = installSubsystemFromFile("feature2.esa");
    		try {
    			startSubsystem(feature2);
    			try {
    				assertEquals("Wrong start level for unmanaged bundle", 5, tb1.adapt(BundleStartLevel.class).getStartLevel());
    				assertEquals("Wrong start level for managed bundle", 1, getBundle(feature2, "org.apache.aries.subsystem.itests.tb2").adapt(BundleStartLevel.class).getStartLevel());
    				assertEquals("Wrong start level for managed bundle", 1, getBundle(feature2, "org.apache.aries.subsystem.itests.tb3").adapt(BundleStartLevel.class).getStartLevel());
    			}
    			finally {
    				stopSubsystemSilently(feature2);
    			}
    		}
    		finally {
    			uninstallSubsystemSilently(feature2);
    		}
    	}
    	finally {
    		uninstallSilently(tb1);
    	}
    }
}
