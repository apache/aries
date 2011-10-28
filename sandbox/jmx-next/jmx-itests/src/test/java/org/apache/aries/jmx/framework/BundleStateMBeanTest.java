/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.jmx.framework;

import static org.apache.aries.itest.ExtraOptions.mavenBundle;
import static org.apache.aries.itest.ExtraOptions.paxLogging;
import static org.apache.aries.itest.ExtraOptions.testOptions;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.newBundle;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.withBnd;
import static org.osgi.jmx.framework.BundleStateMBean.OBJECTNAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.openmbean.TabularData;

import org.apache.aries.jmx.AbstractIntegrationTest;
import org.apache.aries.jmx.codec.BundleData.Header;
import org.junit.Test;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.container.def.PaxRunnerOptions;
import org.ops4j.pax.exam.container.def.options.VMOption;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.options.TimeoutOption;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.jmx.framework.BundleStateMBean;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * @version $Rev$ $Date$
 */
public class BundleStateMBeanTest extends AbstractIntegrationTest {

    @Configuration
    public static Option[] configuration() {
        return testOptions(
                         new VMOption( "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000" ),
                         new TimeoutOption( 0 ),

                        PaxRunnerOptions.rawPaxRunnerOption("config", "classpath:ss-runner.properties"),
                        CoreOptions.equinox().version("3.7.0.v20110613"),
                        paxLogging("INFO"),

                        mavenBundle("org.apache.felix", "org.apache.felix.configadmin"),
                        mavenBundle("org.osgi", "org.osgi.compendium"),
                        mavenBundle("org.apache.aries.jmx", "org.apache.aries.jmx"),
                        mavenBundle("org.apache.aries.jmx", "org.apache.aries.jmx.api"),
                        mavenBundle("org.apache.aries.jmx", "org.apache.aries.jmx.whiteboard"),
                        mavenBundle("org.apache.aries", "org.apache.aries.util"),
                        provision(newBundle()
                                .add(org.apache.aries.jmx.test.bundlea.Activator.class)
                                .add(org.apache.aries.jmx.test.bundlea.api.InterfaceA.class)
                                .add(org.apache.aries.jmx.test.bundlea.impl.A.class)
                                .set(Constants.BUNDLE_SYMBOLICNAME, "org.apache.aries.jmx.test.bundlea")
                                .set(Constants.BUNDLE_VERSION, "2.0.0")
                                .set(Constants.EXPORT_PACKAGE, "org.apache.aries.jmx.test.bundlea.api;version=2.0.0")
                                .set(Constants.IMPORT_PACKAGE,
                                        "org.osgi.framework;version=1.5.0,org.osgi.util.tracker,org.apache.aries.jmx.test.bundleb.api;version=1.1.0;resolution:=optional" +
                                        ",org.osgi.service.cm")
                                .set(Constants.BUNDLE_ACTIVATOR,
                                        org.apache.aries.jmx.test.bundlea.Activator.class.getName())
                                .build(withBnd())),
                        provision(newBundle()
                                .add(org.apache.aries.jmx.test.bundleb.Activator.class)
                                .add(org.apache.aries.jmx.test.bundleb.api.InterfaceB.class)
                                .add(org.apache.aries.jmx.test.bundleb.api.MSF.class)
                                .add(org.apache.aries.jmx.test.bundleb.impl.B.class)
                                .set(Constants.BUNDLE_SYMBOLICNAME,"org.apache.aries.jmx.test.bundleb")
                                .set(Constants.BUNDLE_VERSION, "1.0.0")
                                .set(Constants.EXPORT_PACKAGE,"org.apache.aries.jmx.test.bundleb.api;version=1.1.0")
                                .set(Constants.IMPORT_PACKAGE,"org.osgi.framework;version=1.5.0,org.osgi.util.tracker," +
                                		"org.osgi.service.cm,org.apache.aries.jmx.test.fragmentc")
                                .set(Constants.BUNDLE_ACTIVATOR,
                                        org.apache.aries.jmx.test.bundleb.Activator.class.getName())
                                .build(withBnd())),
                        provision(newBundle()
                                .add(org.apache.aries.jmx.test.fragmentc.C.class)
                                .set(Constants.BUNDLE_SYMBOLICNAME, "org.apache.aries.jmx.test.fragc")
                                .set(Constants.FRAGMENT_HOST, "org.apache.aries.jmx.test.bundlea")
                                .set(Constants.EXPORT_PACKAGE, "org.apache.aries.jmx.test.fragmentc")
                                .build(withBnd())),
                        provision(newBundle()
                                .set(Constants.BUNDLE_SYMBOLICNAME, "org.apache.aries.jmx.test.bundled")
                                .set(Constants.BUNDLE_VERSION, "3.0.0")
                                .set(Constants.REQUIRE_BUNDLE, "org.apache.aries.jmx.test.bundlea;bundle-version=2.0.0")
                                .build(withBnd()))
                        );
    }

    @Override
    public void doSetUp() throws Exception {
        waitForMBean(new ObjectName(BundleStateMBean.OBJECTNAME));
    }

