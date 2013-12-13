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

import java.text.SimpleDateFormat;
import java.util.Currency;

import org.apache.aries.blueprint.sample.Account;
import org.apache.aries.blueprint.sample.AccountFactory;
import org.apache.aries.blueprint.sample.Bar;
import org.apache.aries.blueprint.sample.Foo;
import org.apache.aries.itest.RichBundleContext;

import org.ops4j.pax.exam.Option;
import org.osgi.framework.Bundle;
import org.osgi.service.blueprint.container.BlueprintContainer;
import static org.apache.aries.itest.ExtraOptions.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartup; 
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;

public class Helper {
    public static BlueprintContainer getBlueprintContainerForBundle(RichBundleContext context, String symbolicName) {
        return context.getService(BlueprintContainer.class, "(osgi.blueprint.container.symbolicname=" + symbolicName + ")");
    }

    public static BlueprintContainer getBlueprintContainerForBundle(RichBundleContext context, String symbolicName, long timeout) {
        return context.getService(BlueprintContainer.class, "(osgi.blueprint.container.symbolicname=" + symbolicName + ")", timeout);
    }
    
    public static Option[] blueprintBundles() {
        return blueprintBundles(true);
    }
    
    public static Option[] debug(int port) {
      return flatOptions(vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=" + port),waitForFrameworkStartup());
    }
    
    public static Option[] blueprintBundles(boolean startBlueprint) {
        return flatOptions(
                bundles(
                    // Felix Config Admin
                    "org.apache.felix/org.apache.felix.configadmin",
                    // Felix mvn url handler
                    "org.ops4j.pax.url/pax-url-mvn",
                    
                    "org.apache.aries/org.apache.aries.util",
                    "org.apache.aries.proxy/org.apache.aries.proxy",
                   
                    "org.apache.commons/commons-jexl",
                    "org.osgi/org.osgi.compendium"),
                    mavenBundle("org.ow2.asm", "asm-all"),
                    mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.jexl.evaluator"),
                    
                    ((startBlueprint) ? mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.api") :
                        mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.api").noStart()),
                    ((startBlueprint) ? mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.core") :
                        mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.core").noStart()),
                    ((startBlueprint) ? mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.cm") :
                        mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.cm").noStart()),
                    ((startBlueprint) ? mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.annotation.api") :
                        mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.annotation.api").noStart())
        );
    }
    
    public static void testBlueprintContainer(RichBundleContext context, Bundle bundle) throws Exception {
        BlueprintContainer blueprintContainer = getBlueprintContainerForBundle(context, "org.apache.aries.blueprint.sample");
        assertNotNull(blueprintContainer);

        Object obj = blueprintContainer.getComponentInstance("bar");
        assertNotNull(obj);
        assertEquals(Bar.class, obj.getClass());
        Bar bar = (Bar) obj;
        assertNotNull(bar.getContext());
        assertEquals("Hello FooBar", bar.getValue());
        assertNotNull(bar.getList());
        assertEquals(2, bar.getList().size());
        assertEquals("a list element", bar.getList().get(0));
        assertEquals(Integer.valueOf(5), bar.getList().get(1));
        obj = blueprintContainer.getComponentInstance("foo");
        assertNotNull(obj);
        assertEquals(Foo.class, obj.getClass());
        Foo foo = (Foo) obj;
        assertEquals(5, foo.getA());
        assertEquals(10, foo.getB());
        assertSame(bar, foo.getBar());
        assertEquals(Currency.getInstance("PLN"), foo.getCurrency());
        assertEquals(new SimpleDateFormat("yyyy.MM.dd").parse("2009.04.17"),
                foo.getDate());

        assertTrue(foo.isInitialized());
        assertFalse(foo.isDestroyed());

        obj = context.getService(Foo.class);
        assertNotNull(obj);
        assertEquals(obj, foo);
        
        obj = blueprintContainer.getComponentInstance("accountOne");
        assertNotNull(obj);
        Account account = (Account)obj;
        assertEquals(1, account.getAccountNumber());
     
        obj = blueprintContainer.getComponentInstance("accountTwo");
        assertNotNull(obj);
        account = (Account)obj;
        assertEquals(2, account.getAccountNumber());
        
        obj = blueprintContainer.getComponentInstance("accountThree");
        assertNotNull(obj);
        account = (Account)obj;
        assertEquals(3, account.getAccountNumber());
        
        obj = blueprintContainer.getComponentInstance("accountFactory");
        assertNotNull(obj);
        AccountFactory accountFactory = (AccountFactory)obj;
        assertEquals("account factory", accountFactory.getFactoryName());
        
        bundle.stop();

        Thread.sleep(1000);

        try {
            blueprintContainer = getBlueprintContainerForBundle(context, "org.apache.aries.blueprint.sample", 1);
            fail("BlueprintContainer should have been unregistered");
        } catch (Exception e) {
            // Expected, as the module container should have been unregistered
        }

        assertTrue(foo.isInitialized());
        assertTrue(foo.isDestroyed());
    }
}
