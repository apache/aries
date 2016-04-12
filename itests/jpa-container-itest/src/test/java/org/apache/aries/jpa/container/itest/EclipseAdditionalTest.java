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

import static javax.persistence.spi.PersistenceUnitTransactionType.RESOURCE_LOCAL;
import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.CoreOptions.streamBundle;
import static org.osgi.service.jdbc.DataSourceFactory.OSGI_JDBC_DRIVER_CLASS;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;

import org.apache.aries.jpa.itest.AbstractJPAItest;
import org.eclipse.persistence.config.SessionCustomizer;
import org.eclipse.persistence.sessions.Session;
import org.junit.Assert;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Constants;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;

public class EclipseAdditionalTest extends AbstractJPAItest {

	private static final String CUSTOMIZER_CALLED = "org.apache.aries.jpa.itest.eclipse.customizer";

	@Test
    public void testContextCreationWithStartingBundle() throws Exception {
        getBundleByName("org.apache.aries.jpa.container.itest.bundle.eclipselink").start();
        getEMF("script-test-unit");
    }
    
    @Test
    public void testEntityManagerFactoryBuilderWithIncompletePersistenceUnit() throws Exception {
        getService(EntityManagerFactoryBuilder.class, "(osgi.unit.name=incompleteTestUnit)", 1000);
    }

    @Test
    public void testEntityManagerFactoryBuilderWithIncompletePersistenceUnitAddPlugin() throws Exception {
    	EntityManagerFactoryBuilder builder = getService(EntityManagerFactoryBuilder.class, "(osgi.unit.name=incompleteTestUnit)", 1000);
    	
    	DataSourceFactory dsf = getService(DataSourceFactory.class, 
    			"(" + OSGI_JDBC_DRIVER_CLASS + "=org.apache.derby.jdbc.EmbeddedDriver)");
       
    	Properties jdbcProps = new Properties();
    	jdbcProps.setProperty("url", "jdbc:derby:memory:DSFTEST;create=true");
    	
    	Map<String, Object> props = new HashMap<String, Object>();
    	props.put("javax.persistence.nonJtaDataSource", dsf.createDataSource(jdbcProps));
    	props.put("javax.persistence.transactionType", RESOURCE_LOCAL.name());
    	
    	props.put("org.apache.aries.jpa.eclipselink.plugin.types", SessionCustomizerImpl.class);
    	props.put("eclipselink.session.customizer", SessionCustomizerImpl.class.getName());
    	
    	EntityManagerFactory emf = builder.createEntityManagerFactory(props);
    	emf.createEntityManager();
		assertEquals("invoked", emf
    			.getProperties().get(CUSTOMIZER_CALLED));
    	
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
//         ,debug()
        };
    }

	public static class SessionCustomizerImpl implements SessionCustomizer {
	
		@Override
		public void customize(Session arg0) throws Exception {
			arg0.setProperty(CUSTOMIZER_CALLED, "invoked");
		}
	}
}
