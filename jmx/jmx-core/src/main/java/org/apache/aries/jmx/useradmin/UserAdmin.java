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
package org.apache.aries.jmx.useradmin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;

import org.apache.aries.jmx.codec.AuthorizationData;
import org.apache.aries.jmx.codec.GroupData;
import org.apache.aries.jmx.codec.PropertyData;
import org.apache.aries.jmx.codec.RoleData;
import org.apache.aries.jmx.codec.UserData;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.jmx.JmxConstants;
import org.osgi.jmx.service.useradmin.UserAdminMBean;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

/**
 * <p>
 * <tt>UserAdmin</tt> represents {@link UserAdminMBean} implementation.
 * </p>
 * 
 * @see UserAdminMBean
 * 
 * @version $Rev$ $Date$
 */
public class UserAdmin implements UserAdminMBean {

    /**
     * @see org.osgi.service.useradmin.UserAdmin service reference;
     */
    private org.osgi.service.useradmin.UserAdmin userAdmin;

    /**
     * Constructs new UserAdmin MBean.
     * 
     * @param userAdmin
     *            {@link UserAdmin} service reference.
     */
    public UserAdmin(org.osgi.service.useradmin.UserAdmin userAdmin) {
        this.userAdmin = userAdmin;
    }

    /**
     * Validate Role against roleType.
     * 
     * @see Role#USER
     * @see Role#GROUP
     * @see Role#USER_ANYONE
     * 
     * @param role
     *            Role instance.
     * @param roleType
     *            role type.
     */
    private void validateRoleType(Role role, int roleType) throws IOException {
        if (role.getType() != roleType) {
            throw new IOException("Unexpected role type. Expected " + roleType + " but got " + role.getType());
        }
    }

    /**
     * @see org.osgi.jmx.service.useradmin.UserAdminMBean#addCredential(java.lang.String, byte[], java.lang.String)
     */
    public void addCredential(String key, byte[] value, String username) throws IOException {
        addCredential(key, (Object)value, username);
    }
    
    /**
     * @see org.osgi.jmx.service.useradmin.UserAdminMBean#addCredentialString(String, String, String)
     */
    public void addCredentialString(String key, String value, String username) throws IOException {
        addCredential(key, (Object)value, username);
    }

    private void addCredential(String key, Object value, String username) throws IOException {
        if (username == null) {
            throw new IOException("User name cannot be null");
        }
        if (key == null) {
            throw new IOException("Credential key cannot be null");
        }
        Role role = userAdmin.getRole(username);
        if (role == null) {
            throw new IOException("Operation fails user with provided username = [" + username + "] doesn't exist");
        }
        validateRoleType(role, Role.USER);
        Dictionary<String, Object> credentials = ((User) role).getCredentials();
        if (credentials != null) {
            credentials.put(key, value);
        }
    }
    
    /**
     * @see org.osgi.jmx.service.useradmin.UserAdminMBean#addMember(java.lang.String, java.lang.String)
     */
    public boolean addMember(String groupname, String rolename) throws IOException {
        if (groupname == null) {
            throw new IOException("Group name cannot be null");
        }
        if (rolename == null) {
            throw new IOException("Role name cannot be null");
        }
        Role group = userAdmin.getRole(groupname);
        Role member = userAdmin.getRole(rolename);
        if (group == null) {
            throw new IOException("Operation fails role with provided groupname = [" + groupname + "] doesn't exist");
        }
        validateRoleType(group, Role.GROUP);
        return ((Group) group).addMember(member);
    }

    /**
     * @see org.osgi.jmx.service.useradmin.UserAdminMBean#addPropertyString(String, String, String)
     */
    public void addPropertyString(String key, String value, String rolename) throws IOException {
        addRoleProperty(key, value, rolename);
    }

    /**
     * @see org.osgi.jmx.service.useradmin.UserAdminMBean#addProperty(java.lang.String, byte[], java.lang.String)
     */
    public void addProperty(String key, byte[] value, String rolename) throws IOException {
        addRoleProperty(key, value, rolename); 
    }
    
    /**
     * @see UserAdminMBean#addProperty(String, byte[], String)
     * @see UserAdminMBean#addProperty(String, String, String)
     */
    private void addRoleProperty(String key, Object value, String rolename) throws IOException {
        if (rolename == null) {
            throw new IOException("Role name cannot be null");
        }
        if (key == null) {
            throw new IOException("Property key cannot be null");
        }
        Role role = userAdmin.getRole(rolename);
        if (role == null) {
            throw new IOException("Operation fails role with provided rolename = [" + rolename + "] doesn't exist");
        }
        Dictionary<String, Object>  properties = role.getProperties();
        if (properties != null) {
            properties.put(key, value);
        }        
    }

