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
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.newBundle;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.withBnd;

import java.io.IOException;

import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import org.apache.aries.jmx.AbstractIntegrationTest;
import org.junit.Assert;
import org.junit.Test;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.container.def.PaxRunnerOptions;
import org.ops4j.pax.exam.junit.Configuration;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.jmx.framework.BundleRevisionsStateMBean;
import org.osgi.jmx.framework.PackageStateMBean;

/**
 *
 *
 * @version $Rev: 1190259 $ $Date: 2011-10-28 12:46:48 +0100 (Fri, 28 Oct 2011) $
 */
public class BundleRevisionsStateMBeanTest extends AbstractIntegrationTest {

    @Configuration
    public static Option[] configuration() {
        return testOptions(
                //  new VMOption( "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000" ),
                //  new TimeoutOption( 0 ),

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
        waitForMBean(new ObjectName(PackageStateMBean.OBJECTNAME));
    }

    @Test
    public void testMBeanInterface() throws IOException {
        // BundleStateMBean bsMBean = getMBean(BundleStateMBean.OBJECTNAME, BundleStateMBean.class);
        BundleRevisionsStateMBean brsMBean = getMBean(BundleRevisionsStateMBean.OBJECTNAME, BundleRevisionsStateMBean.class);

        Bundle a = context().getBundleByName("org.apache.aries.jmx.test.bundlea");

        CompositeData wiring = brsMBean.getCurrentWiring(a.getBundleId(), BundleRevisionsStateMBean.PACKAGE_NAMESPACE);

        Assert.assertEquals(BundleRevisionsStateMBean.BUNDLE_WIRING_TYPE, wiring.getCompositeType());
        Assert.assertEquals(a.getBundleId(), wiring.get(BundleRevisionsStateMBean.BUNDLE_ID));

        BundleWiring bw = a.adapt(BundleWiring.class);
        CompositeData[] capabilities = (CompositeData[]) wiring.get(BundleRevisionsStateMBean.CAPABILITIES);
        Assert.assertEquals(bw.getCapabilities(BundleRevisionsStateMBean.PACKAGE_NAMESPACE).size(), capabilities.length);

        /*
        PackageStateMBean packagaState = getMBean(PackageStateMBean.OBJECTNAME, PackageStateMBean.class);
        assertNotNull(packagaState);

        long[] exportingBundles = packagaState.getExportingBundles("org.osgi.jmx.framework", "1.5.0");
        assertNotNull(exportingBundles);
        assertTrue("Should find a bundle exporting org.osgi.jmx.framework", exportingBundles.length > 0);

        long[] exportingBundles2 = packagaState.getExportingBundles("test", "1.0.0");
        assertNull("Shouldn't find a bundle exporting test package", exportingBundles2);

        long[] importingBundlesId = packagaState
                .getImportingBundles("org.osgi.jmx.framework", "1.5.0", exportingBundles[0]);
        assertTrue("Should find bundles importing org.osgi.jmx.framework", importingBundlesId.length > 0);

        TabularData table = packagaState.listPackages();
        assertNotNull("TabularData containing CompositeData with packages info shouldn't be null", table);
        assertEquals("TabularData should be a type PACKAGES", PackageStateMBean.PACKAGES_TYPE, table.getTabularType());
        Collection colData = table.values();
        assertNotNull("Collection of CompositeData shouldn't be null", colData);
        assertFalse("Collection of CompositeData should contain elements", colData.isEmpty());

        boolean isRemovalPending = packagaState.isRemovalPending("org.osgi.jmx.framework", "1.5.0", exportingBundles[0]);
        assertFalse("Should removal pending on org.osgi.jmx.framework be false", isRemovalPending);
        */
    }

}
