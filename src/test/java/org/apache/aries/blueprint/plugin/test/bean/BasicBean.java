package org.apache.aries.blueprint.plugin.test.bean;

import org.apache.aries.blueprint.annotation.bean.Activation;
import org.apache.aries.blueprint.annotation.bean.Bean;
import org.apache.aries.blueprint.annotation.bean.Scope;

@Bean
public class BasicBean {

    @Bean(id = "simpleProducedBean1")
    public SimpleProducedBean getBean1() {
        return null;
    }

    @Bean(id = "simpleProducedBean2", activation = Activation.EAGER, scope = Scope.PROTOTYPE)
    public SimpleProducedBean getBean2() {
        return null;
    }

    @Bean(id = "simpleProducedBean3", activation = Activation.LAZY, scope = Scope.PROTOTYPE, dependsOn = {"simpleProducedBean1", "simpleProducedBean2"}, initMethod = "init1", destroyMethod = "destroy1")
    public SimpleProducedBean getBean3() {
        return null;
    }
}
