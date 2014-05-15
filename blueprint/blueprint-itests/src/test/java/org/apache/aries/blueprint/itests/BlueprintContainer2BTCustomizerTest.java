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

import java.util.HashMap;
import java.util.Map;

import org.apache.aries.itest.RichBundleContext;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.framework.CompositeBundle;
import org.osgi.service.framework.CompositeBundleFactory;

/**
 * This test is based on the BlueprintContainerBTCustomizerTest.  but this test starts the
 * blueprint sample before the blueprint bundle is started so going a slightly 
 * different code path
 *
 */
@SuppressWarnings("deprecation")
@RunWith(PaxExam.class)
public class BlueprintContainer2BTCustomizerTest extends BaseBlueprintContainerBTCustomizerTest {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    @Ignore // This test crashes the vm when run from maven. It works fine when run from eclipse
    public void test() throws Exception {
        
        ServiceReference sr = bundleContext.getServiceReference("org.osgi.service.framework.CompositeBundleFactory");
        if (sr == null) {
            return;
        }

        // install blueprint.sample into the composite context
        CompositeBundleFactory cbf = (CompositeBundleFactory)bundleContext.getService(sr);
        
        Map<String, String> frameworkConfig = new HashMap<String, String>();
        // turn on the line below to enable telnet localhost 10000 to the child framework osgi console
        // frameworkConfig.put("osgi.console", "10000");
        
        // construct composite bundle information
        Map<String, String> compositeManifest = getCompositeManifest();
        
        CompositeBundle cb = cbf.installCompositeBundle(frameworkConfig, "test-composite", compositeManifest);

        BundleContext compositeBundleContext = cb.getCompositeFramework().getBundleContext();
        Bundle bundle = installTestBundle(compositeBundleContext);
        assertNotNull(bundle);
        // install and start the cfg admin bundle in the isolated framework
        Bundle configAdminBundle = installConfigurationAdmin(compositeBundleContext);
        assertNotNull(configAdminBundle);
        
        // start the composite bundle, config admin then the blueprint sample
        cb.start();
        configAdminBundle.start();
        // create a config to check the property placeholder
        applyCommonConfiguration(compositeBundleContext);
        bundle.start();

        startBlueprintBundles();

        // do the test
        Helper.testBlueprintContainer(new RichBundleContext(compositeBundleContext), bundle);
        
        // unget the service
        bundleContext.ungetService(sr);
    }

    // start the blueprint bundle and it should detect the previously started blueprint sample
    private void startBlueprintBundles() throws BundleException, InterruptedException {
        context().getBundleByName("org.apache.aries.blueprint.core").start();
        context().getBundleByName("org.apache.aries.blueprint.cm").start();
        Thread.sleep(2000);
    }

    @Configuration
    public Option[] configuration() {
        return new Option[] {
            baseOptions(),
            Helper.blueprintBundles(false)
        };
    }

}
