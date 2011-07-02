package org.apache.aries.jpa.advanced.features.itest;

import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.options;

import java.util.Arrays;

import javax.persistence.EntityManagerFactory;

import org.apache.aries.jpa.container.PersistenceUnitConstants;
import org.apache.aries.jpa.container.advanced.itest.bundle.entities.Car;
import org.eclipse.persistence.internal.weaving.PersistenceWeaved;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

@RunWith(JUnit4TestRunner.class)
public class EclipseLinkWeavingAndAnnotationScanningTest extends JPAWeavingAndAnnotationScanningTest {
    @Configuration
    public static Option[] eclipseLinkConfig() {
        return options(        
                mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.jpa"),
                mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.core"),
                mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.asm"),
                mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.antlr"),
                
                mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.eclipselink.adapter")
        );
    }
    
    
    @Test
    public void testClassIsWoven() throws Exception {
      getOsgiService(bundleContext, EntityManagerFactory.class, "(&(osgi.unit.name=test-unit)(" + PersistenceUnitConstants.CONTAINER_MANAGED_PERSISTENCE_UNIT + "=true))", DEFAULT_TIMEOUT);
      assertTrue("Not PersistenceCapable", Arrays.asList(Car.class.getInterfaces())
          .contains(PersistenceWeaved.class));
    }

}
