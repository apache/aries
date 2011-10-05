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
package org.apache.aries.jpa.blueprint.aries.itest;

import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.apache.aries.itest.ExtraOptions.*;

import org.apache.aries.itest.AbstractIntegrationTest;
import org.apache.aries.jpa.blueprint.itest.JPATestBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

@RunWith(JUnit4TestRunner.class)
public class JPAInjectionTest extends AbstractIntegrationTest {
 
  @Test
  public void findResources() throws Exception {
    JPATestBean bean = context().getService(JPATestBean.class, "(version=1.0.0)");
    
    assertTrue("No persistence unit injection", bean.pUnitAvailable());
    assertTrue("No persistence context injection", bean.pContextAvailable());
  }
  
  @Test
  public void findResources_110() throws Exception {
    JPATestBean bean = context().getService(JPATestBean.class, "(version=1.1.0)");
    
    assertTrue("No constructor unit injection", bean.constructorPUnitAvailable());
    assertTrue("No constructor context injection", bean.constructorPContextAvailable());
    
    assertTrue("No persistence unit injection", bean.pUnitAvailable());
    assertTrue("No persistence context injection", bean.pContextAvailable());
  }

  @org.ops4j.pax.exam.junit.Configuration
  public static Option[] configuration() {
    return testOptions(
        paxLogging("DEBUG"),

        // Bundles
        mavenBundle("org.osgi", "org.osgi.compendium"),
        mavenBundle("org.apache.aries", "org.apache.aries.util"),
        mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.api"),
        mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.core"), 
        mavenBundle("asm", "asm-all"),
        mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy.api"),
        mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy.impl"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-jpa_2.0_spec"),
        mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.api"),
        mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container"),
        mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container.context"),
        mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.blueprint.aries"),
        mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec"),
        mavenBundle("commons-lang", "commons-lang"),
        mavenBundle("commons-collections", "commons-collections"),
        mavenBundle("commons-pool", "commons-pool"),
        mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.serp"),
        mavenBundle("org.apache.openjpa", "openjpa"),

//        mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.jpa"),
//        mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.core"),
//        mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.asm"),
        
        mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.blueprint.itest.bundle"),
        
        equinox().version("3.5.0"));
  }
}
