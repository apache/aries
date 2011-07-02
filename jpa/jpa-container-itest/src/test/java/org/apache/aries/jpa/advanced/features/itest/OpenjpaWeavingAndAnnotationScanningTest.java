package org.apache.aries.jpa.advanced.features.itest;

import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.options;

import java.util.Arrays;

import javax.persistence.EntityManagerFactory;

import org.apache.aries.jpa.container.PersistenceUnitConstants;
import org.apache.aries.jpa.container.advanced.itest.bundle.entities.Car;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

@RunWith(JUnit4TestRunner.class)
public class OpenjpaWeavingAndAnnotationScanningTest extends JPAWeavingAndAnnotationScanningTest {

    @Configuration
    public static Option[] openjpaConfig() {
        return options(        
                mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.serp"),
                mavenBundle("org.apache.openjpa", "openjpa")
        );
    }
    
    @Test
    public void testClassIsWoven() throws Exception {
      getOsgiService(bundleContext, EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))", DEFAULT_TIMEOUT);
      assertTrue("Not PersistenceCapable", Arrays.asList(Car.class.getInterfaces())
          .contains(PersistenceCapable.class));
    }
    
}
