package org.apache.aries.jpa.context.itest;

import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

@RunWith(JUnit4TestRunner.class)
public class EclipseLinkStartupTest {
    public static final long DEFAULT_TIMEOUT = 10000;

    @Inject
    protected BundleContext bundleContext;
    
    @Test
    public void testContextCreationWithStartingBundle() throws Exception {
        // wait for the Eclipselink provider to come up
        getOsgiService(PersistenceProvider.class);
        
        for (Bundle b : bundleContext.getBundles()) {
            if (b.getSymbolicName().equals("org.apache.aries.jpa.container.itest.bundle.eclipselink")) {
                b.start();
            }
        }
        
        getOsgiService(EntityManagerFactory.class);
    }

    @org.ops4j.pax.exam.junit.Configuration
    public static Option[] configuration() {
        Option[] options = options(
                felix().version("3.2.1"),
                // Log
                mavenBundle("org.ops4j.pax.logging", "pax-logging-api"),
                mavenBundle("org.ops4j.pax.logging", "pax-logging-service"),
                // Felix Config Admin
                mavenBundle("org.apache.felix", "org.apache.felix.configadmin"),
                // Felix mvn url handler
                mavenBundle("org.ops4j.pax.url", "pax-url-mvn"),

                // this is how you set the default log level when using pax
                // logging (logProfile)
//                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level")
//                        .value("DEBUG"),

                // Bundles
                mavenBundle("org.osgi", "org.osgi.compendium"),
                mavenBundle("org.apache.aries", "org.apache.aries.util"),
                // Adding blueprint to the runtime is a hack to placate the
                // maven bundle plugin.
                mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint"),
                mavenBundle("asm", "asm-all"),
                mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy"),
                mavenBundle("org.apache.geronimo.specs", "geronimo-jpa_2.0_spec"),
                mavenBundle("org.apache.aries.jndi", "org.apache.aries.jndi.api"),
                mavenBundle("org.apache.aries.jndi", "org.apache.aries.jndi.core"),
                mavenBundle("org.apache.aries.jndi", "org.apache.aries.jndi.url"),
                mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.api"),
                mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container"),
                mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container.context"),
                mavenBundle("org.apache.aries.transaction", "org.apache.aries.transaction.manager"),
                mavenBundle("org.apache.aries.transaction", "org.apache.aries.transaction.wrappers"),
                mavenBundle("org.apache.derby", "derby"),
                mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec"),
                mavenBundle("commons-lang", "commons-lang"),
                mavenBundle("commons-collections", "commons-collections"),
                mavenBundle("commons-pool", "commons-pool"),

                mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.jpa"),
                mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.core"),
                mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.asm"),
                mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.antlr"),
                
                mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.eclipselink.adapter"),
                mavenBundle("org.apache.aries.jpa", "org.apache.aries.jpa.container.itest.bundle.eclipselink").noStart()
            );
        options = updateOptions(options);
        return options;
    }

    protected static Option[] updateOptions(Option[] options) {
        // We need to add pax-exam-junit here when running with the ibm
        // jdk to avoid the following exception during the test run:
        // ClassNotFoundException: org.ops4j.pax.exam.junit.Configuration
        if ("IBM Corporation".equals(System.getProperty("java.vendor"))) {
            Option[] ibmOptions = options(wrappedBundle(mavenBundle(
                    "org.ops4j.pax.exam", "pax-exam-junit")));
            options = combine(ibmOptions, options);
        }

        return options;
    }

    public static MavenArtifactProvisionOption mavenBundle(String groupId,
            String artifactId) {
        return CoreOptions.mavenBundle().groupId(groupId)
                .artifactId(artifactId).versionAsInProject();
    }

    protected <T> T getOsgiService(Class<T> type, long timeout) {
        return getOsgiService(type, null, timeout);
    }

    protected <T> T getOsgiService(Class<T> type) {
        return getOsgiService(type, null, DEFAULT_TIMEOUT);
    }

    protected <T> T getOsgiService(Class<T> type, String filter, long timeout) {
        return getOsgiService(null, type, filter, timeout);
    }

    protected <T> T getOsgiService(BundleContext bc, Class<T> type,
            String filter, long timeout) {
        ServiceTracker tracker = null;
        try {
            String flt;
            if (filter != null) {
                if (filter.startsWith("(")) {
                    flt = "(&(" + Constants.OBJECTCLASS + "=" + type.getName()
                            + ")" + filter + ")";
                } else {
                    flt = "(&(" + Constants.OBJECTCLASS + "=" + type.getName()
                            + ")(" + filter + "))";
                }
            } else {
                flt = "(" + Constants.OBJECTCLASS + "=" + type.getName() + ")";
            }
            Filter osgiFilter = FrameworkUtil.createFilter(flt);
            tracker = new ServiceTracker(bc == null ? bundleContext : bc,
                    osgiFilter, null);
            tracker.open();
            // Note that the tracker is not closed to keep the reference
            // This is buggy, has the service reference may change i think
            Object svc = type.cast(tracker.waitForService(timeout));
            if (svc == null) {
                throw new RuntimeException("Gave up waiting for service " + flt);
            }
            return type.cast(svc);
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Invalid filter", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
