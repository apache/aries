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

import com.google.common.collect.Sets;
import org.apache.aries.blueprint.plugin.bad.BadBean1;
import org.apache.aries.blueprint.plugin.bad.BadBean2;
import org.apache.aries.blueprint.plugin.bad.BadBean3;
import org.apache.aries.blueprint.plugin.bad.BadFieldBean1;
import org.apache.aries.blueprint.plugin.bad.BadFieldBean2;
import org.apache.aries.blueprint.plugin.bad.BadFieldBean3;
import org.apache.aries.blueprint.plugin.bad.FieldBean4;
import org.apache.aries.blueprint.plugin.test.MyBean1;
import org.apache.aries.blueprint.plugin.test.MyBean3;
import org.apache.aries.blueprint.plugin.test.MyBean4;
import org.apache.aries.blueprint.plugin.test.MyBean5;
import org.apache.aries.blueprint.plugin.test.ServiceAImpl1;
import org.junit.Assert;
import org.junit.Test;

import javax.inject.Named;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BeanTest {

    @Test
    public void testParseMyBean1() {
        Bean bean = new Bean(MyBean1.class);
        bean.resolve(new Context());
        assertEquals(MyBean1.class, bean.clazz);
        assertEquals("myBean1", bean.id); // Name derived from class name
        assertEquals("init", bean.initMethod);
        assertEquals("destroy", bean.destroyMethod);
        Assert.assertEquals(2, bean.persistenceFields.size());
        assertEquals("em", bean.persistenceFields.get(0).getName());
        assertEquals("emf", bean.persistenceFields.get(1).getName());
        assertEquals(1, bean.properties.size());
        assertFalse(bean.isPrototype);
        Property prop = bean.properties.iterator().next();
        assertEquals("bean2", prop.name);
        assertEquals("serviceA", prop.ref);

        Set<TransactionalDef> expectedTxs = Sets.newHashSet(new TransactionalDef("*", "RequiresNew"),
                new TransactionalDef("txNotSupported", "NotSupported"),
                new TransactionalDef("txMandatory", "Mandatory"),
                new TransactionalDef("txNever", "Never"),
                new TransactionalDef("txRequired", "Required"),
                new TransactionalDef("txOverridenWithRequiresNew", "RequiresNew"),
                new TransactionalDef("txSupports", "Supports"));
        assertEquals(expectedTxs, bean.transactionDefs);
    }

    @Test
    public void testParseMyBean3() {
        Bean bean = new Bean(MyBean3.class);
        bean.resolve(new Context());
        assertEquals(MyBean3.class, bean.clazz);
        assertEquals("myBean3", bean.id); // Name derived from class name
        assertNull("There should be no initMethod", bean.initMethod);
        assertNull("There should be no destroyMethod", bean.destroyMethod);
        assertEquals("There should be no persistence fields", 0, bean.persistenceFields.size());
        assertEquals(5, bean.properties.size());
        assertTrue(bean.isPrototype);

        Set<TransactionalDef> expectedTxs = Sets.newHashSet(new TransactionalDef("*", "RequiresNew"),
                new TransactionalDef("txNotSupported", "NotSupported"),
                new TransactionalDef("txMandatory", "Mandatory"),
                new TransactionalDef("txNever", "Never"),
                new TransactionalDef("txRequired", "Required"),
                new TransactionalDef("txRequiresNew", "RequiresNew"),
                new TransactionalDef("txSupports", "Supports"));
        assertEquals(expectedTxs, bean.transactionDefs);
    }

    @Test
    public void testParseNamedBean() {
        Bean bean = new Bean(ServiceAImpl1.class);
        bean.resolve(new Context());
        String definedName = ServiceAImpl1.class.getAnnotation(Named.class).value();
        assertEquals("my1", definedName);
        assertEquals("Name should be defined using @Named", definedName, bean.id);
        assertNull("There should be no initMethod", bean.initMethod);
        assertNull("There should be no destroyMethod", bean.destroyMethod);
        assertEquals("There should be no persistence fields", 0, bean.persistenceFields.size());
        assertTrue("There should be no transaction definition", bean.transactionDefs.isEmpty());
        assertEquals("There should be no properties", 0, bean.properties.size());
        assertTrue(bean.isPrototype);
    }

    @Test
    public void testBlueprintBundleContext() {
        Bean bean = new Bean(MyBean4.class);
        bean.resolve(new Context());
        Property bcProp = bean.properties.iterator().next();
        assertEquals("bundleContext", bcProp.name);
        assertEquals("blueprintBundleContext", bcProp.ref);
        assertFalse(bean.isPrototype);

        Set<TransactionalDef> expectedTxs = Sets.newHashSet(new TransactionalDef("txWithoutClassAnnotation", "Supports"));
        assertEquals(expectedTxs, bean.transactionDefs);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMultipleInitMethods() {
        new Bean(BadBean1.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMultipleDestroyMethods() {
        new Bean(BadBean2.class);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSpringNestedTransactionNotSupported() {
        new Bean(BadBean3.class);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testBadFieldBean1() {
        new Context(BadFieldBean1.class).resolve();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testBadFieldBean2() {
        new Context(BadFieldBean2.class).resolve();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testBadFieldBean3() {
        new Context(BadFieldBean3.class).resolve();
    }

    @Test
    public void testFieldBean4() {
        new Context(FieldBean4.class).resolve();
    }

    @Test
    public void testParseBeanWithConstructorInject() {
        Bean bean = new Bean(MyBean5.class);
        bean.resolve(new Context());
        assertEquals(MyBean5.class, bean.clazz);
        assertEquals("myBean5", bean.id); // Name derived from class name
        assertNull("There should be no initMethod", bean.initMethod);
        assertNull("There should be no destroyMethod", bean.destroyMethod);
        assertTrue("There should be no persistenceUnit", bean.persistenceFields.isEmpty());
        assertEquals(0, bean.properties.size());
        assertEquals(8, bean.constructorArguments.size());
        assertEquals("my2", bean.constructorArguments.get(0).getRef());
        assertEquals("serviceA", bean.constructorArguments.get(1).getRef());
        assertEquals("serviceB", bean.constructorArguments.get(2).getRef());
        assertEquals("100", bean.constructorArguments.get(3).getValue());
        assertEquals("ser1", bean.constructorArguments.get(4).getRef());
        assertEquals("ser2", bean.constructorArguments.get(5).getRef());
        assertEquals("serviceA", bean.constructorArguments.get(6).getRef());
        assertEquals("produced2", bean.constructorArguments.get(7).getRef());
    }

}
