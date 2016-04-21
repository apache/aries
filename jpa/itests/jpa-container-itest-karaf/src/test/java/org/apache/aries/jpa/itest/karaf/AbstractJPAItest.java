/*  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.jpa.itest.karaf;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureSecurity;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.UrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public abstract class AbstractJPAItest {
    private static Logger LOG = LoggerFactory.getLogger(AbstractJPAItest.class);

    /*
     *  The @Inject annotations below currently do not work as the transaction-api/1.2 feature installs an atinject bundle that 
     *  confuses pax exam. As a workaround the services are set below in the @Before method
     */
    
    @Inject
    BundleContext bundleContext;
    
    @Inject
    ConfigurationAdmin configAdmin;
    
    private static Configuration config;
    
    /**
     * Helps to diagnose bundles that are not resolved as it will throw a detailed exception
     * 
     * @throws BundleException
     */
    public void resolveBundles() throws BundleException {
        Bundle[] bundles = bundleContext.getBundles();
        for (Bundle bundle : bundles) {
            if (bundle.getState() == Bundle.INSTALLED) {
                System.out.println("Found non resolved bundle " + bundle.getBundleId() + ":"
                    + bundle.getSymbolicName() + ":" + bundle.getVersion());
                bundle.start();
            }
        }
    }

    public Bundle getBundleByName(String symbolicName) {
        for (Bundle b : bundleContext.getBundles()) {
            if (b.getSymbolicName().equals(symbolicName)) {
                return b;
            }
        }
        return null;
    }
    
    public <T> T getService(Class<T> type, String filter) {
        return getService(type, filter, 10000);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public <T> T getService(Class<T> type, String filter, int timeout) {
        ServiceTracker tracker = null;
        try {
            String objClassFilter = "(" + Constants.OBJECTCLASS + "=" + type.getName() + ")";
            String flt = filter != null ? "(&" + objClassFilter + sanitizeFilter(filter) + ")" : objClassFilter;
            Filter osgiFilter = FrameworkUtil.createFilter(flt);
            tracker = new ServiceTracker(bundleContext, osgiFilter, null);
            tracker.open();

            Object svc = type.cast(tracker.waitForService(timeout));
            if (svc == null) {
                throw new IllegalStateException("Gave up waiting for service " + flt);
            }
            return type.cast(svc);
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Invalid filter", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //tracker.close();
        }
    }

    public String sanitizeFilter(String filter) {
        return filter.startsWith("(") ? filter : "(" + filter + ")";
    }

    protected Option baseOptions() {
        String localRepo = System.getProperty("maven.repo.local");

        if (localRepo == null) {
            localRepo = System.getProperty("org.ops4j.pax.url.mvn.localRepository");
        }
        MavenArtifactUrlReference karafUrl = maven().groupId("org.apache.karaf").artifactId("apache-karaf").version("4.0.1").type("tar.gz");
        UrlReference enterpriseFeatureUrl = maven().groupId("org.apache.karaf.features").artifactId("enterprise").versionAsInProject().type("xml").classifier("features");
        UrlReference jpaFeatureUrl = maven().groupId("org.apache.aries.jpa").artifactId("jpa-features").versionAsInProject().type("xml").classifier("features");
        UrlReference paxJdbcFeatureUrl = maven().groupId("org.ops4j.pax.jdbc").artifactId("pax-jdbc-features").version("0.7.0").type("xml").classifier("features");
        return CoreOptions.composite(
            //KarafDistributionOption.debugConfiguration("8000", true),
            karafDistributionConfiguration().frameworkUrl(karafUrl).name("Apache Karaf").unpackDirectory(new File("target/exam")).useDeployFolder(false),
            configureSecurity().disableKarafMBeanServerBuilder(),
            keepRuntimeFolder(),
            logLevel(LogLevel.INFO),
            when(localRepo != null).useOptions(vmOption("-Dorg.ops4j.pax.url.mvn.localRepository=" + localRepo)),
            features(paxJdbcFeatureUrl, "pax-jdbc-config", "pax-jdbc-h2", "pax-jdbc-pool-dbcp2"),
            features(enterpriseFeatureUrl, "transaction", "http-whiteboard", "hibernate/4.3.6.Final", "scr"),
            features(jpaFeatureUrl, "jpa"),
            mavenBundle("org.apache.aries.jpa.example", "org.apache.aries.jpa.example.tasklist.model").versionAsInProject()
//            replaceConfigurationFile("etc/org.ops4j.pax.logging.cfg", getConfigFile("/etc/org.ops4j.pax.logging.cfg")),
        );

    }

    @Before
    public void createConfigs() throws Exception {
        if (config == null) {
            bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
            configAdmin = getService(ConfigurationAdmin.class, null);
            createConfigForLogging();
            createConfigForDS();

        }
    }

    private void createConfigForDS() throws IOException {
        config = configAdmin.createFactoryConfiguration("org.ops4j.datasource", null);
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(DataSourceFactory.OSGI_JDBC_DRIVER_NAME, "H2-pool-xa");
        props.put(DataSourceFactory.JDBC_DATABASE_NAME, "tasklist;create=true");
        props.put(DataSourceFactory.JDBC_DATASOURCE_NAME, "tasklist");
        config.update(props);
        LOG.info("Created DataSource config tasklist");
    }
    
    private void createConfigForLogging() throws IOException {
        Configuration logConfig = configAdmin.getConfiguration("org.ops4j.pax.logging", null);
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put("log4j.rootLogger", "INFO, stdout");
        props.put("log4j.logger.org.apache.aries.transaction", "DEBUG");
        props.put("log4j.logger.org.apache.aries.transaction.parsing", "DEBUG");
        props.put("log4j.logger.org.apache.aries.jpa.blueprint.impl", "DEBUG");
        props.put("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
        props.put("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
        props.put("log4j.appender.stdout.layout.ConversionPattern", "%d{ISO8601} | %-5.5p | %-16.16t | %c | %m%n");
        logConfig.update(props);
    }

}
