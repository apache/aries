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

import org.apache.aries.itest.RichBundleContext;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.framework.CompositeBundle;

/**
 * This test is based on the BlueprintContainerBTCustomizerTest.  but this test starts the
 * blueprint sample before the blueprint bundle is started so going a slightly 
 * different code path
 *
 */
@SuppressWarnings("deprecation")
@RunWith(PaxExam.class)
public class BlueprintContainer2BTCustomizerTest extends BaseBlueprintContainerBTCustomizerTest {

	@Test
    @Ignore // This test crashes the vm when run from maven. It works fine when run from eclipse
    public void test() throws Exception {
        CompositeBundle cb = createCompositeBundle();
        BundleContext compositeBundleContext = cb.getCompositeFramework().getBundleContext();
        Bundle testBundle = installBundle(compositeBundleContext, sampleBundleOption().getURL());
        Bundle configAdminBundle = installBundle(compositeBundleContext, configAdminOption().getURL());
        
        // start the composite bundle, config admin then the blueprint sample
        cb.start();
        configAdminBundle.start();
        // create a config to check the property placeholder
        applyCommonConfiguration(compositeBundleContext);
        testBundle.start();

        startBlueprintBundles();

        // do the test
        Helper.testBlueprintContainer(new RichBundleContext(compositeBundleContext), testBundle);
    }

    @Configuration
    public Option[] configuration() {
        return new Option[] {
            baseOptions(),
            Helper.blueprintBundles(false)
        };
    }

}
