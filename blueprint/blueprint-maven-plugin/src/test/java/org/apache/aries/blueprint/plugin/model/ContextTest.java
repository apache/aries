/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint.plugin.model;

import org.apache.aries.blueprint.plugin.BlueprintConfigurationImpl;
import org.apache.aries.blueprint.plugin.Generator;
import org.apache.aries.blueprint.plugin.test.MyBean3;
import org.apache.aries.blueprint.plugin.test.MyFactoryBean;
import org.apache.aries.blueprint.plugin.test.MyProduced;
import org.apache.aries.blueprint.plugin.test.ServiceReferences;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.container.Converter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class ContextTest {
    private final Set<String> namespaces = new HashSet<String>(Arrays.asList(Generator.NS_JPA, Generator.NS_TX));
    private final BlueprintConfigurationImpl blueprintConfiguration = new BlueprintConfigurationImpl(namespaces, null);

    @Test
    public void testLists() {
        Context context = new Context(blueprintConfiguration, MyBean3.class);
        Assert.assertEquals(1, context.getBeans().size());
        Assert.assertEquals(0, getOsgiServices(context).size());
    }

    @Test
    public void testLists2() {
        Context context = new Context(blueprintConfiguration, ServiceReferences.class);
        context.resolve();
        Assert.assertEquals(1, context.getBeans().size());
        Assert.assertEquals(3, getOsgiServices(context).size());
    }

    private Set<String> getOsgiServices(Context context) {
        Set<String> blueprintWritersKeys = context.getBlueprintWriters().keySet();
        Set<String> osgiServices = new HashSet<>();
        for (String blueprintWritersKey : blueprintWritersKeys) {
            if (blueprintWritersKey.startsWith("osgiService/")) {
                osgiServices.add(blueprintWritersKey);
            }
        }
        return osgiServices;
    }

    private void assertSpecialRef(String expectedId, Class<?> clazz) {
        Context context = new Context(blueprintConfiguration);
        BeanRef ref = context.getMatching(new BeanRef(clazz));
        assertEquals(expectedId, ref.id);
    }

    @Test
    public void testSpecialRefs() {
        assertSpecialRef("blueprintBundleContext", BundleContext.class);
        assertSpecialRef("blueprintBundle", Bundle.class);
        assertSpecialRef("blueprintContainer", BlueprintContainer.class);
        assertSpecialRef("blueprintConverter", Converter.class);
    }

    @Test
    public void testProduced() throws NoSuchFieldException, SecurityException {
        Context context = new Context(blueprintConfiguration, MyFactoryBean.class);
        context.resolve();
        ProducedBean matching = (ProducedBean) context.getMatching(new BeanRef(MyProduced.class));
        Assert.assertEquals(MyProduced.class, matching.clazz);
        Assert.assertEquals("myFactoryBean", matching.factoryBean.id);
        Assert.assertEquals("create", matching.factoryMethod);
    }

}
