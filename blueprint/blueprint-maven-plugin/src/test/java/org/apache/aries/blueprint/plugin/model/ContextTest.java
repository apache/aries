/**
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
package org.apache.aries.blueprint.plugin.model;

import static org.junit.Assert.assertEquals;

import org.apache.aries.blueprint.plugin.test.MyBean3;
import org.apache.aries.blueprint.plugin.test.MyFactoryBean;
import org.apache.aries.blueprint.plugin.test.MyProduced;
import org.apache.aries.blueprint.plugin.test.ServiceB;
import org.apache.aries.blueprint.plugin.test.ServiceReferences;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.container.Converter;

public class ContextTest {

    @Test
    public void testLists()  {
        Context context = new Context(MyBean3.class);
        Assert.assertEquals(1, context.getBeans().size());
        Assert.assertEquals(0, context.getServiceRefs().size());
    }
    
    @Test
    public void testLists2()  {
        Context context = new Context(ServiceReferences.class);
        Assert.assertEquals(1, context.getBeans().size());
        Assert.assertEquals(1, context.getServiceRefs().size());
    }
    
    @Test
    public void testMatching() throws NoSuchFieldException, SecurityException  {
        Context context = new Context(ServiceReferences.class);
        BeanRef matching = context.getMatching(new BeanRef(ServiceB.class));
        Assert.assertEquals(OsgiServiceRef.class, matching.getClass());
        Assert.assertEquals(ServiceB.class, matching.clazz);
        Assert.assertEquals("serviceB", matching.id);
    }
    
    private void assertSpecialRef(String expectedId, Class<?> clazz) {
        Context context = new Context();
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
    public void testProduced() throws NoSuchFieldException, SecurityException  {
        Context context = new Context(MyFactoryBean.class);
        
        ProducedBean matching = (ProducedBean)context.getMatching(new BeanRef(MyProduced.class));
        Assert.assertEquals(MyProduced.class, matching.clazz);
        Assert.assertEquals("myFactoryBean", matching.factoryBeanId);
        Assert.assertEquals("create", matching.factoryMethod);
    }
    
}
