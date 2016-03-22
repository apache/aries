package org.apache.aries.blueprint.plugin.test;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class MyFactoryNamedBean {
    
    @Produces
    @Named("produced1")
    public MyProduced createBean1() {
        return new MyProduced("My message");
    }

    @Produces
    @Named("produced2")
    @Singleton
    public MyProduced createBean2() {
        return new MyProduced("My message");
    }
}
