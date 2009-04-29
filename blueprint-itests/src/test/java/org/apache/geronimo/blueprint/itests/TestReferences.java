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
package org.apache.geronimo.blueprint.itests;

import java.util.Hashtable;
import java.util.List;

import org.apache.geronimo.blueprint.sample.BindingListener;
import org.apache.geronimo.blueprint.sample.InterfaceA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.knopflerfish;
import static org.ops4j.pax.exam.CoreOptions.mavenConfiguration;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.configProfile;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.logProfile;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.profile;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.context.BlueprintContext;
import org.osgi.service.blueprint.context.ServiceUnavailableException;

@RunWith(JUnit4TestRunner.class)
public class TestReferences extends AbstractIntegrationTest {

    @Test
    public void testUnaryReference() throws Exception {
        BlueprintContext blueprintContext = getBlueprintContextForBundle("blueprint-sample");
        assertNotNull(blueprintContext);

        BindingListener listener = (BindingListener) blueprintContext.getComponent("bindingListener");
        assertNull(listener.getA());
        assertNull(listener.getReference());

        InterfaceA a = (InterfaceA) blueprintContext.getComponent("ref2");
        try {
            a.hello("world");
            fail("A ServiceUnavailableException should have been thrown");
        } catch (ServiceUnavailableException e) {
            // Ignore, expected
        }

        ServiceRegistration reg1 = bundleContext.registerService(InterfaceA.class.getName(), new InterfaceA() {
            public String hello(String msg) {
                return "Hello " + msg + "!";
            }
        }, null);
        assertNotNull(listener.getA());
        assertNotNull(listener.getReference());
        assertEquals("Hello world!", a.hello("world"));

        Hashtable props = new Hashtable();
        props.put(Constants.SERVICE_RANKING, Integer.valueOf(1));
        ServiceRegistration reg2 = bundleContext.registerService(InterfaceA.class.getName(), new InterfaceA() {
            public String hello(String msg) {
                return "Good morning " + msg + "!";
            }
        }, props);
        assertNotNull(listener.getA());
        assertNotNull(listener.getReference());
        assertEquals("Good morning world!", a.hello("world"));

        reg2.unregister();
        assertNotNull(listener.getA());
        assertNotNull(listener.getReference());
        assertEquals("Hello world!", a.hello("world"));

        reg1.unregister();
        assertNull(listener.getA());
        assertNull(listener.getReference());
        try {
            a.hello("world");
            fail("A ServiceUnavailableException should have been thrown");
        } catch (ServiceUnavailableException e) {
            // Ignore, expected
        }
    }

    @Test
    public void testListReferences() throws Exception {
        BlueprintContext blueprintContext = getBlueprintContextForBundle("blueprint-sample");
        assertNotNull(blueprintContext);

        BindingListener listener = (BindingListener) blueprintContext.getComponent("listBindingListener");
        assertNull(listener.getA());
        assertNull(listener.getReference());

        List refs = (List) blueprintContext.getComponent("ref-list");
        assertNotNull(refs);
        assertTrue(refs.isEmpty());

        ServiceRegistration reg1 = bundleContext.registerService(InterfaceA.class.getName(), new InterfaceA() {
            public String hello(String msg) {
                return "Hello " + msg + "!";
            }
        }, null);
        assertNotNull(listener.getA());
        assertNotNull(listener.getReference());
        assertEquals(1, refs.size());
        InterfaceA a = (InterfaceA) refs.get(0);
        assertNotNull(a);
        assertEquals("Hello world!", a.hello("world"));

    }

    @org.ops4j.pax.exam.junit.Configuration
    public static Option[] configuration() {
        Option[] options = options(
            // install log service using pax runners profile abstraction (there are more profiles, like DS)
            logProfile(),
            configProfile(),
            profile("url"),

            // this is how you set the default log level when using pax logging (logProfile)
            systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),

            // Bundles
            mavenBundle("org.apache.geronimo", "blueprint-bundle"),
            mavenBundle("org.apache.geronimo", "blueprint-sample"),

            felix(), equinox() //, knopflerfish()
        );

        // use config generated by the Maven plugin (until PAXEXAM-62/64 get resolved)
        if (TestBlueprintContext.class.getClassLoader().getResource("META-INF/maven/paxexam-config.args") != null) {
            options = OptionUtils.combine(options, mavenConfiguration());
        }

        return options;
    }

}