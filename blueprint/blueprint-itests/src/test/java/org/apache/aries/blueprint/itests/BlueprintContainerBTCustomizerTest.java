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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.framework.CompositeBundle;

/**
 * This test is based on the BlueprintContainerTest.  The difference is that in this test,
 * the blueprint sample is installed into a child framework that is associated with a composite
 * bundle created from CompositeBundleFactory.  This test only runs when the CompositeBundleFactory 
 * service is avail in the OSGi service registry.
 *
 */
@SuppressWarnings("deprecation")
@RunWith(PaxExam.class)
public class BlueprintContainerBTCustomizerTest extends BaseBlueprintContainerBTCustomizerTest {

    @Test
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

        // do the test
        Helper.testBlueprintContainer(new RichBundleContext(compositeBundleContext), testBundle);
    }

	@Configuration
    public Option[] configuration() {
        return new Option[] {
            baseOptions(),
            Helper.blueprintBundles()
        };
    }

}
