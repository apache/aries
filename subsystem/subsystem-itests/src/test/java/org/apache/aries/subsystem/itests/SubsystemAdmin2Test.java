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
package org.apache.aries.subsystem.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Currency;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.aries.subsystem.Subsystem;
import org.apache.aries.subsystem.SubsystemAdmin;
import org.apache.aries.unittest.fixture.ArchiveFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.JarFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.ZipFixture;

import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.container.def.PaxRunnerOptions;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.composite.CompositeAdmin;


@RunWith(JUnit4TestRunner.class)
public class SubsystemAdmin2Test extends AbstractIntegrationTest {

    /* Use @Before not @BeforeClass so as to ensure that these resources
     * are created in the paxweb temp directory, and not in the svn tree 
     */
    static boolean createdSubsystems = false;
    static boolean createdBundles = false;
    static CompositeAdmin ca = null;
    static SubsystemAdmin sa = null;

    @Before
    public void createSubsystems() throws Exception {
      if (createdSubsystems) { 
        return;
      }

      createSubsystem("subsystem2/META-INF/MANIFEST.MF", "subsystem2.eba");
      createSubsystem("subsystem3/META-INF/MANIFEST.MF", "subsystem3.eba");
      
      createdSubsystems = true;
    }

    private Future<Subsystem> installSubsystem(String filename) throws Exception {

        // make sure we are using a framework that provides composite admin service
        ca = getOsgiService(CompositeAdmin.class);
        assertNotNull("Unable to get CompositeAdmin Service", ca);
        System.out.println("Able to get composite admin service");
        
        // obtain subsystem admin service
        sa = getOsgiService(SubsystemAdmin.class);
        assertNotNull("Unable to get SubsystemAdmin Service", sa);
        System.out.println("Able to get subsytem admin service");

        File f = new File(filename);
        return sa.install(f.toURI().toURL().toExternalForm());        
    }
    
    private Future<Subsystem> updateSubsystem(Subsystem subsystem, String filename) throws Exception {

        // make sure we are using a framework that provides composite admin service
        ca = getOsgiService(CompositeAdmin.class);
        assertNotNull("Unable to get CompositeAdmin Service", ca);
        System.out.println("Able to get composite admin service");
        
        // obtain subsystem admin service
        sa = getOsgiService(SubsystemAdmin.class);
        assertNotNull("Unable to get SubsystemAdmin Service", sa);
        System.out.println("Able to get subsytem admin service");

        File f = new File(filename);
        return sa.update(subsystem, new FileInputStream(f));        
    }
    
    private void createSubsystem(String manifest, String filename) throws Exception {
        ZipFixture testEba = ArchiveFixture.newZip();
        
        testEba = testEba.binary("META-INF/MANIFEST.MF", 
                SubsystemAdminTest.class.getClassLoader().getResourceAsStream(manifest))
            .end();
        FileOutputStream fout = new FileOutputStream(filename);
        testEba.writeOut(fout);
        fout.close();
    }
    
    @Test
    public void testInstall() throws Exception {

        Future<Subsystem> subsystemFuture = installSubsystem("subsystem2.eba");
        

        // TODO: when based on ScopeAdmin we can remove this sleep. Currently
        // install of the contents is async wrt Subsystem creation so the
        // contents are note installed when we test for them unless we wait.
        Thread.sleep(10000);
        
        // Give the Future up to 10 seconds to do its thing.
        Subsystem subsystem = subsystemFuture.get(10, TimeUnit.SECONDS);

        assertNotNull("Subsystem should not be null", subsystem);
        
        assertTrue("Subsystem should have a unique id", subsystem.getSubsystemId() > 0);
        assertTrue(subsystem.getLocation().indexOf("subsystem2.eba") != -1);
        assertEquals("felix-file-install2", subsystem.getSymbolicName());
        assertEquals("2.0.8", subsystem.getVersion().toString());
        Collection<Bundle> constituents = subsystem.getConstituents();
        assertEquals("check constituents' size", 1, constituents.size());

        
    }
    
