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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.itest;

import javax.inject.Inject;

import org.osgi.framework.BundleContext;

/**
 * Base class for Pax Exam 1.2.x based unit tests
 * 
 * Contains the injection point and various utilities used in most tests
 */
public abstract class AbstractIntegrationTest {

    /** Gateway to the test OSGi framework */
    @Inject
    protected BundleContext bundleContext;
    
    /**
     * Get a richer version of {@link BundleContext}
     */
    public RichBundleContext context() {
        return new RichBundleContext(bundleContext);
    }
    
    public String getLocalRepo() {
    	String localRepo = System.getProperty("maven.repo.local");
    	if (localRepo == null) {
    		localRepo = System.getProperty("org.ops4j.pax.url.mvn.localRepository");
    	}
    	return localRepo;
    }
}
