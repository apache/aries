package org.apache.aries.tx.control.itests;

import static org.ops4j.pax.exam.CoreOptions.bootClasspathLibrary;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemPackage;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.util.Dictionary;
import java.util.Hashtable;

import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;

public class SimpleOpenJPA_2_4_1_Test extends AbstractSimpleTransactionTest {

	protected String ariesJPAVersion() {
		return "2.4.0-SNAPSHOT";
	}
	
	protected Dictionary<String, Object> getBaseProperties() {
		Dictionary<String, Object> base = new Hashtable<>();
		//This is necessary due to https://issues.apache.org/jira/browse/OPENJPA-2521
		base.put("openjpa.MetaDataFactory", "jpa(Types=org.apache.aries.tx.control.itests.entity.Message)");
		base.put("openjpa.RuntimeUnenhancedClasses", "supported");
		
		//This is necessary as OpenJPA is only JPA 2.0 compliant and does not understand the standard properties
		base.put("openjpa.jdbc.SynchronizeMappings", "buildSchema(ForeignKeys=true, SchemaAction='add,deleteTableContents')");
		
		base.put("openjpa.Log", "DefaultLevel=TRACE");
		return base;
	}
	
	@Override
	protected Option jpaProvider() {
		return CoreOptions.composite(
			// Add JTA 1.1 as a system package because of the link to javax.sql
			
			systemProperty(ARIES_EMF_BUILDER_TARGET_FILTER)
				.value("(osgi.unit.provider=org.apache.openjpa.persistence.PersistenceProviderImpl)"),
			systemPackage("javax.transaction;version=1.1"),
			systemPackage("javax.transaction.xa;version=1.1"),
			bootClasspathLibrary(mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec", "1.1.1")).beforeFramework(),
			
			// OpenJPA bundles and their dependencies (JPA API is available from the tx-control)
			mavenBundle("commons-pool", "commons-pool", "1.5.4"),
			mavenBundle("commons-lang", "commons-lang", "2.4"),
			mavenBundle("commons-collections", "commons-collections", "3.2.2"),
			mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.serp", "1.15.1_1"),
			mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.commons-dbcp", "1.4_3"),
			mavenBundle("org.apache.xbean", "xbean-asm5-shaded", "3.17"),
			mavenBundle("org.apache.openjpa", "openjpa", "2.4.1"));
	}

}
