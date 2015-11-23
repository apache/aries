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

import java.io.InputStream;
import java.util.List;

import org.junit.Test;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.blueprint.container.BlueprintContainer;

import static org.apache.aries.blueprint.itests.Helper.mvnBundle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.provision;

public class SpringTest extends AbstractBlueprintIntegrationTest {

    @Test
    public void testSpringBundle() throws Exception {
        Bundle bundles = context().getBundleByName("org.apache.aries.blueprint.testbundles");
        assertNotNull(bundles);
        bundles.start();

        BlueprintContainer container = startBundleBlueprint("org.apache.aries.blueprint.testbundles");
        List list = (List) container.getComponentInstance("springList");
        System.out.println(list);
    }

    @org.ops4j.pax.exam.Configuration
    public Option[] configuration() {
        return new Option[] {
            baseOptions(),
            Helper.blueprintBundles(),
            mvnBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.aopalliance"),
            mvnBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.spring-core"),
            mvnBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.spring-context"),
            mvnBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.spring-context-support"),
            mvnBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.spring-beans"),
            mvnBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.spring-aop"),
            mvnBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.spring-expression"),
            mvnBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.spring-tx"),
            // test bundle
            mvnBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.testbundles", false),
//            Helper.debug(5006)
        };
    }

}
