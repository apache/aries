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

import org.apache.aries.jpa.container.PersistenceUnitConstants;
import org.apache.aries.jpa.container.advanced.itest.bundle.entities.Car;
import org.eclipse.persistence.internal.weaving.PersistenceWeaved;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.*;

import java.util.Arrays;

import javax.persistence.EntityManagerFactory;

public class EclipseLinkWeavingAndAnnotationScanningTest extends JPAWeavingAndAnnotationScanningTest {
    @Configuration
    public Option[] eclipseLinkConfig() {
        return options(        
            baseOptions(),
            ariesJpa21(),
            eclipseLink(),
            testBundleAdvanced()          
        );
    }
    
    @Test
    public void testClassIsWoven() throws Exception {
      context().getService(EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))");
      
      Thread.sleep(200);
      assertTrue("Not PersistenceCapable", Arrays.asList(Car.class.getInterfaces())
          .contains(PersistenceWeaved.class));
    }
	
}
