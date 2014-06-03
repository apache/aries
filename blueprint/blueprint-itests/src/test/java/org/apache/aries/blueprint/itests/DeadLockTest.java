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

import static org.apache.aries.blueprint.itests.Helper.mvnBundle;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.composite;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.aries.blueprint.itests.comp.ListFactory;
import org.apache.aries.blueprint.itests.comp.Listener;
import org.junit.Test;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

public class DeadLockTest extends AbstractBlueprintIntegrationTest {

    private static final int TOTAL_REF_TEST_BUNDLES = 10;

    @Test
    public void testReferenceListenerDeadlock() throws Exception {
        for (int i=0; i < TOTAL_REF_TEST_BUNDLES; i++) {
            Bundle b = context().getBundleByName("sample" + i);
            b.start();
        }

        // every blueprint container should be up
        for (int i=0; i < TOTAL_REF_TEST_BUNDLES; i++) {
            assertNotNull(Helper.getBlueprintContainerForBundle(context(), "sample" + i));
        }
    }
    
    private InputStream getTestBundle(int no, int total) {
        StringBuilder blueprint = new StringBuilder();
        blueprint.append("<blueprint xmlns=\"http://www.osgi.org/xmlns/blueprint/v1.0.0\">");
        blueprint.append("<bean id=\"listener\" class=\"" + Listener.class.getName() + "\" />");
        
        for (int i=0; i<total; i++) {
            if (i==no) {
                blueprint.append("<service interface=\"java.util.List\">");
                blueprint.append("<service-properties><entry key=\"no\" value=\""+i+"\" /></service-properties>");
                blueprint.append("<bean class=\"" + ListFactory.class.getName() + "\" factory-method=\"create\">");
                blueprint.append("<argument value=\""+i+"\" />");
                blueprint.append("</bean>");
                blueprint.append("</service>");
            } else {
                blueprint.append("<reference availability=\"optional\" id=\"ref"+i+"\" interface=\"java.util.List\" filter=\"(no="+i+")\">");
                blueprint.append("<reference-listener ref=\"listener\" bind-method=\"bind\" unbind-method=\"unbind\" />");
                blueprint.append("</reference>");
            }
        }
        blueprint.append("</blueprint>");
        
        try {
            InputStream is = new ByteArrayInputStream(blueprint.toString().getBytes("UTF-8"));
            return TinyBundles.bundle()
                    .add(Listener.class)
                    .add(ListFactory.class)
                    .add("OSGI-INF/blueprint/blueprint.xml", is)
                    .set(Constants.IMPORT_PACKAGE, "org.osgi.framework")
                    .set(Constants.BUNDLE_SYMBOLICNAME, "sample" + no).build();
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        
        
    }
    
    private Option[] getRefTestBundles() {
        List<Option> refTestBundles = new ArrayList<Option>();
        for (int c=0;c < TOTAL_REF_TEST_BUNDLES; c++) {
            refTestBundles.add(CoreOptions.provision(getTestBundle(c, TOTAL_REF_TEST_BUNDLES)));
        }
        return refTestBundles.toArray(new Option[]{});
    }
    
    @Test
    public void testDeadlock() throws Exception {
      bundleContext.registerService("java.util.Set",new HashSet<Object>(), null);
      
      Bundle bundle = context().getBundleByName("org.apache.aries.blueprint.sample");
      assertNotNull(bundle);

      bundle.start();
      
      Helper.getBlueprintContainerForBundle(context(), "org.apache.aries.blueprint.sample");
      
      // no actual assertions, we just don't want to deadlock
    }
    
    @org.ops4j.pax.exam.Configuration
    public Option[] configuration() {
        return new Option[] {
            baseOptions(),
            Helper.blueprintBundles(),
            mvnBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.sample", false),
            composite(getRefTestBundles()),
        };
    }


}
