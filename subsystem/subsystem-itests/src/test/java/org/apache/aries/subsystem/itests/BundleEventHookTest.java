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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.equinox.region.RegionDigraph;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.MavenConfiguredJUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

@RunWith(MavenConfiguredJUnit4TestRunner.class)
public class BundleEventHookTest extends SubsystemTest {
    /*
	 * Bundle-SymbolicName: bundle.a.jar
	 */
	private static final String BUNDLE_A = "bundle.a.jar";
	/*
	 * Bundle-SymbolicName: bundle.b.jar
	 */
	private static final String BUNDLE_B = "bundle.b.jar";
	
	@Before
	public static void createApplications() throws Exception {
		if (createdApplications) {
			return;
		}
		createBundleA();
		createBundleB();
		createdApplications = true;
	}
	
	private static void createBundleA() throws IOException {
		createBundle(BUNDLE_A);
	}
	
	private static void createBundleB() throws IOException {
		createBundle(BUNDLE_B);
	}
    
    /*
     * See https://issues.apache.org/jira/browse/ARIES-982.
     * 
     * When activating, the subsystems bundle must initialize the root subsystem
     * along with any persisted subsystems. Part of the root subsystem 
     * initialization consists of adding all pre-existing bundles as 
     * constituents. In order to ensure that no bundles are missed, a bundle
     * event hook is registered first. The bundle event hook cannot process
     * events until the initialization is complete. Another part of 
     * initialization consists of registering the root subsystem service.
     * Therefore, a potential deadlock exists if something reacts to the
     * service registration by installing an unmanaged bundle.
     */
    @Test
    public void testNoDeadlockWhenSubsystemsInitializing() throws Exception {
    	final Bundle bundle = getSubsystemCoreBundle();
    	bundle.stop();
    	final AtomicBoolean completed = new AtomicBoolean(false);
    	final ExecutorService executor = Executors.newFixedThreadPool(2);
    	try {
	    	bundleContext.addServiceListener(new ServiceListener() {
				@Override
				public void serviceChanged(ServiceEvent event) {
					Future<?> future = executor.submit(new Runnable() {
						public void run() {
							try {
			    				Bundle a = bundle.getBundleContext().installBundle(BUNDLE_A, new FileInputStream(BUNDLE_A));
			    				completed.set(true);
								a.uninstall();
			    			}
			    			catch (Exception e) {
			    				e.printStackTrace();
			    			}
						}
					});
					try {
						future.get();
						completed.set(true);
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
	    	}, "(&(objectClass=org.osgi.service.subsystem.Subsystem)(subsystem.id=0))");
	    	Future<?> future = executor.submit(new Runnable() {
	    		public void run() {
	    			try {
	    				bundle.start();
	    			}
	    			catch (Exception e) {
	    				e.printStackTrace();
	    			}
	    		}
	    	});
	    	try {
	    		future.get(3, TimeUnit.SECONDS);
	    		assertTrue("Deadlock detected", completed.get());
	    	}
	    	catch (TimeoutException e) {
	    		fail("Deadlock detected");
	    	}
    	}
    	finally {
    		executor.shutdownNow();
    	}
    }
    
    /*
     * Because bundle events are queued for later asynchronous processing while
     * the root subsystem is initializing, it is possible to see an installed
     * event for a bundle that has been uninstalled (i.e. the bundle revision
     * will be null). These events should be ignored.
     */
    @Test
    public void testIgnoreUninstalledBundleInAsyncInstalledEvent() throws Exception {
    	final Bundle core = getSubsystemCoreBundle();
    	core.stop();
    	final AtomicReference<Bundle> a = new AtomicReference<Bundle>();
    	bundleContext.addServiceListener(
    			new ServiceListener() {
					@Override
					public void serviceChanged(ServiceEvent event) {
						if ((event.getType() & (ServiceEvent.REGISTERED | ServiceEvent.MODIFIED)) == 0)
							return;
						if (a.get() != null)
							// We've been here before and already done what needs doing.
							return;
						ServiceReference<Subsystem> sr = (ServiceReference<Subsystem>)event.getServiceReference();
						Subsystem s = bundleContext.getService(sr);
						try {
							// Queue up the installed event.
							a.set(core.getBundleContext().installBundle(BUNDLE_A, new FileInputStream(BUNDLE_A)));
							// Ensure the bundle will be uninstalled before the event is processed.
							a.get().uninstall();
						}
						catch (Exception e) {
							e.printStackTrace();
						}
					}
    			}, 
    			"(&(objectClass=org.osgi.service.subsystem.Subsystem)(subsystem.id=0)(subsystem.state=RESOLVED))");
    	try {
    		// Before the fix, this would fail due to an NPE resulting from a
    		// null bundle revision.
    		core.start();
    	}
    	catch (BundleException e) {
    		e.printStackTrace();
    		fail("Subsystems failed to handle an asynchronous bundle installed event after the bundle was uninstalled");
    	}
    	assertBundleState(a.get(), Bundle.UNINSTALLED);
    	Subsystem root = getRootSubsystem();
    	assertState(Subsystem.State.ACTIVE, root);
    	assertNotConstituent(root, a.get().getSymbolicName());
    }
    
    /*
     * Because bundle events are queued for later asynchronous processing while
     * the root subsystem is initializing, it is possible to see an installed
     * event whose origin bundle has been uninstalled (i.e. the origin bundle's
     * revision will be null). These events should result in the installed
     * bundle being associated with the root subsystem.
     */
    @Test
    public void testIgnoreUninstalledOriginBundleInAsyncInstalledEvent() throws Exception {
    	final Bundle core = getSubsystemCoreBundle();
    	core.stop();
    	final Bundle b = bundleContext.installBundle(BUNDLE_B, new FileInputStream(BUNDLE_B));
    	// Ensure bundle B has a context.
    	b.start();
    	final AtomicReference<Bundle> a = new AtomicReference<Bundle>();
    	bundleContext.addServiceListener(
    			new ServiceListener() {
					@Override
					public void serviceChanged(ServiceEvent event) {
						if ((event.getType() & (ServiceEvent.REGISTERED | ServiceEvent.MODIFIED)) == 0)
							return;
						if (a.get() != null)
							// We've been here before and already done what needs doing.
							return;
						ServiceReference<Subsystem> sr = (ServiceReference<Subsystem>)event.getServiceReference();
						Subsystem s = bundleContext.getService(sr);
						try {
							// Queue up the installed event for bundle A using B's context.
							a.set(b.getBundleContext().installBundle(BUNDLE_A, new FileInputStream(BUNDLE_A)));
							// Ensure the origin bundle will be uninstalled before the event is processed.
							b.uninstall();
						}
						catch (Exception e) {
							e.printStackTrace();
						}
					}
    			}, 
    			"(&(objectClass=org.osgi.service.subsystem.Subsystem)(subsystem.id=0)(subsystem.state=RESOLVED))");
    	try {
    		// Before the fix, this would fail due to an NPE resulting from a
    		// null bundle revision.
    		core.start();
    	}
    	catch (BundleException e) {
    		e.printStackTrace();
    		fail("Subsystems failed to handle an asynchronous bundle installed event after the origin bundle was uninstalled");
    	}
    	assertBundleState(a.get(), Bundle.INSTALLED);
    	assertBundleState(b, Bundle.UNINSTALLED);
    	Subsystem root = getRootSubsystem();
    	assertState(Subsystem.State.ACTIVE, root);
    	assertConstituent(root, a.get().getSymbolicName());
    }
}
