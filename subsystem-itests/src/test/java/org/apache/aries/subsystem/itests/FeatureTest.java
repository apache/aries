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

import java.util.Collection;

import junit.framework.AssertionFailedError;

import org.apache.aries.subsystem.core.internal.ResourceHelper;
import org.junit.Assert;
import org.junit.Test;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Resource;
import org.osgi.service.subsystem.Subsystem;

@ExamReactorStrategy(PerMethod.class)
public class FeatureTest extends SubsystemTest {
	
	@Override
	public void createApplications() throws Exception {
		createApplication("feature2", new String[]{"tb2.jar", "tb3.jar"});
		createApplication("feature1", new String[]{"tb1.jar", "feature2.esa", "tb3.jar"});
		createApplication("feature3", new String[]{"tb3.jar"});
	}

	@Test
	public void testFeature1() throws Exception {
		Subsystem feature1 = installSubsystemFromFile("feature1.esa");
		Subsystem feature2 = null;
		AssertionError error = null;
		try {
			assertSymbolicName("org.apache.aries.subsystem.feature1", feature1);
			assertVersion("1.0.0", feature1);
			assertConstituents(3, feature1);
			assertChildren(1, feature1);
			feature2 = feature1.getChildren().iterator().next();
			assertEvent(feature2, Subsystem.State.INSTALLING, 5000);
			assertEvent(feature2, Subsystem.State.INSTALLED, 5000);
			assertSymbolicName("org.apache.aries.subsystem.feature2", feature2);
			assertVersion("1.0.0", feature2);
			assertConstituent(feature2, "org.apache.aries.subsystem.itests.tb2", Version.parseVersion("2.0.0"), IdentityNamespace.TYPE_BUNDLE);
			assertConstituent(feature2, "org.apache.aries.subsystem.itests.tb3", Version.parseVersion("1.0.0"), IdentityNamespace.TYPE_BUNDLE);
			assertConstituents(2, feature2);
			assertChildren(0, feature2);
			startSubsystem(feature1);
			assertEvent(feature2, Subsystem.State.RESOLVING, 5000);
			assertEvent(feature2, Subsystem.State.RESOLVED, 5000);
			assertEvent(feature2, Subsystem.State.STARTING, 5000);
			assertEvent(feature2, Subsystem.State.ACTIVE, 5000);
			stopSubsystem(feature1);
			assertEvent(feature2, Subsystem.State.STOPPING, 5000);
			assertEvent(feature2, Subsystem.State.RESOLVED, 5000);
		}
		catch (AssertionError e) {
			error = e;
			throw e;
		}
		finally {
			try {
				uninstallSubsystem(feature1);
				if (feature2 != null) {
					assertEvent(feature2, Subsystem.State.INSTALLED, 5000);
					assertEvent(feature2, Subsystem.State.UNINSTALLING, 5000);
					assertEvent(feature2, Subsystem.State.UNINSTALLED, 5000);
					assertNotChild(feature1, feature2);
				}
			}
			catch (AssertionError e) {
				if (error == null)
					throw e;
				e.printStackTrace();
			}
		}
	}
	
	@Test
	public void testPersistence() throws Exception {
		Subsystem feature3Before = installSubsystemFromFile("feature3.esa");
		Subsystem feature3After = null;
		AssertionError error = null;
		try {
			assertFeature3(feature3Before);
			// Uninstall then reinstall the subsystem for a more robust test of the subsystem ID persistence.
			uninstallSubsystem(feature3Before);
			feature3Before = installSubsystemFromFile("feature3.esa");
			assertLastId(2);
			assertFeature3(feature3Before);
			Bundle bundle = getSubsystemCoreBundle();
			bundle.stop();
			resetLastId();
			bundle.start();
			Subsystem root = getRootSubsystem();
			assertChildren(1, root);
			feature3After = root.getChildren().iterator().next();
			assertLastId(2);
			assertFeature3(feature3After);
		}
		catch (AssertionError e) {
			error = e;
			throw e;
		}
		finally {
			try {
				if (feature3After != null) {
					uninstallSubsystem(feature3After);
				}
			}
			catch (AssertionError e) {
				if (error == null)
					throw e;
				e.printStackTrace();
			}
		}
	}
	
	@Test
	public void testSharedContent() throws Exception {
		Subsystem feature1 = installSubsystemFromFile("feature1.esa");
		AssertionError error = null;
		try {
			assertConstituent(feature1, "org.apache.aries.subsystem.itests.tb3", Version.parseVersion("1.0.0"), IdentityNamespace.TYPE_BUNDLE);
			Subsystem feature2 = feature1.getChildren().iterator().next();
			// TODO This needs to be better implemented and put into a utility method on the superclass.
			while (!feature2.getState().equals(Subsystem.State.INSTALLED))
				Thread.sleep(100);
			assertConstituent(feature2, "org.apache.aries.subsystem.itests.tb3", Version.parseVersion("1.0.0"), IdentityNamespace.TYPE_BUNDLE);
			uninstallSubsystem(feature2);
			assertNotChild(feature1, feature2);
			assertConstituent(feature1, "org.apache.aries.subsystem.itests.tb3", Version.parseVersion("1.0.0"), IdentityNamespace.TYPE_BUNDLE);
		}
		catch (AssertionError e) {
			error = e;
			throw e;
		}
		finally {
			try {
				uninstallSubsystem(feature1);
			}
			catch (AssertionError e) {
				if (error == null)
					throw e;
				e.printStackTrace();
			}
		}
	}
	
