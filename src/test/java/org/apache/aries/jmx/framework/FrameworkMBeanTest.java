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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import org.apache.aries.jmx.AbstractIntegrationTest;
import org.apache.aries.jmx.codec.BatchActionResult;
import org.junit.Test;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.osgi.jmx.framework.FrameworkMBean;

/**
 * 
 * 
 * @version $Rev$ $Date$
 */
public class FrameworkMBeanTest extends AbstractIntegrationTest {    

    @Configuration
    public static Option[] configuration() {
        
        Option[] options = CoreOptions.options(
            CoreOptions.equinox(),
            mavenBundle("org.ops4j.pax.logging", "pax-logging-api"), 
            mavenBundle("org.ops4j.pax.logging", "pax-logging-service"), 
            mavenBundle("org.apache.aries.jmx", "org.apache.aries.jmx")
        );
        
        options = updateOptions(options);
        return options;
    }

    @Override
    public void doSetUp() throws Exception {
        waitForMBean(new ObjectName(FrameworkMBean.OBJECTNAME));
    }
    
    @Test
    public void testMBeanInterface() throws IOException {
        FrameworkMBean framework = getMBean(FrameworkMBean.OBJECTNAME, FrameworkMBean.class);
        assertNotNull(framework);
        
        long[] bundleIds = new long[]{1,2};
        int[] newlevels = new int[]{1,1};
        CompositeData compData = framework.setBundleStartLevels(bundleIds, newlevels);
        assertNotNull(compData);
        
        BatchActionResult batch2 = BatchActionResult.from(compData);
        assertNotNull(batch2.getCompleted());
        assertTrue(batch2.isSuccess());
        assertNull(batch2.getError());
        assertNull(batch2.getRemainingItems());
                
        File file = File.createTempFile("bundletest", ".jar");
        file.deleteOnExit();        
        Manifest man = new Manifest();
        man.getMainAttributes().putValue("Manifest-Version", "1.0");
        JarOutputStream jaros = new JarOutputStream(new FileOutputStream(file), man);
        jaros.flush();
        jaros.close();
        
        long bundleId = 0;
        try {
            bundleId = framework.installBundleFromURL(file.getAbsolutePath(), file.toURI().toString());
        } catch (Exception e) {
            fail("Installation of test bundle shouldn't fail");
        }
        
        try{
            framework.uninstallBundle(bundleId);
        } catch (Exception e) {
            fail("Uninstallation of test bundle shouldn't fail");
        }
    }

}