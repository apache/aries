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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.aries.subsystem.example.helloIsolation.HelloIsolation;
import org.apache.aries.subsystem.scope.InstallInfo;
import org.apache.aries.subsystem.scope.Scope;
import org.apache.aries.subsystem.scope.ScopeAdmin;
import org.apache.aries.subsystem.scope.ScopeUpdate;
import org.apache.aries.subsystem.scope.SharePolicy;
import org.apache.aries.subsystem.scope.impl.ScopeAdminServiceFactory;
import org.apache.aries.unittest.fixture.ArchiveFixture;
import org.apache.aries.unittest.fixture.ArchiveFixture.ZipFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.container.def.PaxRunnerOptions;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.Capability;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;


@RunWith(JUnit4TestRunner.class)
public class ScopeAdminTest extends AbstractIntegrationTest {

    /* Use @Before not @BeforeClass so as to ensure that these resources
     * are created in the paxweb temp directory, and not in the svn tree 
     */
    static boolean createdApplications = false;
    BundleTracker bt;
    int addEventCount;
    int modifyEventCount;
    int removeEventCount;
    
    @After
    public void tearDown() throws Exception {
        if (bt != null) {
            bt.close();
        }
    }
    
    @Test
    public void testBundleServiceIsolation() throws Exception {
        // make sure we are using a framework that provides composite admin service
        ScopeAdmin scopeAdmin = getOsgiService(ScopeAdmin.class);
        assertNotNull("scope admin should not be null", scopeAdmin);
        System.out.println("able to get scope admin service");

        bt = new BundleTracker(bundleContext, Bundle.INSTALLED | Bundle.UNINSTALLED | Bundle.ACTIVE, new BundleTrackerCustomizer() {

            public synchronized Object addingBundle(Bundle bundle, BundleEvent event) {
                if (event == null) {
                    System.out.println("ScopeAdminTest - adding Bundle: " + bundle.getSymbolicName() + " event: null");
                } else {
                    System.out.println("ScopeAdminTest - adding Bundle: " + bundle.getSymbolicName() + " event: " + event.getType());
                    addEventCount++;
                }
                return bundle;
            }

            public synchronized void modifiedBundle(Bundle bundle, BundleEvent event,
                    Object object) {
                if (event == null) {
                    System.out.println("ScopeAdminTest - modifying Bundle: " + bundle.getSymbolicName() + " event: null");
                } else {
                    System.out.println("ScopeAdminTest - modifying Bundle: " + bundle.getSymbolicName() + " event: " + event.getType());
                    modifyEventCount++;
                }
                
                
            }

            public synchronized void removedBundle(Bundle bundle, BundleEvent event,
                    Object object) {
                if (event == null) {
                    System.out.println("ScopeAdminTest - removing Bundle: " + bundle.getSymbolicName() + " event: null");
                } else {
                    System.out.println("ScopeAdminTest - removing Bundle: " + bundle.getSymbolicName() + " event: " + event.getType());
                    removeEventCount++;
                }
            }
            
        });
        bt.open();
        
        ScopeUpdate su = scopeAdmin.newScopeUpdate();
        
        ScopeUpdate childScopeUpdate = su.newChild("scope_test1");
        
        // build up installInfo object for the scope
        InstallInfo info1 = new InstallInfo(new URL("mvn:org.apache.aries.subsystem.example/org.apache.aries.subsystem.example.helloIsolation/0.3-SNAPSHOT"), "helloIsolation");
        InstallInfo info2 = new InstallInfo(new URL("mvn:org.apache.aries.subsystem.example/org.apache.aries.subsystem.example.helloIsolationRef/0.3-SNAPSHOT"), "helloIsolationRef");

        List<InstallInfo> bundlesToInstall = childScopeUpdate.getBundlesToInstall();
        bundlesToInstall.add(info1);
        bundlesToInstall.add(info2);
        
        // add bundles to be installed, based on subsystem content
        su.commit();
        
        assertEquals("add event count should be 0 since 0 bundles are installed in root scope", 0, addEventCount);
        assertEquals("modify event count should be 0", 0, modifyEventCount);
        assertEquals("remove event count should be 0", 0, removeEventCount);
        // start all bundles in the scope scope_test1
        Collection<Bundle> bundlesToStart = childScopeUpdate.getBundles();
        for (Bundle b : bundlesToStart) {
            b.start();
            
        }
        
        assertEquals("add event count should be 0 since 0 bundles are installed in root scope", 0, addEventCount);
        assertEquals("modify event count should be 0", 0, modifyEventCount);
        assertEquals("remove event count should be 0", 0, removeEventCount);
        
        try {
            ServiceReference sr = bundleContext.getServiceReference(HelloIsolation.class.getName());
            fail("should not be able to get the sr for HelloIsolation service");
        } catch (Exception ex) {
            // expected 
        } catch (Error er) {
            // expected
        }
        
        // test bundle find hooks
        Bundle[] bundles = bundleContext.getBundles();
        for (Bundle b : bundles) {
            System.out.println("Bundle is " + b.getBundleId() + ": " + b.getSymbolicName());
            if (b.getSymbolicName().indexOf("org.apache.aries.subsystem.example.helloIsolation") > -1) {
                fail("bundles with name starts with org.apache.aries.subsystem.example.helloIsolation should be in a different scope");
            }
        }
        
        // test bundle service find hook
        //ServiceReference sr = bundleContext.getServiceReference(HelloIsolation.class.getName());
        //assertNull("sr should be null", sr);
        Collection<Scope> children = scopeAdmin.getScope().getChildren();
        assertEquals(1, children.size());
        
        for (Scope child : children) {
            if (child.getName().equals("scope_test1")) {
                Collection<Bundle> buns = child.getBundles();
                assertEquals(2, buns.size());
                assertEquals(0, child.getChildren().size());
                for (Bundle b : buns) {
                    assertTrue(b.getSymbolicName().indexOf("org.apache.aries.subsystem.example.helloIsolation") > -1);
                }
                
            }
        }
        
        // install a test bundle in the root scope
        URL url = new URL("mvn:org.apache.felix/org.apache.felix.fileinstall/2.0.8");
        bundleContext.installBundle("org.apache.felix.fileinstall-rootScope", url.openStream());
        assertEquals("add event count should be 1 since 1 bundles are installed", 1, addEventCount);
        assertEquals("modify event count should be 0", 0, modifyEventCount);
        assertEquals("remove event count should be 0", 0, removeEventCount);
        
        
        // remove child scope
        su = scopeAdmin.newScopeUpdate();
        Collection<Scope> scopes = su.getToBeRemovedChildren();
        
        // obtain child scope admin from service registry
        String filter = "ScopeName=scope_test1";
        ScopeAdmin childScopeAdmin = getOsgiService(ScopeAdmin.class, filter, DEFAULT_TIMEOUT);
        assertEquals(scopeAdmin.getScope(), childScopeAdmin.getParentScope());
        scopes.add(childScopeAdmin.getScope());
        su.commit();
        
        childScopeAdmin = null;
        try {
            childScopeAdmin = getOsgiService(ScopeAdmin.class, filter, DEFAULT_TIMEOUT);
        } catch (Exception ex) {
            // ignore
        }
        assertNull("scope admin service for the scope should be unregistered", childScopeAdmin);
        
    }
    
