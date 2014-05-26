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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.options;

import java.io.IOException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Set;

import javax.management.ObjectName;
import javax.management.openmbean.TabularData;

import org.apache.aries.jmx.AbstractIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.Constants;
import org.osgi.jmx.framework.PackageStateMBean;

/**
 *
 *
 * @version $Rev$ $Date$
 */
public class PackageStateMBeanTest extends AbstractIntegrationTest {

    @Configuration
    public Option[] configuration() {
        return options(
        		jmxRuntime(),
        		bundlea()
        		);
    }

    @Before
    public void doSetUp() {
        waitForMBean(PackageStateMBean.OBJECTNAME);
    }

    @Test
    public void testObjectName() throws Exception {
        Set<ObjectName> names = mbeanServer.queryNames(new ObjectName(PackageStateMBean.OBJECTNAME + ",*"), null);
        assertEquals(1, names.size());
        ObjectName name = names.iterator().next();
        Hashtable<String, String> props = name.getKeyPropertyList();
        assertEquals(context().getProperty(Constants.FRAMEWORK_UUID), props.get("uuid"));
        assertEquals(context().getBundle(0).getSymbolicName(), props.get("framework"));
    }

    @Test
    public void testMBeanInterface() throws IOException {
        PackageStateMBean packagaState = getMBean(PackageStateMBean.OBJECTNAME, PackageStateMBean.class);
        assertNotNull(packagaState);

        long[] exportingBundles = packagaState.getExportingBundles("org.osgi.jmx.framework", "1.7.0");
        assertNotNull(exportingBundles);
        assertTrue("Should find a bundle exporting org.osgi.jmx.framework", exportingBundles.length > 0);

        long[] exportingBundles2 = packagaState.getExportingBundles("test", "1.0.0");
        assertNull("Shouldn't find a bundle exporting test package", exportingBundles2);

        long[] importingBundlesId = packagaState
                .getImportingBundles("org.osgi.jmx.framework", "1.7.0", exportingBundles[0]);
        assertTrue("Should find bundles importing org.osgi.jmx.framework", importingBundlesId.length > 0);

        TabularData table = packagaState.listPackages();
        assertNotNull("TabularData containing CompositeData with packages info shouldn't be null", table);
        assertEquals("TabularData should be a type PACKAGES", PackageStateMBean.PACKAGES_TYPE, table.getTabularType());
        Collection<?> colData = table.values();
        assertNotNull("Collection of CompositeData shouldn't be null", colData);
        assertFalse("Collection of CompositeData should contain elements", colData.isEmpty());

        boolean isRemovalPending = packagaState.isRemovalPending("org.osgi.jmx.framework", "1.7.0", exportingBundles[0]);
        assertFalse("Should removal pending on org.osgi.jmx.framework be false", isRemovalPending);
    }

}