    /**
     * @see org.osgi.jmx.service.useradmin.UserAdminMBean#addRequiredMember(java.lang.String, java.lang.String)
     */
    public boolean addRequiredMember(String groupname, String rolename) throws IOException {
        if (groupname == null) {
            throw new IOException("Group name cannot be null");
        }
        if (rolename == null) {
            throw new IOException("Role name cannot be null");
        }
        Role group = userAdmin.getRole(groupname);
        Role member = userAdmin.getRole(rolename);
        if (group == null) {
            throw new IOException("Operation fails role with provided groupname = [" + groupname + "] doesn't exist");
        }
        validateRoleType(group, Role.GROUP);
        return ((Group) group).addRequiredMember(member);
    }

    /**
     * @see org.osgi.jmx.service.useradmin.UserAdminMBean#createGroup(java.lang.String)
     */
    public void createGroup(String name) throws IOException {
        if (name == null) {
            throw new IOException("Group name cannot be null");
        }
        userAdmin.createRole(name, Role.GROUP);
    }

    /**
     * @see org.osgi.jmx.service.useradmin.UserAdminMBean#createRole(java.lang.String)
     */
    public void createRole(String name) throws IOException {
        throw new IOException("Deprecated: use createGroup or createUser");
    }

    /**
     * @see org.osgi.jmx.service.useradmin.UserAdminMBean#createUser(java.lang.String)
     */
    public void createUser(String name) throws IOException {
        if (name == null) {
            throw new IOException("User name cannot be null");
        }
        userAdmin.createRole(name, Role.USER);
    }

    /**
     * @see org.osgi.jmx.service.useradmin.UserAdminMBean#getAuthorization(java.lang.String)
     */
    public CompositeData getAuthorization(String username) throws IOException {
        if (username== null) {
            throw new IOException("User name cannot be null");
        }
        Role role = userAdmin.getRole(username);
        if (role == null) {
            return null;
        }
        validateRoleType(role, Role.USER);
        Authorization auth = userAdmin.getAuthorization((User) role);
        if (auth == null) {
            return null;
        }

        return new AuthorizationData(auth).toCompositeData();
    }

    /**
     * @see org.osgi.jmx.service.useradmin.UserAdminMBean#getCredentials(java.lang.String)
     */
    public TabularData getCredentials(String username) throws IOException {
        if (username == null) {
            throw new IOException("User name cannot be null");
        }
        Role role = userAdmin.getRole(username);
        if (role == null) {
            return null;
        }
        validateRoleType(role, Role.USER);
        Dictionary<String, Object> credentials = ((User) role).getCredentials();
        if (credentials == null) {
            return null;
        }
        TabularData data = new TabularDataSupport(JmxConstants.PROPERTIES_TYPE);
        for (Enumeration<String> keys = credentials.keys(); keys.hasMoreElements();) {
            String key = keys.nextElement();
            data.put(PropertyData.newInstance(key, credentials.get(key)).toCompositeData());
        }
        return data;
    }

    /**
     * @see org.osgi.jmx.service.useradmin.UserAdminMBean#getGroup(java.lang.String)
     */
    public CompositeData getGroup(String groupname) throws IOException {
        if (groupname == null) {
            throw new IOException("Group name cannot be null");
        }
        Role role = userAdmin.getRole(groupname);
        if (role == null) {
            return null;
        }
        validateRoleType(role, Role.GROUP);
        return new GroupData((Group) role).toCompositeData();
    }

    /**
     * @see org.osgi.jmx.service.useradmin.UserAdminMBean#getGroups(java.lang.String)
     */
    public String[] getGroups(String filter) throws IOException {
        Role[] roles = null;
        try {
            roles = userAdmin.getRoles(filter);
        } catch (InvalidSyntaxException ise) {
            IOException ioex = new IOException("Operation fails illegal filter provided: " + filter);
            ioex.initCause(ise);
            throw ioex;
        }

        if (roles == null) {
            return null;
        }

        return getRoleByType(roles, Role.GROUP);
    }

    /**
     * @see org.osgi.jmx.service.useradmin.UserAdminMBean#getImpliedRoles(java.lang.String)
     */
    public String[] getImpliedRoles(String username) throws IOException {
        if (username == null) {
            throw new IOException("User name cannot be null");
        }
        Role role = userAdmin.getRole(username);
        if (role != null) {
            validateRoleType(role, Role.USER);
            Authorization auth = userAdmin.getAuthorization((User) role);
            if (auth != null) {
                return auth.getRoles();
            }
        }
        return null;
    }