    @Test
    public void testPackageIsolation() throws Exception {
        // make sure we are using a framework that provides composite admin service
        ScopeAdmin scopeAdmin = getOsgiService(ScopeAdmin.class);
        assertNotNull("scope admin should not be null", scopeAdmin);
        System.out.println("able to get scope admin service");

        ScopeUpdate su = scopeAdmin.newScopeUpdate();
        
        ScopeUpdate childScopeUpdate = su.newChild("scope_test1");
        
        // build up installInfo object for the scope
        InstallInfo info1 = new InstallInfo(new URL("mvn:org.apache.aries.subsystem.example/org.apache.aries.subsystem.example.helloIsolation/0.3-SNAPSHOT"), "helloIsolation");
        InstallInfo info2 = new InstallInfo(new URL("mvn:org.apache.aries.subsystem.example/org.apache.aries.subsystem.example.helloIsolationRef/0.3-SNAPSHOT"), "helloIsolationRef");

        List<InstallInfo> bundlesToInstall = childScopeUpdate.getBundlesToInstall();
        bundlesToInstall.add(info1);
        bundlesToInstall.add(info2);
        
        // add bundles to be installed, based on subsystem content
        su.commit();
        
        // start all bundles in the scope scope_test1
        Collection<Bundle> bundlesToStart = childScopeUpdate.getBundles();
        for (Bundle b : bundlesToStart) {
            b.start();
            
        }
        
        // install helloIsolationRef1 bundle in the root scope
        URL url1 = new URL("mvn:org.apache.aries.subsystem.example/org.apache.aries.subsystem.example.helloIsolationRef/1.0.0");
        Bundle helloIsolationRef = bundleContext.installBundle("helloIsolationRef1-rootScope", url1.openStream());
   
        try {
            helloIsolationRef.start();
            fail("should not be able to start helloIsolationRef since missing import packages");
        } catch (Exception ex) {
            // expect resolving error
        }
        
        URL url2 = new URL("mvn:org.apache.aries.subsystem.example/org.apache.aries.subsystem.example.helloIsolation/1.0.0");
        Bundle helloIsolation = bundleContext.installBundle("helloIsolation1-rootScope", url2.openStream());
 
        helloIsolation.start();
        
        // should be able to start the bundle now.
        helloIsolationRef.start();
               
        // remove helloIsolationRef & helloIsolation
        helloIsolationRef.uninstall();
        helloIsolation.uninstall();
        
        // remove child scope
        su = scopeAdmin.newScopeUpdate();
        Collection<Scope> scopes = su.getToBeRemovedChildren();
        
        // obtain child scope admin from service registry
        String filter = "ScopeName=scope_test1";
        ScopeAdmin childScopeAdmin = getOsgiService(ScopeAdmin.class, filter, DEFAULT_TIMEOUT);
        assertEquals(scopeAdmin.getScope(), childScopeAdmin.getParentScope());
        scopes.add(childScopeAdmin.getScope());
        su.commit();
        
        childScopeAdmin = null;
        try {
            childScopeAdmin = getOsgiService(ScopeAdmin.class, filter, DEFAULT_TIMEOUT);
        } catch (Exception ex) {
            // ignore
        }
        assertNull("scope admin service for the scope should be unregistered", childScopeAdmin);
        
    }
    
