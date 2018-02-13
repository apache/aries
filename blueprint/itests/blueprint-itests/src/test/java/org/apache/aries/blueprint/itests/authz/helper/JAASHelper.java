/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint.itests.authz.helper;

import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

public class JAASHelper {

    public static <T> void doAs(final String[] groups, PrivilegedAction<T> action) {
        Configuration config = new Configuration() {
    
            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                Map<String, Object> options = new HashMap<String, Object>();
                options.put("username", "dummy"); // The user does not matter
                options.put("groups", groups);
                AppConfigurationEntry entry = new AppConfigurationEntry(SimpleLoginModule.class.getName(),
                                                                        LoginModuleControlFlag.REQUIRED,
                                                                        options);
                return new AppConfigurationEntry[] {
                    entry
                };
            }
    
        };
        try {
            LoginContext lc = new LoginContext("test", new Subject(), null, config);
            lc.login();
            Subject.doAs(lc.getSubject(), action);
            lc.logout();
        } catch (LoginException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    
}