    /**
     * @see org.osgi.jmx.service.useradmin.UserAdminMBean#getMembers(java.lang.String)
     */
    public String[] getMembers(String groupname) throws IOException {
        if (groupname == null) {
            throw new IOException("Group name cannot be null");
        }
        Role role = userAdmin.getRole(groupname);
        if (role != null) {
            validateRoleType(role, Role.GROUP);
            Role[] roles = ((Group) role).getMembers();
            if (roles != null) {
                String[] members = new String[roles.length];
                for (int i = 0; i < roles.length; i++) {
                    members[i] = roles[i].getName();
                }
                return members;
            }
        }
        return null;
    }

    /**
     * @see org.osgi.jmx.service.useradmin.UserAdminMBean#getProperties(java.lang.String)
     */
    public TabularData getProperties(String rolename) throws IOException {
        if (rolename == null) {
            throw new IOException("Role name cannot be null");
        }
        Role role = userAdmin.getRole(rolename);
        if (role == null) {
            return null;
        }
        Dictionary<String, Object> properties = role.getProperties();
        if (properties == null) {
            return null;
        }
        TabularData data = new TabularDataSupport(JmxConstants.PROPERTIES_TYPE);
        for (Enumeration<String> keys = properties.keys(); keys.hasMoreElements();) {
            String key = keys.nextElement();
            data.put(PropertyData.newInstance(key, properties.get(key)).toCompositeData());
        }
        return data;

    }

    /**
     * @see org.osgi.jmx.service.useradmin.UserAdminMBean#getRequiredMembers(java.lang.String)
     */
    public String[] getRequiredMembers(String groupname) throws IOException {
        if (groupname == null) {
            throw new IOException("Group name cannot be null");
        }
        Role role = userAdmin.getRole(groupname);
        if (role != null) {
            validateRoleType(role, Role.GROUP);
            Role[] roles = ((Group) role).getRequiredMembers();
            if (roles != null) {
                String[] reqMembers = new String[roles.length];
                for (int i = 0; i < roles.length; i++) {
                    reqMembers[i] = roles[i].getName();
                }
                return reqMembers;
            }
        }
        return null;
    }

    /**
     * @see org.osgi.jmx.service.useradmin.UserAdminMBean#getRole(java.lang.String)
     */
    public CompositeData getRole(String name) throws IOException {
        if (name == null) {
            throw new IOException("Role name cannot be null");
        }
        Role role = userAdmin.getRole(name);
        if (role == null) {
            return null;
        }
        return new RoleData(role).toCompositeData();
    }

    /**
     * @see org.osgi.jmx.service.useradmin.UserAdminMBean#getRoles(java.lang.String)
     */
    public String[] getRoles(String filter) throws IOException {
        Role[] roles = null;
        try {
            roles = userAdmin.getRoles(filter);
        } catch (InvalidSyntaxException ise) {
            IOException ioex = new IOException("Operation fails illegal filter provided: " + filter);
            ioex.initCause(ise);
            throw ioex;
        }
        if (roles == null) {
            return null;
        }
        return getRoleByType(roles, Role.ROLE);
    }

    /**
     * @see org.osgi.jmx.service.useradmin.UserAdminMBean#getUser(java.lang.String)
     */
    public CompositeData getUser(String username) throws IOException {
        if (username == null) {
            throw new IOException("User name cannot be null");
        }
        Role role = userAdmin.getRole(username);
        if (role == null) {
            return null;
        }
        validateRoleType(role, Role.USER);
        return new UserData((User) role).toCompositeData();
    }

    /**
     * @see org.osgi.jmx.service.useradmin.UserAdminMBean#getUserWithProperty(String, String)
     */
    public String getUserWithProperty(String key, String value) throws IOException {
        if (key == null) {
            throw new IOException("Property key cannot be null");
        }
        User user = userAdmin.getUser(key, value);
        if (user == null) {
            return null;
        }
        return user.getName();
    }

    /**
     * @see org.osgi.jmx.service.useradmin.UserAdminMBean#getUsers(java.lang.String)
     */
    public String[] getUsers(String filter) throws IOException {
        Role[] roles = null;
        try {
            roles = userAdmin.getRoles(filter);
        } catch (InvalidSyntaxException ise) {
            IOException ioex = new IOException("Operation fails illegal filter provided: " + filter);
            ioex.initCause(ise);
            throw ioex;
        }
        if (roles == null) {
            return null;
        }
        return getRoleByType(roles, Role.USER);
    }

