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

public class XAEclipseLink_2_6_0_Test extends XAJPATransactionTest {

	@Override
	protected Option jpaProvider() {
		return CoreOptions.composite(
				// Add JTA 1.1 as a system package because of the link to javax.sql
				systemProperty(ARIES_EMF_BUILDER_TARGET_FILTER)
					.value("(osgi.unit.provider=org.eclipse.persistence.jpa.PersistenceProvider)"),
				systemPackage("javax.transaction;version=1.1"),
				systemPackage("javax.transaction.xa;version=1.1"),
				bootClasspathLibrary(mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec", "1.1.1")),
				
				// EclipseLink bundles and their dependencies (JPA API is available from the tx-control)
				mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.jpa", "2.6.0"),
				mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.core", "2.6.0"),
				mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.asm", "2.6.0"),
				mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.antlr", "2.6.0"),
				mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.jpa.jpql", "2.6.0"),
				mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.eclipselink.adapter", "2.4.0-SNAPSHOT"));
	}

}
