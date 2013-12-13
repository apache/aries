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
import java.util.Hashtable;

import org.apache.aries.itest.AbstractIntegrationTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import static org.apache.aries.itest.ExtraOptions.*;

/**
 * this test is based on blueprint container test, but this test starts the
 * blueprint sample before the blueprint bundle is started so going a slightly 
 * different code path
 *
 */
@RunWith(JUnit4TestRunner.class)
public class BlueprintContainer2Test extends AbstractIntegrationTest {

    @Test
    public void test() throws Exception {
        // Create a config to check the property placeholder
        ConfigurationAdmin ca = context().getService(ConfigurationAdmin.class);
        Configuration cf = ca.getConfiguration("blueprint-sample-placeholder", null);
        Hashtable props = new Hashtable();
        props.put("key.b", "10");
        cf.update(props);

        Bundle bundle = context().getBundleByName("org.apache.aries.blueprint.sample");
        Bundle blueprintBundle = context().getBundleByName("org.apache.aries.blueprint.core");
        Bundle blueprintCMBundle = context().getBundleByName("org.apache.aries.blueprint.cm");
        assertNotNull(bundle);
        assertNotNull(bundle);

        bundle.start();
        blueprintBundle.start();
        blueprintCMBundle.start();
        
        // do the test
        Helper.testBlueprintContainer(context(), bundle);
    }

    @org.ops4j.pax.exam.junit.Configuration
    public static Option[] configuration() {
        return testOptions(
            paxLogging("DEBUG"),
            Helper.blueprintBundles(false),
            
            mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.sample").noStart(),

            equinox().version("3.5.0")
        );
    }

}
