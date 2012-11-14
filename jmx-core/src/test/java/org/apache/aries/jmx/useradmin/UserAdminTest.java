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
import java.util.Dictionary;
import java.util.Hashtable;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import org.apache.aries.jmx.codec.AuthorizationData;
import org.apache.aries.jmx.codec.GroupData;
import org.apache.aries.jmx.codec.RoleData;
import org.apache.aries.jmx.codec.UserData;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.jmx.JmxConstants;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

/**
 * UserAdminMBean test case.
 * 
 * @version $Rev$ $Date$
 */
public class UserAdminTest {

    @Mock
    private org.osgi.service.useradmin.UserAdmin userAdmin;
    private UserAdmin mbean;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mbean = new UserAdmin(userAdmin);
    }

    /**
     * Test method for
     * {@link org.apache.aries.jmx.useradmin.UserAdmin#addCredential(java.lang.String, byte[], java.lang.String)}.
     * 
     * @throws IOException
     */
    @Test
    public void testAddCredential() throws IOException {
        User user1 = Mockito.mock(User.class);
        Dictionary<String, Object> credentials = new Hashtable<String, Object>();
        Mockito.when(userAdmin.getRole("user1")).thenReturn(user1);
        Mockito.when(user1.getType()).thenReturn(Role.USER);
        Mockito.when(user1.getCredentials()).thenReturn(credentials);
        mbean.addCredential("password", new byte[] { 1, 2 }, "user1");
        Assert.assertArrayEquals(new byte[] { 1, 2 }, (byte[]) credentials.get("password"));

    }

    /**
     * Test method for
     * {@link org.apache.aries.jmx.useradmin.UserAdmin#addCredentialString(String, String, String)}
     * .
     * 
     * @throws IOException
     */
    @Test
    public void testAddCredentialString() throws IOException {
        User user1 = Mockito.mock(User.class);
        Dictionary<String, Object> credentials = new Hashtable<String, Object>();
        Mockito.when(userAdmin.getRole("user1")).thenReturn(user1);
        Mockito.when(user1.getType()).thenReturn(Role.USER);
        Mockito.when(user1.getCredentials()).thenReturn(credentials);
        mbean.addCredentialString("password", "1234", "user1");
        Assert.assertEquals("1234", (String) credentials.get("password"));
    }

    /**
     * Test method for {@link org.apache.aries.jmx.useradmin.UserAdmin#addMember(java.lang.String, java.lang.String)}.
     * 
     * @throws IOException
     */
    @Test
    public void testAddMember() throws IOException {
        Group group1 = Mockito.mock(Group.class);
        User user1 = Mockito.mock(User.class);
        Mockito.when(userAdmin.getRole("group1")).thenReturn(group1);
        Mockito.when(userAdmin.getRole("user1")).thenReturn(user1);
        Mockito.when(group1.getType()).thenReturn(Role.GROUP);
        Mockito.when(group1.addMember(user1)).thenReturn(true);
        boolean isAdded = mbean.addMember("group1", "user1");
        Assert.assertTrue(isAdded);
        Mockito.verify(group1).addMember(user1);
    }

    /**
     * Test method for
     * {@link org.apache.aries.jmx.useradmin.UserAdmin#addPropertyString(String, String, String)}
     * .
     * 
     * @throws IOException
     */
    @Test
    public void testAddPropertyString() throws IOException {
        User user1 = Mockito.mock(User.class);
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        Mockito.when(userAdmin.getRole("user1")).thenReturn(user1);
        Mockito.when(user1.getType()).thenReturn(Role.USER);
        Mockito.when(user1.getProperties()).thenReturn(props);
        mbean.addPropertyString("key", "1234", "user1");
        Assert.assertEquals("1234", (String) props.get("key"));
    }

    /**
     * Test method for
     * {@link org.apache.aries.jmx.useradmin.UserAdmin#addProperty(java.lang.String, byte[], java.lang.String)}.
     * 
     * @throws IOException
     */
    @Test
    public void testAddProperty() throws IOException {
        User user1 = Mockito.mock(User.class);
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        Mockito.when(userAdmin.getRole("user1")).thenReturn(user1);
        Mockito.when(user1.getType()).thenReturn(Role.USER);
        Mockito.when(user1.getProperties()).thenReturn(props);
        mbean.addProperty("key", new byte[] { 1, 2 }, "user1");
        Assert.assertArrayEquals(new byte[] { 1, 2 }, (byte[]) props.get("key"));
    }

    /**
     * Test method for
     * {@link org.apache.aries.jmx.useradmin.UserAdmin#addRequiredMember(java.lang.String, java.lang.String)}.
     * 
     * @throws IOException
     */
    @Test
    public void testAddRequiredMember() throws IOException {
        Group group1 = Mockito.mock(Group.class);
        User user1 = Mockito.mock(User.class);
        Mockito.when(userAdmin.getRole("group1")).thenReturn(group1);
        Mockito.when(userAdmin.getRole("user1")).thenReturn(user1);
        Mockito.when(group1.getType()).thenReturn(Role.GROUP);
        Mockito.when(group1.addRequiredMember(user1)).thenReturn(true);
        boolean isAdded = mbean.addRequiredMember("group1", "user1");
        Assert.assertTrue(isAdded);
        Mockito.verify(group1).addRequiredMember(user1);
    }

    /**
     * Test method for {@link org.apache.aries.jmx.useradmin.UserAdmin#createGroup(java.lang.String)}.
     * 
     * @throws IOException
     */
    @Test
    public void testCreateGroup() throws IOException {
        mbean.createGroup("group1");
        Mockito.verify(userAdmin).createRole("group1", Role.GROUP);
    }

    /**
     * Test method for {@link org.apache.aries.jmx.useradmin.UserAdmin#createRole(java.lang.String)}.
     * 
     * @throws IOException
     */
    @Test
    public void testCreateRole() throws IOException {
        try {
            mbean.createRole("role1");
            Assert.fail("Function did not throw exception as expected");
        } catch (IOException e) {
            // expected
        }
    }

    /**
     * Test method for {@link org.apache.aries.jmx.useradmin.UserAdmin#createUser(java.lang.String)}.
     * 
     * @throws IOException
     */
    @Test
    public void testCreateUser() throws IOException {
        mbean.createUser("user1");
        Mockito.verify(userAdmin).createRole("user1", Role.USER);
    }

    /**
     * Test method for {@link org.apache.aries.jmx.useradmin.UserAdmin#getAuthorization(java.lang.String)}.
     * 
     * @throws IOException
     */
    @Test
    public void testGetAuthorization() throws IOException {
        Authorization auth = Mockito.mock(Authorization.class);
        User user = Mockito.mock(User.class);
        Mockito.when(user.getType()).thenReturn(Role.USER);
        Mockito.when(userAdmin.getAuthorization(user)).thenReturn(auth);
        Mockito.when(userAdmin.getRole("role1")).thenReturn(user);
        Mockito.when(auth.getName()).thenReturn("auth1");
        Mockito.when(auth.getRoles()).thenReturn(new String[]{"role1"});
        CompositeData data = mbean.getAuthorization("role1");
        Assert.assertNotNull(data);
        AuthorizationData authData = AuthorizationData.from(data);
        Assert.assertNotNull(authData);
        Assert.assertEquals("auth1", authData.getName());
        Assert.assertArrayEquals(new String[] { "role1" }, authData.getRoles());
    }

    /**
     * Test method for {@link org.apache.aries.jmx.useradmin.UserAdmin#getCredentials(java.lang.String)}.
     * 
     * @throws IOException
     */
    @Test
    public void testGetCredentials() throws IOException {
        User user1 = Mockito.mock(User.class);
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put("key", "value");
        Mockito.when(user1.getCredentials()).thenReturn(properties);
        Mockito.when(user1.getType()).thenReturn(Role.USER);
        Mockito.when(userAdmin.getRole(Mockito.anyString())).thenReturn(user1);
        TabularData data = mbean.getCredentials("user1");
        Assert.assertNotNull(data);
        Assert.assertEquals(JmxConstants.PROPERTIES_TYPE, data.getTabularType());
        CompositeData composite = data.get(new Object[] { "key" });
        Assert.assertNotNull(composite);
        Assert.assertEquals("key", (String) composite.get(JmxConstants.KEY));
        Assert.assertEquals("value", (String) composite.get(JmxConstants.VALUE));
    }

    /**
     * Test method for {@link org.apache.aries.jmx.useradmin.UserAdmin#getGroup(java.lang.String)}.
     * 
     * @throws IOException
     */
    @Test
    public void testGetGroup() throws IOException {
        Group group1 = Mockito.mock(Group.class);
        Mockito.when(group1.getType()).thenReturn(Role.GROUP);
        Mockito.when(group1.getName()).thenReturn("group1");
        Role role1 = Mockito.mock(Role.class);
        Mockito.when(role1.getName()).thenReturn("role1");
        Role role2 = Mockito.mock(Role.class);
        Mockito.when(role2.getName()).thenReturn("role2");
        Mockito.when(group1.getRequiredMembers()).thenReturn(new Role[] { role1 });
        Mockito.when(group1.getMembers()).thenReturn(new Role[] { role2 });
        Mockito.when(userAdmin.getRole(Mockito.anyString())).thenReturn(group1);
        CompositeData data = mbean.getGroup("group1");
        Assert.assertNotNull(data);
        GroupData group = GroupData.from(data);
        Assert.assertNotNull(group);
        Assert.assertEquals("group1", group.getName());
        Assert.assertEquals(Role.GROUP, group.getType());
        Assert.assertArrayEquals(new String[] { "role2" }, group.getMembers());
        Assert.assertArrayEquals(new String[] { "role1" }, group.getRequiredMembers());
        Mockito.verify(userAdmin).getRole(Mockito.anyString());
    }

    /**
     * Test method for {@link org.apache.aries.jmx.useradmin.UserAdmin#getGroups(java.lang.String)}.
     * 
     * @throws Exception
     */
    @Test
    public void testGetGroups() throws Exception {
        Group group1 = Mockito.mock(Group.class);
        Mockito.when(group1.getType()).thenReturn(Role.GROUP);
        Mockito.when(group1.getName()).thenReturn("group1");
        Mockito.when(userAdmin.getRoles("name=group1")).thenReturn(new Role[] { group1 });
        String[] groups = mbean.getGroups("name=group1");
        Assert.assertArrayEquals(new String[] { "group1" }, groups);
    }

    /**
     * Test method for {@link org.apache.aries.jmx.useradmin.UserAdmin#getImpliedRoles(java.lang.String)}.
     * 
     * @throws IOException
     */
    @Test
    public void testGetImpliedRoles() throws IOException {
        User user1 = Mockito.mock(User.class);
        Authorization auth = Mockito.mock(Authorization.class);
        Mockito.when(user1.getType()).thenReturn(Role.USER);
        Mockito.when(auth.getRoles()).thenReturn(new String[] { "role1" });
        Mockito.when(userAdmin.getRole("role1")).thenReturn(user1);
        Mockito.when(userAdmin.getAuthorization(user1)).thenReturn(auth);
        String[] roles = mbean.getImpliedRoles("role1");
        Assert.assertArrayEquals(new String[] { "role1" }, roles);
    }

    /**
     * Test method for {@link org.apache.aries.jmx.useradmin.UserAdmin#getMembers(java.lang.String)}.
     * 
     * @throws IOException
     */
    @Test
    public void testGetMembers() throws IOException {
        Group group1 = Mockito.mock(Group.class);
        Mockito.when(group1.getType()).thenReturn(Role.GROUP);
        Mockito.when(group1.getName()).thenReturn("group1");
        User user1 = Mockito.mock(Group.class);
        Mockito.when(user1.getName()).thenReturn("user1");
        Mockito.when(group1.getMembers()).thenReturn(new Role[] { user1 });
        Mockito.when(userAdmin.getRole("group1")).thenReturn(group1);
        String[] members = mbean.getMembers("group1");
        Assert.assertArrayEquals(new String[] { "user1" }, members);
    }

    /**
     * Test method for {@link org.apache.aries.jmx.useradmin.UserAdmin#getProperties(java.lang.String)}.
     * 
     * @throws IOException
     */
    @Test
    public void testGetProperties() throws IOException {
        User user1 = Mockito.mock(User.class);
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put("key", "value");
        Mockito.when(user1.getProperties()).thenReturn(properties);
        Mockito.when(userAdmin.getRole(Mockito.anyString())).thenReturn(user1);
        TabularData data = mbean.getProperties("user1");
        Assert.assertNotNull(data);
        Assert.assertEquals(JmxConstants.PROPERTIES_TYPE, data.getTabularType());
        CompositeData composite = data.get(new Object[] { "key" });
        Assert.assertNotNull(composite);
        Assert.assertEquals("key", (String) composite.get(JmxConstants.KEY));
        Assert.assertEquals("value", (String) composite.get(JmxConstants.VALUE));
    }

    /**
     * Test method for {@link org.apache.aries.jmx.useradmin.UserAdmin#getRequiredMembers(java.lang.String)}.
     * 
     * @throws IOException
     */
    @Test
    public void testGetRequiredMembers() throws IOException {
        Group group1 = Mockito.mock(Group.class);
        Mockito.when(group1.getType()).thenReturn(Role.GROUP);
        Mockito.when(group1.getName()).thenReturn("group1");
        User user1 = Mockito.mock(Group.class);
        Mockito.when(user1.getName()).thenReturn("user1");
        Mockito.when(group1.getRequiredMembers()).thenReturn(new Role[] { user1 });
        Mockito.when(userAdmin.getRole("group1")).thenReturn(group1);
        String[] members = mbean.getRequiredMembers("group1");
        Assert.assertArrayEquals(new String[] { "user1" }, members);
    }

    /**
     * Test method for {@link org.apache.aries.jmx.useradmin.UserAdmin#getRole(java.lang.String)}.
     * 
     * @throws IOException
     */
    @Test
    public void testGetRole() throws IOException {
        User user1 = Mockito.mock(User.class);
        Mockito.when(user1.getType()).thenReturn(Role.USER);
        Mockito.when(user1.getName()).thenReturn("user1");
        Mockito.when(userAdmin.getRole(Mockito.anyString())).thenReturn(user1);
        CompositeData data = mbean.getRole("user1");
        Assert.assertNotNull(data);
        RoleData role = RoleData.from(data);
        Assert.assertNotNull(role);
        Assert.assertEquals("user1", role.getName());
        Assert.assertEquals(Role.USER, role.getType());
        Mockito.verify(userAdmin).getRole(Mockito.anyString());
    }

    /**
     * Test method for {@link org.apache.aries.jmx.useradmin.UserAdmin#getRoles(java.lang.String)}.
     * 
     * @throws Exception
     */
    @Test
    public void testGetRoles() throws Exception {
        User user1 = Mockito.mock(User.class);
        Mockito.when(user1.getType()).thenReturn(Role.USER);
        Mockito.when(user1.getName()).thenReturn("user1");
        Mockito.when(userAdmin.getRoles("name=user1")).thenReturn(new Role[] { user1 });
        String[] roles = mbean.getRoles("name=user1");
        Assert.assertArrayEquals(new String[] { "user1" }, roles);
    }

    /**
     * Test method for {@link org.apache.aries.jmx.useradmin.UserAdmin#getUser(java.lang.String)}.
     * 
     * @throws IOException
     */
    @Test
    public void testGetUser() throws IOException {
        User user1 = Mockito.mock(User.class);
        Mockito.when(user1.getType()).thenReturn(Role.USER);
        Mockito.when(user1.getName()).thenReturn("user1");
        Mockito.when(userAdmin.getRole(Mockito.anyString())).thenReturn(user1);
        CompositeData data = mbean.getUser("user1");
        Assert.assertNotNull(data);
        UserData user = UserData.from(data);
        Assert.assertNotNull(user);
        Assert.assertEquals("user1", user.getName());
        Assert.assertEquals(Role.USER, user.getType());
        Mockito.verify(userAdmin).getRole(Mockito.anyString());
    }

    /**
     * Test method for {@link org.apache.aries.jmx.useradmin.UserAdmin#getUserWithProperty(String, String)}.
     * 
     * @throws IOException
     */
    @Test
    public void testGetUserString() throws IOException {
        User user1 = Mockito.mock(User.class);
        Mockito.when(user1.getType()).thenReturn(Role.USER);
        Mockito.when(user1.getName()).thenReturn("user1");
        Mockito.when(userAdmin.getUser("key", "valuetest")).thenReturn(user1);
        String username = mbean.getUserWithProperty("key", "valuetest");
        Assert.assertEquals(username, "user1");
    }

    /**
     * Test method for {@link org.apache.aries.jmx.useradmin.UserAdmin#getUsers(java.lang.String)}.
     * 
     * @throws Exception
     */
    @Test
    public void testGetUsers() throws Exception {
        User user1 = Mockito.mock(User.class);
        Mockito.when(user1.getType()).thenReturn(Role.USER);
        Mockito.when(user1.getName()).thenReturn("user1");
        Mockito.when(userAdmin.getRoles("name=user1")).thenReturn(new Role[] { user1 });
        String[] roles = mbean.getUsers("name=user1");
        Assert.assertArrayEquals(new String[] { "user1" }, roles);
    }

    /**
     * Test method for {@link org.apache.aries.jmx.useradmin.UserAdmin#listGroups()}.
     * 
     * @throws Exception
     */
    @Test
    public void testListGroups() throws Exception {
        Group group1 = Mockito.mock(Group.class);
        Mockito.when(group1.getType()).thenReturn(Role.GROUP);
        Mockito.when(group1.getName()).thenReturn("group1");
        Group group2 = Mockito.mock(Group.class);
        Mockito.when(group2.getType()).thenReturn(Role.GROUP);
        Mockito.when(group2.getName()).thenReturn("group2");
        Mockito.when(userAdmin.getRoles(null)).thenReturn(new Role[] { group1, group2 });
        String[] groups = mbean.listGroups();
        Assert.assertArrayEquals(new String[] { "group1", "group2" }, groups);
    }

    /**
     * Test method for {@link org.apache.aries.jmx.useradmin.UserAdmin#listRoles()}.
     * 
     * @throws Exception
     */
    @Test
    public void testListRoles() throws Exception {
        User user1 = Mockito.mock(User.class);
        Mockito.when(user1.getType()).thenReturn(Role.USER);
        Mockito.when(user1.getName()).thenReturn("user1");
        User user2 = Mockito.mock(User.class);
        Mockito.when(user2.getType()).thenReturn(Role.USER);
        Mockito.when(user2.getName()).thenReturn("user2");
        Mockito.when(userAdmin.getRoles(null)).thenReturn(new Role[] { user1, user2 });
        String[] roles = mbean.listRoles();
        Assert.assertArrayEquals(new String[] { "user1", "user2" }, roles);
    }

    /**
     * Test method for {@link org.apache.aries.jmx.useradmin.UserAdmin#listUsers()}.
     * 
     * @throws Exception
     */
    @Test
    public void testListUsers() throws Exception {
        User user1 = Mockito.mock(User.class);
        Mockito.when(user1.getType()).thenReturn(Role.USER);
        Mockito.when(user1.getName()).thenReturn("user1");
        User user2 = Mockito.mock(User.class);
        Mockito.when(user2.getType()).thenReturn(Role.USER);
        Mockito.when(user2.getName()).thenReturn("user2");
        Mockito.when(userAdmin.getRoles(null)).thenReturn(new Role[] { user1, user2 });
        String[] roles = mbean.listUsers();
        Assert.assertArrayEquals(new String[] { "user1", "user2" }, roles);
    }

    /**
     * Test method for
     * {@link org.apache.aries.jmx.useradmin.UserAdmin#removeCredential(java.lang.String, java.lang.String)}.
     * 
     * @throws IOException
     */
    @Test
    public void testRemoveCredential() throws IOException {
        User user1 = Mockito.mock(User.class);
        Dictionary<String, Object> cred = new Hashtable<String, Object>();
        Mockito.when(userAdmin.getRole("user1")).thenReturn(user1);
        Mockito.when(user1.getType()).thenReturn(Role.USER);
        Mockito.when(user1.getCredentials()).thenReturn(cred);
        mbean.removeCredential("key", "user1");
        Assert.assertEquals(0, cred.size());
    }

    /**
     * Test method for {@link org.apache.aries.jmx.useradmin.UserAdmin#removeGroup(java.lang.String)}.
     * 
     * @throws IOException
     */
    @Test
    public void testRemoveGroup() throws IOException {
        Mockito.when(userAdmin.removeRole("group1")).thenReturn(true);
        boolean isRemoved = mbean.removeGroup("group1");
        Assert.assertTrue(isRemoved);
    }

    /**
     * Test method for {@link org.apache.aries.jmx.useradmin.UserAdmin#removeMember(java.lang.String, java.lang.String)}
     * .
     * 
     * @throws IOException
     */
    @Test
    public void testRemoveMember() throws IOException {
        Group group1 = Mockito.mock(Group.class);
        User user1 = Mockito.mock(User.class);
        Mockito.when(userAdmin.getRole("group1")).thenReturn(group1);
        Mockito.when(userAdmin.getRole("user1")).thenReturn(user1);
        Mockito.when(group1.getType()).thenReturn(Role.GROUP);
        Mockito.when(group1.removeMember(user1)).thenReturn(true);
        boolean isAdded = mbean.removeMember("group1", "user1");
        Assert.assertTrue(isAdded);
        Mockito.verify(group1).removeMember(user1);
    }

    /**
     * Test method for
     * {@link org.apache.aries.jmx.useradmin.UserAdmin#removeProperty(java.lang.String, java.lang.String)}.
     * 
     * @throws IOException
     */
    @Test
    public void testRemoveProperty() throws IOException {
        User user1 = Mockito.mock(User.class);
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        Mockito.when(userAdmin.getRole("user1")).thenReturn(user1);
        Mockito.when(user1.getType()).thenReturn(Role.USER);
        Mockito.when(user1.getProperties()).thenReturn(props);
        mbean.removeProperty("key", "user1");
        Assert.assertEquals(0, props.size());
    }

    /**
     * Test method for {@link org.apache.aries.jmx.useradmin.UserAdmin#removeRole(java.lang.String)}.
     * 
     * @throws IOException
     */
    @Test
    public void testRemoveRole() throws IOException {
        Mockito.when(userAdmin.removeRole("role1")).thenReturn(true);
        boolean isRemoved = mbean.removeRole("role1");
        Assert.assertTrue(isRemoved);
    }

    /**
     * Test method for {@link org.apache.aries.jmx.useradmin.UserAdmin#removeUser(java.lang.String)}.
     * 
     * @throws IOException
     */
    @Test
    public void testRemoveUser() throws IOException {
        Mockito.when(userAdmin.removeRole("user1")).thenReturn(true);
        boolean isRemoved = mbean.removeUser("user1");
        Assert.assertTrue(isRemoved);
    }

}
