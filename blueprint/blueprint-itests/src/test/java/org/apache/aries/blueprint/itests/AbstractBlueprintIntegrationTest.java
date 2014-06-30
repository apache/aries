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
package org.apache.aries.blueprint.itests;

import static org.apache.aries.blueprint.itests.Helper.mvnBundle;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.CoreOptions.when;

import java.io.InputStream;
import java.util.Hashtable;

import org.apache.aries.itest.AbstractIntegrationTest;
import org.apache.aries.itest.RichBundleContext;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Base class for Pax Exam 1.2.x based unit tests
 * 
 * Contains the injection point and various utilities used in most tests
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public abstract class AbstractBlueprintIntegrationTest extends AbstractIntegrationTest {
    public static final long DEFAULT_TIMEOUT = 15000;

    protected BlueprintContainer startBundleBlueprint(String symbolicName) throws BundleException {
        Bundle b = context().getBundleByName(symbolicName);
        assertNotNull("Bundle " + symbolicName + " not found", b);
        b.start();
        BlueprintContainer beanContainer = Helper.getBlueprintContainerForBundle(context(), symbolicName);
        assertNotNull(beanContainer);
        return beanContainer;
    }
    
    public Option baseOptions() {
        String localRepo = System.getProperty("maven.repo.local");
        if (localRepo == null) {
            localRepo = System.getProperty("org.ops4j.pax.url.mvn.localRepository");
        }
        return composite(
                junitBundles(),
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),
                when(localRepo != null).useOptions(vmOption("-Dorg.ops4j.pax.url.mvn.localRepository=" + localRepo)),
                mvnBundle("org.ops4j.pax.logging", "pax-logging-api"),
                mvnBundle("org.ops4j.pax.logging", "pax-logging-service")
         );
    }
    
    public InputStream getResource(String path) {
    	InputStream is = this.getClass().getClassLoader().getResourceAsStream(path);
    	if (is == null) {
    		throw new IllegalArgumentException("Resource not found " + path);
    	}
    	return is;
    }

	protected void applyCommonConfiguration(BundleContext ctx) throws Exception {
	    ConfigurationAdmin ca = (new RichBundleContext(ctx)).getService(ConfigurationAdmin.class);        
	    Configuration cf = ca.getConfiguration("blueprint-sample-placeholder", null);
	    Hashtable<String, String> props = new Hashtable<String, String>();
	    props.put("key.b", "10");
	    cf.update(props);
	}

	protected Bundle getSampleBundle() {
		Bundle bundle = context().getBundleByName("org.apache.aries.blueprint.sample");
		assertNotNull(bundle);
		return bundle;
	}

	protected MavenArtifactProvisionOption sampleBundleOption() {
		return CoreOptions.mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.sample").versionAsInProject();
	}

	protected void startBlueprintBundles() throws BundleException,
			InterruptedException {
			    context().getBundleByName("org.apache.aries.blueprint.core").start();
			    context().getBundleByName("org.apache.aries.blueprint.cm").start();
			    Thread.sleep(2000);
			}
}
