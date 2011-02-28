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
package org.apache.aries.jmx.codec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.OpenDataException;

import org.osgi.jmx.service.useradmin.UserAdminMBean;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;

/**
 * <p>
 * <tt>GroupData</tt> represents Group Type @see {@link UserAdminMBean#GROUP_TYPE}.It is a codec
 * for the <code>CompositeData</code> representing a Group.
 * </p>
 * </p>
 * 
 * @version $Rev$ $Date$
 */
public class GroupData extends UserData {

    /**
     * @see UserAdminMBean#MEMBERS_ITEM
     * @see UserAdminMBean#MEMBERS
     */
    private String[] members;
    /**
     * @see UserAdminMBean#REQUIRED_MEMBERS
     * @see UserAdminMBean#REQUIRED_MEMBERS_ITEM
     */
    private String[] requiredMembers;

    /**
     * Constructs new GroupData from Group object.
     * @param group {@link Group} instance.
     */
    public GroupData(Group group) {
        super(group.getName(), Role.GROUP);
        setRequiredMembers(group);
        setMembers(group);
    }

    /**
     * Constructs new GroupData.
     * 
     * @param name group name.
     * @param members basic members.
     * @param requiredMembers required members.
     */
    public GroupData(String name, String[] members, String[] requiredMembers) {
        super(name, Role.GROUP);
        this.members = members;
        this.requiredMembers = requiredMembers;
    }

    /**
     * Translates GroupData to CompositeData represented by compositeType {@link UserAdminMBean#GROUP_TYPE}.
     * 
     * @return translated GroupData to compositeData.
     */
    public CompositeData toCompositeData() {
        try {
            Map<String, Object> items = new HashMap<String, Object>();
            items.put(UserAdminMBean.NAME, name);
            items.put(UserAdminMBean.TYPE, type);
            items.put(UserAdminMBean.MEMBERS, members);
            items.put(UserAdminMBean.REQUIRED_MEMBERS, requiredMembers);
            return new CompositeDataSupport(UserAdminMBean.GROUP_TYPE, items);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Can't create CompositeData" + e);
        }
    }

    /**
     * Static factory method to create GroupData from CompositeData object.
     * 
     * @param data
     *            {@link CompositeData} instance.
     * @return GroupData instance.
     */
    public static GroupData from(CompositeData data) {
        if (data == null) {
            return null;
        }
        String name = (String) data.get(UserAdminMBean.NAME);
        String[] members = (String[]) data.get(UserAdminMBean.MEMBERS);
        String[] requiredMembers = (String[]) data.get(UserAdminMBean.REQUIRED_MEMBERS);
        return new GroupData(name, members, requiredMembers);
    }

    /**
     * @return the members
     */
    public String[] getMembers() {
        return members;
    }

    /**
     * @return the requiredMembers
     */
    public String[] getRequiredMembers() {
        return requiredMembers;
    }

    /**
     * Sets group basic members by getting them from Group object.
     * @param group {@link Group} instance.
     */
    private void setMembers(org.osgi.service.useradmin.Group group) {
        Role[] roles = group.getMembers();
        if (roles != null) {
            List<String> members = new ArrayList<String>();
            for (Role role : roles) {
                members.add(role.getName());
            }
            this.members = members.toArray(new String[roles.length]);
        }
    }

    /**
     * Sets group required members by getting them from Group object.
     * @param group {@link Group} instance.
     */
    private void setRequiredMembers(org.osgi.service.useradmin.Group group) {
        Role[] requiredRoles = group.getRequiredMembers();
        if (requiredRoles != null) {
            List<String> reqMembers = new ArrayList<String>();
            for (Role role : requiredRoles) {
                reqMembers.add(role.getName());
            }
            this.requiredMembers = reqMembers.toArray(new String[requiredRoles.length]);
        }
    }

}