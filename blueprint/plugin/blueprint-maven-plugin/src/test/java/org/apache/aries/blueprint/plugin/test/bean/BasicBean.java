/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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
