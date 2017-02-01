package org.apache.aries.blueprint.plugin.test.bean;

import org.apache.aries.blueprint.annotation.bean.Activation;
import org.apache.aries.blueprint.annotation.bean.Bean;
import org.apache.aries.blueprint.annotation.bean.Scope;

@Bean(activation = Activation.LAZY, scope = Scope.PROTOTYPE, dependsOn = {"basicBean", "namedBean1"}, initMethod = "init", destroyMethod = "destroy")
public class BeanWithCallbackMethods {
    public void init() {
    }

    public void destroy() {
    }
}
