# jpa-experiments
Exploring better ways than current aries-jpa to support jpa in OSGi

# copy DataSource config
cat https://raw.githubusercontent.com/cschneider/jpa-experiments/master/org.ops4j.datasource-tasklist.cfg | tac -f etc/org.ops4j.datasource-tasklist.cfg 

feature:repo-add mvn:org.ops4j.pax.jdbc/pax-jdbc-features/0.5.0/xml/features
feature:install scr transaction pax-jdbc-config pax-jdbc-h2 pax-jdbc-pool-dbcp2 http-whiteboard

install -s mvn:org.hibernate.javax.persistence/hibernate-jpa-2.1-api/1.0.0.Final

# Hibernate + Dependencies
# Normally we would use the karaf feature but it install aries jpa which we do not want
install -s mvn:org.apache.servicemix.bundles/org.apache.servicemix.bundles.antlr/2.7.7_5                
install -s mvn:org.apache.servicemix.bundles/org.apache.servicemix.bundles.ant/1.8.2_2                  
install -s mvn:org.apache.servicemix.bundles/org.apache.servicemix.bundles.dom4j/1.6.1_5                
install -s mvn:org.apache.servicemix.bundles/org.apache.servicemix.bundles.serp/1.14.1_1                
install -s mvn:com.fasterxml/classmate/0.9.0                                                            
install -s mvn:org.javassist/javassist/3.18.1-GA                                                        
install -s mvn:org.jboss.spec.javax.security.jacc/jboss-jacc-api_1.4_spec/1.0.2.Final                   
install -s mvn:org.jboss/jandex/1.2.2.Final                                                             
install -s mvn:org.jboss.logging/jboss-logging/3.1.4.GA                                                 
install -s mvn:org.hibernate.common/hibernate-commons-annotations/4.0.4.Final                           
install -s mvn:org.hibernate/hibernate-core/4.3.6.Final                                                 
install -s mvn:org.hibernate/hibernate-entitymanager/4.3.6.Final                                        
install -s mvn:org.hibernate/hibernate-osgi/4.3.6.Final 

# Eclipselink + Dependencies
install -s mvn:org.eclipse.persistence/javax.persistence/2.1.0
install -s mvn:org.eclipse.persistence/org.eclipse.persistence.core/2.6.0
install -s mvn:org.eclipse.persistence/org.eclipse.persistence.asm/2.6.0
install -s mvn:org.eclipse.persistence/org.eclipse.persistence.jpa/2.6.0
install -s mvn:org.eclipse.persistence/org.eclipse.persistence.antlr/2.6.0
install -s mvn:org.eclipse.persistence/org.eclipse.persistence.jpa.jpql/2.6.0
install -s mvn:org.apache.aries.jpa/org.apache.aries.jpa.eclipselink.adapter/1.0.0-SNAPSHOT


# Our JPA service implementation
install -s mvn:org.apache.aries.jpa/org.apache.aries.jpa.api/2.0.0-SNAPSHOT
install -s mvn:org.apache.aries.jpa/org.apache.aries.jpa.container/2.0.0-SNAPSHOT
install -s mvn:org.apache.aries.jpa/org.apache.aries.jpa.support/2.0.0-SNAPSHOT

# Closure based example. (Make sure to start karaf with JDK 8)
install -s mvn:org.apache.aries.jpa.example/jpa-example-tasklist-model/2.0.0-SNAPSHOT
install -s mvn:org.apache.aries.jpa.example/jpa-example-tasklist-closure/2.0.0-SNAPSHOT
install -s mvn:org.apache.aries.jpa.example/jpa-example-tasklist-ui/2.0.0-SNAPSHOT

# Blueprint based example
install -s mvn:org.apache.aries.jpa/org.apache.aries.jpa.blueprint/2.0.0-SNAPSHOT
install -s mvn:org.apache.aries.jpa.example/org.apache.aries.jpa.example.tasklist.model/2.0.0-SNAPSHOT
install -s mvn:org.apache.aries.jpa.example/org.apache.aries.jpa.example.tasklist.blueprint/2.0.0-SNAPSHOT
install -s mvn:org.apache.aries.jpa.example/jpa-example-tasklist-ui/2.0.0-SNAPSHOT

