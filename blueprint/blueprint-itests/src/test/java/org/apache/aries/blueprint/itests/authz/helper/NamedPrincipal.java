package org.apache.aries.blueprint.itests.authz.helper;

import java.security.Principal;

public class NamedPrincipal implements Principal {
    private String name;

    public NamedPrincipal(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
    
}