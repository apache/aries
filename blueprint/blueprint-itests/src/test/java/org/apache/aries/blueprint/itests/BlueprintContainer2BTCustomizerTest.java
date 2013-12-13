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
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.itest.RichBundleContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.framework.CompositeBundle;
import org.osgi.service.framework.CompositeBundleFactory;

import static org.apache.aries.itest.ExtraOptions.*;

/**
 * This test is based on the BlueprintContainerBTCustomizerTest.  but this test starts the
 * blueprint sample before the blueprint bundle is started so going a slightly 
 * different code path
 *
 */
@RunWith(JUnit4TestRunner.class)
public class BlueprintContainer2BTCustomizerTest extends BaseBlueprintContainerBTCustomizerTest {

    @Test
    public void test() throws Exception {
        
        ServiceReference sr = bundleContext.getServiceReference("org.osgi.service.framework.CompositeBundleFactory");
        if (sr != null) {
             // install blueprint.sample into the composite context
            CompositeBundleFactory cbf = (CompositeBundleFactory)bundleContext.getService(sr);
            
            Map<String, String> frameworkConfig = new HashMap<String, String>();
            // turn on the line below to enable telnet localhost 10000 to the child framework osgi console
            // frameworkConfig.put("osgi.console", "10000");
            
            // construct composite bundle information
            Map<String, String> compositeManifest = getCompositeManifest();
            
            CompositeBundle cb = cbf.installCompositeBundle(frameworkConfig, "test-composite", compositeManifest);

            BundleContext compositeBundleContext = cb.getCompositeFramework().getBundleContext();
            // install the blueprint sample onto the framework associated with the composite bundle
            MavenArtifactProvisionOption mapo = mavenBundleInTest(getClass().getClassLoader(), "org.apache.aries.blueprint", "org.apache.aries.blueprint.sample");
            // let's use input stream to avoid invoking mvn url handler which isn't avail in the child framework.
            InputStream is = new URL(mapo.getURL()).openStream();
            Bundle bundle = compositeBundleContext.installBundle(mapo.getURL(), is);
            assertNotNull(bundle);
            // install and start the cfg admin bundle in the isolated framework
            Bundle cfgAdminBundle = installConfigurationAdmin(compositeBundleContext);
            assertNotNull(cfgAdminBundle);            
            
            // start the composite bundle, the config admin then the blueprint sample
            cb.start();
            cfgAdminBundle.start();         
            // create a config to check the property placeholder
            applyCommonConfiguration(compositeBundleContext);
            bundle.start();
            
            // start the blueprint bundle and it should detect the previously started blueprint sample
            Bundle blueprintBundle = context().getBundleByName("org.apache.aries.blueprint.core");
            blueprintBundle.start();
            Bundle blueprintCMBundle = context().getBundleByName("org.apache.aries.blueprint.cm");
            blueprintCMBundle.start();
            Thread.sleep(2000);
            
            

            // do the test
            Helper.testBlueprintContainer(new RichBundleContext(compositeBundleContext), bundle);
            
            // unget the service
            bundleContext.ungetService(sr);
        }
    }

    @org.ops4j.pax.exam.junit.Configuration
    public static Option[] configuration() {
        return testOptions(
            paxLogging("DEBUG"),
            Helper.blueprintBundles(false),
            //vmOption ("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5006"),
            equinox().version("3.5.0")
        );
    }

}
