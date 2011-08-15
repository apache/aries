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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.subsystem.Subsystem;

@RunWith(JUnit4TestRunner.class)
public class BasicTest extends SubsystemTest {
	/*
	 * When the subsystems implementation bundle is installed, there should be
	 * a Subsystem service available.
	 */
    @Test
    public void test1() throws Exception {
    	Bundle[] bundles = bundleContext.getBundles();
    	boolean found = false;
    	for (Bundle bundle : bundles) {
    		if ("org.apache.aries.subsystem.core".equals(bundle.getSymbolicName())) {
    			found = true;
    			break;
    		}
    	}
    	assertTrue("Subsystems implementation bundle not found", found);
    	ServiceReference<Subsystem> serviceReference = bundleContext.getServiceReference(Subsystem.class);
    	assertNotNull("Reference to subsystem service not found", serviceReference);
    	Subsystem subsystem = bundleContext.getService(serviceReference);
    	assertNotNull("Subsystem service not found", subsystem);
    }
}
