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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.util.Map;

import org.apache.aries.blueprint.BlueprintConstants;
import org.apache.aries.blueprint.sample.Foo;
import org.apache.aries.blueprint.sample.FooRegistrationListener;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.service.blueprint.container.BlueprintContainer;

@RunWith(JUnit4TestRunner.class)
public class TestRegistrationListener extends AbstractIntegrationTest {

    @Test
    public void testWithAutoExportEnabled() throws Exception {

        BlueprintContainer blueprintContainer = getBlueprintContainerForBundle("org.apache.aries.blueprint.sample");
        assertNotNull(blueprintContainer);

        Foo foo = getOsgiService(Foo.class, "(" + BlueprintConstants.COMPONENT_NAME_PROPERTY + "=foo)", DEFAULT_TIMEOUT);
        assertEquals(5, foo.getA());

        FooRegistrationListener listener = 
            (FooRegistrationListener) blueprintContainer.getComponentInstance("fooRegistrationListener");

        // If registration listener works fine, the registration method should
        // have already been called and properties that were passed to this
        // method should have been not null

        Map props = listener.getProperties();
        assertNotNull(props);

        assertTrue(props.containsKey(BlueprintConstants.COMPONENT_NAME_PROPERTY));
        assertEquals("foo", props.get(BlueprintConstants.COMPONENT_NAME_PROPERTY));

        assertTrue(props.containsKey("key"));
        assertEquals("value", props.get("key"));

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

                // this is how you set the default log level when using pax
                // logging (logProfile)
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),

                // Bundles
                mavenBundle("org.apache.aries", "org.apache.aries.util"), 
                mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint"), 
                mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.sample"),
                mavenBundle("org.osgi", "org.osgi.compendium"),

                // org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption("-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),

                equinox().version("3.5.0"));
        options = updateOptions(options);
        return options;
    }

}