    @Test
    public void testUninstall() throws Exception {

        // Note, if the subsystem is already installed, then this is a no-op
        Future<Subsystem> subsystemFuture = installSubsystem("subsystem2.eba");
        
        // TODO: when based on ScopeAdmin we can remove this sleep. Currently
        // install of the contents is async wrt Subsystem creation so the
        // contents are note installed when we test for them unless we wait.
        Thread.sleep(10000);
        
        // Give the Future up to 10 seconds to do its thing.
        Subsystem subsystem = subsystemFuture.get(10, TimeUnit.SECONDS);

        assertNotNull("Subsystem should not be null", subsystem);
        
        String name = subsystem.getSymbolicName();
        Version version = subsystem.getVersion();
        
        sa.uninstall(subsystem);
        
        assertEquals("Check state is UNINSTALLED", Subsystem.State.UNINSTALLED, subsystem.getState());
        
        subsystem = sa.getSubsystem(name, version);
        
        assertNull("Returned Subsystem should be null", subsystem);
    }
    
    @Test
    public void testUpdate() throws Exception {

        // Note, if the subsystem is already installed, then this is a no-op
        Future<Subsystem> installFuture = installSubsystem("subsystem2.eba");
        
        // TODO: when based on ScopeAdmin we can remove this sleep. Currently
        // install of the contents is async wrt Subsystem creation so the
        // contents are note installed when we test for them unless we wait.
        Thread.sleep(10000);
        
        // Give the Future up to 10 seconds to do its thing.
        Subsystem subsystem = installFuture.get(10, TimeUnit.SECONDS);

        assertNotNull("Subsystem should not be null", subsystem);

        /*
         * Test an update that adds another bundle to the content
         */
        
        // Update the subsystem
        Future<Subsystem> updateFuture = updateSubsystem(subsystem, "subsystem3.eba");
        
        // TODO: when based on ScopeAdmin we can remove this sleep. Currently
        // install of the contents is async wrt Subsystem creation so the
        // contents are note installed when we test for them unless we wait.
        Thread.sleep(10000);
        
        // Give the Future up to 10 seconds to do its thing.
        subsystem = updateFuture.get(10, TimeUnit.SECONDS);
        
        Collection<Bundle> constituents = subsystem.getConstituents();
        assertEquals("Check constituents' size", 2, constituents.size());

        /*
         * Test an update that removes a bundle to the content
         */

        // Update the subsystem
        updateFuture = updateSubsystem(subsystem, "subsystem2.eba");
        
        // TODO: when based on ScopeAdmin we can remove this sleep. Currently
        // install of the contents is async wrt Subsystem creation so the
        // contents are note installed when we test for them unless we wait.
        Thread.sleep(10000);
        
        // Give the Future up to 10 seconds to do its thing.
        subsystem = updateFuture.get(10, TimeUnit.SECONDS);
        
        constituents = subsystem.getConstituents();
        assertEquals("Check constituents' size", 1, constituents.size());
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
            mavenBundle("org.apache.aries.subsystem", "org.apache.aries.subsystem.executor"),
            mavenBundle("org.apache.aries.testsupport", "org.apache.aries.testsupport.unit"),
            mavenBundle("org.apache.aries.application", "org.apache.aries.application.api"),
            mavenBundle("org.apache.aries", "org.apache.aries.util"),
            mavenBundle("org.apache.aries.application", "org.apache.aries.application.utils"),
            mavenBundle("org.apache.felix", "org.apache.felix.bundlerepository"),
            mavenBundle("org.apache.aries.subsystem", "org.apache.aries.subsystem.api"),
            mavenBundle("org.apache.aries.subsystem", "org.apache.aries.subsystem.core"),

            //org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption("-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),

            PaxRunnerOptions.rawPaxRunnerOption("config", "classpath:ss-runner.properties"),

            equinox().version("v43prototype-3.6.0.201003231329")
        );
        options = updateOptions(options);
        return options;
    }

}
