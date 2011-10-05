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
package org.apache.aries.jpa.context.itest;

import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.apache.aries.itest.ExtraOptions.*;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;

import org.apache.aries.itest.AbstractIntegrationTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;

@RunWith(JUnit4TestRunner.class)
public class EclipseLinkStartupTest extends AbstractIntegrationTest {
    
    @Test
    public void testContextCreationWithStartingBundle() throws Exception {
        // wait for the Eclipselink provider to come up
        context().getService(PersistenceProvider.class);
        
        for (Bundle b : bundleContext.getBundles()) {
            if (b.getSymbolicName().equals("org.apache.aries.jpa.container.itest.bundle.eclipselink")) {
                b.start();
            }
        }
        
        context().getService(EntityManagerFactory.class);
    }

    @org.ops4j.pax.exam.junit.Configuration
    public static Option[] configuration() {
        return testOptions(
                felix().version("3.2.1"),
                paxLogging("INFO"),

                // Bundles
                mavenBundle("org.osgi", "org.osgi.compendium"),
                mavenBundle("org.apache.aries", "org.apache.aries.util"),
                // Adding blueprint to the runtime is a hack to placate the
                // maven bundle plugin.
                mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.api"),
                mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.core"),
                mavenBundle("asm", "asm-all"),
                mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy.api"),
                mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy.impl"),
                mavenBundle("org.apache.geronimo.specs", "geronimo-jpa_2.0_spec"),
                mavenBundle("org.apache.aries.jndi", "org.apache.aries.jndi.api"),
                mavenBundle("org.apache.aries.jndi", "org.apache.aries.jndi.core"),
                mavenBundle("org.apache.aries.jndi", "org.apache.aries.jndi.url"),
                mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.api"),
                mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container"),
                mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container.context"),
                mavenBundle("org.apache.aries.transaction", "org.apache.aries.transaction.manager"),
                mavenBundle("org.apache.aries.transaction", "org.apache.aries.transaction.wrappers"),
                mavenBundle("org.apache.derby", "derby"),
                mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec"),
                mavenBundle("commons-lang", "commons-lang"),
                mavenBundle("commons-collections", "commons-collections"),
                mavenBundle("commons-pool", "commons-pool"),

                mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.jpa"),
                mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.core"),
                mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.asm"),
                mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.antlr"),
                
                mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.eclipselink.adapter"),
                mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container.itest.bundle.eclipselink").noStart()
            );
    }
}
