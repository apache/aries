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
package org.apache.aries.jmx.permissionadmin;

import static org.apache.aries.itest.ExtraOptions.mavenBundle;
import static org.apache.aries.itest.ExtraOptions.paxLogging;
import static org.apache.aries.itest.ExtraOptions.testOptions;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.newBundle;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.withBnd;

import java.io.IOException;

import javax.management.ObjectName;

import org.apache.aries.jmx.AbstractIntegrationTest;
import org.junit.Test;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.container.def.PaxRunnerOptions;
import org.ops4j.pax.exam.junit.Configuration;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.jmx.service.permissionadmin.PermissionAdminMBean;
import org.osgi.service.permissionadmin.PermissionAdmin;
import org.osgi.service.permissionadmin.PermissionInfo;

/**
 *
 *
 * @version $Rev$ $Date$
 */
public class PermissionAdminMBeanTest extends AbstractIntegrationTest {

    @Configuration
    public static Option[] configuration() {
        return testOptions(
            PaxRunnerOptions.rawPaxRunnerOption("config", "classpath:ss-runner.properties"),
            CoreOptions.equinox().version("3.8.0.V20120529-1548"),
            paxLogging("INFO"),
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
                    .set(Constants.IMPORT_PACKAGE,
                            "org.osgi.framework;version=1.5.0,org.osgi.util.tracker,org.apache.aries.jmx.test.bundleb.api;version=1.1.0;resolution:=optional")
                    .set(Constants.BUNDLE_ACTIVATOR,
                            org.apache.aries.jmx.test.bundlea.Activator.class.getName())
                    .build(withBnd()))
                    /* For debugging, uncomment the next two lines */
//                     vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=7778"),
//                     waitForFrameworkStartup()
        );
    }

    @Override
    public void doSetUp() throws Exception {
        waitForMBean(new ObjectName(PermissionAdminMBean.OBJECTNAME));
    }

    @Test
    public void testMBeanInterface() throws IOException {
        PermissionAdminMBean mBean = getMBean(PermissionAdminMBean.OBJECTNAME, PermissionAdminMBean.class);
        PermissionAdmin permAdminService = getService(PermissionAdmin.class);
        assertNotNull(permAdminService);

        String[] serviceLocation = permAdminService.getLocations();
        String[] mBeanLocations = mBean.listLocations();
        assertArrayEquals(serviceLocation, mBeanLocations);

        PermissionInfo defPerm = new PermissionInfo("AllPermission", "*", "*");
        permAdminService.setDefaultPermissions(new PermissionInfo[]{defPerm});
        PermissionInfo[] permissions = permAdminService.getDefaultPermissions();
        assertNotNull(permissions);

        String[] encoded = toEncodedPerm(permissions);
        String[] mBeanDefPermissions = mBean.listDefaultPermissions();
        assertArrayEquals(encoded, mBeanDefPermissions);

        Bundle a = context().getBundleByName("org.apache.aries.jmx.test.bundlea");
        assertNotNull(a);

        String location = a.getLocation();

        PermissionInfo bundleaPerm = new PermissionInfo("ServicePermission", "ServiceA", "GET");
        mBean.setPermissions(location, new String[]{bundleaPerm.getEncoded()});

        String[] serviceBundleaPerm = toEncodedPerm(permAdminService.getPermissions(location));
        String[] mBeanBundleaPerm = mBean.getPermissions(location);
        assertNotNull(mBeanBundleaPerm);
        assertArrayEquals(serviceBundleaPerm, mBeanBundleaPerm);

        PermissionInfo defaultPerm = new PermissionInfo("AllPermission", "*", "GET");
        mBean.setDefaultPermissions(new String[]{defaultPerm.getEncoded()});

        String[] serviceDefaultPerm = toEncodedPerm(permAdminService.getDefaultPermissions());
        String[] mBeanDefaultPerm = mBean.listDefaultPermissions();
        assertNotNull(mBeanDefaultPerm);
        assertArrayEquals(serviceDefaultPerm, mBeanDefaultPerm);
    }

    private String[] toEncodedPerm(PermissionInfo[] permissions){
        assertNotNull(permissions);
        String[] encoded = new String[permissions.length];
        for (int i = 0; i < permissions.length; i++) {
            PermissionInfo info = permissions[i];
            encoded[i] = info.getEncoded();
        }
        return encoded;
    }

    private <S> S getService(Class<S> serviceInterface){
        ServiceReference ref =  bundleContext.getServiceReference(serviceInterface.getName());
        if(ref != null){
            Object service = bundleContext.getService(ref);
            if(service != null){
                return (S)service;
            }
        }
        return null;
    }
}