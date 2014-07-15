package org.apache.aries.blueprint.authorization.impl.test;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

@RolesAllowed("admin")
public class SecuredClass {
    
    public void admin() {
        
    }
    
    @RolesAllowed("user")
    public void user() {
        
    }
    
    @PermitAll
    public void anon() {
        
    }
    
    @DenyAll
    public void closed() {
        
    }
}