    // test sharing the helloIsolation package from the test scope.
    @Test
    public void testPackageSharingFromTestScope() throws Exception {
        // make sure we are using a framework that provides composite admin service
        ScopeAdmin scopeAdmin = getOsgiService(ScopeAdmin.class);
        assertNotNull("scope admin should not be null", scopeAdmin);
        System.out.println("able to get scope admin service");

        ScopeUpdate su = scopeAdmin.newScopeUpdate();
        
        ScopeUpdate childScopeUpdate = su.newChild("scope_test1");
        
        Map<String, List<SharePolicy>> sharePolicies = childScopeUpdate.getSharePolicies(SharePolicy.TYPE_EXPORT);
        final Filter filter1 = FrameworkUtil.createFilter(
                "(&" + 
                  "(osgi.package=org.apache.aries.subsystem.example.helloIsolation)" +
                ")");
        final Filter filter2 = FrameworkUtil.createFilter(
                "(&" + 
                  "(osgi.service=org.apache.aries.subsystem.example.helloIsolation.HelloIsolation)" +
                ")");
        List<SharePolicy> packagePolicies = sharePolicies.get(Capability.PACKAGE_CAPABILITY);
        if (packagePolicies == null) {
            packagePolicies = new ArrayList<SharePolicy>();
            sharePolicies.put(Capability.PACKAGE_CAPABILITY, packagePolicies);
        }
        packagePolicies.add(new SharePolicy(SharePolicy.TYPE_EXPORT, Capability.PACKAGE_CAPABILITY, filter1));
        List<SharePolicy> servicePolicies = sharePolicies.get(ScopeAdminServiceFactory.SERVICE_CAPABILITY);
        if (servicePolicies == null) {
            servicePolicies = new ArrayList<SharePolicy>();
            sharePolicies.put(ScopeAdminServiceFactory.SERVICE_CAPABILITY, servicePolicies);
        }
        servicePolicies.add(new SharePolicy(SharePolicy.TYPE_EXPORT, ScopeAdminServiceFactory.SERVICE_CAPABILITY, filter2));


        // build up installInfo object for the scope
        InstallInfo info1 = new InstallInfo(new URL("mvn:org.apache.aries.subsystem.example/org.apache.aries.subsystem.example.helloIsolation/0.3-SNAPSHOT"), "helloIsolation");
        InstallInfo info2 = new InstallInfo(new URL("mvn:org.apache.aries.subsystem.example/org.apache.aries.subsystem.example.helloIsolationRef/0.3-SNAPSHOT"), "helloIsolationRef");

        List<InstallInfo> bundlesToInstall = childScopeUpdate.getBundlesToInstall();
        bundlesToInstall.add(info1);
        bundlesToInstall.add(info2);
        
        // add bundles to be installed, based on subsystem content
        su.commit();
        
        // start all bundles in the scope scope_test1
        Collection<Bundle> bundlesToStart = childScopeUpdate.getBundles();
        for (Bundle b : bundlesToStart) {
            b.start();
            
        }
        
        // install helloIsolationRef1 bundle in the root scope
        URL url1 = new URL("mvn:org.apache.aries.subsystem.example/org.apache.aries.subsystem.example.helloIsolationRef/1.0.0");
        Bundle helloIsolationRef = bundleContext.installBundle("helloIsolationRef1-rootScope", url1.openStream());
   
        try {
            helloIsolationRef.start();
        } catch (Exception ex) {
            fail("should be able to start helloIsolationRef by import packages from scope_test1");
        }
        
        // remove helloIsolationRef
        helloIsolationRef.uninstall();
        // remove child scope
        su = scopeAdmin.newScopeUpdate();
        Collection<Scope> scopes = su.getToBeRemovedChildren();
        
        // obtain child scope admin from service registry
        String filter = "ScopeName=scope_test1";
        ScopeAdmin childScopeAdmin = getOsgiService(ScopeAdmin.class, filter, DEFAULT_TIMEOUT);
        assertEquals(scopeAdmin.getScope(), childScopeAdmin.getParentScope());
        scopes.add(childScopeAdmin.getScope());
        su.commit();
        
        childScopeAdmin = null;
        try {
            childScopeAdmin = getOsgiService(ScopeAdmin.class, filter, DEFAULT_TIMEOUT);
        } catch (Exception ex) {
            // ignore
        }
        assertNull("scope admin service for the scope should be unregistered", childScopeAdmin);
        
    }

