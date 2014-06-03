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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Currency;

import org.apache.aries.blueprint.sample.Account;
import org.apache.aries.blueprint.sample.AccountFactory;
import org.apache.aries.blueprint.sample.Bar;
import org.apache.aries.blueprint.sample.Foo;
import org.apache.aries.itest.RichBundleContext;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.Bundle;
import org.osgi.service.blueprint.container.BlueprintContainer;

public class Helper {
    private static final String SAMPLE_SYM_NAME = "org.apache.aries.blueprint.sample";

    public static BlueprintContainer getBlueprintContainerForBundle(RichBundleContext context, String symbolicName) {
        return context.getService(BlueprintContainer.class, "(osgi.blueprint.container.symbolicname=" + symbolicName + ")");
    }

    public static BlueprintContainer getBlueprintContainerForBundle(RichBundleContext context, String symbolicName, long timeout) {
        return context.getService(BlueprintContainer.class, "(osgi.blueprint.container.symbolicname=" + symbolicName + ")", timeout);
    }
    
    public static Option blueprintBundles() {
        return blueprintBundles(true);
    }
    
    public static Option debug(int port) {
      return CoreOptions.vmOption("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=" + port);
    }
    
    public static Option blueprintBundles(boolean startBlueprint) {
        return composite(
                mvnBundle("org.ow2.asm", "asm-all"),
                mvnBundle("org.apache.felix", "org.apache.felix.configadmin"),
                mvnBundle("org.ops4j.pax.url", "pax-url-aether"),
                mvnBundle("org.apache.aries.testsupport", "org.apache.aries.testsupport.unit"),
                mvnBundle("org.apache.aries", "org.apache.aries.util"),
                mvnBundle("org.apache.aries.proxy", "org.apache.aries.proxy.api"),
                mvnBundle("org.apache.aries.proxy", "org.apache.aries.proxy.impl"),
                mvnBundle("org.apache.commons", "commons-jexl"),
                mvnBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.jexl.evaluator"),
                mvnBundle("org.apache.xbean", "xbean-asm4-shaded"),
                mvnBundle("org.apache.xbean", "xbean-bundleutils"),
                mvnBundle("org.apache.xbean", "xbean-finder-shaded"),
                mvnBundle("org.apache.aries.quiesce", "org.apache.aries.quiesce.api", startBlueprint),
                mvnBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.api", startBlueprint),
                mvnBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.core", startBlueprint),
                mvnBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.cm", startBlueprint),
                mvnBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.annotation.api", startBlueprint),
                mvnBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.annotation.impl", startBlueprint)
        );
    }

    public static Option mvnBundle(String groupId, String artifactId) {
        return mavenBundle(groupId, artifactId).versionAsInProject();
    }

    public static Option mvnBundle(String groupId, String artifactId, boolean start) {
        return mavenBundle(groupId, artifactId).versionAsInProject().start(start);
    }

    public static void testBlueprintContainer(RichBundleContext context, Bundle bundle) throws Exception {
        BlueprintContainer blueprintContainer = getBlueprintContainerForBundle(context, SAMPLE_SYM_NAME);
        assertNotNull(blueprintContainer);

        Bar bar = getInstance(blueprintContainer, "bar", Bar.class);
        checkBar(bar);
        
        Foo foo = getInstance(blueprintContainer, "foo", Foo.class);
        checkFoo(bar, foo);

        Foo fooService = context.getService(Foo.class);
        assertNotNull(fooService);
        checkFoo(bar, fooService);
        
        // TODO Does not work
        //assertEquals(obj, foo);
        
        Account account = getInstance(blueprintContainer, "accountOne", Account.class);
        assertEquals(1, account.getAccountNumber());
     
        Account account2 = getInstance(blueprintContainer, "accountTwo", Account.class);
        assertEquals(2, account2.getAccountNumber());
        
        Account account3 = getInstance(blueprintContainer, "accountThree", Account.class);
        assertEquals(3, account3.getAccountNumber());
        
        AccountFactory accountFactory = getInstance(blueprintContainer, "accountFactory", AccountFactory.class);
        assertEquals("account factory", accountFactory.getFactoryName());
        
        bundle.stop();

        Thread.sleep(1000);

        try {
            blueprintContainer = getBlueprintContainerForBundle(context, SAMPLE_SYM_NAME, 1);
            fail("BlueprintContainer should have been unregistered");
        } catch (Exception e) {
            // Expected, as the module container should have been unregistered
        }

        assertTrue(foo.isInitialized());
        assertTrue(foo.isDestroyed());
    }

    private static void checkBar(Bar bar) {
        assertNotNull(bar.getContext());
        assertEquals("Hello FooBar", bar.getValue());
        assertNotNull(bar.getList());
        assertEquals(2, bar.getList().size());
        assertEquals("a list element", bar.getList().get(0));
        assertEquals(Integer.valueOf(5), bar.getList().get(1));
    }

    private static void checkFoo(Bar bar, Foo foo) throws ParseException {
        assertEquals(5, foo.getA());
        assertEquals(10, foo.getB());
        assertSame(bar, foo.getBar());
        assertEquals(Currency.getInstance("PLN"), foo.getCurrency());
        assertEquals(new SimpleDateFormat("yyyy.MM.dd").parse("2009.04.17"),
                foo.getDate());

        assertTrue(foo.isInitialized());
        assertFalse(foo.isDestroyed());
    }
    
    @SuppressWarnings("unchecked")
    private static <T>T getInstance(BlueprintContainer container, String name, Class<T> clazz) {
        Object obj = container.getComponentInstance(name);
        assertNotNull(obj);
        assertEquals(clazz, obj.getClass());
        return (T) obj;
    }

}
