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

import java.util.HashMap;
import java.util.Map;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.OpenDataException;

import org.osgi.jmx.service.useradmin.UserAdminMBean;
import org.osgi.service.useradmin.Authorization;

/**
 * <p>
 * <tt>AuthorizationData</tt> represents Authorization Type @see {@link UserAdminMBean#AUTORIZATION_TYPE}.It is a codec
 * for the <code>CompositeData</code> representing an Authorization .
 * </p>
 * 
 *
 * @version $Rev$ $Date$
 */
public class AuthorizationData {
    
    /**
     * authorization context name.
     */
    private String name;
    /**
     * roles implied by authorization context.
     */
    private String[] roles;
    
    /**
     * Constructs new AuthorizationData from Authorization. 
     * @param auth {@link Authorization} instance.
     */
    public AuthorizationData(Authorization auth){
        this.name = auth.getName();
        this.roles = auth.getRoles();
    }
    
    /**
     * Constructs new AuthorizationData.
     * 
     * @param name of authorization context.
     * @param roles implied by authorization context.
     */
    public AuthorizationData(String name, String[] roles){
        this.name = name;
        this.roles = roles;
    }
    /**
     * Translates AuthorizationData to CompositeData represented by
     * compositeType {@link UserAdminMBean#AUTORIZATION_TYPE}.
     * 
     * @return translated AuthorizationData to compositeData.
     */
    public CompositeData toCompositeData() {
        try {
            Map<String, Object> items = new HashMap<String, Object>();
            items.put(UserAdminMBean.NAME, name);
            items.put(UserAdminMBean.ROLES, roles);
            return new CompositeDataSupport(UserAdminMBean.AUTORIZATION_TYPE, items);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Can't create CompositeData" + e);
        }
    }

    /**
     * Static factory method to create AuthorizationData from CompositeData object.
     * 
     * @param data {@link CompositeData} instance.
     * @return AuthorizationData instance.
     */
    public static AuthorizationData from(CompositeData data) {
        if(data == null){
            return null;
        }
        String name = (String) data.get(UserAdminMBean.NAME);
        String[] roles = (String[]) data.get(UserAdminMBean.ROLES);
        return new AuthorizationData(name, roles);
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the roles
     */
    public String[] getRoles() {
        return roles;
    }

}