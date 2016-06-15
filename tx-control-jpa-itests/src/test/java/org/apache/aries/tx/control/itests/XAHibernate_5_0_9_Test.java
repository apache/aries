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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.tx.control.itests;

import static org.ops4j.pax.exam.CoreOptions.bootClasspathLibrary;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemPackage;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.MethodRule;
import org.junit.runners.model.Statement;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XAHibernate_5_0_9_Test extends XAJPATransactionTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(XAHibernate_5_0_9_Test.class);
	
	@Rule
    public MethodRule rule = (s,m,o) -> {
	    	return new Statement() {
				@Override
					public void evaluate() throws Throwable {
						try {
							s.evaluate();
						} catch (Throwable t) {
							if(!hibernateBugOccurred)
								throw t;
						}					
					}
	    		};
		};

	private boolean hibernateBugOccurred = false;
	
	@Before
	public void clearBugState() {
		hibernateBugOccurred  = false;
	}
		
	@After
	public void hibernateBug() {
		try {
			
			Class<?> m1Clazz = getMessageEntityFrom(XA_TEST_UNIT_1).getClass();
			Class<?> m2Clazz = getMessageEntityFrom(XA_TEST_UNIT_2).getClass();
			
			hibernateBugOccurred = txControl.notSupported(() -> {
					Class<?> hibernateM1Clazz = em1.getMetamodel()
							.getEntities().iterator().next().getJavaType();
					Class<?> hibernateM2Clazz = em2.getMetamodel()
							.getEntities().iterator().next().getJavaType();
					
					if(hibernateM1Clazz != m1Clazz ||
							hibernateM2Clazz != m2Clazz) {
						LOGGER.warn("Encountered Hibernate bug: {}",
								"https://hibernate.atlassian.net/browse/HHH-10855");
						return true;
					}
					return false;
				});
		} catch (Exception e) {
			hibernateBugOccurred = false;
			LOGGER.error("Unable to check the Hibernate bug", e);
			// Just swallow this so we don't hide an underlying test problem
		}
	}

	protected String ariesJPAVersion() {
		return "2.4.0";
	}
	
	
	@Override
	protected Option jpaProvider() {
		return CoreOptions.composite(
			// Add JTA 1.1 as a system package because of the link to javax.sql
			// Also set javax.xml.stream to 1.0 due to hibernate's funny packaging
			
			systemProperty(ARIES_EMF_BUILDER_TARGET_FILTER)
				.value("(osgi.unit.provider=org.hibernate.jpa.HibernatePersistenceProvider)"),
			systemPackage("javax.xml.stream;version=1.0"),
			systemPackage("javax.xml.stream.events;version=1.0"),
			systemPackage("javax.xml.stream.util;version=1.0"),
			systemPackage("javax.transaction;version=1.1"),
			systemPackage("javax.transaction.xa;version=1.1"),
			bootClasspathLibrary(mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec", "1.1.1")).beforeFramework(),
			
			// Hibernate bundles and their dependencies (JPA API is available from the tx-control)
			mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.antlr", "2.7.7_5"),
			mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.dom4j", "1.6.1_5"),
			mavenBundle("org.javassist", "javassist", "3.18.1-GA"),
			mavenBundle("org.jboss.logging", "jboss-logging", "3.3.0.Final"),
			mavenBundle("org.jboss", "jandex", "2.0.0.Final"),
			mavenBundle("org.hibernate.common", "hibernate-commons-annotations", "5.0.1.Final"),
			mavenBundle("org.hibernate", "hibernate-core", "5.0.9.Final"),
			mavenBundle("org.hibernate", "hibernate-osgi", "5.0.9.Final"),
			mavenBundle("org.hibernate", "hibernate-entitymanager", "5.0.9.Final"));
	}

}
