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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.aries.subsystem.AriesSubsystem;
import org.apache.aries.subsystem.itests.SubsystemTest;
import org.apache.aries.subsystem.itests.util.TestRequirement;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.tinybundles.core.InnerClassStrategy;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Requirement;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

/*
 * https://issues.apache.org/jira/browse/ARIES-1429
 * 
 * NullPointerException at org.apache.aries.subsystem.core.internal.WovenClassListener.modified 
 * at org.apache.aries.subsystem.core.internal.RegionUpdater.addRequirements.
 */
public class Aries1429Test extends SubsystemTest {
	/*
	 * Subsystem-SymbolicName: application.a.esa
	 */
	private static final String APPLICATION_A = "application.a.esa";
	/*
	 * Bundle-SymbolicName: bundle.a.jar
	 */
	private static final String BUNDLE_A = "bundle.a.jar";
	
	private static boolean createdTestFiles;
	
	private void createApplicationA() throws IOException {
		createApplicationAManifest();
		createSubsystem(APPLICATION_A);
	}
	
	private void createApplicationAManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_A);
		createManifest(APPLICATION_A + ".mf", attributes);
	}
	
	@Before
	public void createTestFiles() throws Exception {
		if (createdTestFiles)
			return;
		createApplicationA();
		createdTestFiles = true;
	}
	
	@Test
    public void testMissingParentChildEdgeTolerated() throws Exception {
		final AtomicBoolean weavingHookCalled = new AtomicBoolean();
		final AtomicReference<FrameworkEvent> frameworkEvent = new AtomicReference<FrameworkEvent>();
		bundleContext.registerService(
    			WeavingHook.class, 
    			new WeavingHook() {
    				@Override
    				public void weave(WovenClass wovenClass) {
    					Bundle bundle = wovenClass.getBundleWiring().getBundle();
    					if (BUNDLE_A.equals(bundle.getSymbolicName())) {
    						wovenClass.getDynamicImports().add("com.acme.tnt");
    						weavingHookCalled.set(true);
    					}
    				}
    			}, 
    			null);
    	Subsystem applicationA = installSubsystemFromFile(APPLICATION_A);
    	try {
	    	removeConnectionWithParent(applicationA);
			BundleContext context = applicationA.getBundleContext();
			Bundle bundleA = context.installBundle(
					BUNDLE_A, 
					TinyBundles
							.bundle()
							.add(getClass().getClassLoader().loadClass("a.A"), InnerClassStrategy.NONE)
							.set(Constants.BUNDLE_SYMBOLICNAME, BUNDLE_A)
							.build(TinyBundles.withBnd()));
			bundleContext.addFrameworkListener(
	    			new FrameworkListener() {
						@Override
						public void frameworkEvent(FrameworkEvent event) {
							if (FrameworkEvent.ERROR == event.getType()
									&& getSubsystemCoreBundle().equals(event.getBundle())) {
								frameworkEvent.set(event);
								if (event.getThrowable() != null) {
									event.getThrowable().printStackTrace();
								}
							}
						}
	    			});
			bundleA.loadClass("a.A");
			assertTrue("Weaving hook not called", weavingHookCalled.get());
			Thread.sleep(1000);
			assertNull("An exception was thrown", frameworkEvent.get());
    	}
    	finally {
    		uninstallSubsystemSilently(applicationA);
    	}
    }
    
    @Test
    public void testMissingParentChildEdgeNotTolerated() throws Exception {
    	Subsystem applicationA = installSubsystemFromFile(APPLICATION_A);
    	try {
    		removeConnectionWithParent(applicationA);
    		try {
    			((AriesSubsystem)applicationA).addRequirements(
    					Collections.singletonList(
    							(Requirement) new TestRequirement.Builder()
    							.namespace(PackageNamespace.PACKAGE_NAMESPACE)
    							.attribute(PackageNamespace.PACKAGE_NAMESPACE, "org.osgi.framework")
    							.build()));
    			fail("No exception received");
    		}
    		catch (SubsystemException e) {
    			Throwable cause = e.getCause();
    			assertNotNull("Wrong cause", cause);
    			assertEquals("Wrong cause", IllegalStateException.class, cause.getClass());
    		}
    	}
    	finally {
    		uninstallSubsystemSilently(applicationA);
    	}
    }
}
