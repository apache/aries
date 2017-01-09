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
