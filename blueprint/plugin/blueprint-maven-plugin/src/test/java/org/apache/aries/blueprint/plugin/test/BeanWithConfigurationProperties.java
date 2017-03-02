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
package org.apache.aries.blueprint.plugin.test;

import org.apache.aries.blueprint.annotation.config.ConfigProperties;
import org.ops4j.pax.cdi.api.OsgiService;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Properties;

@Singleton
public class BeanWithConfigurationProperties {

    @Inject
    @ConfigProperties(pid = "aries.test1", update = true)
    private Properties prop1;

    @Inject
    @ConfigProperties(pid = "aries.test2")
    @Named("testProps2")
    private Properties prop2;

    public BeanWithConfigurationProperties(
            @ConfigProperties(pid = "aries.test5", update = true) @Named("testProps5") Properties prop5,
            @ConfigProperties(pid = "aries.test6") Properties prop6) {
    }

    @ConfigProperties(pid = "aries.test3", update = true)
    @Inject
    public void setProp3(Properties prop3) {
    }

    @ConfigProperties(pid = "aries.test4")
    @Named("testProps4")
    @Inject
    public void setProp4(Properties prop4) {
    }

    @Inject
    public void setProp7(@ConfigProperties(pid = "aries.test7", update = false) Properties prop7) {
    }

    @Produces
    @Named("withProperties8")
    public MyProducedWithConstructor createBeanWithProperties8(@ConfigProperties(pid = "aries.test8") Properties prop8) {
        return null;
    }

    @Produces
    @Named("withProperties9")
    public MyProducedWithConstructor createBeanWithProperties9(@ConfigProperties(pid = "aries.test9", update = true) @Named("testProps9") Properties prop9) {
        return null;
    }
}
