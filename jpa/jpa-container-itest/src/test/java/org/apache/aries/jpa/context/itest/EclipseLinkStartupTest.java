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

import org.apache.aries.jpa.itest.AbstractJPAItest;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.Bundle;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;

public class EclipseLinkStartupTest extends AbstractJPAItest {
    
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

    @Configuration
    public Option[] configuration() {
        return CoreOptions.options(
        		baseOptions(),
        		ariesJpa21(),
        		eclipseLink(),
        		testBundleEclipseLink().noStart()
            );
    }
}