    /**
     * @see org.osgi.jmx.service.useradmin.UserAdminMBean#listGroups()
     */
    public String[] listGroups() throws IOException {
        Role[] roles = null;
        try {
            roles = userAdmin.getRoles(null);
        } catch (InvalidSyntaxException e) {
            // shouldn't happened we are not using filter
        }
        if (roles == null) {
            return null;
        }
        return getRoleByType(roles, Role.GROUP);
    }

    /**
     * @see org.osgi.jmx.service.useradmin.UserAdminMBean#listRoles()
     */
    public String[] listRoles() throws IOException {
        Role[] roles = null;
        try {
            roles = userAdmin.getRoles(null);
        } catch (InvalidSyntaxException e) {
            // shouldn't happened we are not using filter
        }

        if (roles == null) {
            return null;
        }

        return getRoleByType(roles, Role.ROLE);
    }

    /**
     * @see org.osgi.jmx.service.useradmin.UserAdminMBean#listUsers()
     */
    public String[] listUsers() throws IOException {
        Role[] roles = null;
        try {
            roles = userAdmin.getRoles(null);
        } catch (InvalidSyntaxException e) {
            // shouldn't happened we are not using filter
        }
        if (roles == null) {
            return null;
        }
        return getRoleByType(roles, Role.USER);
    }

    /**
     * Gets role names by type from provided roles array.
     * 
     * @param roles
     *            array of Role's.
     * @param roleType
     *            role Type.
     * @return array of role names.
     */
    private String[] getRoleByType(Role[] roles, int roleType) {
        List<String> rs = new ArrayList<String>();
        for (Role role : roles) {
            if (roleType == Role.ROLE) {
                rs.add(role.getName());
                continue;
            }

            if (role.getType() == roleType) {
                rs.add(role.getName());
            }
        }
        return rs.toArray(new String[rs.size()]);
    }

    /**
     * @see org.osgi.jmx.service.useradmin.UserAdminMBean#removeCredential(java.lang.String, java.lang.String)
     */
    public void removeCredential(String key, String username) throws IOException {
        if (username == null) {
            throw new IOException("User name cannot be null");
        }
        if (key == null) {
            throw new IOException("Credential key cannot be null");
        }
        Role role = userAdmin.getRole(username);
        if (role == null) {
            throw new IOException("Operation fails can't find user with username = [" + username + "] doesn't exist");
        }
        validateRoleType(role, Role.USER);
        ((User) role).getCredentials().remove(key);
    }

    /**
     * @see org.osgi.jmx.service.useradmin.UserAdminMBean#removeGroup(java.lang.String)
     */
    public boolean removeGroup(String name) throws IOException {
        if (name == null) {
            throw new IOException("Group name cannot be null");
        }
        return userAdmin.removeRole(name);
    }

    /**
     * @see org.osgi.jmx.service.useradmin.UserAdminMBean#removeMember(java.lang.String, java.lang.String)
     */
    public boolean removeMember(String groupname, String rolename) throws IOException {
        if (groupname == null) {
            throw new IOException("Group name cannot be null");
        }
        if (rolename == null) {
            throw new IOException("Role name cannot be null");
        }
        Role group = userAdmin.getRole(groupname);
        Role member = userAdmin.getRole(rolename);
        if (group == null) {
            throw new IOException("Operation fails role with provided groupname = [" + groupname + "] doesn't exist");
        }
        validateRoleType(group, Role.GROUP);
        return ((Group) group).removeMember(member);
    }

    /**
     * @see org.osgi.jmx.service.useradmin.UserAdminMBean#removeProperty(java.lang.String, java.lang.String)
     */
    public void removeProperty(String key, String rolename) throws IOException {
        if (rolename == null) {
            throw new IOException("Role name cannot be null");
        }
        Role role = userAdmin.getRole(rolename);
        if (role == null) {
            throw new IOException("Operation fails role with provided rolename = [" + rolename + "] doesn't exist");
        }
        role.getProperties().remove(key);
    }

    /**
     * @see org.osgi.jmx.service.useradmin.UserAdminMBean#removeRole(java.lang.String)
     */
    public boolean removeRole(String name) throws IOException {
        if (name == null) {
            throw new IOException("Role name cannot be null");
        }
        return userAdmin.removeRole(name);
    }

    /**
     * @see org.osgi.jmx.service.useradmin.UserAdminMBean#removeUser(java.lang.String)
     */
    public boolean removeUser(String name) throws IOException {
        if (name == null) {
            throw new IOException("User name cannot be null");
        }
        return userAdmin.removeRole(name);
    }

}
