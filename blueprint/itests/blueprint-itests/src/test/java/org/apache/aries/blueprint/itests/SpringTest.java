/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint.itests;

import java.util.List;

import org.apache.aries.blueprint.testbundles.BeanC;
import org.apache.aries.blueprint.testbundles.BeanCItf;
import org.junit.Test;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.Bundle;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import static org.apache.aries.blueprint.itests.Helper.mvnBundle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

public class SpringTest extends AbstractBlueprintIntegrationTest {

    @Test
    public void testSpringBundle() throws Exception {
        Bundle bundles = context().getBundleByName("org.apache.aries.blueprint.testbundles");
        assertNotNull(bundles);
        bundles.start();

        BlueprintContainer container = startBundleBlueprint("org.apache.aries.blueprint.testbundles");
        List list = (List) container.getComponentInstance("springList");
        System.out.println(list);

        BeanCItf beanC = (BeanCItf) list.get(4);
        assertEquals(1, beanC.getInitialized());

        try {
            beanC.doSomething();
            fail("Should have thrown an exception because the transaction manager is not defined");
        } catch (NoSuchBeanDefinitionException e) {
            // expected
        }
    }

    @org.ops4j.pax.exam.Configuration
    public Option[] configuration() {
        return new Option[] {
            baseOptions(),
            Helper.blueprintBundles(),
            // Blueprint spring
            mvnBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.spring"),
            // Spring
            mvnBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.aopalliance"),
            mvnBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.spring-core"),
            mvnBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.spring-context"),
            mvnBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.spring-context-support"),
            mvnBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.spring-beans"),
            mvnBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.spring-aop"),
            mvnBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.spring-expression"),
            mvnBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.spring-tx"),
            // Axon namespace handler for testing
            mavenBundle("org.axonframework", "axon-core", "2.4.4"),
            // test bundle
            mvnBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.testbundles", false),
        };
    }

}
