package org.apache.aries.blueprint.plugin.test;

import javax.inject.Inject;

public class MyProduced {
    private String message;
    
    @Inject
    ServiceA serviceA;

    public MyProduced(String message) {
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }
}
