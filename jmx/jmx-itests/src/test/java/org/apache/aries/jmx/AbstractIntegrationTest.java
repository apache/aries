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
package org.apache.aries.jmx;

import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.streamBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;
import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

import javax.inject.Inject;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;

import org.apache.aries.jmx.test.MbeanServerActivator;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * @version $Rev$ $Date$
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public abstract class AbstractIntegrationTest extends org.apache.aries.itest.AbstractIntegrationTest {
    protected ServiceReference reference;
    
    @Inject
    protected MBeanServer mbeanServer;

	public Option baseOptions() {
        String localRepo = System.getProperty("maven.repo.local");
        if (localRepo == null) {
            localRepo = System.getProperty("org.ops4j.pax.url.mvn.localRepository");
        }
        return composite(
                junitBundles(),
                // this is how you set the default log level when using pax
                // logging (logProfile)
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),
                when(localRepo != null).useOptions(vmOption("-Dorg.ops4j.pax.url.mvn.localRepository=" + localRepo))
         );
    }
    
	protected Option jmxRuntime() {
		return composite(
				baseOptions(),
				mavenBundle("org.osgi", "org.osgi.compendium").versionAsInProject(),
				mavenBundle("org.apache.aries", "org.apache.aries.util").versionAsInProject(),
				mavenBundle("org.apache.felix", "org.apache.felix.configadmin").versionAsInProject(),
				jmxBundle(),
				mavenBundle("org.apache.aries.jmx", "org.apache.aries.jmx.api").versionAsInProject(),
				mavenBundle("org.apache.aries.jmx", "org.apache.aries.jmx.whiteboard").versionAsInProject(),
				mavenBundle("org.apache.aries.testsupport", "org.apache.aries.testsupport.unit").versionAsInProject(),
				mbeanServerBundle()
				);
	}
    
	/**
	 * jmx bundle to provision. Override this with jmxWhiteBoardBundle for the whiteboard tests
	 * @return
	 */
	protected MavenArtifactProvisionOption jmxBundle() {
		return mavenBundle("org.apache.aries.jmx", "org.apache.aries.jmx").version("1.1.2-SNAPSHOT");
	}
	
	protected MavenArtifactProvisionOption jmxWhiteBoardBundle() {
		return mavenBundle("org.apache.aries.jmx", "org.apache.aries.jmx.core.whiteboard").version("1.1.2-SNAPSHOT");
	}
	
	protected Option mbeanServerBundle() {
		return provision(bundle()
		        .add(MbeanServerActivator.class)
		        .set(Constants.BUNDLE_ACTIVATOR, MbeanServerActivator.class.getName())
		        .build(withBnd()));
	}
	
	protected Option bundlea() {
		return provision(bundle()
		        .add(org.apache.aries.jmx.test.bundlea.Activator.class)
		        .add(org.apache.aries.jmx.test.bundlea.api.InterfaceA.class)
		        .add(org.apache.aries.jmx.test.bundlea.impl.A.class)
		        .set(Constants.BUNDLE_SYMBOLICNAME, "org.apache.aries.jmx.test.bundlea")
		        .set(Constants.BUNDLE_VERSION, "2.0.0")
		        .set(Constants.EXPORT_PACKAGE, "org.apache.aries.jmx.test.bundlea.api;version=2.0.0")
		        .set(Constants.IMPORT_PACKAGE,
		                "org.osgi.framework;version=1.5.0,org.osgi.util.tracker,org.apache.aries.jmx.test.bundleb.api;version=1.1.0;resolution:=optional" +
		                ",org.osgi.service.cm")
		        .set(Constants.BUNDLE_ACTIVATOR,
		                org.apache.aries.jmx.test.bundlea.Activator.class.getName())
		        .build(withBnd()));
	}
    
	protected Option bundleb() {
		return provision(bundle()
		        .add(org.apache.aries.jmx.test.bundleb.Activator.class)
		        .add(org.apache.aries.jmx.test.bundleb.api.InterfaceB.class)
		        .add(org.apache.aries.jmx.test.bundleb.api.MSF.class)
		        .add(org.apache.aries.jmx.test.bundleb.impl.B.class)
		        .set(Constants.BUNDLE_SYMBOLICNAME,"org.apache.aries.jmx.test.bundleb")
		        .set(Constants.BUNDLE_VERSION, "1.0.0")
		        .set(Constants.EXPORT_PACKAGE,"org.apache.aries.jmx.test.bundleb.api;version=1.1.0")
		        .set(Constants.IMPORT_PACKAGE,"org.osgi.framework;version=1.5.0,org.osgi.util.tracker," +
		        		"org.osgi.service.cm,org.apache.aries.jmx.test.fragmentc")
		        .set(Constants.BUNDLE_ACTIVATOR,
		                org.apache.aries.jmx.test.bundleb.Activator.class.getName())
		        .build(withBnd()));
	}
    
	protected Option fragmentc() {
		return streamBundle(bundle()
		        .add(org.apache.aries.jmx.test.fragmentc.C.class)
		        .set(Constants.BUNDLE_SYMBOLICNAME, "org.apache.aries.jmx.test.fragc")
		        .set(Constants.FRAGMENT_HOST, "org.apache.aries.jmx.test.bundlea")
		        .set(Constants.EXPORT_PACKAGE, "org.apache.aries.jmx.test.fragmentc")
		        .build(withBnd())).noStart();
	}
	
	protected Option bundled() {
		return provision(bundle()
		        .set(Constants.BUNDLE_SYMBOLICNAME, "org.apache.aries.jmx.test.bundled")
		        .set(Constants.BUNDLE_VERSION, "3.0.0")
		        .set(Constants.REQUIRE_BUNDLE, "org.apache.aries.jmx.test.bundlea;bundle-version=2.0.0")
		        .build(withBnd()));
	}
	
	protected Option bundlee() {
		return provision(bundle()
		        .set(Constants.BUNDLE_SYMBOLICNAME, "org.apache.aries.jmx.test.bundlee")
		        .set(Constants.BUNDLE_DESCRIPTION, "%desc")
		        .add("OSGI-INF/l10n/bundle.properties", getBundleProps("desc", "Description"))
		        .add("OSGI-INF/l10n/bundle_nl.properties", getBundleProps("desc", "Omschrijving"))
		        .build(withBnd()));
	}

    private InputStream getBundleProps(String key, String value) {
        try {
            Properties p = new Properties();
            p.put(key, value);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            p.store(baos, "");
            return new ByteArrayInputStream(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
	
    protected ObjectName waitForMBean(String name) {
        return waitForMBean(name, 10);
    }

    protected ObjectName waitForMBean(String name, int timeoutInSeconds) {
        int i=0;
        while (true) {
            ObjectName queryName;
			try {
				queryName = new ObjectName(name.toString() + ",*");
			} catch (Exception e) {
				throw new IllegalArgumentException("Invalid name " + name, e);
			}
            Set<ObjectName> result = mbeanServer.queryNames(queryName, null);
            if (result.size() > 0)
                return result.iterator().next();

            if (i == timeoutInSeconds * 10)
                throw new RuntimeException(name + " mbean is not available after waiting " + timeoutInSeconds + " seconds");

            i++;
            try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
        }
    }

    protected <T> T getMBean(String name, Class<T> type) {
        ObjectName objectName = waitForMBean(name);
        return getMBean(objectName, type);
    }

    protected <T> T getMBean(ObjectName objectName, Class<T> type) {
        return MBeanServerInvocationHandler.newProxyInstance(mbeanServer, objectName, type, false);
    }
    
    protected Bundle getBundleByName(String symName) {
    	Bundle b = context().getBundleByName(symName);
        assertNotNull("Bundle " + symName + "should be installed", b);
        return b;
    }
}