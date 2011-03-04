/**
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
package org.apache.aries.spifly;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.apache.aries.spifly.api.SpiFlyConstants;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Version;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleWiring;

public class ClientWeavingHookTest {
    @Before
    public void setUp() {
        Activator.activator = new Activator();
    }
    
    @After
    public void tearDown() {
        Activator.activator = null;
    }
        
    @Test
    public void testClientWeavingHookBasicServiveLoaderUsage() throws Exception {
        Dictionary<String, String> consumerHeaders = new Hashtable<String, String>();
        consumerHeaders.put(SpiFlyConstants.SPI_CONSUMER_HEADER, "true");

        // Register the bundle that provides the SPI implementation.
        Bundle providerBundle = mockProviderBundle("impl1", 1);        
        Activator.activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle);

        Bundle consumerBundle = mockConsumerBundle(consumerHeaders, providerBundle);
        Bundle spiFlyBundle = mockSpiFlyBundle("spifly", Version.parseVersion("1.9.4"), consumerBundle, providerBundle);                      

        WeavingHook wh = new ClientWeavingHook(spiFlyBundle.getBundleContext());
        
        // Weave the TestClient class.
        URL clsUrl = getClass().getResource("TestClient.class");
        Assert.assertNotNull("Precondition", clsUrl);
        WovenClass wc = new MyWovenClass(clsUrl, "org.apache.aries.spifly.TestClient", consumerBundle);
        Assert.assertEquals("Precondition", 0, wc.getDynamicImports().size());
        wh.weave(wc);
        Assert.assertEquals(1, wc.getDynamicImports().size());
        String di1 = "org.apache.aries.spifly;bundle-symbolic-name=spifly;bundle-version=1.9.4";
        String di2 = "org.apache.aries.spifly;bundle-version=1.9.4;bundle-symbolic-name=spifly";
        String di = wc.getDynamicImports().get(0);
        Assert.assertTrue("Weaving should have added a dynamic import", di1.equals(di) || di2.equals(di));        
                        
        // Invoke the woven class and check that it propertly sets the TCCL so that the 
        // META-INF/services/org.apache.aries.mytest.MySPI file from impl1 is visible.
        Class<?> cls = wc.getDefinedClass();
        Method method = cls.getMethod("test", new Class [] {String.class});
        Object result = method.invoke(cls.newInstance(), "hello");
        Assert.assertEquals("olleh", result);
    }

    @Test
    public void testClientWeavingHookAltServiceLoaderLoadUnprocessed() throws Exception {
        Bundle spiFlyBundle = mockSpiFlyBundle();               
       
        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(SpiFlyConstants.SPI_CONSUMER_HEADER, "true");
        Bundle consumerBundle = mockConsumerBundle(headers, spiFlyBundle);

        WeavingHook wh = new ClientWeavingHook(spiFlyBundle.getBundleContext());
        
        // Weave the TestClient class.
        URL clsUrl = getClass().getResource("UnaffectedTestClient.class");
        Assert.assertNotNull("Precondition", clsUrl);
        WovenClass wc = new MyWovenClass(clsUrl, "org.apache.aries.spifly.UnaffectedTestClient", consumerBundle);
        Assert.assertEquals("Precondition", 0, wc.getDynamicImports().size());
        wh.weave(wc);

        Assert.assertEquals("The client is not affected so no additional imports should have been added", 
            0, wc.getDynamicImports().size());
                
        // ok the weaving is done, now prepare the registry for the call
        Bundle providerBundle = mockProviderBundle("impl1", 1);        
        Activator.activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle);
        
        // Invoke the woven class and check that it propertly sets the TCCL so that the 
        // META-INF/services/org.apache.aries.mytest.MySPI file from impl1 is visible.
        Class<?> cls = wc.getDefinedClass();
        Method method = cls.getMethod("test", new Class [] {String.class});
        Object result = method.invoke(cls.newInstance(), "hello");
        Assert.assertEquals("impl4", result);
    }

    @Test
    public void testClientWeavingHookMultipleProviders() throws Exception {
        Bundle spiFlyBundle = mockSpiFlyBundle();

        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(SpiFlyConstants.SPI_CONSUMER_HEADER, "true");
        Bundle consumerBundle = mockConsumerBundle(headers, spiFlyBundle);

        WeavingHook wh = new ClientWeavingHook(spiFlyBundle.getBundleContext());

        // Weave the TestClient class.
        URL clsUrl = getClass().getResource("TestClient.class");
        WovenClass wc = new MyWovenClass(clsUrl, "org.apache.aries.spifly.TestClient", consumerBundle);
        wh.weave(wc);

        Bundle providerBundle1 = mockProviderBundle("impl1", 1);
        Bundle providerBundle2 = mockProviderBundle("impl2", 2);
        
        // Register in reverse order to make sure the order in which bundles are sorted is correct
        Activator.activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle2);
        Activator.activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle1);

        // Invoke the woven class and check that it propertly sets the TCCL so that the 
        // META-INF/services/org.apache.aries.mytest.MySPI files from impl1 and impl2 are visible.
        Class<?> cls = wc.getDefinedClass();
        Method method = cls.getMethod("test", new Class [] {String.class});
        Object result = method.invoke(cls.newInstance(), "hello");
        Assert.assertEquals("All three services should be invoked in the correct order", "ollehHELLO5", result);        
    }
    
    @Test
    public void testClientSpecifyingProvider() throws Exception {
        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(SpiFlyConstants.SPI_CONSUMER_HEADER, "java.util.ServiceLoader#load(java.lang.Class);bundle=impl2");

        Bundle providerBundle1 = mockProviderBundle("impl1", 1);
        Bundle providerBundle2 = mockProviderBundle("impl2", 2);
        Activator.activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle1);
        Activator.activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle2);

        Bundle consumerBundle = mockConsumerBundle(headers, providerBundle1, providerBundle2);
        Bundle spiFlyBundle = mockSpiFlyBundle(consumerBundle, providerBundle1, providerBundle2);        
        WeavingHook wh = new ClientWeavingHook(spiFlyBundle.getBundleContext());

        // Weave the TestClient class.
        URL clsUrl = getClass().getResource("TestClient.class");
        WovenClass wc = new MyWovenClass(clsUrl, "org.apache.aries.spifly.TestClient", consumerBundle);
        wh.weave(wc);

        // Invoke the woven class and check that it propertly sets the TCCL so that the 
        // META-INF/services/org.apache.aries.mytest.MySPI file from impl2 is visible.
        Class<?> cls = wc.getDefinedClass();
        Method method = cls.getMethod("test", new Class [] {String.class});
        Object result = method.invoke(cls.newInstance(), "hello");
        Assert.assertEquals("Only the services from bundle impl2 should be selected", "HELLO5", result);        
    }
    
    @Test
    public void testClientSpecifyingProviderVersion() throws Exception {
        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(SpiFlyConstants.SPI_CONSUMER_HEADER, "java.util.ServiceLoader#load(java.lang.Class);bundle=impl2:version=1.2.3");

        Bundle providerBundle1 = mockProviderBundle("impl1", 1);
        Bundle providerBundle2 = mockProviderBundle("impl2", 2);
        Bundle providerBundle3 = mockProviderBundle("impl2_123", 3, new Version(1, 2, 3));
        Activator.activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle1);
        Activator.activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle2);
        Activator.activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle3);

        Bundle consumerBundle = mockConsumerBundle(headers, providerBundle1, providerBundle2, providerBundle3);
        Bundle spiFlyBundle = mockSpiFlyBundle(consumerBundle, providerBundle1, providerBundle2, providerBundle3);        
        WeavingHook wh = new ClientWeavingHook(spiFlyBundle.getBundleContext());

        // Weave the TestClient class.
        URL clsUrl = getClass().getResource("TestClient.class");
        WovenClass wc = new MyWovenClass(clsUrl, "org.apache.aries.spifly.TestClient", consumerBundle);
        wh.weave(wc);

        // Invoke the woven class and check that it propertly sets the TCCL so that the 
        // META-INF/services/org.apache.aries.mytest.MySPI file from impl2 is visible.
        Class<?> cls = wc.getDefinedClass();
        Method method = cls.getMethod("test", new Class [] {String.class});
        Object result = method.invoke(cls.newInstance(), "hello");
        Assert.assertEquals("Only the services from bundle impl2 should be selected", "Updated!hello!Updated", result);        
    }

    @Test
    public void testClientMultipleTargetBundles() throws Exception {
        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(SpiFlyConstants.SPI_CONSUMER_HEADER, 
                "java.util.ServiceLoader#load(java.lang.Class);bundle=impl1|impl4");

        Bundle providerBundle1 = mockProviderBundle("impl1", 1);
        Bundle providerBundle2 = mockProviderBundle("impl2", 2);
        Bundle providerBundle4 = mockProviderBundle("impl4", 4);
        Activator.activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle1);
        Activator.activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle2);
        Activator.activator.registerProviderBundle("org.apache.aries.mytest.AltSPI", providerBundle2);
        Activator.activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle4);        
        Activator.activator.registerProviderBundle("org.apache.aries.mytest.AltSPI", providerBundle4);        

        Bundle consumerBundle = mockConsumerBundle(headers, providerBundle1, providerBundle2, providerBundle4);
        Bundle spiFlyBundle = mockSpiFlyBundle(consumerBundle, providerBundle1, providerBundle2, providerBundle4);        
        WeavingHook wh = new ClientWeavingHook(spiFlyBundle.getBundleContext());

        // Weave the TestClient class.
        URL clsUrl = getClass().getResource("TestClient.class");
        WovenClass wc = new MyWovenClass(clsUrl, "org.apache.aries.spifly.TestClient", consumerBundle);
        wh.weave(wc);

        // Invoke the woven class and check that it propertly sets the TCCL so that the 
        // META-INF/services/org.apache.aries.mytest.MySPI file from impl2 is visible.
        Class<?> cls = wc.getDefinedClass();
        Method method = cls.getMethod("test", new Class [] {String.class});
        Object result = method.invoke(cls.newInstance(), "hello");
        Assert.assertEquals("All providers should be selected for this one", "ollehimpl4", result);        
    }
    
    @Test
    public void testClientMultipleTargetBundles2() throws Exception {
        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(SpiFlyConstants.SPI_CONSUMER_HEADER, 
                "java.util.ServiceLoader#load(java.lang.Class);bundleId=1|4");

        Bundle providerBundle1 = mockProviderBundle("impl1", 1);
        Bundle providerBundle2 = mockProviderBundle("impl2", 2);
        Bundle providerBundle4 = mockProviderBundle("impl4", 4);
        Activator.activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle1);
        Activator.activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle2);
        Activator.activator.registerProviderBundle("org.apache.aries.mytest.AltSPI", providerBundle2);
        Activator.activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle4);        
        Activator.activator.registerProviderBundle("org.apache.aries.mytest.AltSPI", providerBundle4);        

        Bundle consumerBundle = mockConsumerBundle(headers, providerBundle1, providerBundle2, providerBundle4);
        Bundle spiFlyBundle = mockSpiFlyBundle(consumerBundle, providerBundle1, providerBundle2, providerBundle4);        
        WeavingHook wh = new ClientWeavingHook(spiFlyBundle.getBundleContext());

        // Weave the TestClient class.
        URL clsUrl = getClass().getResource("TestClient.class");
        WovenClass wc = new MyWovenClass(clsUrl, "org.apache.aries.spifly.TestClient", consumerBundle);
        wh.weave(wc);

        // Invoke the woven class and check that it propertly sets the TCCL so that the 
        // META-INF/services/org.apache.aries.mytest.MySPI file from impl2 is visible.
        Class<?> cls = wc.getDefinedClass();
        Method method = cls.getMethod("test", new Class [] {String.class});
        Object result = method.invoke(cls.newInstance(), "hello");
        Assert.assertEquals("All providers should be selected for this one", "ollehimpl4", result);        
    }

    @Test
    public void testClientSpecificProviderLoadArgument() throws Exception {
        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(SpiFlyConstants.SPI_CONSUMER_HEADER, 
                "java.util.ServiceLoader#load(java.lang.Class[org.apache.aries.mytest.MySPI])," +
                "java.util.ServiceLoader#load(java.lang.Class[org.apache.aries.mytest.AltSPI]);bundle=impl4");

        Bundle providerBundle1 = mockProviderBundle("impl1", 1);
        Bundle providerBundle2 = mockProviderBundle("impl2", 2);
        Bundle providerBundle4 = mockProviderBundle("impl4", 4);
        Activator.activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle1);
        Activator.activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle2);
        Activator.activator.registerProviderBundle("org.apache.aries.mytest.AltSPI", providerBundle2);
        Activator.activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle4);        
        Activator.activator.registerProviderBundle("org.apache.aries.mytest.AltSPI", providerBundle4);        

        Bundle consumerBundle = mockConsumerBundle(headers, providerBundle1, providerBundle2, providerBundle4);
        Bundle spiFlyBundle = mockSpiFlyBundle(consumerBundle, providerBundle1, providerBundle2, providerBundle4);        
        WeavingHook wh = new ClientWeavingHook(spiFlyBundle.getBundleContext());

        // Weave the TestClient class.
        URL clsUrl = getClass().getResource("TestClient.class");
        WovenClass wc = new MyWovenClass(clsUrl, "org.apache.aries.spifly.TestClient", consumerBundle);
        wh.weave(wc);

        // Invoke the woven class and check that it propertly sets the TCCL so that the 
        // META-INF/services/org.apache.aries.mytest.MySPI file from impl2 is visible.
        Class<?> cls = wc.getDefinedClass();
        Method method = cls.getMethod("test", new Class [] {String.class});
        Object result = method.invoke(cls.newInstance(), "hello");
        Assert.assertEquals("All providers should be selected for this one", "ollehHELLO5impl4", result);        

        // Weave the AltTestClient class.
        URL cls2Url = getClass().getResource("AltTestClient.class");
        WovenClass wc2 = new MyWovenClass(cls2Url, "org.apache.aries.spifly.AltTestClient", consumerBundle);
        wh.weave(wc2);

        // Invoke the AltTestClient
        Class<?> cls2 = wc2.getDefinedClass();
        Method method2 = cls2.getMethod("test", new Class [] {long.class});
        Object result2 = method2.invoke(cls2.newInstance(), 4096);
        Assert.assertEquals("Only the services from bundle impl4 should be selected", -4096L*4096L, result2);        
    }
    
    @Test
    public void testClientSpecifyingDifferentMethodsLimitedToDifferentProviders() throws Exception {
        Dictionary<String, String> headers1 = new Hashtable<String, String>();
        headers1.put(SpiFlyConstants.SPI_CONSUMER_HEADER, 
                "javax.xml.parsers.DocumentBuilderFactory#newInstance();bundle=impl3," +
                "java.util.ServiceLoader#load(java.lang.Class[org.apache.aries.mytest.MySPI]);bundle=impl4");

        Dictionary<String, String> headers2 = new Hashtable<String, String>();
        headers2.put(SpiFlyConstants.SPI_CONSUMER_HEADER, 
                "javax.xml.parsers.DocumentBuilderFactory#newInstance();bundle=system.bundle," +
                "java.util.ServiceLoader#load;bundle=impl1");

        Dictionary<String, String> headers3 = new Hashtable<String, String>();
        headers3.put(SpiFlyConstants.SPI_CONSUMER_HEADER, 
                "org.acme.blah#someMethod();bundle=mybundle");

        Bundle providerBundle1 = mockProviderBundle("impl1", 1);
        Bundle providerBundle2 = mockProviderBundle("impl2", 2);
        Bundle providerBundle3 = mockProviderBundle("impl3", 3);
        Bundle providerBundle4 = mockProviderBundle("impl4", 4);
        Activator.activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle1);
        Activator.activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle2);
        Activator.activator.registerProviderBundle("org.apache.aries.mytest.AltSPI", providerBundle2);
        Activator.activator.registerProviderBundle("javax.xml.parsers.DocumentBuilderFactory", providerBundle3);
        Activator.activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle4);        
        Activator.activator.registerProviderBundle("org.apache.aries.mytest.AltSPI", providerBundle4);
        
        Bundle consumerBundle1 = mockConsumerBundle(headers1, providerBundle1, providerBundle2, providerBundle3, providerBundle4);
        Bundle consumerBundle2 = mockConsumerBundle(headers2, providerBundle1, providerBundle2, providerBundle3, providerBundle4);
        Bundle consumerBundle3 = mockConsumerBundle(headers3, providerBundle1, providerBundle2, providerBundle3, providerBundle4);
        Bundle spiFlyBundle = mockSpiFlyBundle(consumerBundle1, consumerBundle2, consumerBundle3,
                providerBundle1, providerBundle2, providerBundle3, providerBundle4);
        WeavingHook wh = new ClientWeavingHook(spiFlyBundle.getBundleContext());
        
        testConsumerBundleWeaving(consumerBundle1, wh, "impl4", "org.apache.aries.spifly.impl3.MyAltDocumentBuilderFactory");                
        testConsumerBundleWeaving(consumerBundle2, wh, "olleh", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");                
        testConsumerBundleWeaving(consumerBundle3, wh, "", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");                
    }

    private void testConsumerBundleWeaving(Bundle consumerBundle, WeavingHook wh, String testClientResult, String jaxpClientResult) throws Exception {
        // Weave the TestClient class.
        URL clsUrl = getClass().getResource("TestClient.class");
        WovenClass wc = new MyWovenClass(clsUrl, "org.apache.aries.spifly.TestClient", consumerBundle);
        wh.weave(wc);
        
        // Invoke the woven class and check that it propertly sets the TCCL so that the 
        // META-INF/services/org.apache.aries.mytest.MySPI file from impl2 is visible.
        Class<?> cls = wc.getDefinedClass();
        Method method = cls.getMethod("test", new Class [] {String.class});
        Object result = method.invoke(cls.newInstance(), "hello");
        Assert.assertEquals(testClientResult, result);        
        
        URL clsUrl2 = getClass().getResource("JaxpClient.class");
        WovenClass wc2 = new MyWovenClass(clsUrl2, "org.apache.aries.spifly.JaxpClient", consumerBundle);
        wh.weave(wc2);
        
        Class<?> cls2 = wc2.getDefinedClass();
        Method method2 = cls2.getMethod("test", new Class [] {});
        Class<?> result2 = (Class<?>) method2.invoke(cls2.newInstance());
        Assert.assertEquals(jaxpClientResult, result2.getName());
    }
        
    @Test
    public void testJAXPClientWantsJREImplementation1() throws Exception {
        Bundle systembundle = mockSystemBundle();

        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(SpiFlyConstants.SPI_CONSUMER_HEADER, "javax.xml.parsers.DocumentBuilderFactory#newInstance()");
        Bundle consumerBundle = mockConsumerBundle(headers, systembundle);

        WeavingHook wh = new ClientWeavingHook(mockSpiFlyBundle(consumerBundle, systembundle).getBundleContext());

        URL clsUrl = getClass().getResource("JaxpClient.class");
        WovenClass wc = new MyWovenClass(clsUrl, "org.apache.aries.spifly.JaxpClient", consumerBundle);
        wh.weave(wc);
        
        Class<?> cls = wc.getDefinedClass();
        Method method = cls.getMethod("test", new Class [] {});
        Class<?> result = (Class<?>) method.invoke(cls.newInstance());
        Assert.assertEquals("JAXP implementation from JRE", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl", result.getName());                
    }
    
    // If there is an alternate implementation it should always be favoured over the JRE one
    @Test
    public void testJAXPClientWantsAltImplementation1() throws Exception {
        Bundle systembundle = mockSystemBundle();

        Bundle providerBundle = mockProviderBundle("impl3", 1);
        Activator.activator.registerProviderBundle("javax.xml.parsers.DocumentBuilderFactory", providerBundle);

        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(SpiFlyConstants.SPI_CONSUMER_HEADER, "javax.xml.parsers.DocumentBuilderFactory#newInstance()");
        Bundle consumerBundle = mockConsumerBundle(headers, providerBundle, systembundle);

        WeavingHook wh = new ClientWeavingHook(mockSpiFlyBundle(consumerBundle, providerBundle, systembundle).getBundleContext());

        URL clsUrl = getClass().getResource("JaxpClient.class");
        WovenClass wc = new MyWovenClass(clsUrl, "org.apache.aries.spifly.JaxpClient", consumerBundle);
        wh.weave(wc);
        
        Class<?> cls = wc.getDefinedClass();
        Method method = cls.getMethod("test", new Class [] {});
        Class<?> result = (Class<?>) method.invoke(cls.newInstance());
        Assert.assertEquals("JAXP implementation from JRE", "org.apache.aries.spifly.impl3.MyAltDocumentBuilderFactory", result.getName());                
    }

    @Test
    public void testJAXPClientWantsJREImplementation2() throws Exception {
        Bundle systembundle = mockSystemBundle();
        
        Bundle providerBundle = mockProviderBundle("impl3", 1);
        Activator.activator.registerProviderBundle("javax.xml.parsers.DocumentBuilderFactory", providerBundle);

        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(SpiFlyConstants.SPI_CONSUMER_HEADER, "javax.xml.parsers.DocumentBuilderFactory#newInstance();bundleId=0");
        Bundle consumerBundle = mockConsumerBundle(headers, providerBundle, systembundle);

        WeavingHook wh = new ClientWeavingHook(mockSpiFlyBundle(consumerBundle, providerBundle, systembundle).getBundleContext());

        URL clsUrl = getClass().getResource("JaxpClient.class");
        WovenClass wc = new MyWovenClass(clsUrl, "org.apache.aries.spifly.JaxpClient", consumerBundle);
        wh.weave(wc);
        
        Class<?> cls = wc.getDefinedClass();
        Method method = cls.getMethod("test", new Class [] {});
        Class<?> result = (Class<?>) method.invoke(cls.newInstance());
        Assert.assertEquals("JAXP implementation from JRE", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl", result.getName());                
    }

    @Test
    public void testJAXPClientWantsAltImplementation2() throws Exception {
        Bundle systembundle = mockSystemBundle();

        Bundle providerBundle = mockProviderBundle("impl3", 1);
        Activator.activator.registerProviderBundle("javax.xml.parsers.DocumentBuilderFactory", providerBundle);

        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(SpiFlyConstants.SPI_CONSUMER_HEADER, "javax.xml.parsers.DocumentBuilderFactory#newInstance();bundle=impl3");
        Bundle consumerBundle = mockConsumerBundle(headers, providerBundle, systembundle);
        
        WeavingHook wh = new ClientWeavingHook(mockSpiFlyBundle(consumerBundle, providerBundle, systembundle).getBundleContext());

        URL clsUrl = getClass().getResource("JaxpClient.class");
        WovenClass wc = new MyWovenClass(clsUrl, "org.apache.aries.spifly.JaxpClient", consumerBundle);
        wh.weave(wc);
        
        Class<?> cls = wc.getDefinedClass();
        Method method = cls.getMethod("test", new Class [] {});
        Class<?> result = (Class<?>) method.invoke(cls.newInstance());
        Assert.assertEquals("JAXP implementation from alternative bundle", "org.apache.aries.spifly.impl3.MyAltDocumentBuilderFactory", result.getName());                        
    }
    
    private Bundle mockSpiFlyBundle(Bundle ... bundles) throws Exception {
        return mockSpiFlyBundle("spifly", new Version(1, 0, 0), bundles);
    }
    
    private Bundle mockSpiFlyBundle(String bsn, Version version, Bundle ... bundles) throws Exception {
        Bundle spiFlyBundle = EasyMock.createMock(Bundle.class);

        BundleContext spiFlyBundleContext = EasyMock.createMock(BundleContext.class);
        EasyMock.expect(spiFlyBundleContext.getBundle()).andReturn(spiFlyBundle).anyTimes();
        List<Bundle> allBundles = new ArrayList<Bundle>(Arrays.asList(bundles));
        allBundles.add(spiFlyBundle);
        EasyMock.expect(spiFlyBundleContext.getBundles()).andReturn(allBundles.toArray(new Bundle [] {})).anyTimes();
        EasyMock.replay(spiFlyBundleContext);

        EasyMock.expect(spiFlyBundle.getSymbolicName()).andReturn(bsn).anyTimes();
        EasyMock.expect(spiFlyBundle.getVersion()).andReturn(version).anyTimes();
        EasyMock.expect(spiFlyBundle.getBundleContext()).andReturn(spiFlyBundleContext).anyTimes();
        EasyMock.replay(spiFlyBundle);

        // Set the bundle context for testing purposes
        Field bcField = Activator.class.getDeclaredField("bundleContext");
        bcField.setAccessible(true);
        bcField.set(Activator.activator, spiFlyBundle.getBundleContext());
        
        return spiFlyBundle;
    }

    private Bundle mockProviderBundle(String subdir, long id) throws Exception {
        return mockProviderBundle(subdir, id, Version.emptyVersion);
    }
    
    private Bundle mockProviderBundle(String subdir, long id, Version version) throws Exception {
        URL url = getClass().getResource("/" + getClass().getName().replace('.', '/') + ".class");
        File classFile = new File(url.getFile());
        File baseDir = new File(classFile.getParentFile(), subdir);
        File directory = new File(baseDir, "/META-INF/services");

        // Do a directory listing of the applicable META-INF/services directory
        List<String> resources = new ArrayList<String>();
        for (File f : directory.listFiles()) {
            String fileName = f.getName();
            if (fileName.startsWith(".") || fileName.endsWith("."))
                continue;
            
            
            // Needs to be something like: META-INF/services/org.apache.aries.mytest.MySPI
            String path = f.getAbsolutePath().substring(baseDir.getAbsolutePath().length());
            path = path.replace('\\', '/');
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            resources.add(path);
        }
            
        // Set up the classloader that will be used by the ASM-generated code as the TCCL. 
        // It can load a META-INF/services file
        ClassLoader cl = new TestImplClassLoader(subdir, resources.toArray(new String [] {}));
        
        // The BundleWiring API is used on the bundle by the generated code to obtain its classloader
        BundleWiring bw = EasyMock.createMock(BundleWiring.class);
        EasyMock.expect(bw.getClassLoader()).andReturn(cl).anyTimes();
        EasyMock.replay(bw);
        
        Bundle providerBundle = EasyMock.createMock(Bundle.class);
        EasyMock.expect(providerBundle.adapt(BundleWiring.class)).andReturn(bw).anyTimes();
        String bsn = subdir;
        int idx = bsn.indexOf('_');
        if (idx > 0) {
            bsn = bsn.substring(0, idx);
        }
        EasyMock.expect(providerBundle.getSymbolicName()).andReturn(bsn).anyTimes();
        EasyMock.expect(providerBundle.getBundleId()).andReturn(id).anyTimes();
        EasyMock.expect(providerBundle.getVersion()).andReturn(version).anyTimes();
        EasyMock.replay(providerBundle);
        return providerBundle;
    }

    private Bundle mockConsumerBundle(Dictionary<String, String> headers, Bundle ... otherBundles) {
        // Create a mock object for the client bundle which holds the code that uses ServiceLoader.load() 
        // or another SPI invocation.
        BundleContext bc = EasyMock.createMock(BundleContext.class);
        
        Bundle consumerBundle = EasyMock.createMock(Bundle.class);
        EasyMock.expect(consumerBundle.getSymbolicName()).andReturn("testConsumer").anyTimes();
        EasyMock.expect(consumerBundle.getHeaders()).andReturn(headers).anyTimes();
        EasyMock.expect(consumerBundle.getBundleContext()).andReturn(bc).anyTimes();
        EasyMock.expect(consumerBundle.getBundleId()).andReturn(Long.MAX_VALUE).anyTimes();
        EasyMock.replay(consumerBundle);        

        List<Bundle> allBundles = new ArrayList<Bundle>(Arrays.asList(otherBundles));
        allBundles.add(consumerBundle);
        EasyMock.expect(bc.getBundles()).andReturn(allBundles.toArray(new Bundle [] {})).anyTimes();
        EasyMock.replay(bc);

        return consumerBundle;
    }
    
    private Bundle mockSystemBundle() {
        Bundle systemBundle = EasyMock.createMock(Bundle.class);
        EasyMock.expect(systemBundle.getBundleId()).andReturn(0L).anyTimes();
        EasyMock.expect(systemBundle.getSymbolicName()).andReturn("system.bundle").anyTimes();
        EasyMock.replay(systemBundle);
        
        return systemBundle;
    }
            
    public static class TestImplClassLoader extends URLClassLoader {
        private final List<String> resources;
        private final String prefix;
        
        public TestImplClassLoader(String subdir, String ... resources) {
            super(new URL [] {}, TestImplClassLoader.class.getClassLoader());
            
            this.prefix = TestImplClassLoader.class.getPackage().getName().replace('.', '/') + "/" + subdir + "/";
            this.resources = Arrays.asList(resources);
        }

        @Override
        public URL findResource(String name) {
            if (resources.contains(name)) {
                return getClass().getClassLoader().getResource(prefix + name);
            } else {
                return super.findResource(name);
            }
        }

        @Override
        public Enumeration<URL> findResources(String name) throws IOException {
            if (resources.contains(name)) {
                return getClass().getClassLoader().getResources(prefix + name);
            } else {
                return super.findResources(name);
            }
        }
    }    

    private static class MyWovenClass implements WovenClass {
        byte [] bytes;
        final String className;
        final Bundle bundleContainingOriginalClass;
        List<String> dynamicImports = new ArrayList<String>();
        boolean weavingComplete = false;
        
        private MyWovenClass(URL clazz, String name, Bundle bundle) throws Exception {
            bytes = Streams.suck(clazz.openStream());
            className = name;
            bundleContainingOriginalClass = bundle;
        }

        @Override
        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public void setBytes(byte[] newBytes) {
            bytes = newBytes;
        }

        @Override
        public List<String> getDynamicImports() {
            return dynamicImports;
        }

        @Override
        public boolean isWeavingComplete() {
            return weavingComplete;
        }

        @Override
        public String getClassName() {
            return className;
        }

        @Override
        public ProtectionDomain getProtectionDomain() {
            return null;
        }

        @Override
        public Class<?> getDefinedClass() {
            try {
                weavingComplete = true;
                return new MyWovenClassClassLoader(className, getBytes(), getClass().getClassLoader(), bundleContainingOriginalClass).loadClass(className);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public BundleWiring getBundleWiring() {
            BundleWiring bw = EasyMock.createMock(BundleWiring.class);
            EasyMock.expect(bw.getBundle()).andReturn(bundleContainingOriginalClass);
            EasyMock.replay(bw);
            return bw;
        }
    }
    
    private static class MyWovenClassClassLoader extends ClassLoader implements BundleReference {
        private final String className;
        private final Bundle bundle;
        private final byte [] bytes;
        
        public MyWovenClassClassLoader(String className, byte[] bytes, ClassLoader parent, Bundle bundle) {
            super(parent);
            
            this.className = className;
            this.bundle = bundle;
            this.bytes = bytes;            
        }
        
        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve)
                throws ClassNotFoundException {
            if (name.equals(className)) {
                return defineClass(className, bytes, 0, bytes.length);
            } else {
                return super.loadClass(name, resolve);
            }
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            return loadClass(name, false);
        }

        @Override
        public Bundle getBundle() {
            return bundle;
        }
    }    
}
