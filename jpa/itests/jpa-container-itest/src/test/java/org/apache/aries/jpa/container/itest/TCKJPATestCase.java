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
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * Simulates some tests from the tck
 */
public class TCKJPATestCase extends AbstractJPAItest {

    @Test
	public void testEntityManagerFactoryWithIncompletePersistenceUnit() throws Exception {
		EntityManagerFactory emf = getService(EntityManagerFactory.class, "(osgi.unit.name=incompleteTestUnit)", false); 
		Assert.assertNull("There should be no EntityManagerFactory registered since this persistence unit is incomplete", emf);
		Bundle testBundle = getBundleByName("incompleteTestUnit");
		testBundle.uninstall();
	}

    @Configuration
    public Option[] configuration() {
    	InputStream testBundle = TinyBundles.bundle()
    			.set(Constants.BUNDLE_SYMBOLICNAME, "incompleteTestUnit")
    			.set("Meta-Persistence", " ")
    			.add("META-INF/persistence.xml", this.getClass().getResourceAsStream("persistence.xml"))
    			.build(TinyBundles.withBnd());
        return new Option[] {
            baseOptions(), //
            ariesJpa20(), //
            derbyDSF(), //
            hibernate(), //
            streamBundle(testBundle),
            debug()
        };
    }
}
