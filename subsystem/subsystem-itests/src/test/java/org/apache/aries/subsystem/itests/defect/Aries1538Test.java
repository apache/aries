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

import org.apache.aries.subsystem.core.archive.AriesProvisionDependenciesDirective;
import org.apache.aries.subsystem.itests.SubsystemTest;
import org.apache.aries.subsystem.itests.util.BundleArchiveBuilder;
import org.apache.aries.subsystem.itests.util.SubsystemArchiveBuilder;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

public class Aries1538Test extends SubsystemTest {
	@Test
	public void testEffectiveActiveApacheAriesProvisionDependenciesInstall() throws Exception {
		testEffectiveActive(AriesProvisionDependenciesDirective.INSTALL);
	}
	
	@Test
	public void testEffectiveActiveApacheAriesProvisionDependenciesResolve() throws Exception {
		testEffectiveActive(AriesProvisionDependenciesDirective.RESOLVE);
	}
	
	@Test
	public void testSubstitutableExportApacheAriesProvisionDependenciesInstall() throws Exception {
		testSubstitutableExport(AriesProvisionDependenciesDirective.INSTALL);
	}
	
	@Test
	public void testSubstituableExportApacheAriesProvisionDependenciesResolve() throws Exception {
		testSubstitutableExport(AriesProvisionDependenciesDirective.RESOLVE);
	}
	
	@Test
	public void testHostFragmentCircularDependencyApacheAriesProvisionDependenciesInstall() throws Exception {
		testHostFragmentCircularDependency(AriesProvisionDependenciesDirective.INSTALL);
	}
	
	@Test
	public void testHostFragmentCircularDependencyApacheAriesProvisionDependenciesResolve() throws Exception {
		testHostFragmentCircularDependency(AriesProvisionDependenciesDirective.RESOLVE);
	}
	
	private void testEffectiveActive(AriesProvisionDependenciesDirective provisionDependencies) throws Exception {
		boolean flag = AriesProvisionDependenciesDirective.INSTALL.equals(provisionDependencies);
		BundleArchiveBuilder bab = new BundleArchiveBuilder();
		bab.symbolicName("bundle");
		bab.requireCapability("osgi.service;filter:=\"(&(objectClass=java.lang.Object)(foo=bar))\";effective:=active");
		bab.exportPackage("foo");
		Subsystem root = getRootSubsystem();
		Bundle a = root.getBundleContext().installBundle(
				"a", 
				bab.build());
		uninstallableBundles.add(a);
		startBundle(a);
		try {
			Subsystem subsystem = installSubsystem(
					root,
					"subsystem", 
					new SubsystemArchiveBuilder()
							.symbolicName("subsystem")
							.type(SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION + ';' 
										+ provisionDependencies.toString())
							.bundle(
									"b", 
									new BundleArchiveBuilder()
											.symbolicName("b")
											.importPackage("foo")
											.build())
							.build(),
					flag
			);
			uninstallableSubsystems.add(subsystem);
			startSubsystem(subsystem, flag);
			stoppableSubsystems.add(subsystem);
		}
		catch (SubsystemException e) {
			e.printStackTrace();
			fail("Subsystem should have installed and started");
		}
	}
	
	private void testSubstitutableExport(AriesProvisionDependenciesDirective provisionDependencies) throws Exception {
		boolean flag = AriesProvisionDependenciesDirective.INSTALL.equals(provisionDependencies);
		BundleArchiveBuilder hostBuilder = new BundleArchiveBuilder();
		hostBuilder.symbolicName("host");
		BundleArchiveBuilder fragmentBuilder = new BundleArchiveBuilder();
		fragmentBuilder.symbolicName("fragment");
		fragmentBuilder.exportPackage("foo");
		fragmentBuilder.importPackage("foo");
		fragmentBuilder.header("Fragment-Host", "host");
		Subsystem root = getRootSubsystem();
		Bundle host = root.getBundleContext().installBundle(
				"host", 
				hostBuilder.build());
		uninstallableBundles.add(host);
		Bundle fragment = root.getBundleContext().installBundle(
				"fragment", 
				fragmentBuilder.build());
		uninstallableBundles.add(fragment);
		startBundle(host);
		try {
			Subsystem subsystem = installSubsystem(
					root,
					"subsystem", 
					new SubsystemArchiveBuilder()
							.symbolicName("subsystem")
							.type(SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION + ';' 
										+ provisionDependencies.toString())
							.bundle(
									"a", 
									new BundleArchiveBuilder()
											.symbolicName("a")
											.importPackage("foo")
											.build())
							.build(),
					flag
			);
			uninstallableSubsystems.add(subsystem);
			startSubsystem(subsystem, flag);
			stoppableSubsystems.add(subsystem);
		}
		catch (SubsystemException e) {
			e.printStackTrace();
			fail("Subsystem should have installed and started");
		}
	}
	
	private void testHostFragmentCircularDependency(AriesProvisionDependenciesDirective provisionDependencies) throws Exception {
		boolean flag = AriesProvisionDependenciesDirective.INSTALL.equals(provisionDependencies);
		BundleArchiveBuilder hostBuilder = new BundleArchiveBuilder();
		hostBuilder.symbolicName("host");
		hostBuilder.exportPackage("foo");
		hostBuilder.importPackage("bar");
		BundleArchiveBuilder fragmentBuilder = new BundleArchiveBuilder();
		fragmentBuilder.symbolicName("fragment");
		fragmentBuilder.exportPackage("bar");
		fragmentBuilder.importPackage("foo");
		fragmentBuilder.header("Fragment-Host", "host");
		Subsystem root = getRootSubsystem();
		Bundle host = root.getBundleContext().installBundle(
				"host", 
				hostBuilder.build());
		uninstallableBundles.add(host);
		Bundle fragment = root.getBundleContext().installBundle(
				"fragment", 
				fragmentBuilder.build());
		uninstallableBundles.add(fragment);
		startBundle(host);
		try {
			Subsystem subsystem = installSubsystem(
					root,
					"subsystem", 
					new SubsystemArchiveBuilder()
							.symbolicName("subsystem")
							.type(SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION + ';' 
										+ provisionDependencies.toString())
							.bundle(
									"a", 
									new BundleArchiveBuilder()
											.symbolicName("a")
											.importPackage("foo")
											.build())
							.build(),
					flag
			);
			uninstallableSubsystems.add(subsystem);
			startSubsystem(subsystem, flag);
			stoppableSubsystems.add(subsystem);
		}
		catch (SubsystemException e) {
			e.printStackTrace();
			fail("Subsystem should have installed and started");
		}
	}
}
