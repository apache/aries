package org.apache.aries.blueprint.plugin.test.bean;

import org.apache.aries.blueprint.annotation.bean.Activation;
import org.apache.aries.blueprint.annotation.bean.Bean;
import org.apache.aries.blueprint.annotation.bean.Scope;

@Bean(id = "namedBean1", activation = Activation.EAGER, scope = Scope.PROTOTYPE)
public class NamedBean {
}
