package org.apache.aries.jpa.context.itest;

import static org.ops4j.pax.exam.CoreOptions.options;

import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

@RunWith(JUnit4TestRunner.class)
public class OpenjpaContextTest extends JPAContextTest {
    @Configuration
    public static Option[] openjpaConfig() {
        return options(        
                mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.serp"),
                mavenBundle("org.apache.openjpa", "openjpa"),
                mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container.itest.bundle")
        );
    }
}
