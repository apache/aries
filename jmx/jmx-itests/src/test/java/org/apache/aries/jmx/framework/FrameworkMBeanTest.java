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

import java.io.IOException;

import javax.management.openmbean.CompositeData;

import static junit.framework.Assert.*;

import org.apache.aries.jmx.AbstractIntegrationTest;
import org.apache.aries.jmx.codec.BatchActionResult;
import org.junit.Assert;
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
        return CoreOptions.options(CoreOptions.equinox(), 
                CoreOptions.mavenBundle().groupId("org.apache.aries.jmx").artifactId("aries-jmx").versionAsInProject()
        );
    }  

    @Test
    public void testSetBundleStartLevels() throws IOException {
        FrameworkMBean framework = getMBean(FrameworkMBean.OBJECTNAME, FrameworkMBean.class);
        assertNotNull(framework);
        long[] bundleIds = new long[]{1,2};
        int[] newlevels = new int[]{1,1};
        CompositeData compData = framework.setBundleStartLevels(bundleIds, newlevels);
        assertNotNull(compData);
        BatchActionResult batch2 = BatchActionResult.from(compData);
        Assert.assertNotNull(batch2.getCompleted());
        Assert.assertTrue(batch2.isSuccess());
        Assert.assertNull(batch2.getError());
        Assert.assertNull(batch2.getRemainingItems());
    }

}