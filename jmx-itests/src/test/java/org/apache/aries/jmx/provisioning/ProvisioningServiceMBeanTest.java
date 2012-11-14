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
package org.apache.aries.jmx.provisioning;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.osgi.service.provisioning.ProvisioningService.PROVISIONING_AGENT_CONFIG;
import static org.osgi.service.provisioning.ProvisioningService.PROVISIONING_REFERENCE;

import static org.apache.aries.itest.ExtraOptions.*;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Dictionary;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import javax.management.ObjectName;
import javax.management.openmbean.TabularData;

import org.apache.aries.jmx.AbstractIntegrationTest;
import org.apache.aries.jmx.codec.PropertyData;
import org.junit.Ignore;
import org.junit.Test;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.osgi.jmx.JmxConstants;
import org.osgi.jmx.service.provisioning.ProvisioningServiceMBean;
import org.osgi.service.provisioning.ProvisioningService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * 
 * 
 * @version $Rev$ $Date$
 */
public class ProvisioningServiceMBeanTest extends AbstractIntegrationTest {


    @Configuration
    public static Option[] configuration() {
        return testOptions(
                        CoreOptions.equinox(),
                        paxLogging("INFO"),
                        mavenBundle("org.osgi", "org.osgi.compendium"), 
                        mavenBundle("org.apache.aries.jmx", "org.apache.aries.jmx.whiteboard"),
                        mavenBundle("org.apache.aries.jmx", "org.apache.aries.jmx"),
                        mavenBundle("org.apache.aries", "org.apache.aries.util"));
    }

    @Override
    public void doSetUp() throws Exception {
        waitForMBean(new ObjectName(ProvisioningServiceMBean.OBJECTNAME));
    }

    @Ignore("For now.. Cannot find public repo for org.eclipse.equinox.ip")
    @Test
    @SuppressWarnings("unchecked")
    public void testMBeanInterface() throws Exception {

        ProvisioningServiceMBean mbean = getMBean(ProvisioningServiceMBean.OBJECTNAME, ProvisioningServiceMBean.class);
        assertNotNull(mbean);
        
        ServiceTracker tracker = new ServiceTracker(bundleContext, ProvisioningService.class.getName(), null);
        tracker.open();
        ProvisioningService ps = (ProvisioningService) tracker.getService();
        assertNotNull(ps);
        
        Dictionary<String, Object> info;
        
        // add information URL (create temp zip file)
        
        File  provZip = File.createTempFile("Prov-jmx-itests", ".zip");
        Manifest man = new Manifest();
        man.getMainAttributes().putValue("Manifest-Version", "1.0");
        man.getMainAttributes().putValue("Content-Type", "application/zip");
        JarOutputStream jout = new JarOutputStream(new FileOutputStream(provZip), man);
        ZipEntry entry = new ZipEntry(PROVISIONING_AGENT_CONFIG);
        jout.putNextEntry( entry );
        jout.write(new byte[] { 10, 20, 30 });
        jout.closeEntry();
        jout.flush();
        jout.close();
        
        provZip.deleteOnExit();
        
        mbean.addInformationFromZip(provZip.toURL().toExternalForm());
        
        //check the info has been added
        
        info = ps.getInformation();
        assertNotNull(info);
        assertTrue(info.size() >= 1);
        byte[] config = (byte[]) info.get(PROVISIONING_AGENT_CONFIG);
        assertNotNull(config);
        assertArrayEquals(new byte[] { 10, 20, 30 }, config);
        
        
        // test list information
        
        TabularData data = mbean.listInformation();
        assertNotNull(data);
        assertEquals(JmxConstants.PROPERTIES_TYPE, data.getTabularType());
        assertTrue(data.values().size() >= 1);
        PropertyData<byte[]> configEntry = PropertyData.from(data.get(new Object[] {PROVISIONING_AGENT_CONFIG }));
        assertNotNull(configEntry);
        assertArrayEquals(new byte[] { 10, 20, 30 }, configEntry.getValue());

        
        // test add information
        
        PropertyData<String> reference = PropertyData.newInstance(PROVISIONING_REFERENCE, "rsh://0.0.0.0/provX");
        data.put(reference.toCompositeData());
        
        mbean.addInformation(data);
        
        info = ps.getInformation();
        assertNotNull(info);
        assertTrue(info.size() >= 2);
        config = (byte[]) info.get(PROVISIONING_AGENT_CONFIG);
        assertNotNull(config);
        assertArrayEquals(new byte[] { 10, 20, 30 }, config);
        String ref = (String) info.get(PROVISIONING_REFERENCE);
        assertNotNull(ref);
        assertEquals("rsh://0.0.0.0/provX", ref);
        
        
        // test set information
        
        data.clear();
        PropertyData<String> newRef = PropertyData.newInstance(PROVISIONING_REFERENCE, "rsh://0.0.0.0/newProvRef");
        data.put(newRef.toCompositeData());
        
        mbean.setInformation(data);
        info = ps.getInformation();
        assertNotNull(info);
        assertTrue(info.size() >= 1);
        assertNull(info.get(PROVISIONING_AGENT_CONFIG));
       
        ref = (String) info.get(PROVISIONING_REFERENCE);
        assertNotNull(ref);
        assertEquals("rsh://0.0.0.0/newProvRef", ref);
        
        
    }
}
