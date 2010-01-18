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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;

import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import org.apache.aries.jmx.AbstractIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.osgi.jmx.framework.PackageStateMBean;

/**
 * 
 * 
 * @version $Rev$ $Date$
 */
public class PackageStateMBeanTest extends AbstractIntegrationTest {

    @Configuration
    public static Option[] configuration() {
        Option[] options = CoreOptions.options(CoreOptions.equinox(), mavenBundle("org.ops4j.pax.logging",
                "pax-logging-api"), mavenBundle("org.ops4j.pax.logging", "pax-logging-service"), mavenBundle(
                "org.apache.aries.jmx", "org.apache.aries.jmx"));
        options = updateOptions(options);
        return options;
    }

    @Before
    public void doSetUp() throws Exception {
        super.setUp();
        int i = 0;
        while (true) {
            try {
                mbeanServer.getObjectInstance(new ObjectName(PackageStateMBean.OBJECTNAME));
                break;
            } catch (InstanceNotFoundException e) {
                if (i == 5) {
                    throw new Exception("PackageStateMBean not available after waiting 5 seconds");
                }
            }
            i++;
            Thread.sleep(1000);
        }
    }

    @Test
    public void testMBeanInterface() throws IOException {
        PackageStateMBean packagaState = getMBean(PackageStateMBean.OBJECTNAME, PackageStateMBean.class);
        assertNotNull(packagaState);
        long exportingBundleId = packagaState.getExportingBundle("org.osgi.jmx.framework", "1.5.0");
        assertTrue("Should find a bundle exporting org.osgi.jmx.framework", exportingBundleId > -1);

        long exportingBundleId_2 = packagaState.getExportingBundle("test", "1.0.0");
        assertTrue("Shouldn't find a bundle exporting test package", exportingBundleId_2 == -1);

        long[] importingBundlesId = packagaState.getImportingBundles("org.osgi.jmx.framework", "1.5.0");
        assertTrue("Should find bundles importing org.osgi.jmx.framework", importingBundlesId.length > 0);

        TabularData table = packagaState.listPackages();
        assertNotNull("TabularData containing CompositeData with packages info shouldn't be null", table);
        assertEquals("TabularData should be a type PACKAGES", PackageStateMBean.PACKAGES_TYPE, table.getTabularType());
        //Collection<CompositeData> colData = table.values();
        //assertNotNull("Collection of CompositeData shouldn't be null", colData);
        //assertFalse("Collection of CompositeData should contain elements", colData.isEmpty());

        boolean isRemovalPending = packagaState.isRemovalPending("org.osgi.jmx.framework", "1.5.0");
        assertFalse("Should removal pending on org.osgi.jmx.framework be false", isRemovalPending);
    }

}
