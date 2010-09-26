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
package org.apache.aries.blueprint.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Properties;

import org.apache.aries.blueprint.sample.Account;
import org.apache.aries.blueprint.sample.AccountFactory;
import org.apache.aries.blueprint.sample.Bar;
import org.apache.aries.blueprint.sample.Foo;
import org.junit.After;
import org.junit.Before;
import org.ops4j.pax.exam.CoreOptions;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import static org.ops4j.pax.exam.OptionUtils.combine;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.util.tracker.ServiceTracker;

public abstract class AbstractIntegrationTest {

    public static final long DEFAULT_TIMEOUT = 30000;

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
    
    @Inject
    protected BundleContext bundleContext;

    protected BlueprintContainer getBlueprintContainerForBundle(String symbolicName) throws Exception {
        return getBlueprintContainerForBundle(symbolicName, DEFAULT_TIMEOUT);
    }

    protected BlueprintContainer getBlueprintContainerForBundle(String symbolicName, long timeout) throws Exception {
        return getOsgiService(BlueprintContainer.class, "(osgi.blueprint.container.symbolicname=" + symbolicName + ")", timeout);
    }
    
    protected BlueprintContainer getBlueprintContainerForBundle(BundleContext bc, String symbolicName, long timeout) throws Exception {
        return getOsgiService(bc, BlueprintContainer.class, "(osgi.blueprint.container.symbolicname=" + symbolicName + ")", timeout);
    }

    protected <T> T getOsgiService(Class<T> type, long timeout) {
        return getOsgiService(type, null, timeout);
    }

    protected <T> T getOsgiService(Class<T> type) {
        return getOsgiService(type, null, DEFAULT_TIMEOUT);
    }
    
    protected <T> T getOsgiService(BundleContext bc, Class<T> type, String filter, long timeout) {
        ServiceTracker tracker = null;
        try {
            String flt;
            if (filter != null) {
                if (filter.startsWith("(")) {
                    flt = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")" + filter + ")";
                } else {
                    flt = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")(" + filter + "))";
                }
            } else {
                flt = "(" + Constants.OBJECTCLASS + "=" + type.getName() + ")";
            }
            Filter osgiFilter = FrameworkUtil.createFilter(flt);
            tracker = new ServiceTracker(bc == null ? bundleContext : bc, osgiFilter, null);
            tracker.open();
            
            // add tracker to the list of trackers we close at tear down
            srs.add(tracker);
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
    
    protected <T> T getOsgiService(Class<T> type, String filter, long timeout) {
        return getOsgiService(null, type, filter, timeout);
    }

    protected Bundle installBundle(String groupId, String artifactId) throws Exception {
        MavenArtifactProvisionOption mvnUrl = mavenBundle(groupId, artifactId);
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
            URL fileURL = AbstractIntegrationTest.class.getClassLoader().getResource( filePath );
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
    
    protected void testBlueprintContainer(Bundle bundle) throws Exception {
        testBlueprintContainer(bundleContext, bundle);
    }
    
    
    protected void testBlueprintContainer(BundleContext bc, Bundle bundle) throws Exception {
        BlueprintContainer blueprintContainer = getBlueprintContainerForBundle(
                bc == null ? bundleContext : bc, "org.apache.aries.blueprint.sample",
                5000);
        assertNotNull(blueprintContainer);

        Object obj = blueprintContainer.getComponentInstance("bar");
        assertNotNull(obj);
        assertEquals(Bar.class, obj.getClass());
        Bar bar = (Bar) obj;
        assertNotNull(bar.getContext());
        assertEquals("Hello FooBar", bar.getValue());
        assertNotNull(bar.getList());
        assertEquals(2, bar.getList().size());
        assertEquals("a list element", bar.getList().get(0));
        assertEquals(Integer.valueOf(5), bar.getList().get(1));
        obj = blueprintContainer.getComponentInstance("foo");
        assertNotNull(obj);
        assertEquals(Foo.class, obj.getClass());
        Foo foo = (Foo) obj;
        assertEquals(5, foo.getA());
        assertEquals(10, foo.getB());
        assertSame(bar, foo.getBar());
        assertEquals(Currency.getInstance("PLN"), foo.getCurrency());
        assertEquals(new SimpleDateFormat("yyyy.MM.dd").parse("2009.04.17"),
                foo.getDate());

        assertTrue(foo.isInitialized());
        assertFalse(foo.isDestroyed());

        obj = getOsgiService(bc == null ? bundleContext : bc, Foo.class, null, 5000);
        assertNotNull(obj);
        assertEquals(obj, foo);
        
        obj = blueprintContainer.getComponentInstance("accountOne");
        assertNotNull(obj);
        Account account = (Account)obj;
        assertEquals(1, account.getAccountNumber());
     
        obj = blueprintContainer.getComponentInstance("accountTwo");
        assertNotNull(obj);
        account = (Account)obj;
        assertEquals(2, account.getAccountNumber());
        
        obj = blueprintContainer.getComponentInstance("accountThree");
        assertNotNull(obj);
        account = (Account)obj;
        assertEquals(3, account.getAccountNumber());
        
        obj = blueprintContainer.getComponentInstance("accountFactory");
        assertNotNull(obj);
        AccountFactory accountFactory = (AccountFactory)obj;
        assertEquals("account factory", accountFactory.getFactoryName());
        
        bundle.stop();

        Thread.sleep(1000);

        try {
            blueprintContainer = getBlueprintContainerForBundle(bc == null ? bundleContext : bc, 
                    "org.apache.aries.blueprint.sample", 1);
            fail("BlueprintContainer should have been unregistered");
        } catch (Exception e) {
            // Expected, as the module container should have been unregistered
        }

        assertTrue(foo.isInitialized());
        assertTrue(foo.isDestroyed());
    }

}
