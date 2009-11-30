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

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.jmx.service.permissionadmin.PermissionAdminMBean;
import org.osgi.service.permissionadmin.PermissionInfo;

/**
 * {@link PermissionAdminMBean} test case.
 * 
 * 
 * @version $Rev$ $Date$
 */
public class PermissionAdminTest {

    @Mock
    private org.osgi.service.permissionadmin.PermissionAdmin permAdmin;
    private PermissionAdminMBean mbean;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mbean = new PermissionAdmin(permAdmin);
    }

    @Test
    public void testGetPermissions() throws IOException {
        PermissionInfo info = new PermissionInfo("Admin", "test", "get");
        PermissionInfo[] permInfos = new PermissionInfo[] { info, info };

        Mockito.when(permAdmin.getPermissions(Mockito.anyString())).thenReturn(permInfos);
        String[] permissions = mbean.getPermissions("test");

        Assert.assertNotNull(permissions);
        Assert.assertEquals(2, permissions.length);
        Assert.assertArrayEquals("Checks encoded permissions", new String[] { info.getEncoded(), info.getEncoded() },
                permissions);
        
        Mockito.reset(permAdmin);
        Mockito.when(permAdmin.getPermissions(Mockito.anyString())).thenReturn(null);
        String[] permissions2 = mbean.getPermissions("test");

        Assert.assertNull(permissions2);
    }

    @Test
    public void testListDefaultPermissions() throws IOException {
        PermissionInfo info = new PermissionInfo("Admin", "test", "get");
        PermissionInfo[] permInfos = new PermissionInfo[] { info, info };

        Mockito.when(permAdmin.getDefaultPermissions()).thenReturn(permInfos);
        String[] permissions = mbean.listDefaultPermissions();

        Assert.assertNotNull(permissions);
        Assert.assertEquals(2, permissions.length);
        Assert.assertArrayEquals("Checks encoded default permissions", new String[] { info.getEncoded(), info.getEncoded() },
                permissions);
        
        Mockito.reset(permAdmin);
        Mockito.when(permAdmin.getDefaultPermissions()).thenReturn(null);
        String[] permissions2 = mbean.listDefaultPermissions();

        Assert.assertNull(permissions2);
    }

    @Test
    public void testListLocations() throws IOException {
        String[] locations1 = new String[] { "test1", "test2" };
        Mockito.when(permAdmin.getLocations()).thenReturn(locations1);
        String[] locations2 = mbean.listLocations();
        Assert.assertNotNull(locations2);
        Assert.assertEquals(2, locations2.length);
        Assert.assertSame(locations1, locations2);
    }

    @Test
    public void testSetDefaultPermissions() throws IOException {
        PermissionInfo info1 = new PermissionInfo("Admin", "test", "get");
        PermissionInfo info2 = new PermissionInfo("Admin", "test2", "get");
        PermissionInfo[] permInfos = new PermissionInfo[] { info1, info2 };
        String[] encodedPermissions = new String[2];
        int i = 0;
        for (PermissionInfo info : permInfos) {
            encodedPermissions[i++] = info.getEncoded();
        }
        mbean.setDefaultPermissions(encodedPermissions);
        Mockito.verify(permAdmin).setDefaultPermissions(permInfos);
    }

    @Test
    public void testSetPermissions() throws IOException {
        PermissionInfo info1 = new PermissionInfo("Admin", "test", "set");
        PermissionInfo info2 = new PermissionInfo("Admin", "test2", "set");
        PermissionInfo[] permInfos = new PermissionInfo[] { info1, info2 };
        String[] encodedPermissions = new String[2];
        int i = 0;
        for (PermissionInfo info : permInfos) {
            encodedPermissions[i++] = info.getEncoded();
        }
        mbean.setPermissions("test", encodedPermissions);
        Mockito.verify(permAdmin).setPermissions("test", permInfos);
    }

}
