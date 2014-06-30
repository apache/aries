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
package org.apache.aries.jmx.whiteboard.integration;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import junit.framework.TestCase;

import org.apache.aries.jmx.whiteboard.integration.helper.IntegrationTestBase;
import org.apache.aries.jmx.whiteboard.integration.helper.TestClass;
import org.apache.aries.jmx.whiteboard.integration.helper.TestClassMBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.ServiceRegistration;

/**
 * The <code>MBeanTest</code> tests MBean registration with MBean Servers
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class MBeanServerTest extends IntegrationTestBase {

    @Test
    public void test_MServerBean() throws Exception {
        final String instanceName = "simple.test.instance";
        final String objectNameString = "domain:instance=" + instanceName;
        final ObjectName objectName = new ObjectName(objectNameString);
        final TestClass testInstance = new TestClass(instanceName);

        // get or create the dynamic MBean Server
        final MBeanServer server = getOrCreateMBeanServer();

        // MBean server not registered as service, unknown object
        assertNotRegistered(server, objectName);

        // expect the MBean to be registered with the static server
        final ServiceRegistration mBeanReg = registerService(
            TestClassMBean.class.getName(), testInstance, objectNameString);

        // MBean server not registered, expect object to not be known
        assertNotRegistered(server, objectName);

        // register MBean server, expect MBean registered
        ServiceRegistration mBeanServerReg = registerMBeanServer(server);
        assertRegistered(server, objectName);

        // expect MBean to return expected value
        TestCase.assertEquals(instanceName,
            server.getAttribute(objectName, "InstanceName"));

        // unregister MBean server, expect MBean to be unregistered
        mBeanServerReg.unregister();
        assertNotRegistered(server, objectName);

        // unregister MBean, expect to not be registered any more
        mBeanReg.unregister();
        assertNotRegistered(server, objectName);
    }

}
