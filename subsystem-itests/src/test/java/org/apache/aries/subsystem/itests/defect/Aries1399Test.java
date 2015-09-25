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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.aries.subsystem.itests.SubsystemTest;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.subsystem.Subsystem;

import aQute.bnd.osgi.Constants;

/*
 * https://issues.apache.org/jira/browse/ARIES-1399
 * 
 * Trunk fails OSGi R6 CT
 */
public class Aries1399Test extends SubsystemTest {
    /*
	 * Bundle-SymbolicName: bundle.a.jar
	 */
	private static final String BUNDLE_A = "bundle.a.jar";
	
	private static boolean createdTestFiles;
	
	@Before
	public void createTestFiles() throws Exception {
		if (createdTestFiles)
			return;
		createBundleA();
		createdTestFiles = true;
	}
	
	private void createBundleA() throws IOException {
		createBundle(name(BUNDLE_A));
	}
	
	@Test 
    public void testBundleEventOrder() throws Exception {
    	Subsystem root = getRootSubsystem();
    	BundleContext context = root.getBundleContext();
    	final List<BundleEvent> events = Collections.synchronizedList(new ArrayList<BundleEvent>());
    	context.addBundleListener(
    			new SynchronousBundleListener() {
					@Override
					public void bundleChanged(BundleEvent event) {
						events.add(event);
					}
    			});
    	Bundle bundle = context.installBundle(
    			"bundle", 
    			TinyBundles.bundle().set(Constants.BUNDLE_SYMBOLICNAME, "bundle").build());
    	try {
    		bundle.start();
    		// INSTALLED, RESOLVED, STARTING, STARTED
    		assertEquals(4, events.size());
    		assertEquals(BundleEvent.INSTALLED, events.get(0).getType());
    		assertEquals(BundleEvent.RESOLVED, events.get(1).getType());
    		assertEquals(BundleEvent.STARTING, events.get(2).getType());
    		assertEquals(BundleEvent.STARTED, events.get(3).getType());
    	}
    	finally {
    		uninstallSilently(bundle);
    	}
    }
}
