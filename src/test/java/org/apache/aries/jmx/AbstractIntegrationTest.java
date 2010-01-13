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

import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import static org.junit.Assert.*;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;

/**
 * 
 * 
 *
 * @version $Rev$ $Date$
 */
@RunWith(JUnit4TestRunner.class)
public class AbstractIntegrationTest {
    
    ServiceRegistration registration;
    ServiceReference reference;
    protected MBeanServer mbeanServer;

    @Inject
    protected BundleContext bundleContext;
    
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
    }
    
    @After
    public void tearDown() throws Exception {
        bundleContext.ungetService(reference);
        //plainRegistration.unregister();
    }
    
    @SuppressWarnings("unchecked")
    protected <T> T getMBean(String name, Class<T> type) {
        ObjectName objectName = null;
        try {
            objectName = new ObjectName(name);
        } catch (Exception e) {
            fail(e.toString());
        }
        assertNotNull(mbeanServer);
        assertNotNull(objectName);
        T mbean = (T) MBeanServerInvocationHandler.newProxyInstance(mbeanServer, objectName,
                type, false);
        return mbean;
    }
    
    protected Bundle getBundle(String symbolicName) {
        return getBundle(symbolicName, null);
    }
    
    protected Bundle getBundle(String bundleSymbolicName, String version) {
        Bundle result = null;
        for (Bundle b : bundleContext.getBundles()) {
            if ( b.getSymbolicName().equals(bundleSymbolicName) ) {
                if (version == null || b.getVersion().equals(Version.parseVersion(version))) {
                    result = b;
                    break;
                }
            }
        }
        return result;
    }
    
    public static MavenArtifactProvisionOption mavenBundle(String groupId, String artifactId) {
        return CoreOptions.mavenBundle().groupId(groupId).artifactId(artifactId).versionAsInProject();
    }

    protected static Option[] updateOptions(Option[] options) {
        // We need to add pax-exam-junit here when running with the ibm
        // jdk to avoid the following exception during the test run:
        // ClassNotFoundException: org.ops4j.pax.exam.junit.Configuration
        if ("IBM Corporation".equals(System.getProperty("java.vendor"))) {
            Option[] ibmOptions = options(
                wrappedBundle(mavenBundle("org.ops4j.pax.exam", "pax-exam-junit"))
            );
            options = combine(ibmOptions, options);
        }

        return options;
    }

}