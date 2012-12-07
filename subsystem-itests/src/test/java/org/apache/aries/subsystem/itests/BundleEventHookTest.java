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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.MavenConfiguredJUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;

@RunWith(MavenConfiguredJUnit4TestRunner.class)
public class BundleEventHookTest extends SubsystemTest {
    /*
	 * Bundle-SymbolicName: bundle.a.jar
	 */
	private static final String BUNDLE_A = "bundle.a.jar";
	
	@Before
	public static void createApplications() throws Exception {
		if (createdApplications) {
			return;
		}
		createBundleA();
		createdApplications = true;
	}
	
	private static void createBundleA() throws IOException {
		createBundle(BUNDLE_A);
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
}
