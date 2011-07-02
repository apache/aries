package org.apache.aries.jpa.context.itest;

import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

import static org.ops4j.pax.exam.CoreOptions.options;

@RunWith(JUnit4TestRunner.class)
public class EclipseLinkContextTest extends JPAContextTest {
    @Configuration
    public static Option[] eclipseLinkConfig() {
        return options(        
                mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.jpa"),
                mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.core"),
                mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.asm"),
                mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.antlr"),
                
                mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.eclipselink.adapter"),
                mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container.itest.bundle.eclipselink")
        );
    }
}