    @Test
    public void testMBeanInterface() throws Exception {

        BundleStateMBean mbean = getMBean(OBJECTNAME, BundleStateMBean.class);
        assertNotNull(mbean);

        //get bundles

        Bundle a = context().getBundleByName("org.apache.aries.jmx.test.bundlea");
        assertNotNull(a);

        Bundle b = context().getBundleByName("org.apache.aries.jmx.test.bundleb");
        assertNotNull(b);

        Bundle frag = context().getBundleByName("org.apache.aries.jmx.test.fragc");
        assertNotNull(frag);

        Bundle d = context().getBundleByName("org.apache.aries.jmx.test.bundled");
        assertNotNull(d);

        // exportedPackages

        String[] exports = mbean.getExportedPackages(a.getBundleId());
        assertEquals(2, exports.length);

        List<String> packages = Arrays.asList(exports);
        assertTrue(packages.contains("org.apache.aries.jmx.test.bundlea.api;2.0.0"));
        assertTrue(packages.contains("org.apache.aries.jmx.test.fragmentc;0.0.0"));

        //fragments

        long[] fragments = mbean.getFragments(a.getBundleId());
        assertEquals(1, fragments.length);
        assertEquals(frag.getBundleId() , fragments[0]);

        //headers

        TabularData headers = mbean.getHeaders(b.getBundleId());
        assertNotNull(headers);
        assertEquals(BundleStateMBean.HEADERS_TYPE, headers.getTabularType());
        assertTrue(headers.values().size() >= 4 );
        assertEquals("org.apache.aries.jmx.test.bundleb", Header.from(headers.get(new Object[] {Constants.BUNDLE_SYMBOLICNAME})).getValue());

        //hosts

        long[] hosts = mbean.getHosts(frag.getBundleId());
        assertEquals(1, hosts.length);
        assertEquals(a.getBundleId() , hosts[0]);

        //imported packages

        String[] imports = mbean.getImportedPackages(a.getBundleId());
        assertTrue(imports.length >= 3);
        List<String> importedPackages = Arrays.asList(imports);

        Version version = getPackageVersion("org.osgi.framework");
        assertTrue(importedPackages.contains("org.osgi.framework;" + version.toString()));
        assertTrue(importedPackages.contains("org.apache.aries.jmx.test.bundleb.api;1.1.0"));

        //last modified

        assertTrue(mbean.getLastModified(b.getBundleId()) > 0);

        //location

        assertEquals(b.getLocation(), mbean.getLocation(b.getBundleId()));

        //registered services

        long[] serviceIds = mbean.getRegisteredServices(a.getBundleId());
        assertEquals(1, serviceIds.length);

        //required bundles

        long[] required = mbean.getRequiredBundles(d.getBundleId());
        assertEquals(1, required.length);
        assertEquals(a.getBundleId(), required[0]);

        //requiring bundles

        long[] requiring = mbean.getRequiringBundles(a.getBundleId());
        assertEquals(3, requiring.length);
        assertTrue(b.getSymbolicName(), arrayContains(b.getBundleId(), requiring));
        assertTrue(frag.getSymbolicName(), arrayContains(frag.getBundleId(), requiring));
        assertTrue(d.getSymbolicName(), arrayContains(d.getBundleId(), requiring));

        //services in use

        long[] servicesInUse = mbean.getServicesInUse(a.getBundleId());
        assertEquals(1, servicesInUse.length);

        //start level

        long startLevel = mbean.getStartLevel(b.getBundleId());
        assertTrue(startLevel >= 0);

        //state

        assertEquals("ACTIVE", mbean.getState(b.getBundleId()));

        //isFragment

        assertFalse(mbean.isFragment(b.getBundleId()));
        assertTrue(mbean.isFragment(frag.getBundleId()));

        //isRemovalPending
        assertFalse(mbean.isRemovalPending(b.getBundleId()));

        // isRequired

        assertTrue(mbean.isRequired(a.getBundleId()));
        assertTrue(mbean.isRequired(b.getBundleId()));

        // listBundles

        TabularData bundlesTable = mbean.listBundles();
        assertNotNull(bundlesTable);
        assertEquals(BundleStateMBean.BUNDLES_TYPE, bundlesTable.getTabularType());
        assertEquals(bundleContext.getBundles().length, bundlesTable.values().size());


        // notifications

        final List<Notification> received = new ArrayList<Notification>();

        mbeanServer.addNotificationListener(new ObjectName(BundleStateMBean.OBJECTNAME), new NotificationListener() {
            public void handleNotification(Notification notification, Object handback) {
               received.add(notification);
            }
        }, null, null);

        assertEquals(Bundle.ACTIVE, b.getState());
        b.stop();
        assertEquals(Bundle.RESOLVED, b.getState());
        b.start();
        assertEquals(Bundle.ACTIVE, b.getState());

        int i = 0;
        while (received.size() < 2 && i < 3) {
            Thread.sleep(1000);
            i++;
        }

        assertEquals(2, received.size());

    }

    private Version getPackageVersion(String packageName) {
        ServiceReference paRef = context().getServiceReference(PackageAdmin.class.getName());
        PackageAdmin pa = (PackageAdmin) context().getService(paRef);
        ExportedPackage pkg = pa.getExportedPackage(packageName);
        Version version = pkg.getVersion();
        return version;
    }

    private static boolean arrayContains(long value, long[] values) {
        for (long i : values) {
            if (i == value) {
                return true;
            }
        }
        return false;
    }
}
