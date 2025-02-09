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
package org.apache.aries.subsystem.scope.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.net.URL;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.aries.itest.AbstractIntegrationTest;
import org.apache.aries.subsystem.example.helloIsolation.HelloIsolation;
import org.apache.aries.subsystem.scope.InstallInfo;
import org.apache.aries.subsystem.scope.Scope;
import org.apache.aries.subsystem.scope.ScopeUpdate;
import org.apache.aries.subsystem.scope.SharePolicy;
import org.junit.After;
import org.junit.Ignore;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.PackagePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.service.condpermadmin.ConditionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionUpdate;
import org.osgi.service.permissionadmin.PermissionInfo;
import org.osgi.util.tracker.BundleTracker;

@Ignore
public class ScopeSecurityTest extends AbstractTest {

    /* Use @Before not @BeforeClass so as to ensure that these resources
     * are created in the paxweb temp directory, and not in the svn tree 
     */
    static boolean createdApplications = false;
    BundleTracker bt;
    int addEventCount;
    int modifyEventCount;
    int removeEventCount;
    private final static PermissionInfo[] adminAllowInfo = {
        new PermissionInfo("java.security.AllPermission", "*", "*"),
        new PermissionInfo("java.lang.RuntimePermission", "loadLibrary.*", "*"),
        new PermissionInfo("java.lang.RuntimePermission", "queuePrintJob", "*"),
        new PermissionInfo("java.net.SocketPermission", "*", "connect"),
        new PermissionInfo("java.util.PropertyPermission", "*", "read"),
        new PermissionInfo("org.osgi.framework.PackagePermission", "*", "exportonly,import"),
        new PermissionInfo("org.osgi.framework.ServicePermission", "*", "get,register"),
        new PermissionInfo("org.osgi.framework.AdminPermission", "*", "execute,resolve"),
    };
    
    @After
    public void tearDown() throws Exception {
        if (bt != null) {
            bt.close();
        }
    }
    
