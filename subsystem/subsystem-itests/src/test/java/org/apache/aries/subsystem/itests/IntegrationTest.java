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

import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

public abstract class IntegrationTest {
	public static final long DEFAULT_TIMEOUT = 10000;
	
	protected static boolean createdApplications = false;

    private Map<String, ServiceTracker> srs;

    @Before
    public void setUp() throws Exception {
        srs = new HashMap<String, ServiceTracker>();
    }
    
    @After
    public void tearDown() throws Exception {
        for (ServiceTracker st : srs.values()) {
            if (st != null) {
                st.close();
            }  
        }
    }
    
    @Inject
    protected BundleContext bundleContext;

    protected <T> T getOsgiService(Class<T> type, long timeout) {
        return getOsgiService(type, null, timeout);
    }

    protected <T> T getOsgiService(Class<T> type) {
        return getOsgiService(type, null, DEFAULT_TIMEOUT);
    }
    
    protected <T> T getOsgiService(BundleContext bc, Class<T> type, String filter, long timeout) {
        if (filter != null) {
            if (filter.startsWith("(")) {
                filter = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")" + filter + ")";
            } else {
                filter = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")(" + filter + "))";
            }
        } else {
            filter = "(" + Constants.OBJECTCLASS + "=" + type.getName() + ")";
        }
        ServiceTracker tracker = srs.get(filter);
        if (tracker == null) {
        	try {
        		Filter osgiFilter = FrameworkUtil.createFilter(filter);
                tracker = new ServiceTracker(bc == null ? bundleContext : bc, osgiFilter, null);
                tracker.open();
                // add tracker to the list of trackers we close at tear down
                srs.put(filter, tracker);
        	} 
        	catch (InvalidSyntaxException e) {
                throw new IllegalArgumentException("Invalid filter", e);
            }
        }
        try {
            Object svc = tracker.waitForService(timeout);
            if (svc == null) {
                throw new RuntimeException("Gave up waiting for service " + filter);
            }
            return type.cast(svc);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
    protected <T> T getOsgiService(Class<T> type, String filter, long timeout) {
        return getOsgiService(null, type, filter, timeout);
    }

    protected Bundle installBundle(String groupId, String artifactId) throws Exception {
        MavenArtifactProvisionOption mvnUrl = mavenBundleInTest(groupId, artifactId);
        return bundleContext.installBundle(mvnUrl.getURL());
    }
    
    protected Bundle installBundle(String groupId, String artifactId, String version) throws Exception {
    	MavenArtifactProvisionOption mvnUrl = CoreOptions.mavenBundle().groupId(groupId).artifactId(artifactId).version(version);
        return bundleContext.installBundle(mvnUrl.getURL());
    }

    protected Bundle getInstalledBundle(String symbolicName) {
        for (Bundle b : bundleContext.getBundles()) {
            if (b.getSymbolicName().equals(symbolicName)) {
                return b;
            }
        }
        return null;
    }

    public static MavenArtifactProvisionOption mavenBundle(String groupId, String artifactId) {
        return CoreOptions.mavenBundle().groupId(groupId).artifactId(artifactId).versionAsInProject();
    }
    
    public static MavenArtifactProvisionOption mavenBundleInTest(String groupId, String artifactId) {
        return CoreOptions.mavenBundle().groupId(groupId).artifactId(artifactId).version(getArtifactVersion(groupId, artifactId));
    }

    //TODO getArtifactVersion and getFileFromClasspath are borrowed and modified from pax-exam.  They should be moved back ASAP.
    public static String getArtifactVersion( final String groupId,
                                             final String artifactId )
    {
        final Properties dependencies = new Properties();
        try
        {
            InputStream in = getFileFromClasspath("META-INF/maven/dependencies.properties");
            try {
                dependencies.load(in);
            } finally {
                in.close();
            }
            final String version = dependencies.getProperty( groupId + "/" + artifactId + "/version" );
            if( version == null )
            {
                throw new RuntimeException(
                    "Could not resolve version. Do you have a dependency for " + groupId + "/" + artifactId
                    + " in your maven project?"
                );
            }
            return version;
        }
        catch( IOException e )
        {
            // TODO throw a better exception
            throw new RuntimeException(
                "Could not resolve version. Did you configured the plugin in your maven project?"
                + "Or maybe you did not run the maven build and you are using an IDE?"
            );
        }
    }

    private static InputStream getFileFromClasspath( final String filePath )
        throws FileNotFoundException
    {
        try
        {
            URL fileURL = IntegrationTest.class.getClassLoader().getResource( filePath );
            if( fileURL == null )
            {
                throw new FileNotFoundException( "File [" + filePath + "] could not be found in classpath" );
            }
            return fileURL.openStream();
        }
        catch (IOException e)
        {
            throw new FileNotFoundException( "File [" + filePath + "] could not be found: " + e.getMessage() );
        }
    }


    protected static Option[] updateOptions(Option[] options) {
        // We need to add pax-exam-junit here when running with the ibm
        // jdk to avoid the following exception during the test run:
        // ClassNotFoundException: org.ops4j.pax.exam.junit.Configuration
        if ("IBM Corporation".equals(System.getProperty("java.vendor"))) {
            Option[] ibmOptions = options(
                wrappedBundle(mavenBundle("org.ops4j.pax.exam", "pax-exam-junit"))
            );
            options = combine(ibmOptions, options);
        }

        return options;
    }
    
    protected Bundle findBundleBySymbolicName(String symbolicName) {
    	Bundle result = getInstalledBundle(symbolicName);
		assertNotNull("Unable to find bundle with symbolic name: " + symbolicName, result);
		return result;
    }
}
