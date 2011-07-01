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
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.List;

import org.apache.aries.unittest.fixture.ArchiveFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.Fixture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

@RunWith(JUnit4TestRunner.class)
public class BlueprintContainerTest extends AbstractIntegrationTest {

    @Test
    public void test() throws Exception {
        // Create a config to check the property placeholder
        ConfigurationAdmin ca = getOsgiService(ConfigurationAdmin.class);
        Configuration cf = ca.getConfiguration("blueprint-sample-placeholder", null);
        Hashtable props = new Hashtable();
        props.put("key.b", "10");
        cf.update(props);

        Bundle bundle = getInstalledBundle("org.apache.aries.blueprint.sample");
        assertNotNull(bundle);

        bundle.start();
        
        // do the test
        testBlueprintContainer(bundle);
    }
    
    @Test
    public void testReferenceListenerDeadlock() throws Exception {
        List<Bundle> bundles = new ArrayList<Bundle>();
        int total = 10;
        for (int i=0; i<total; i++) {
            bundles.add(bundleContext.installBundle("sample"+i, getTestBundle(i, total)));
        }
        
        for (Bundle b : bundles) b.start();
        
        // every blueprint container should be up
        for (Bundle b : bundles) {
          assertNotNull(getBlueprintContainerForBundle(b.getSymbolicName()));
        }
    }
    
    private InputStream getTestBundle(int no, int total) throws Exception {
        StringBuilder blueprint = new StringBuilder();
        blueprint.append("<blueprint xmlns=\"http://www.osgi.org/xmlns/blueprint/v1.0.0\">");
        blueprint.append("<bean id=\"listener\" class=\"org.apache.aries.blueprint.itests.comp.Listener\" />");
        
        for (int i=0; i<total; i++) {
            if (i==no) {
                blueprint.append("<service interface=\"java.util.List\">");
                blueprint.append("<service-properties><entry key=\"no\" value=\""+i+"\" /></service-properties>");
                blueprint.append("<bean class=\"org.apache.aries.blueprint.itests.comp.ListFactory\" factory-method=\"create\">");
                blueprint.append("<argument value=\""+i+"\" />");
                blueprint.append("</bean>");
                blueprint.append("</service>");
            } else {
                blueprint.append("<reference availability=\"optional\" id=\"ref"+i+"\" interface=\"java.util.List\" filter=\"(no="+i+")\">");
                blueprint.append("<reference-listener ref=\"listener\" bind-method=\"bind\" unbind-method=\"unbind\" />");
                blueprint.append("</reference>");
            }
        }
        blueprint.append("</blueprint>");
        
        Fixture jar = ArchiveFixture.newJar()
            .manifest().symbolicName("sample"+no)
                .attribute("Import-Package", "org.osgi.framework")
            .end()
            .binary("org/apache/aries/blueprint/itests/comp/Listener.class",
                    getClass().getClassLoader().getResourceAsStream(
                            "org/apache/aries/blueprint/itests/comp/Listener.class"))
            .binary("org/apache/aries/blueprint/itests/comp/ListFactory.class",
                    getClass().getClassLoader().getResourceAsStream(
                            "org/apache/aries/blueprint/itests/comp/ListFactory.class"))
                            
            .file("OSGI-INF/blueprint/blueprint.xml", blueprint.toString())
            .end();
        
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        jar.writeOut(bout);
        
        return new ByteArrayInputStream(bout.toByteArray());
    }
    
    @Test
    public void testDeadlock() throws Exception {
      bundleContext.registerService("java.util.Set",new HashSet<Object>(), null);
      
      Bundle bundle = getInstalledBundle("org.apache.aries.blueprint.sample");
      assertNotNull(bundle);

      bundle.start();
      
      getBlueprintContainerForBundle(bundleContext, "org.apache.aries.blueprint.sample",DEFAULT_TIMEOUT);
      
      // no actual assertions, we just don't want to deadlock
    }
    
    @Test
    public void testScheduledExecMemoryLeak() throws Exception {
        Fixture jar = ArchiveFixture.newJar()
            .manifest().symbolicName("test.bundle").end()
            .file("OSGI-INF/blueprint/blueprint.xml")
                .line("<blueprint xmlns=\"http://www.osgi.org/xmlns/blueprint/v1.0.0\">")
                .line("<reference interface=\"java.util.List\" />")
                .line("</blueprint>").end().end();
        
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        jar.writeOut(bout);
        
        Bundle b = bundleContext.installBundle("test.bundle", new ByteArrayInputStream(bout.toByteArray()));
        
        for (int i=0; i<16; i++) System.gc();
        long startFreeMemory = Runtime.getRuntime().freeMemory();
        
        // 3000 iterations on a Mac 1.6 JVM leaks 30+ mb, 2000 leaks a bit more than 20, 
        // 10000 iterations would be close to OutOfMemory however by that stage the test runs very slowly
        for (int i=0; i<3000; i++) {
            b.start();
            // give the container some time to operate, otherwise it probably won't even get to create a future
            Thread.sleep(10);
            b.stop();
        }
        
        for (int i=0; i<16; i++) System.gc();
        long endFreeMemory = Runtime.getRuntime().freeMemory();
        
        long lossage = startFreeMemory - endFreeMemory;
        assertTrue("We lost: "+lossage, lossage < 10000000);
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
            systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),

            // Bundles
            mavenBundle("org.apache.aries", "org.apache.aries.util"),
            mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy"),
            mavenBundle("asm", "asm-all"),
            mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint"),
            mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.sample").noStart(),
            mavenBundle("org.osgi", "org.osgi.compendium"),
            mavenBundle("org.apache.aries.testsupport", "org.apache.aries.testsupport.unit"),
            //org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption("-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),

            equinox().version("3.5.0")
        );
        options = updateOptions(options);
        return options;
    }

}
