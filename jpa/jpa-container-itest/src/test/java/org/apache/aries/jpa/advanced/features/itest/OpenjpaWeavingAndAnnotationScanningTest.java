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
package org.apache.aries.jpa.advanced.features.itest;

import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.apache.aries.itest.ExtraOptions.*;

import java.util.Arrays;

import javax.persistence.EntityManagerFactory;

import org.apache.aries.jpa.container.PersistenceUnitConstants;
import org.apache.aries.jpa.container.advanced.itest.bundle.entities.Car;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

@RunWith(JUnit4TestRunner.class)
public class OpenjpaWeavingAndAnnotationScanningTest extends JPAWeavingAndAnnotationScanningTest {

    @Configuration
    public static Option[] openjpaConfig() {
        return options(        
                mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.serp"),
                mavenBundle("org.apache.openjpa", "openjpa")
        );
    }
    
    @Test
    public void testClassIsWoven() throws Exception {
      context().getService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))");
      assertTrue("Not PersistenceCapable", Arrays.asList(Car.class.getInterfaces())
          .contains(PersistenceCapable.class));
    }
    
}
