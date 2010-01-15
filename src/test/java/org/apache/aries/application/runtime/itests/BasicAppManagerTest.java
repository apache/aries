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
package org.apache.aries.application.runtime.itests;

import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.File;

import org.apache.aries.application.filesystem.IDirectory;
import org.apache.aries.application.management.AriesApplicationManager;
import org.apache.aries.application.utils.filesystem.FileSystem;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

@RunWith(JUnit4TestRunner.class)
public class BasicAppManagerTest extends AbstractIntegrationTest {

  @Test
  public void getStarted() throws Exception {
    AriesApplicationManager manager = getOsgiService(AriesApplicationManager.class);
    
    /**
     * Now write some java code to generate an .eba that we can execute 
      
        File hack = new File(
     
        "c:/svn/trunk/application/application-management/target/ariesApplicationManagerImplTest/stored.eba");
    IDirectory ihack = FileSystem.getFSRoot(hack);
    
    if (ihack == null) { 
      System.out.println ("** ihack is null");
    } else { 
      System.out.println ("** ihack is valid");
    }
    manager.createApplication(ihack); */
  }

  @org.ops4j.pax.exam.junit.Configuration
  public static Option[] configuration() {
    Option[] options = options(
        // Log
        mavenBundle("org.ops4j.pax.logging", "pax-logging-api"),
        mavenBundle("org.ops4j.pax.logging", "pax-logging-service"),
        // Felix Config Admin
        mavenBundle("org.apache.felix", "org.apache.felix.configadmin"),
        // Felix mvn url handler
        mavenBundle("org.ops4j.pax.url", "pax-url-mvn"),

        // this is how you set the default log level when using pax
        // logging (logProfile)
        systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("DEBUG"),

        // Bundles
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.api"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.utils"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.management"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime"),
        mavenBundle("org.apache.aries.application", "org.apache.aries.application.runtime"),
        mavenBundle("org.apache.aries", "org.apache.aries.util"),
        mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint"), 
        mavenBundle("org.osgi", "org.osgi.compendium"),

        equinox().version("3.5.0"));
    options = updateOptions(options);
    return options;
  }
}
