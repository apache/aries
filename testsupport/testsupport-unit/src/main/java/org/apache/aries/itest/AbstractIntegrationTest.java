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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.itest;

import org.ops4j.pax.exam.Option;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import javax.inject.Inject;

import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.CoreOptions.when;

/**
 * Base class for Pax Exam based unit tests
 * <p>
 * Contains the injection point and various utilities used in most tests
 */
public abstract class AbstractIntegrationTest {

    /**
     * Gateway to the test OSGi framework
     */
    @Inject
    protected BundleContext bundleContext;

    /**
     * Get a richer version of {@link BundleContext}
     */
    public RichBundleContext context() {
        return new RichBundleContext(bundleContext);
    }

    public String getLocalRepo() {
        String localRepo = System.getProperty("maven.repo.local");
        if (localRepo == null) {
            localRepo = System.getProperty("org.ops4j.pax.url.mvn.localRepository");
        }
        return localRepo;
    }


    /**
     * Help to diagnose bundles that did not start
     *
     * @throws BundleException
     */
    public void showBundles() throws BundleException {
        Bundle[] bundles = bundleContext.getBundles();
        for (Bundle bundle : bundles) {
            System.out.println(bundle.getBundleId() + ":" + bundle.getSymbolicName() + ":" + bundle.getVersion() + ":" + bundle.getState());
        }
    }

    /**
     * Helps to diagnose bundles that are not resolved as it will throw a detailed exception
     *
     * @throws BundleException
     */
    public void resolveBundles() throws BundleException {
        System.out.println("Checking for bundles");
        Bundle[] bundles = bundleContext.getBundles();
        for (Bundle bundle : bundles) {
            if (bundle.getState() == Bundle.INSTALLED) {
                System.out.println("Found non resolved bundle " + bundle.getBundleId() + ":" + bundle.getSymbolicName() + ":" + bundle.getVersion());
                bundle.start();
            }
        }
    }

    protected static Option setPaxExamLogLevel(String level) {
        return systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value(level);
    }

    protected static Option addPaxLoggingBundles() {
        return composite(
                mavenBundle("org.ops4j.pax.logging", "pax-logging-api").versionAsInProject(),
                mavenBundle("org.ops4j.pax.logging", "pax-logging-service").versionAsInProject()
        );
    }

    protected static Option addPaxLoggingV2Bundles() {
        return composite(
                mavenBundle("org.ops4j.pax.logging", "pax-logging-api").versionAsInProject(),
                mavenBundle("org.ops4j.pax.logging", "pax-logging-log4j2").versionAsInProject()
        );
    }

    protected static Option addAsmBundles() {
        return composite(
                mavenBundle("org.ow2.asm", "asm").versionAsInProject(),
                mavenBundle("org.ow2.asm", "asm-commons").versionAsInProject(),
                mavenBundle("org.ow2.asm", "asm-tree").versionAsInProject(),
                mavenBundle("org.ow2.asm", "asm-analysis").versionAsInProject()
        );
    }

    protected static Option setupRemoteDebugging() {
        String remoteDebuggingEnabled = System.getProperty("aries.remote.debugging.enabled");
        String remoteDebuggingPort = System.getProperty("aries.remote.debugging.port", "5006");
        return when("TRUE".equalsIgnoreCase(remoteDebuggingEnabled))
                .useOptions(
                        composite(
                                vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=" + remoteDebuggingPort),
                                systemTimeout(0)
                        )
                );
    }

    protected Option configurePaxUrlLocalMavenRepoIfNeeded() {
        String localRepo = getLocalRepo();
        return when(localRepo != null)
                .useOptions(vmOption("-Dorg.ops4j.pax.url.mvn.localRepository=" + localRepo));
    }
}
