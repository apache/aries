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

import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;

/**
 * We test Hibernate 5.2.1, and not 5.2.0 because of Hibernate bug HHH-10807
 */
public class XAHibernate_5_2_1_Test extends XAJPATransactionTest {

	protected Option ariesJPAVersion() {
		return mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container", "2.5.0");
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
			mavenBundle("com.fasterxml", "classmate", "1.3.0"),
			mavenBundle("org.javassist", "javassist", "3.20.0-GA"),
			mavenBundle("org.jboss.logging", "jboss-logging", "3.3.0.Final"),
			mavenBundle("org.jboss", "jandex", "2.0.3.Final"),
			mavenBundle("org.hibernate.common", "hibernate-commons-annotations", "5.0.1.Final"),
			mavenBundle("org.hibernate", "hibernate-core", "5.2.1.Final"),
			mavenBundle("org.hibernate", "hibernate-osgi", "5.2.1.Final"));
	}

}
