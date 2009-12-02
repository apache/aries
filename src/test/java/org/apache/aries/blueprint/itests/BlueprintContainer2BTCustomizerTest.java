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
package org.apache.aries.blueprint.itests;

import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.framework.CompositeBundle;
import org.osgi.service.framework.CompositeBundleFactory;

/**
 * This test is based on the BlueprintContainerBTCustomizerTest.  but this test starts the
 * blueprint sample before the blueprint bundle is started so going a slightly 
 * different code path
 *
 */
@RunWith(JUnit4TestRunner.class)
public class BlueprintContainer2BTCustomizerTest extends AbstractIntegrationTest {

    @Test
    public void test() throws Exception {
        // Create a config to check the property placeholder
        ConfigurationAdmin ca = getOsgiService(ConfigurationAdmin.class);
        Configuration cf = ca.getConfiguration("blueprint-sample-placeholder", null);
        Hashtable props = new Hashtable();
        props.put("key.b", "10");
        cf.update(props);

        
        ServiceReference sr = bundleContext.getServiceReference("org.osgi.service.framework.CompositeBundleFactory");
        if (sr != null) {
             // install blueprint.sample into the composite context
            CompositeBundleFactory cbf = (CompositeBundleFactory)bundleContext.getService(sr);
            
            Map<String, String> frameworkConfig = new HashMap<String, String>();
            // turn on the line below to enable telnet localhost 10000 to the child framework osgi console
            // frameworkConfig.put("osgi.console", "10000");
            
            // construct composite bundle information
            Map<String, String> compositeManifest = new HashMap<String, String>();
            compositeManifest.put(Constants.BUNDLE_SYMBOLICNAME, "test-composite");
            compositeManifest.put(Constants.BUNDLE_VERSION, "1.0.0");
            // this import-package is used by the blueprint.sample
            compositeManifest.put(Constants.IMPORT_PACKAGE, "org.osgi.service.blueprint.container");
            // this export-package is used by pax junit runner as it needs to see the blueprint sample package 
            // for the test after the blueprint sample is started.
            compositeManifest.put(Constants.EXPORT_PACKAGE, "org.apache.aries.blueprint.sample");
            
            CompositeBundle cb = cbf.installCompositeBundle(frameworkConfig, "test-composite", compositeManifest);

            BundleContext compositeBundleContext = cb.getCompositeFramework().getBundleContext();
            // install the blueprint sample onto the framework associated with the composite bundle
            MavenArtifactProvisionOption mapo = CoreOptions.mavenBundle().groupId("org.apache.aries.blueprint").artifactId("org.apache.aries.blueprint.sample").version( "1.0.0-incubating-SNAPSHOT");
            // let's use input stream to avoid invoking mvn url handler which isn't avail in the child framework.
            InputStream is = new URL(mapo.getURL()).openStream();
            Bundle bundle = compositeBundleContext.installBundle(mapo.getURL(), is);
            assertNotNull(bundle);
            
            // start the composite bundle then the blueprint sample
            cb.start();
            bundle.start();
            
            // start the blueprint bundle and it should detect the previously started blueprint sample
            Bundle blueprintBundle = getInstalledBundle("org.apache.aries.blueprint");
            blueprintBundle.start();
            //Thread.sleep(5000);

            // do the test
            testBlueprintContainer(compositeBundleContext, bundle);
            
        }
    }

    @org.ops4j.pax.exam.junit.Configuration
    public static Option[] configuration() {
        Option[] options = options(
            // Log
            mavenBundle("org.ops4j.pax.logging", "pax-logging-api"),
            mavenBundle("org.ops4j.pax.logging", "pax-logging-service"),
            // Felix Config Admin
            mavenBundle("org.apache.felix", "org.apache.felix.configadmin"),
            // Felix mvn url handler
            mavenBundle("org.ops4j.pax.url", "pax-url-mvn"),


            // this is how you set the default log level when using pax logging (logProfile)
            systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("DEBUG"),

            // Bundles
            mavenBundle("org.apache.aries", "org.apache.aries.util"),
            mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint").noStart(),
            // don't install the blueprint sample here as it will be installed onto the same framework as the blueprint core bundle
            // mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.sample").noStart(),
            mavenBundle("org.osgi", "org.osgi.compendium"),
            // org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption("-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),

            equinox().version("3.5.0")
        );
        options = updateOptions(options);
        return options;
    }

}
