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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.transaction.itests;

import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.modifyBundle;

import java.io.IOException;
import java.io.InputStream;

import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Customizer;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.container.def.options.VMOption;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.BootClasspathLibraryOption;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.TimeoutOption;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.util.tracker.ServiceTracker;

@RunWith(JUnit4TestRunner.class)
public abstract class AbstractIntegrationTest {

    public static final long DEFAULT_TIMEOUT = 30000;
    
    @Inject
    protected BundleContext bundleContext;

    @org.ops4j.pax.exam.junit.Configuration
    public static Option[] configuration() {
        Option[] options = options(
                customizer(),
                bootClasspathLibrary("org.apache.geronimo.specs", "geronimo-jta_1.1_spec"),

                // Log
                mavenBundle("org.ops4j.pax.logging", "pax-logging-api"),
                mavenBundle("org.ops4j.pax.logging", "pax-logging-service"),

                // this is how you set the default log level when using pax
                // logging (logProfile)
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("DEBUG"),

                // Bundles
                mavenBundle("org.osgi", "org.osgi.compendium"),
                mavenBundle("asm", "asm-all"),
                mavenBundle("org.apache.derby", "derby"),
                mavenBundle("org.apache.aries", "org.apache.aries.util"),
                mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint"), 
                mavenBundle("org.apache.aries.transaction", "org.apache.aries.transaction.manager"),
                mavenBundle("org.apache.aries.transaction", "org.apache.aries.transaction.blueprint"),
                mavenBundle("org.apache.aries.transaction", "org.apache.aries.transaction.testbundle"),
                mavenBundle("org.apache.aries.transaction", "org.apache.aries.transaction.testds"),

                //new VMOption( "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000" ),
                //new TimeoutOption( 0 ),

                equinox().version("3.5.0"));
        options = updateOptions(options);
        return options;
    }

    public static Customizer customizer() {
        return new Customizer()
        {
            @Override
            public InputStream customizeTestProbe( InputStream testProbe )
            throws IOException
            {
                return modifyBundle( testProbe )
                .set( Constants.IMPORT_PACKAGE, "javax.transaction;version=\"[0.0.0,1.1.0)\"" )
                .build();
            }
        };
    }

    protected static BootClasspathLibraryOption bootClasspathLibrary(String groupId, String artifactId) {
          MavenArtifactUrlReference maur =
              new MavenArtifactUrlReference().groupId(groupId).artifactId(artifactId)
              .versionAsInProject();
          return new BootClasspathLibraryOption(maur);
      }

    protected static MavenArtifactProvisionOption mavenBundle(String groupId, String artifactId) {
        return CoreOptions.mavenBundle().groupId(groupId).artifactId(artifactId)
            .versionAsInProject();
      }

    protected Bundle getBundle(String symbolicName) {
        return getBundle(symbolicName, null);
      }

    protected Bundle getBundle(String bundleSymbolicName, String version) {
        Bundle result = null;
        for (Bundle b : bundleContext.getBundles()) {
          if (b.getSymbolicName().equals(bundleSymbolicName)) {
            if (version == null
                || b.getVersion().equals(Version.parseVersion(version))) {
              result = b;
              break;
            }
          }
        }
        return result;
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

    protected <T> T getOsgiService(Class<T> type, long timeout) {
        return getOsgiService(type, null, timeout);
      }

    protected <T> T getOsgiService(Class<T> type) {
        return getOsgiService(type, null, DEFAULT_TIMEOUT);
      }

    protected <T> T getOsgiService(Class<T> type, String filter, long timeout) {
        return getOsgiService(null, type, filter, timeout);
      }

    protected <T> T getOsgiService(BundleContext bc, Class<T> type, String filter,
            long timeout) {
                ServiceTracker tracker = null;
                try {
                  String flt;
                  if (filter != null) {
                    if (filter.startsWith("(")) {
                      flt = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")"
                          + filter + ")";
                    } else {
                      flt = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")("
                          + filter + "))";
                    }
                  } else {
                    flt = "(" + Constants.OBJECTCLASS + "=" + type.getName() + ")";
                  }
                  Filter osgiFilter = FrameworkUtil.createFilter(flt);
                  tracker = new ServiceTracker(bc == null ? bundleContext : bc, osgiFilter,
                      null);
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