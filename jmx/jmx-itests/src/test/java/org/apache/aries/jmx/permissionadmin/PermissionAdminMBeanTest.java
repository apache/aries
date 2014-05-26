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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.options;

import java.io.IOException;

import org.apache.aries.jmx.AbstractIntegrationTest;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.Bundle;
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
    public Option[] configuration() {
        return options(
            jmxRuntime(),
            bundlea()
                    /* For debugging, uncomment the next two lines */
//                     vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=7778"),
//                     waitForFrameworkStartup()
        );
    }

    @Test
    public void testMBeanInterface() throws IOException {
        PermissionAdminMBean mBean = getMBean(PermissionAdminMBean.OBJECTNAME, PermissionAdminMBean.class);
        PermissionAdmin permAdminService = context().getService(PermissionAdmin.class);
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

}