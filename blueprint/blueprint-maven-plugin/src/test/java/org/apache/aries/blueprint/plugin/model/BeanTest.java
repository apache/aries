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
import org.apache.aries.blueprint.plugin.BlueprintConfigurationImpl;
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
import org.junit.Test;

import javax.inject.Named;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BeanTest {
    private static final String NS_JPA1 = "http://aries.apache.org/xmlns/jpa/v1.1.0";
    private static final String NS_TX1 = "http://aries.apache.org/xmlns/transactions/v1.1.0";
    
    private final Set<String> namespaces = new HashSet<String>(Arrays.asList(NS_JPA1, NS_TX1));
    private final BlueprintConfigurationImpl blueprintConfiguration = new BlueprintConfigurationImpl(namespaces, null, null);
    private final Context context = new Context(blueprintConfiguration);

    @Test
    public void testParseMyBean1() {
        Bean bean = new Bean(MyBean1.class, context);
        bean.resolve(context);
        assertEquals(MyBean1.class, bean.clazz);
        assertEquals("myBean1", bean.id); // Name derived from class name
        assertEquals(2, getPersistenceFields(bean).size());
        assertEquals(Sets.newHashSet("em", "emf"), getPersistenceFields(bean));
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
        assertEquals(expectedTxs, getTransactionalDefs(bean));
    }

    @Test
    public void testParseMyBean3() {
        Bean bean = new Bean(MyBean3.class, context);
        bean.resolve(context);
        assertEquals(MyBean3.class, bean.clazz);
        assertEquals("myBean3", bean.id); // Name derived from class name
        assertEquals("There should be no persistence fields", 0, getPersistenceFields(bean).size());
        assertEquals(5, bean.properties.size());
        assertTrue(bean.isPrototype);

        Set<TransactionalDef> expectedTxs = Sets.newHashSet(new TransactionalDef("*", "RequiresNew"),
            new TransactionalDef("txNotSupported", "NotSupported"),
            new TransactionalDef("txMandatory", "Mandatory"),
            new TransactionalDef("txNever", "Never"),
            new TransactionalDef("txRequired", "Required"),
            new TransactionalDef("txRequiresNew", "RequiresNew"),
            new TransactionalDef("txSupports", "Supports"));
        assertEquals(expectedTxs, getTransactionalDefs(bean));
    }

    @Test
    public void testParseNamedBean() {
        Bean bean = new Bean(ServiceAImpl1.class, context);
        bean.resolve(context);
        String definedName = ServiceAImpl1.class.getAnnotation(Named.class).value();
        assertEquals("my1", definedName);
        assertEquals("Name should be defined using @Named", definedName, bean.id);
        assertEquals("There should be no persistence fields", 0,getPersistenceFields(bean).size());
        assertTrue("There should be no transaction definition", getTransactionalDefs(bean).isEmpty());
        assertEquals("There should be no properties", 0, bean.properties.size());
        assertTrue(bean.isPrototype);
    }

    @Test
    public void testBlueprintBundleContext() {
        Bean bean = new Bean(MyBean4.class, context);
        bean.resolve(context);
        Property bcProp = bean.properties.iterator().next();
        assertEquals("bundleContext", bcProp.name);
        assertEquals("blueprintBundleContext", bcProp.ref);
        assertFalse(bean.isPrototype);

        Set<TransactionalDef> expectedTxs = Sets.newHashSet(new TransactionalDef("txWithoutClassAnnotation", "Supports"));
        assertEquals(expectedTxs, getTransactionalDefs(bean));
    }

    private Set<TransactionalDef> getTransactionalDefs(Bean bean) {
        Set<String> beanWriters = bean.beanContentWriters.keySet();
        Set<TransactionalDef> transactionalDefs = new HashSet<>();
        for (String beanWriter : beanWriters) {
            if (beanWriter.startsWith("javax.transactional.method/")) {
                String[] splitId = beanWriter.split("/");
                transactionalDefs.add(new TransactionalDef(splitId[2], splitId[3]));
            }
        }
        return transactionalDefs;
    }

    private Set<String> getPersistenceFields(Bean bean) {
        Set<String> beanWriters = bean.beanContentWriters.keySet();
        Set<String> persistenceFields = new HashSet<>();
        for (String beanWriter : beanWriters) {
            if (beanWriter.startsWith("javax.persistence.field.")) {
                persistenceFields.add(beanWriter.split("/")[1]);
            }
        }
        return persistenceFields;
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMultipleInitMethods() {
        new Bean(BadBean1.class, context);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMultipleDestroyMethods() {
        new Bean(BadBean2.class, context);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSpringNestedTransactionNotSupported() {
        new Bean(BadBean3.class, context);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testBadFieldBean1() {
        new Context(blueprintConfiguration, BadFieldBean1.class).resolve();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testBadFieldBean2() {
        new Context(blueprintConfiguration, BadFieldBean2.class).resolve();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testBadFieldBean3() {
        new Context(blueprintConfiguration, BadFieldBean3.class).resolve();
    }

    @Test
    public void testFieldBean4() {
        new Context(blueprintConfiguration, FieldBean4.class).resolve();
    }

    @Test
    public void testParseBeanWithConstructorInject() {
        Bean bean = new Bean(MyBean5.class, context);
        bean.resolve(context);
        assertEquals(MyBean5.class, bean.clazz);
        assertEquals("myBean5", bean.id); // Name derived from class name
        assertTrue("There should be no persistenceUnit", getPersistenceFields(bean).isEmpty());
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
