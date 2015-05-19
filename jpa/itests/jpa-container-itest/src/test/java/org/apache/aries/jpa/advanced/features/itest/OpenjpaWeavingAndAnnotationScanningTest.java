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

import java.util.Arrays;

import org.apache.aries.jpa.container.advanced.itest.bundle.entities.Car;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

// TODO The Test persistence unit does not seem to be created. Reenable when this works 
public class OpenjpaWeavingAndAnnotationScanningTest extends JPAWeavingAndAnnotationScanningTest {

    @Configuration
    public Option[] openjpaConfig() {
        return new Option[] {
            baseOptions(), //
            openJpa(), //
            derbyDSF(), //
            ariesJpa20(), //
            transactionWrapper(), //
            testBundleAdvanced(), //
        };
    }

    @Test
    public void testClassIsWoven() throws Exception {
        assertTrue("Not PersistenceCapable",
                   Arrays.asList(Car.class.getInterfaces()).contains(PersistenceCapable.class));
    }

}