    // test sharing the helloIsolation package & service from the root scope.
    @Test
    public void testPackageSharingFromRootScope() throws Exception {
        // install helloIsolationRef1 bundle in the root scope
        URL url1 = new URL("mvn:org.apache.aries.subsystem.example/org.apache.aries.subsystem.example.helloIsolation/1.0.0");
        Bundle helloIsolation = bundleContext.installBundle("helloIsolation1-rootScope", url1.openStream());
   
        try {
            helloIsolation.start();
        } catch (Exception ex) {
            fail("should be able to start helloIsolation");
        }
        
        // make sure we are using a framework that provides composite admin service
        ScopeAdmin scopeAdmin = getOsgiService(ScopeAdmin.class);
        assertNotNull("scope admin should not be null", scopeAdmin);
        System.out.println("able to get scope admin service");

        ScopeUpdate su = scopeAdmin.newScopeUpdate();
        
        ScopeUpdate childScopeUpdate = su.newChild("scope_test1");
        
        Map<String, List<SharePolicy>> sharePolicies = childScopeUpdate.getSharePolicies(SharePolicy.TYPE_IMPORT);
        final Filter filter1 = FrameworkUtil.createFilter(
                "(&" + 
                  "(osgi.package=org.apache.aries.subsystem.example.helloIsolation)" +
                ")");
        final Filter filter2 = FrameworkUtil.createFilter(
                "(&" + 
                  "(osgi.service=org.apache.aries.subsystem.example.helloIsolation.HelloIsolation)" +
                ")");
        List<SharePolicy> packagePolicies = sharePolicies.get(Capability.PACKAGE_CAPABILITY);
        if (packagePolicies == null) {
            packagePolicies = new ArrayList<SharePolicy>();
            sharePolicies.put(Capability.PACKAGE_CAPABILITY,packagePolicies);
        }
        packagePolicies.add(new SharePolicy(SharePolicy.TYPE_IMPORT, Capability.PACKAGE_CAPABILITY, filter1));
        List<SharePolicy> servicePolicies = sharePolicies.get(ScopeAdminServiceFactory.SERVICE_CAPABILITY);
        if (servicePolicies == null) {
            servicePolicies = new ArrayList<SharePolicy>();
            sharePolicies.put(ScopeAdminServiceFactory.SERVICE_CAPABILITY, servicePolicies);
        }
        servicePolicies.add(new SharePolicy(SharePolicy.TYPE_IMPORT, ScopeAdminServiceFactory.SERVICE_CAPABILITY, filter2));


        // build up installInfo object for the scope
        InstallInfo info2 = new InstallInfo(new URL("mvn:org.apache.aries.subsystem.example/org.apache.aries.subsystem.example.helloIsolationRef/0.3-SNAPSHOT"), "helloIsolationRef");

        List<InstallInfo> bundlesToInstall = childScopeUpdate.getBundlesToInstall();
        bundlesToInstall.add(info2);
        
        // add bundles to be installed, based on subsystem content
        su.commit();
        
        // start all bundles in the scope scope_test1
        Collection<Bundle> bundlesToStart = childScopeUpdate.getBundles();
        for (Bundle b : bundlesToStart) {
            try {
                b.start();
            } catch (Exception ex) {
                fail("should be able to start helloIsolationRef in scope_test1");
            }
            
        }
 
        // remove helloIsolation in root scope
        helloIsolation.uninstall();
        
        // remove child scope
        su = scopeAdmin.newScopeUpdate();
        Collection<Scope> scopes = su.getToBeRemovedChildren();
        
        // obtain child scope admin from service registry
        String filter = "ScopeName=scope_test1";
        ScopeAdmin childScopeAdmin = getOsgiService(ScopeAdmin.class, filter, DEFAULT_TIMEOUT);
        assertEquals(scopeAdmin.getScope(), childScopeAdmin.getParentScope());
        scopes.add(childScopeAdmin.getScope());
        su.commit();
        
        childScopeAdmin = null;
        try {
            childScopeAdmin = getOsgiService(ScopeAdmin.class, filter, DEFAULT_TIMEOUT);
        } catch (Exception ex) {
            // ignore
        }
        assertNull("scope admin service for the scope should be unregistered", childScopeAdmin);
    }
    