    //@Test
    public void testScopeSecurityWithServiceIsolation() throws Exception {
        ScopeUpdate su = scope.newScopeUpdate();
        
        ScopeUpdate childScopeUpdate = su.newChild("scope_test1");
        
        // build up installInfo object for the scope
        InstallInfo info1 = new InstallInfo("helloIsolation", new URL("mvn:org.apache.aries.subsystem.example/org.apache.aries.subsystem.example.helloIsolation/0.4-SNAPSHOT"));
        InstallInfo info2 = new InstallInfo("helloIsolationRef", new URL("mvn:org.apache.aries.subsystem.example/org.apache.aries.subsystem.example.helloIsolationRef/0.4-SNAPSHOT"));

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
        
        try {
            ServiceReference sr = bundleContext.getServiceReference("org.apache.aries.subsystem.example.helloIsolation.HelloIsolation");
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
        Collection<Scope> children = scope.getChildren();
        assertEquals(1, children.size());
        
        for (Scope child : children) {
            if (child.getName().equals("scope_test1")) {
                Collection<Bundle> buns = child.getBundles();
                assertEquals(2, buns.size());
                assertEquals(0, child.getChildren().size());
                BundleContext childScopebundleContext = null;

                for (Bundle b : buns) {
                    assertTrue(b.getSymbolicName().indexOf("org.apache.aries.subsystem.example.helloIsolation") > -1);
                    if (b.getSymbolicName().indexOf("org.apache.aries.subsystem.example.helloIsolationRef") > -1) {
                        childScopebundleContext = b.getBundleContext();
                    }

                }
                assertNotNull(childScopebundleContext);
                ServiceReference sr = childScopebundleContext.getServiceReference("org.apache.aries.subsystem.example.helloIsolation.HelloIsolation");
                assertNotNull("sr is not null", sr);
                System.out.println("got the sr, go get service next");
                Object obj = childScopebundleContext.getService(sr);
                //HelloIsolation hi = (HelloIsolation)childScopebundleContext.getService(sr);
                
            }
        }
        
        // install a test bundle in the root scope
        URL url = new URL("mvn:org.apache.felix/org.apache.felix.fileinstall/2.0.8");
        bundleContext.installBundle("org.apache.felix.fileinstall-rootScope", url.openStream());
        
        // remove child scope
        su = scope.newScopeUpdate();
        Collection<ScopeUpdate> scopes = su.getChildren();
        
        // obtain child scope admin from service registry
//        String filter = "ScopeName=scope_test1";
        Scope childScopeAdmin = childScopeUpdate.getScope();
        assertEquals(scope, childScopeAdmin.getParent());
        scopes.remove(childScopeUpdate);
        su.commit();
        
        assertFalse(scope.getChildren().contains(childScopeAdmin));
        su = scope.newScopeUpdate();
        assertFalse(su.getChildren().contains(childScopeUpdate));
        
//        childScopeAdmin = null;
//        try {
//            childScopeAdmin = getOsgiService(Scope.class, filter, DEFAULT_TIMEOUT);
//        } catch (Exception ex) {
//            // ignore
//        }
//        assertNull("scope admin service for the scope should be unregistered", childScopeAdmin);
        
    }
    
    //@Test
    public void testScopeSecurityWithServiceShared() throws Exception {
        
        SecurityManager security = System.getSecurityManager();
        assertNotNull("Security manager should not be null", security);
       
        Bundle[] bundles = bundleContext.getBundles();
        
        for (Bundle b : bundles) {
            // set up condition permission for scope
            if (b.getSymbolicName().indexOf("subsystem.scope.impl") > -1) {
                ServiceReference permRef = bundleContext.getServiceReference(ConditionalPermissionAdmin.class.getName());

                ConditionalPermissionAdmin permAdmin = (ConditionalPermissionAdmin) bundleContext.getService(permRef);
                ConditionalPermissionUpdate update = permAdmin.newConditionalPermissionUpdate();

                List<ConditionalPermissionInfo> infos = update.getConditionalPermissionInfos();
                //infos.clear();

                // set up the conditionInfo
                ConditionInfo[] conditionInfo = new ConditionInfo[] {new ConditionInfo("org.osgi.service.condpermadmin.BundleLocationCondition", new String[]{b.getLocation()})}; 
                // Set up permissions which are common to all applications
                infos.add(permAdmin.newConditionalPermissionInfo(null, conditionInfo, adminAllowInfo, "allow"));
                update.commit();
            }
            
        }
        
        ScopeUpdate su = scope.newScopeUpdate();
        
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
        List<SharePolicy> packagePolicies = sharePolicies.get(BundleRevision.PACKAGE_NAMESPACE);
        if (packagePolicies == null) {
            packagePolicies = new ArrayList<SharePolicy>();
            sharePolicies.put(BundleRevision.PACKAGE_NAMESPACE, packagePolicies);
        }
        packagePolicies.add(new SharePolicy(SharePolicy.TYPE_EXPORT, BundleRevision.PACKAGE_NAMESPACE, filter1));
        List<SharePolicy> servicePolicies = sharePolicies.get("scope.share.service");
        if (servicePolicies == null) {
            servicePolicies = new ArrayList<SharePolicy>();
            sharePolicies.put("scope.share.service", servicePolicies);
        }
        servicePolicies.add(new SharePolicy(SharePolicy.TYPE_EXPORT, "scope.share.service", filter2));

        // build up installInfo object for the scope
        InstallInfo info1 = new InstallInfo("helloIsolation", new URL("mvn:org.apache.aries.subsystem.example/org.apache.aries.subsystem.example.helloIsolation/0.4-SNAPSHOT"));
        InstallInfo info2 = new InstallInfo("helloIsolationRef", new URL("mvn:org.apache.aries.subsystem.example/org.apache.aries.subsystem.example.helloIsolationRef/0.4-SNAPSHOT"));

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
        
        try {
            ServiceReference sr = bundleContext.getServiceReference("org.apache.aries.subsystem.example.helloIsolation.HelloIsolation");
            fail("should not be able to get the sr for HelloIsolation service");
        } catch (Exception ex) {
            // expected 
        } catch (Error er) {
            // expected
        }
        
        // test bundle find hooks
        bundles = bundleContext.getBundles();
        for (Bundle b : bundles) {
            System.out.println("Bundle is " + b.getBundleId() + ": " + b.getSymbolicName());
            if (b.getSymbolicName().indexOf("org.apache.aries.subsystem.example.helloIsolation") > -1) {
                fail("bundles with name starts with org.apache.aries.subsystem.example.helloIsolation should be in a different scope");
            }
        }
        
        // test bundle service find hook
        //ServiceReference sr = bundleContext.getServiceReference(HelloIsolation.class.getName());
        //assertNull("sr should be null", sr);
        Collection<Scope> children = scope.getChildren();
        assertEquals(1, children.size());
        
        for (Scope child : children) {
            if (child.getName().equals("scope_test1")) {
                Collection<Bundle> buns = child.getBundles();
                assertEquals(2, buns.size());
                assertEquals(0, child.getChildren().size());
                BundleContext childScopebundleContext = null;

                for (Bundle b : buns) {
                    assertTrue(b.getSymbolicName().indexOf("org.apache.aries.subsystem.example.helloIsolation") > -1);
                    if (b.getSymbolicName().indexOf("org.apache.aries.subsystem.example.helloIsolationRef") > -1) {
                        childScopebundleContext = b.getBundleContext();
                    }

                }
                assertNotNull(childScopebundleContext);
                ServiceReference sr = childScopebundleContext.getServiceReference("org.apache.aries.subsystem.example.helloIsolation.HelloIsolation");
                assertNotNull("sr is not null", sr);
                System.out.println("got the sr, go get service next");
                HelloIsolation hi = (HelloIsolation)childScopebundleContext.getService(sr);
                hi.hello();
                
                Permission permission = new PackagePermission("*", PackagePermission.IMPORT);
                hi.checkPermission(permission);
            }
        }
        
        // install a test bundle in the root scope
        URL url = new URL("mvn:org.apache.felix/org.apache.felix.fileinstall/2.0.8");
        bundleContext.installBundle("org.apache.felix.fileinstall-rootScope", url.openStream());
        
        // remove child scope
        su = scope.newScopeUpdate();
        Collection<ScopeUpdate> scopes = su.getChildren();
        
        // obtain child scope admin from service registry
//        String filter = "ScopeName=scope_test1";
        Scope childScopeAdmin = childScopeUpdate.getScope();
        assertEquals(scope, childScopeAdmin.getParent());
        scopes.remove(childScopeUpdate);
        su.commit();
        
        assertFalse(scope.getChildren().contains(childScopeAdmin));
        su = scope.newScopeUpdate();
        assertFalse(su.getChildren().contains(childScopeUpdate));
        
//        childScopeAdmin = null;
//        try {
//            childScopeAdmin = getOsgiService(Scope.class, filter, DEFAULT_TIMEOUT);
//        } catch (Exception ex) {
//            // ignore
//        }
//        assertNull("scope admin service for the scope should be unregistered", childScopeAdmin);
        
    }
    
   
    @Configuration
    public Option[] configuration() {
        return options(
        	baseOptions(),

            // Bundles
            mavenBundle("org.apache.aries.testsupport", "org.apache.aries.testsupport.unit"),
            mavenBundle("org.apache.aries.application", "org.apache.aries.application.api"),
            mavenBundle("org.apache.aries", "org.apache.aries.util"),
            mavenBundle("org.apache.aries.application", "org.apache.aries.application.utils"),
            mavenBundle("org.apache.felix", "org.apache.felix.bundlerepository"),
            mavenBundle("org.apache.aries.subsystem", "org.apache.aries.subsystem.api"),
            mavenBundle("org.apache.aries.subsystem", "org.apache.aries.subsystem.scope.api"),
            mavenBundle("org.apache.aries.subsystem", "org.apache.aries.subsystem.scope.impl")

            // uncomment the following line if you want to turn on security.  the policy file can be found in src/test/resources dir and you want to update the value of -Djava.security.policy to 
            // the exact location of the policy file.
            //org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption("-Declipse.security=osgi -Djava.security.policy=/policy"),
        );
    }

   
}
