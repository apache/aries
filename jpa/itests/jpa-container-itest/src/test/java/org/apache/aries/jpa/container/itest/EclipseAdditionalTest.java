/*  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.jpa.container.itest;

import static org.ops4j.pax.exam.CoreOptions.streamBundle;

import java.io.InputStream;

import javax.persistence.EntityManagerFactory;

import org.apache.aries.jpa.itest.AbstractJPAItest;
import org.junit.Assert;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Constants;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;

public class EclipseAdditionalTest extends AbstractJPAItest {

    @Test
    public void testContextCreationWithStartingBundle() throws Exception {
        getBundleByName("org.apache.aries.jpa.container.itest.bundle.eclipselink").start();
        getEMF("script-test-unit");
    }
    
    @Test
    public void testEntityManagerFactoryBuilderWithIncompletePersistenceUnit() throws Exception {
        getService(EntityManagerFactoryBuilder.class, "(osgi.unit.name=incompleteTestUnit)", 1000);
    }
    
    @Test(expected = IllegalStateException.class)
    public void testEntityManagerFactoryWithIncompletePersistenceUnit() throws Exception {
        getService(EntityManagerFactory.class, "(osgi.unit.name=incompleteTestUnit)", 1000);
        Assert.fail("There should be no EntityManagerFactory registered since this persistence unit is incomplete");
    }

    @Configuration
    public Option[] configuration() {
        InputStream testBundle = TinyBundles.bundle()
            .set(Constants.BUNDLE_SYMBOLICNAME, "incompleteTestUnit") //
            .set("Meta-Persistence", " ") //
            .add("META-INF/persistence.xml", this.getClass().getResourceAsStream("/persistence.xml")) //
            .build(TinyBundles.withBnd());
        return new Option[] {//
            baseOptions(),//
            ariesJpa21(),//
            jta12Bundles(), //
            eclipseLink(),//
            derbyDSF(), //
            testBundleEclipseLink().noStart(),//
            streamBundle(testBundle)
        // debug()
        };
    }
}
