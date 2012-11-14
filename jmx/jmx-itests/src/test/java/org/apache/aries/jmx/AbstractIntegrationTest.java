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
package org.apache.aries.jmx;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * @version $Rev$ $Date$
 */
@RunWith(JUnit4TestRunner.class)
public class AbstractIntegrationTest extends org.apache.aries.itest.AbstractIntegrationTest {

    ServiceRegistration registration;
    ServiceReference reference;
    protected MBeanServer mbeanServer;

    @Before
    public void setUp() throws Exception {
        mbeanServer = MBeanServerFactory.createMBeanServer();

        registration = bundleContext.registerService(MBeanServer.class
                .getCanonicalName(), mbeanServer, null);

        String key = MBeanServer.class.getCanonicalName();
        System.out.println(key);

        reference = bundleContext.getServiceReference(key);
        assertNotNull(reference);
        MBeanServer mbeanService = (MBeanServer) bundleContext.getService(reference);
        assertNotNull(mbeanService);

        doSetUp();
    }

    /**
     * A hook for subclasses.
     *
     * @throws Exception
     */
    protected void doSetUp() throws Exception {}

    @After
    public void tearDown() throws Exception {
        bundleContext.ungetService(reference);
        //plainRegistration.unregister();
    }

    protected ObjectName waitForMBean(ObjectName name) throws Exception {
        return waitForMBean(name, 10);
    }

    protected ObjectName waitForMBean(ObjectName name, int timeoutInSeconds) throws Exception {
        int i=0;
        while (true) {
            ObjectName queryName = new ObjectName(name.toString() + ",*");
            Set<ObjectName> result = mbeanServer.queryNames(queryName, null);
            if (result.size() > 0)
                return result.iterator().next();

            if (i == timeoutInSeconds)
                throw new Exception(name + " mbean is not available after waiting " + timeoutInSeconds + " seconds");

            i++;
            Thread.sleep(1000);
        }
    }

    @SuppressWarnings("unchecked")
    protected <T> T getMBean(String name, Class<T> type) {
        ObjectName objectName = null;
        try {
            objectName = new ObjectName(name + ",*");
        } catch (Exception e) {
            fail(e.toString());
        }
        assertNotNull(mbeanServer);
        assertNotNull(objectName);

        Set<ObjectName> names = mbeanServer.queryNames(objectName, null);
        if (names.size() == 0) {
            fail("Object name not found: " + objectName);
        }

        T mbean = MBeanServerInvocationHandler.newProxyInstance(mbeanServer, names.iterator().next(),
                type, false);
        return mbean;
    }

    protected <T> T getMBean(ObjectName objectName, Class<T> type) {
        T mbean = MBeanServerInvocationHandler.newProxyInstance(mbeanServer, objectName, type, false);
        return mbean;
    }
}