	private void assertContainsConstituent(Collection<Resource> constituents, Resource constituent) {
		for (Resource resource : constituents) {
			if (ResourceHelper.areEqual(constituent, resource))
				return;
		}
		Assert.fail("Constituent not found");
	}
	
	private void assertContainsChild(Collection<Subsystem> children, Subsystem child) {
		for (Subsystem subsystem : children) {
			try {
				assertEquals(child, subsystem);
				return;
			}
			catch (AssertionError e) {}
		}
		Assert.fail("Child not found");
	}
	
	private void assertEquals(Subsystem subsystem1, Subsystem subsystem2) {
		assertChildrenEqual(subsystem1.getChildren(), subsystem2.getChildren());
		assertConstituentsEqual(subsystem1.getConstituents(), subsystem2.getConstituents());
		Assert.assertEquals("Headers were not equal", subsystem1.getSubsystemHeaders(null), subsystem2.getSubsystemHeaders(null));
		Assert.assertEquals("Locations were not equal", subsystem1.getLocation(), subsystem2.getLocation());
		assertParentsEqual(subsystem1.getParents(), subsystem2.getParents());
		Assert.assertEquals("States were not equal", subsystem1.getState(), subsystem2.getState());
		Assert.assertEquals("IDs were not equal", subsystem1.getSubsystemId(), subsystem2.getSubsystemId());
		Assert.assertEquals("Symbolic names were not equal", subsystem1.getSymbolicName(), subsystem2.getSymbolicName());
		Assert.assertEquals("Versions were not equal", subsystem1.getVersion(), subsystem2.getVersion());
	}
	
	private void assertParentsEqual(Subsystem parent1, Subsystem parent2) {
		if (parent1 == null || parent2 == null) {
			Assert.assertTrue("Parents were not equal", parent1 == null && parent2 == null);
			return;
		}
		assertConstituentsEqual(parent1.getConstituents(), parent2.getConstituents());
		Assert.assertEquals("Headers were not equal", parent1.getSubsystemHeaders(null), parent2.getSubsystemHeaders(null));
		Assert.assertEquals("Locations were not equal", parent1.getLocation(), parent2.getLocation());
		assertParentsEqual(parent1.getParents(), parent2.getParents());
		Assert.assertEquals("States were not equal", parent1.getState(), parent2.getState());
		Assert.assertEquals("IDs were not equal", parent1.getSubsystemId(), parent2.getSubsystemId());
		Assert.assertEquals("Symbolic names were not equal", parent1.getSymbolicName(), parent2.getSymbolicName());
		Assert.assertEquals("Versions were not equal", parent1.getVersion(), parent2.getVersion());
	}
	
	private void assertParentsEqual(Subsystem parent1, Collection<Subsystem> parents2) {
		for (Subsystem parent2 : parents2) {
			try {
				assertParentsEqual(parent1, parent2);
				return;
			}
			catch (AssertionFailedError e) {}
		}
		Assert.fail("Parent not found: " + parent1.getSymbolicName());
	}
	
	private void assertParentsEqual(Collection<Subsystem> parents1, Collection<Subsystem> parents2) {
		Assert.assertEquals("Size not equal", parents1.size(), parents2.size());
		for (Subsystem parent1 : parents1) {
			assertParentsEqual(parent1, parents2);
		}
	}
	
	private void assertConstituentsEqual(Collection<Resource> resources1, Collection<Resource> resources2) {
		Assert.assertEquals("Constituent size does not match", resources1.size(), resources2.size());
		for (Resource resource : resources1) {
			assertContainsConstituent(resources2, resource);
		}
	}
	
	private void assertChildrenEqual(Collection<Subsystem> subsystems1, Collection<Subsystem> subsystems2) {
		Assert.assertEquals("Children size does not match", subsystems1.size(), subsystems2.size());
		for (Subsystem subsystem : subsystems1) {
			assertContainsChild(subsystems2, subsystem);
		}
	}
	
	private void assertFeature3(Subsystem subsystem) {
		assertChildren(0, subsystem);
		assertConstituents(1, subsystem);
		assertConstituent(subsystem, "org.apache.aries.subsystem.itests.tb3", Version.parseVersion("1.0.0"), IdentityNamespace.TYPE_BUNDLE);
//		subsystem.getHeaders();
//		subsystem.getHeaders("");
//		subsystem.getState();
		assertSymbolicName("org.apache.aries.subsystem.feature3", subsystem);
		assertVersion("0.0.0", subsystem);
	}
}