    // test ability to select the helloIsolation package from which scope it wants to use
    // not necessarily the highest version one by default.
    @Test
    public void testScopeAffinity() throws Exception {
        // make sure we are using a framework that provides composite admin service
        ScopeAdmin scopeAdmin = getOsgiService(ScopeAdmin.class);
        assertNotNull("scope admin should not be null", scopeAdmin);
        System.out.println("able to get scope admin service");

        // install helloIsolation 0.3 in scope_test1
        Scope scope1 = createScope(scopeAdmin, "scope_test1", 
                "mvn:org.apache.aries.subsystem.example/org.apache.aries.subsystem.example.helloIsolation/0.3-SNAPSHOT",
                "0.3");
        
        // install helloIsolation 2.0 in scope_test2
        Scope scope2 = createScope(scopeAdmin, "scope_test2", 
                "mvn:org.apache.aries.subsystem.example/org.apache.aries.subsystem.example.helloIsolation/2.0.0",
                "2.0");
        
        // install helloIsolationRef 2.0 in scope_test3
        ScopeUpdate su = scopeAdmin.newScopeUpdate();
        
        ScopeUpdate childScopeUpdate = su.newChild("scope_test3");
        Scope scope3 = childScopeUpdate.getScope();
        
        Map<String, List<SharePolicy>> sharePolicies = childScopeUpdate.getSharePolicies(SharePolicy.TYPE_IMPORT);
        /*final Filter filter1 = FrameworkUtil.createFilter(
                "(&" + 
                  "(osgi.package=org.apache.aries.subsystem.example.helloIsolation)" +
                  "(bundle-symbolic-name=org.apache.aries.subsystem.example.helloIsolation)" + 
                  "(bundle-version<=1.1)" + 
                ")");*/
        final Filter filter1 = FrameworkUtil.createFilter(
                "(&" + 
                  "(osgi.package=org.apache.aries.subsystem.example.helloIsolation)" +
                  //"(scopeName=scope_test1)" +  
                ")");
        final Filter filter2 = FrameworkUtil.createFilter(
                "(&" + 
                  "(osgi.service=org.apache.aries.subsystem.example.helloIsolation.HelloIsolation)" +
                ")");
        List<SharePolicy> packagePolicies = sharePolicies.get(Capability.PACKAGE_CAPABILITY);
        if (packagePolicies == null) {
            packagePolicies = new ArrayList<SharePolicy>();
            sharePolicies.put(Capability.PACKAGE_CAPABILITY,packagePolicies);
        }
        packagePolicies.add(new SharePolicy(SharePolicy.TYPE_IMPORT, Capability.PACKAGE_CAPABILITY, filter1));
        List<SharePolicy> servicePolicies = sharePolicies.get(ScopeAdminServiceFactory.SERVICE_CAPABILITY);
        if (servicePolicies == null) {
            servicePolicies = new ArrayList<SharePolicy>();
            sharePolicies.put(ScopeAdminServiceFactory.SERVICE_CAPABILITY, servicePolicies);
        }
        servicePolicies.add(new SharePolicy(SharePolicy.TYPE_IMPORT, ScopeAdminServiceFactory.SERVICE_CAPABILITY, filter2));


        // build up installInfo object for the scope
        InstallInfo info2 = new InstallInfo(new URL("mvn:org.apache.aries.subsystem.example/org.apache.aries.subsystem.example.helloIsolationRef/0.3-SNAPSHOT"), "helloIsolationRef");

        List<InstallInfo> bundlesToInstall = childScopeUpdate.getBundlesToInstall();
        bundlesToInstall.add(info2);
        
        // add bundles to be installed, based on subsystem content
        su.commit();
        
        // start all bundles in the scope scope_test3
        Collection<Bundle> bundlesToStart = childScopeUpdate.getBundles();
        for (Bundle b : bundlesToStart) {
            try {
                b.start();
            } catch (Exception ex) {
                fail("should be able to start helloIsolationRef in scope_test1");
            }
            
        }
        
        /*  // install helloIsolationRef in root scope
        URL url1 = new URL("mvn:org.apache.aries.subsystem.example/org.apache.aries.subsystem.example.helloIsolationRef/1.0.0");
        Bundle helloIsolationRef = bundleContext.installBundle("helloIsolationRef1-rootScope", url1.openStream());
   
        try {
            helloIsolationRef.start();
        } catch (Exception ex) {
            fail("should be able to start helloIsolationRef");
        }*/
        
        // remove child scope - cleanup
        su = scopeAdmin.newScopeUpdate();
        Collection<Scope> scopes = su.getToBeRemovedChildren();
        scopes.add(scope1);
        scopes.add(scope2);
        scopes.add(scope3);
        su.commit();
        
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
            mavenBundle("org.apache.aries.testsupport", "org.apache.aries.testsupport.unit"),
            mavenBundle("org.apache.aries.application", "org.apache.aries.application.api"),
            mavenBundle("org.apache.aries", "org.apache.aries.util"),
            mavenBundle("org.apache.aries.application", "org.apache.aries.application.utils"),
            mavenBundle("org.apache.felix", "org.apache.felix.bundlerepository"),
            mavenBundle("org.apache.aries.subsystem", "org.apache.aries.subsystem.api"),
            mavenBundle("org.apache.aries.subsystem", "org.apache.aries.subsystem.scope.api"),
            mavenBundle("org.apache.aries.subsystem", "org.apache.aries.subsystem.scope.impl"),

            //org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption("-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),

            PaxRunnerOptions.rawPaxRunnerOption("config", "classpath:ss-runner.properties"),

            equinox().version("3.7.0.v20100910")
        );
        options = updateOptions(options);
        return options;
    }

    private Scope createScope(ScopeAdmin scopeAdmin, String scopeName, String loc, String version) throws MalformedURLException, InvalidSyntaxException, BundleException {
        ScopeUpdate su = scopeAdmin.newScopeUpdate();
        
        ScopeUpdate childScopeUpdate = su.newChild(scopeName);
        
        Map<String, List<SharePolicy>> sharePolicies = childScopeUpdate.getSharePolicies(SharePolicy.TYPE_EXPORT);
        final Filter filter1 = FrameworkUtil.createFilter(
                "(&" + 
                  "(osgi.package=org.apache.aries.subsystem.example.helloIsolation)" +
                  "(version=" + version + ")" +
                ")");
        final Filter filter2 = FrameworkUtil.createFilter(
                "(&" + 
                  "(osgi.service=org.apache.aries.subsystem.example.helloIsolation.HelloIsolation)" +
                ")");
        List<SharePolicy> packagePolicies = sharePolicies.get(Capability.PACKAGE_CAPABILITY);
        if (packagePolicies == null) {
            packagePolicies = new ArrayList<SharePolicy>();
            sharePolicies.put(Capability.PACKAGE_CAPABILITY, packagePolicies);
        }
        packagePolicies.add(new SharePolicy(SharePolicy.TYPE_EXPORT, Capability.PACKAGE_CAPABILITY, filter1));
        List<SharePolicy> servicePolicies = sharePolicies.get(ScopeAdminServiceFactory.SERVICE_CAPABILITY);
        if (servicePolicies == null) {
            servicePolicies = new ArrayList<SharePolicy>();
            sharePolicies.put(ScopeAdminServiceFactory.SERVICE_CAPABILITY, servicePolicies);
        }
        servicePolicies.add(new SharePolicy(SharePolicy.TYPE_EXPORT, ScopeAdminServiceFactory.SERVICE_CAPABILITY, filter2));


        // build up installInfo object for the scope
        InstallInfo info1 = new InstallInfo(new URL(loc), "helloIsolation_" + scopeName);

        List<InstallInfo> bundlesToInstall = childScopeUpdate.getBundlesToInstall();
        bundlesToInstall.add(info1);
        
        // add bundles to be installed, based on subsystem content
        su.commit();
        
        // start all bundles in the scope scope_test1
        Collection<Bundle> bundlesToStart = childScopeUpdate.getBundles();
        for (Bundle b : bundlesToStart) {
            b.start();
        }
        
        return childScopeUpdate.getScope();
    }
}
