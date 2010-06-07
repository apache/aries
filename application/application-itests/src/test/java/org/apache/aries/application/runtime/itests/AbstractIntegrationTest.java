/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
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
package org.apache.aries.application.runtime.itests;

import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
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
import org.osgi.framework.Version;
import org.osgi.util.tracker.ServiceTracker;

@RunWith(JUnit4TestRunner.class)
public class AbstractIntegrationTest {

  public static final long DEFAULT_TIMEOUT = 30000;

  @Inject
  protected BundleContext bundleContext;
  
  private List<ServiceTracker> srs;

  @Before
  public void setUp() {
      srs = new ArrayList<ServiceTracker>();
  }
  
  @After
  public void tearDown() throws Exception{
      for (ServiceTracker st : srs) {
          if (st != null) {
              st.close();
          }  
      }
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

  public static MavenArtifactProvisionOption mavenBundle(String groupId,
      String artifactId) {
    return CoreOptions.mavenBundle().groupId(groupId).artifactId(artifactId)
        .versionAsInProject();
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

  protected <T> T getOsgiService(BundleContext bc, Class<T> type,
      String filter, long timeout) {
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
      
      // add tracker to the list of trackers we close at tear down
      srs.add(tracker);

      Object x = tracker.waitForService(timeout);
      Object svc = type.cast(x);
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