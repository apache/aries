package org.apache.aries.blueprint.itests.authz.helper;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

public class SimpleLoginModule implements LoginModule {

    private Subject subject;
    private String name;
    private String[] groups;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
                           Map<String, ?> options) {
        this.subject = subject;
        this.name = (String)options.get("username");
        this.groups = (String[])options.get("groups");
    }

    @Override
    public boolean login() throws LoginException {
        return true;
    }

    @Override
    public boolean commit() throws LoginException {
        subject.getPrincipals().add(new UserPrincipal(name));
        for (String group : groups) {
            subject.getPrincipals().add(new GroupPrincipal(group));
        }
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        subject.getPrincipals().clear();
        return true;
    }
    
}
