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

import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import static org.junit.Assert.*;

import javax.management.InstanceNotFoundException;
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
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.jmx.framework.BundleStateMBean;
import org.osgi.util.tracker.ServiceTracker;

/**
 * 
 * 
 *
 * @version $Rev$ $Date$
 */
@RunWith(JUnit4TestRunner.class)
public class AbstractIntegrationTest {
    
    public static final long DEFAULT_TIMEOUT = 30000;

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
        
        doSetUp();
    }
    
    /**
     * A hook for subclasses.
     * 
     * @throws Exception
     */
    protected void doSetUp() throws Exception {           
    }
    
    @After
    public void tearDown() throws Exception {
        bundleContext.ungetService(reference);
        //plainRegistration.unregister();
    }
    
    protected void waitForMBean(ObjectName name) throws Exception {
        waitForMBean(name, 10);        
    }
    
    protected void waitForMBean(ObjectName name, int timeoutInSeconds) throws Exception {
        int i=0;
        while (true) {
            try {
                mbeanServer.getObjectInstance(name);
                break;
            } catch (InstanceNotFoundException e) {
                if (i == timeoutInSeconds) {
                    throw new Exception(name + " mbean is not available after waiting " + timeoutInSeconds + " seconds");
                }
            }
            i++;
            Thread.sleep(1000);
        }
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
    
    protected <T> T getOsgiService(Class<T> type, long timeout) {
        return getOsgiService(type, null, timeout);
    }

    protected <T> T getOsgiService(Class<T> type) {
        return getOsgiService(type, null, DEFAULT_TIMEOUT);
    }

    protected <T> T getOsgiService(Class<T> type, String filter, long timeout) {
        ServiceTracker tracker = null;
        try {
            String flt;
            if (filter != null) {
                if (filter.startsWith("(")) {
                    flt = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")" + filter + ")";
                } else {
                    flt = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")(" + filter + "))";
                }
            } else {
                flt = "(" + Constants.OBJECTCLASS + "=" + type.getName() + ")";
            }
            Filter osgiFilter = FrameworkUtil.createFilter(flt);
            tracker = new ServiceTracker(bundleContext, osgiFilter, null);
            tracker.open(true);
            // Note that the tracker is not closed to keep the reference
            // This is buggy, as the service reference may change i think
            Object svc = type.cast(tracker.waitForService(timeout));
            if (svc == null) {
                Dictionary dic = bundleContext.getBundle().getHeaders();
                System.err.println("Test bundle headers: " + explode(dic));

                for (ServiceReference ref : asCollection(bundleContext.getAllServiceReferences(null, null))) {
                    System.err.println("ServiceReference: " + ref);
                }

                for (ServiceReference ref : asCollection(bundleContext.getAllServiceReferences(null, flt))) {
                    System.err.println("Filtered ServiceReference: " + ref);
                }

                throw new RuntimeException("Gave up waiting for service " + flt);
            }
            return type.cast(svc);
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Invalid filter", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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

    /*
     * Explode the dictionary into a ,-delimited list of key=value pairs
     */
    private static String explode(Dictionary dictionary) {
        Enumeration keys = dictionary.keys();
        StringBuffer result = new StringBuffer();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            result.append(String.format("%s=%s", key, dictionary.get(key)));
            if (keys.hasMoreElements()) {
                result.append(", ");
            }
        }
        return result.toString();
    }

    /*
     * Provides an iterable collection of references, even if the original array is null
     */
    private static final Collection<ServiceReference> asCollection(ServiceReference[] references) {
        List<ServiceReference> result = new LinkedList<ServiceReference>();
        if (references != null) {
            for (ServiceReference reference : references) {
                result.add(reference);
            }
        }
        return result;
    }

}