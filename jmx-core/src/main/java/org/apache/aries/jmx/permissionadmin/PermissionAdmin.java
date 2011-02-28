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

import org.osgi.jmx.service.permissionadmin.PermissionAdminMBean;
import org.osgi.service.permissionadmin.PermissionInfo;

/**
 * <p>
 * <tt>PermissionAdmin</tt> represents implementation of PermissionAdminMBean.
 * </p>
 * @see PermissionAdminMBean
 * 
 * @version $Rev$ $Date$
 */
public class PermissionAdmin implements PermissionAdminMBean {

    /**
     * {@link org.osgi.service.permissionadmin.PermissionAdmin} service.
     */
    private org.osgi.service.permissionadmin.PermissionAdmin permAdmin;

    /**
     * Constructs new PermissionAdmin MBean. 
     * 
     * @param permAdmin {@link org.osgi.service.permissionadmin.PermissionAdmin} service reference.
     */
    public PermissionAdmin(org.osgi.service.permissionadmin.PermissionAdmin permAdmin) {
        this.permAdmin = permAdmin;
    }

    /**
     * @see org.osgi.jmx.service.permissionadmin.PermissionAdminMBean#getPermissions(java.lang.String)
     */
    public String[] getPermissions(String location) throws IOException {
        PermissionInfo[] permissions = permAdmin.getPermissions(location);
        if (permissions != null) {
            String[] encoded = new String[permissions.length];
            for (int i = 0; i < permissions.length; i++) {
                PermissionInfo info = permissions[i];
                encoded[i] = info.getEncoded();
            }
            return encoded;
        }
        return null;
    }

    /**
     * @see org.osgi.jmx.service.permissionadmin.PermissionAdminMBean#listDefaultPermissions()
     */
    public String[] listDefaultPermissions() throws IOException {
        PermissionInfo[] permissions = permAdmin.getDefaultPermissions();
        if (permissions != null) {
            String[] encoded = new String[permissions.length];
            for (int i = 0; i < permissions.length; i++) {
                PermissionInfo info = permissions[i];
                encoded[i] = info.getEncoded();
            }
            return encoded;
        }
        return null;
    }

    /**
     * @see org.osgi.jmx.service.permissionadmin.PermissionAdminMBean#listLocations()
     */
    public String[] listLocations() throws IOException {
        return permAdmin.getLocations();
    }

    /**
     * @see org.osgi.jmx.service.permissionadmin.PermissionAdminMBean#setDefaultPermissions(java.lang.String[])
     */
    public void setDefaultPermissions(String[] encodedPermissions) throws IOException {
        if(encodedPermissions == null){
            //default permissions are to be removed
            permAdmin.setDefaultPermissions(null);
            return;
        }
        PermissionInfo[] permissions = new PermissionInfo[encodedPermissions.length];
        for (int i = 0; i < encodedPermissions.length; i++) {
            permissions[i] = new PermissionInfo(encodedPermissions[i]);
        }
        permAdmin.setDefaultPermissions(permissions);

    }

    /**
     * @see org.osgi.jmx.service.permissionadmin.PermissionAdminMBean#setPermissions(java.lang.String,
     *      java.lang.String[])
     */
    public void setPermissions(String location, String[] encodedPermissions) throws IOException {
        if(encodedPermissions == null){
            //default permissions are to be removed
            permAdmin.setPermissions(location, null);
            return;
        }
        PermissionInfo[] permissions = new PermissionInfo[encodedPermissions.length];
        for (int i = 0; i < encodedPermissions.length; i++) {
            permissions[i] = new PermissionInfo(encodedPermissions[i]);
        }
        permAdmin.setPermissions(location, permissions);
    }

}
