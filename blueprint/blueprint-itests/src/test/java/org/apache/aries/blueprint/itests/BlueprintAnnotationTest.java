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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

import java.text.SimpleDateFormat;
import java.util.Currency;

import org.apache.aries.blueprint.sample.Bar;
import org.apache.aries.blueprint.sample.Foo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.blueprint.container.BlueprintContainer;

@RunWith(PaxExam.class)
public class BlueprintAnnotationTest extends AbstractBlueprintIntegrationTest {

    @Test
    public void test() throws Exception {
        BlueprintContainer blueprintContainer = getBlueprintContainerForBundle("org.apache.aries.blueprint.sample-annotation");
    
        assertNotNull(blueprintContainer);
    
        Object obj = blueprintContainer.getComponentInstance("bar");
        assertNotNull(obj);
        assertEquals(Bar.class, obj.getClass());
        Bar bar = (Bar) obj;
        assertEquals("Hello FooBar", bar.getValue());
        
        obj = blueprintContainer.getComponentInstance("foo");
        assertNotNull(obj);
        assertEquals(Foo.class, obj.getClass());
        Foo foo = (Foo) obj;
        assertEquals(5, foo.getA());
       // assertEquals(10, foo.getB());
        assertSame(bar, foo.getBar());
        assertEquals(Currency.getInstance("PLN"), foo.getCurrency());
        assertEquals(new SimpleDateFormat("yyyy.MM.dd").parse("2009.04.17"),
                foo.getDate());

        assertTrue(foo.isInitialized());
        assertFalse(foo.isDestroyed());
        
        assertNotNull(blueprintContainer.getComponentInstance("fragment"));
    
        obj = context().getService(Foo.class, null, 5000);
        assertNotNull(obj);
        assertEquals(foo.toString(), obj.toString());
    }

    private BlueprintContainer getBlueprintContainerForBundle(String symbolicName) throws InvalidSyntaxException, InterruptedException {
        return context().getService(BlueprintContainer.class, "(osgi.blueprint.container.symbolicname=" + symbolicName + ")", 15000);
    }    

    @Configuration
    public Option[] configuration() {
        return new Option[] {
            baseOptions(),
            Helper.blueprintBundles(),
            mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.sample-annotation").versionAsInProject(),
            mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.sample-fragment").versionAsInProject().noStart(),
        };
    }

}
