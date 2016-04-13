package org.apache.aries.tx.control.itests;

import static org.ops4j.pax.exam.CoreOptions.bootClasspathLibrary;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemPackage;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;

public class SimpleEclipseLink_2_6_0_Test extends AbstractSimpleTransactionTest {

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
