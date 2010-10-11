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
package org.apache.aries.subsystem.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.util.concurrent.Executor;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.aries.subsystem.executor.SimpleExecutor;

import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.container.def.PaxRunnerOptions;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;


@RunWith(JUnit4TestRunner.class)
public class SimpleExecutorTest extends AbstractIntegrationTest {

    
    @Test
    public void test() throws Exception {        
        // obtain executor service
        Executor executor = getOsgiService(Executor.class);
        assertNotNull("Executor should not be null", executor);
        System.out.println("Able to get Executor service");
        
        // check the service is of the right implementation type
        assertEquals(SimpleExecutor.class.getName(), executor.getClass().getName());
        System.out.println("The Executor service is of type: " + SimpleExecutor.class.getName());

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


            // this is how you set the default log level when using pax logging (logProfile)
            systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("DEBUG"),

            // Bundles
            mavenBundle("org.apache.aries.testsupport", "org.apache.aries.testsupport.unit"),
            mavenBundle("org.apache.aries.subsystem", "org.apache.aries.subsystem.executor"),

            //org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption("-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),

            PaxRunnerOptions.rawPaxRunnerOption("config", "classpath:ss-runner.properties"),

            equinox().version("v43prototype-3.6.0.201003231329")
        );
        options = updateOptions(options);
        return options;
    }

}
