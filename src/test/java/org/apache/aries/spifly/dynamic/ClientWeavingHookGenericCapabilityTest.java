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
package org.apache.aries.spifly.dynamic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.aries.spifly.BaseActivator;
import org.apache.aries.spifly.SpiFlyConstants;
import org.apache.aries.spifly.Streams;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
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

public class ClientWeavingHookGenericCapabilityTest {
    DynamicWeavingActivator activator;

    @Before
    public void setUp() {
        activator = new DynamicWeavingActivator();
        BaseActivator.activator = activator;
    }

    @After
    public void tearDown() {
        BaseActivator.activator = null;
        activator = null;
    }

    @Test
    public void testBasicServiceLoaderUsage() throws Exception {
        Dictionary<String, String> consumerHeaders = new Hashtable<String, String>();
        consumerHeaders.put(SpiFlyConstants.REQUIRE_CAPABILITY, SpiFlyConstants.CLIENT_REQUIREMENT);

        // Register the bundle that provides the SPI implementation.
        Bundle providerBundle = mockProviderBundle("impl1", 1);
        activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle, new HashMap<String, Object>());

        Bundle consumerBundle = mockConsumerBundle(consumerHeaders, providerBundle);
        activator.addConsumerWeavingData(consumerBundle, SpiFlyConstants.REQUIRE_CAPABILITY);

        Bundle spiFlyBundle = mockSpiFlyBundle("spifly", Version.parseVersion("1.9.4"), consumerBundle, providerBundle);
        WeavingHook wh = new ClientWeavingHook(spiFlyBundle.getBundleContext(), activator);

        // Weave the TestClient class.
        URL clsUrl = getClass().getResource("TestClient.class");
        Assert.assertNotNull("Precondition", clsUrl);

        String clientClassName = "org.apache.aries.spifly.dynamic.TestClient";
        WovenClass wc = new MyWovenClass(clsUrl, clientClassName, consumerBundle);
        Assert.assertEquals("Precondition", 0, wc.getDynamicImports().size());
        wh.weave(wc);
        Assert.assertEquals(1, wc.getDynamicImports().size());
        String di1 = "org.apache.aries.spifly;bundle-symbolic-name=spifly;bundle-version=1.9.4";
        String di2 = "org.apache.aries.spifly;bundle-version=1.9.4;bundle-symbolic-name=spifly";
        String di = wc.getDynamicImports().get(0);
        Assert.assertTrue("Weaving should have added a dynamic import", di1.equals(di) || di2.equals(di));

        // Invoke the woven class and check that it properly sets the TCCL so that the
        // META-INF/services/org.apache.aries.mytest.MySPI file from impl1 is visible.
        Class<?> cls = wc.getDefinedClass();
        Method method = cls.getMethod("test", new Class [] {String.class});
        Object result = method.invoke(cls.newInstance(), "hello");
        Assert.assertEquals("olleh", result);
    }


    @Test
    public void testTCCLResetting() throws Exception {
        ClassLoader cl = new URLClassLoader(new URL [] {});
        Thread.currentThread().setContextClassLoader(cl);
        Assert.assertSame("Precondition", cl, Thread.currentThread().getContextClassLoader());

        Dictionary<String, String> consumerHeaders = new Hashtable<String, String>();
        consumerHeaders.put(SpiFlyConstants.REQUIRE_CAPABILITY, SpiFlyConstants.CLIENT_REQUIREMENT);

        // Register the bundle that provides the SPI implementation.
        Bundle providerBundle = mockProviderBundle("impl1", 1);
        activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle, new HashMap<String, Object>());

        Bundle consumerBundle = mockConsumerBundle(consumerHeaders, providerBundle);
        activator.addConsumerWeavingData(consumerBundle, SpiFlyConstants.REQUIRE_CAPABILITY);

        Bundle spiFlyBundle = mockSpiFlyBundle("spifly", Version.parseVersion("1.9.4"), consumerBundle, providerBundle);
        WeavingHook wh = new ClientWeavingHook(spiFlyBundle.getBundleContext(), activator);

        // Weave the TestClient class.
        URL clsUrl = getClass().getResource("TestClient.class");
        Assert.assertNotNull("Precondition", clsUrl);

        String clientClassName = "org.apache.aries.spifly.dynamic.TestClient";
        WovenClass wc = new MyWovenClass(clsUrl, clientClassName, consumerBundle);
        Assert.assertEquals("Precondition", 0, wc.getDynamicImports().size());
        wh.weave(wc);
        Assert.assertEquals(1, wc.getDynamicImports().size());
        String di1 = "org.apache.aries.spifly;bundle-symbolic-name=spifly;bundle-version=1.9.4";
        String di2 = "org.apache.aries.spifly;bundle-version=1.9.4;bundle-symbolic-name=spifly";
        String di = wc.getDynamicImports().get(0);
        Assert.assertTrue("Weaving should have added a dynamic import", di1.equals(di) || di2.equals(di));

        // Invoke the woven class and check that it properly sets the TCCL so that the
        // META-INF/services/org.apache.aries.mytest.MySPI file from impl1 is visible.
        Class<?> cls = wc.getDefinedClass();
        Method method = cls.getMethod("test", new Class [] {String.class});
        method.invoke(cls.newInstance(), "hi there");

        Assert.assertSame(cl, Thread.currentThread().getContextClassLoader());
    }


    @Test
    public void testTCCLResettingOnException() throws Exception {
        ClassLoader cl = new URLClassLoader(new URL [] {});
        Thread.currentThread().setContextClassLoader(cl);
        Assert.assertSame("Precondition", cl, Thread.currentThread().getContextClassLoader());

        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(SpiFlyConstants.REQUIRE_CAPABILITY, SpiFlyConstants.CLIENT_REQUIREMENT);

        Bundle providerBundle5 = mockProviderBundle("impl5", 1);
        activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle5, new HashMap<String, Object>());

        Bundle consumerBundle = mockConsumerBundle(headers, providerBundle5);
        activator.addConsumerWeavingData(consumerBundle, SpiFlyConstants.REQUIRE_CAPABILITY);

        Bundle spiFlyBundle = mockSpiFlyBundle(consumerBundle,providerBundle5);
        WeavingHook wh = new ClientWeavingHook(spiFlyBundle.getBundleContext(), activator);

        // Weave the TestClient class.
        URL clsUrl = getClass().getResource("TestClient.class");
        WovenClass wc = new MyWovenClass(clsUrl, "org.apache.aries.spifly.dynamic.TestClient", consumerBundle);
        wh.weave(wc);

        Class<?> cls = wc.getDefinedClass();
        Method method = cls.getMethod("test", new Class [] {String.class});

        // Invoke the woven class, check that it properly set the TCCL so that the implementation of impl5 is called.
        // That implementation throws an exception, after which we are making sure that the TCCL is set back appropriately.
        try {
            method.invoke(cls.newInstance(), "hello");
            Assert.fail("Invocation should have thrown an exception");
        } catch (InvocationTargetException ite) {
            RuntimeException re = (RuntimeException) ite.getCause();
            String msg = re.getMessage();
            Assert.assertEquals("Uh-oh: hello", msg);

            // The TCCL should have been reset correctly
            Assert.assertSame(cl, Thread.currentThread().getContextClassLoader());
        }
    }

    @Test
    public void testAltServiceLoaderLoadUnprocessed() throws Exception {
        Bundle spiFlyBundle = mockSpiFlyBundle();

        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(SpiFlyConstants.REQUIRE_CAPABILITY, SpiFlyConstants.CLIENT_REQUIREMENT);
        Bundle consumerBundle = mockConsumerBundle(headers, spiFlyBundle);

        WeavingHook wh = new ClientWeavingHook(spiFlyBundle.getBundleContext(), activator);

        // Weave the TestClient class.
        URL clsUrl = getClass().getResource("UnaffectedTestClient.class");
        Assert.assertNotNull("Precondition", clsUrl);
        WovenClass wc = new MyWovenClass(clsUrl, "org.apache.aries.spifly.dynamic.UnaffectedTestClient", consumerBundle);
        Assert.assertEquals("Precondition", 0, wc.getDynamicImports().size());
        wh.weave(wc);

        Assert.assertEquals("The client is not affected so no additional imports should have been added",
            0, wc.getDynamicImports().size());

        // ok the weaving is done, now prepare the registry for the call
        Bundle providerBundle = mockProviderBundle("impl1", 1);
        activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle, new HashMap<String, Object>());

        // Invoke the woven class and check that it propertly sets the TCCL so that the
        // META-INF/services/org.apache.aries.mytest.MySPI file from impl1 is visible.
        Class<?> cls = wc.getDefinedClass();
        Method method = cls.getMethod("test", new Class [] {String.class});
        Object result = method.invoke(cls.newInstance(), "hello");
        Assert.assertEquals("impl4", result);
    }

    @Test
    public void testMultipleProviders() throws Exception {
        Bundle spiFlyBundle = mockSpiFlyBundle();

        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(SpiFlyConstants.REQUIRE_CAPABILITY, SpiFlyConstants.CLIENT_REQUIREMENT);

        Bundle consumerBundle = mockConsumerBundle(headers, spiFlyBundle);
        activator.addConsumerWeavingData(consumerBundle, SpiFlyConstants.REQUIRE_CAPABILITY);

        WeavingHook wh = new ClientWeavingHook(spiFlyBundle.getBundleContext(), activator);

        // Weave the TestClient class.
        URL clsUrl = getClass().getResource("TestClient.class");
        WovenClass wc = new MyWovenClass(clsUrl, "org.apache.aries.spifly.dynamic.TestClient", consumerBundle);
        wh.weave(wc);

        Bundle providerBundle1 = mockProviderBundle("impl1", 1);
        Bundle providerBundle2 = mockProviderBundle("impl2", 2);

        // Register in reverse order to make sure the order in which bundles are sorted is correct
        activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle2, new HashMap<String, Object>());
        activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle1, new HashMap<String, Object>());

        // Invoke the woven class and check that it propertly sets the TCCL so that the
        // META-INF/services/org.apache.aries.mytest.MySPI files from impl1 and impl2 are visible.
        Class<?> cls = wc.getDefinedClass();
        Method method = cls.getMethod("test", new Class [] {String.class});
        Object result = method.invoke(cls.newInstance(), "hello");
        Assert.assertEquals("All three services should be invoked in the correct order", "ollehHELLO5", result);
    }

    /* This is currently not supported in the generic model
    @Test
    public void testClientSpecifyingProvider() throws Exception {
        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(SpiFlyConstants.REQUIRE_CAPABILITY, SpiFlyConstants.CLIENT_REQUIREMENT +
                "; " + SpiFlyConstants.PROVIDER_FILTER_DIRECTIVE + ":=\"(bundle-symbolic-name=impl2)\"");

        Bundle providerBundle1 = mockProviderBundle("impl1", 1);
        Bundle providerBundle2 = mockProviderBundle("impl2", 2);
        activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle1, new HashMap<String, Object>());
        activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle2, new HashMap<String, Object>());

        Bundle consumerBundle = mockConsumerBundle(headers, providerBundle1, providerBundle2);
        activator.addConsumerWeavingData(consumerBundle, SpiFlyConstants.REQUIRE_CAPABILITY);

        Bundle spiFlyBundle = mockSpiFlyBundle(consumerBundle, providerBundle1, providerBundle2);
        WeavingHook wh = new ClientWeavingHook(spiFlyBundle.getBundleContext(), activator);

        // Weave the TestClient class.
        URL clsUrl = getClass().getResource("TestClient.class");
        WovenClass wc = new MyWovenClass(clsUrl, "org.apache.aries.spifly.dynamic.TestClient", consumerBundle);
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
        headers.put(SpiFlyConstants.REQUIRE_CAPABILITY, SpiFlyConstants.CLIENT_REQUIREMENT +
                "; " + SpiFlyConstants.PROVIDER_FILTER_DIRECTIVE + ":=\"(&(bundle-symbolic-name=impl2)(bundle-version=1.2.3))\"");

        Bundle providerBundle1 = mockProviderBundle("impl1", 1);
        Bundle providerBundle2 = mockProviderBundle("impl2", 2);
        Bundle providerBundle3 = mockProviderBundle("impl2_123", 3, new Version(1, 2, 3));
        activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle1, new HashMap<String, Object>());
        activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle2, new HashMap<String, Object>());
        activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle3, new HashMap<String, Object>());

        Bundle consumerBundle = mockConsumerBundle(headers, providerBundle1, providerBundle2, providerBundle3);
        activator.addConsumerWeavingData(consumerBundle, SpiFlyConstants.REQUIRE_CAPABILITY);
        Bundle spiFlyBundle = mockSpiFlyBundle(consumerBundle, providerBundle1, providerBundle2, providerBundle3);
        WeavingHook wh = new ClientWeavingHook(spiFlyBundle.getBundleContext(), activator);

        // Weave the TestClient class.
        URL clsUrl = getClass().getResource("TestClient.class");
        WovenClass wc = new MyWovenClass(clsUrl, "org.apache.aries.spifly.dynamic.TestClient", consumerBundle);
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
        headers.put(SpiFlyConstants.REQUIRE_CAPABILITY, SpiFlyConstants.CLIENT_REQUIREMENT +
                "; " + SpiFlyConstants.PROVIDER_FILTER_DIRECTIVE + ":=\"(|(bundle-symbolic-name=impl1)(bundle-symbolic-name=impl4))\"");

        Bundle providerBundle1 = mockProviderBundle("impl1", 1);
        Bundle providerBundle2 = mockProviderBundle("impl2", 2);
        Bundle providerBundle4 = mockProviderBundle("impl4", 4);
        activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle1, new HashMap<String, Object>());
        activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle2, new HashMap<String, Object>());
        activator.registerProviderBundle("org.apache.aries.mytest.AltSPI", providerBundle2, new HashMap<String, Object>());
        activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle4, new HashMap<String, Object>());
        activator.registerProviderBundle("org.apache.aries.mytest.AltSPI", providerBundle4, new HashMap<String, Object>());

        Bundle consumerBundle = mockConsumerBundle(headers, providerBundle1, providerBundle2, providerBundle4);
        activator.addConsumerWeavingData(consumerBundle, SpiFlyConstants.REQUIRE_CAPABILITY);
        Bundle spiFlyBundle = mockSpiFlyBundle(consumerBundle, providerBundle1, providerBundle2, providerBundle4);
        WeavingHook wh = new ClientWeavingHook(spiFlyBundle.getBundleContext(), activator);

        // Weave the TestClient class.
        URL clsUrl = getClass().getResource("TestClient.class");
        WovenClass wc = new MyWovenClass(clsUrl, "org.apache.aries.spifly.dynamic.TestClient", consumerBundle);
        wh.weave(wc);

        // Invoke the woven class and check that it propertly sets the TCCL so that the
        // META-INF/services/org.apache.aries.mytest.MySPI file from impl2 is visible.
        Class<?> cls = wc.getDefinedClass();
        Method method = cls.getMethod("test", new Class [] {String.class});
        Object result = method.invoke(cls.newInstance(), "hello");
        Assert.assertEquals("All providers should be selected for this one", "ollehimpl4", result);
    }
    */

    @Test
    public void testServiceFiltering() throws Exception {
        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(SpiFlyConstants.REQUIRE_CAPABILITY, SpiFlyConstants.CLIENT_REQUIREMENT + "," +
            SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE +
                "; filter:=\"(osgi.serviceloader=org.apache.aries.mytest.AltSPI)\";");

        Bundle providerBundle2 = mockProviderBundle("impl2", 2);
        Bundle providerBundle4 = mockProviderBundle("impl4", 4);
        activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle2, new HashMap<String, Object>());
        activator.registerProviderBundle("org.apache.aries.mytest.AltSPI", providerBundle2, new HashMap<String, Object>());
        activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle4, new HashMap<String, Object>());
        activator.registerProviderBundle("org.apache.aries.mytest.AltSPI", providerBundle4, new HashMap<String, Object>());

        Bundle consumerBundle = mockConsumerBundle(headers, providerBundle2, providerBundle4);
        activator.addConsumerWeavingData(consumerBundle, SpiFlyConstants.REQUIRE_CAPABILITY);

        Bundle spiFlyBundle = mockSpiFlyBundle(consumerBundle, providerBundle2, providerBundle4);
        WeavingHook wh = new ClientWeavingHook(spiFlyBundle.getBundleContext(), activator);

        // Weave the TestClient class.
        URL clsUrl = getClass().getResource("TestClient.class");
        WovenClass wc = new MyWovenClass(clsUrl, "org.apache.aries.spifly.dynamic.TestClient", consumerBundle);
        wh.weave(wc);

        // Invoke the woven class. Since MySPI wasn't selected nothing should be returned
        Class<?> cls = wc.getDefinedClass();
        Method method = cls.getMethod("test", new Class [] {String.class});
        Object result = method.invoke(cls.newInstance(), "hello");
        Assert.assertEquals("No providers should be selected for this one", "", result);

        // Weave the AltTestClient class.
        URL cls2Url = getClass().getResource("AltTestClient.class");
        WovenClass wc2 = new MyWovenClass(cls2Url, "org.apache.aries.spifly.dynamic.AltTestClient", consumerBundle);
        wh.weave(wc2);

        // Invoke the AltTestClient
        Class<?> cls2 = wc2.getDefinedClass();
        Method method2 = cls2.getMethod("test", new Class [] {long.class});
        Object result2 = method2.invoke(cls2.newInstance(), 4096);
        Assert.assertEquals("All Providers should be selected", (4096L*4096L)-4096L, result2);

    }

    @Test
    public void testServiceFilteringAlternative() throws Exception {
        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(SpiFlyConstants.REQUIRE_CAPABILITY, SpiFlyConstants.CLIENT_REQUIREMENT + "," +
                SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE +
                "; filter:=\"(|(!(osgi.serviceloader=org.apache.aries.mytest.AltSPI))" +
                            "(&(osgi.serviceloader=org.apache.aries.mytest.AltSPI)(bundle-symbolic-name=impl4)))\"");

        Bundle providerBundle1 = mockProviderBundle("impl1", 1);
        Bundle providerBundle2 = mockProviderBundle("impl2", 2);
        Bundle providerBundle4 = mockProviderBundle("impl4", 4);
        activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle1, new HashMap<String, Object>());
        HashMap<String, Object> attrs2 = new HashMap<String, Object>();
        attrs2.put("bundle-symbolic-name", "impl2");
        activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle2, attrs2);
        activator.registerProviderBundle("org.apache.aries.mytest.AltSPI", providerBundle2, attrs2);
        HashMap<String, Object> attrs4 = new HashMap<String, Object>();
        attrs4.put("bundle-symbolic-name", "impl4");
        activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle4, attrs4);
        activator.registerProviderBundle("org.apache.aries.mytest.AltSPI", providerBundle4, attrs4);

        Bundle consumerBundle = mockConsumerBundle(headers, providerBundle1, providerBundle2, providerBundle4);
        activator.addConsumerWeavingData(consumerBundle, SpiFlyConstants.REQUIRE_CAPABILITY);

        Bundle spiFlyBundle = mockSpiFlyBundle(consumerBundle, providerBundle1, providerBundle2, providerBundle4);
        WeavingHook wh = new ClientWeavingHook(spiFlyBundle.getBundleContext(), activator);

        // Weave the TestClient class.
        URL clsUrl = getClass().getResource("TestClient.class");
        WovenClass wc = new MyWovenClass(clsUrl, "org.apache.aries.spifly.dynamic.TestClient", consumerBundle);
        wh.weave(wc);

        // Invoke the woven class and check that it propertly sets the TCCL so that the
        // META-INF/services/org.apache.aries.mytest.MySPI file from impl2 is visible.
        Class<?> cls = wc.getDefinedClass();
        Method method = cls.getMethod("test", new Class [] {String.class});
        Object result = method.invoke(cls.newInstance(), "hello");
        Assert.assertEquals("All providers should be selected for this one", "ollehHELLO5impl4", result);

        // Weave the AltTestClient class.
        URL cls2Url = getClass().getResource("AltTestClient.class");
        WovenClass wc2 = new MyWovenClass(cls2Url, "org.apache.aries.spifly.dynamic.AltTestClient", consumerBundle);
        wh.weave(wc2);

        // Invoke the AltTestClient
        Class<?> cls2 = wc2.getDefinedClass();
        Method method2 = cls2.getMethod("test", new Class [] {long.class});
        Object result2 = method2.invoke(cls2.newInstance(), 4096);
        Assert.assertEquals("Only the services from bundle impl4 should be selected", -4096L, result2);
    }

    @Test
    public void testServiceFilteringNarrow() throws Exception {
        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(SpiFlyConstants.REQUIRE_CAPABILITY, SpiFlyConstants.CLIENT_REQUIREMENT + "," +
                SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE +
                "; filter:=\"(&(osgi.serviceloader=org.apache.aries.mytest.AltSPI)(bundle-symbolic-name=impl4))\"");

        Bundle providerBundle1 = mockProviderBundle("impl1", 1);
        Bundle providerBundle2 = mockProviderBundle("impl2", 2);
        Bundle providerBundle4 = mockProviderBundle("impl4", 4);
        activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle1, new HashMap<String, Object>());
        HashMap<String, Object> attrs2 = new HashMap<String, Object>();
        attrs2.put("bundle-symbolic-name", "impl2");
        activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle2, attrs2);
        activator.registerProviderBundle("org.apache.aries.mytest.AltSPI", providerBundle2, attrs2);
        HashMap<String, Object> attrs4 = new HashMap<String, Object>();
        attrs4.put("bundle-symbolic-name", "impl4");
        activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle4, attrs4);
        activator.registerProviderBundle("org.apache.aries.mytest.AltSPI", providerBundle4, attrs4);

        Bundle consumerBundle = mockConsumerBundle(headers, providerBundle1, providerBundle2, providerBundle4);
        activator.addConsumerWeavingData(consumerBundle, SpiFlyConstants.REQUIRE_CAPABILITY);

        Bundle spiFlyBundle = mockSpiFlyBundle(consumerBundle, providerBundle1, providerBundle2, providerBundle4);
        WeavingHook wh = new ClientWeavingHook(spiFlyBundle.getBundleContext(), activator);

        // Weave the TestClient class.
        URL clsUrl = getClass().getResource("TestClient.class");
        WovenClass wc = new MyWovenClass(clsUrl, "org.apache.aries.spifly.dynamic.TestClient", consumerBundle);
        wh.weave(wc);

        // Invoke the woven class and check that it propertly sets the TCCL so that the
        // META-INF/services/org.apache.aries.mytest.MySPI file from impl2 is visible.
        Class<?> cls = wc.getDefinedClass();
        Method method = cls.getMethod("test", new Class [] {String.class});
        Object result = method.invoke(cls.newInstance(), "hello");
        Assert.assertEquals("No providers should be selected here", "", result);

        // Weave the AltTestClient class.
        URL cls2Url = getClass().getResource("AltTestClient.class");
        WovenClass wc2 = new MyWovenClass(cls2Url, "org.apache.aries.spifly.dynamic.AltTestClient", consumerBundle);
        wh.weave(wc2);

        // Invoke the AltTestClient
        Class<?> cls2 = wc2.getDefinedClass();
        Method method2 = cls2.getMethod("test", new Class [] {long.class});
        Object result2 = method2.invoke(cls2.newInstance(), 4096);
        Assert.assertEquals("Only the services from bundle impl4 should be selected", -4096L, result2);
    }

    @Test
    public void testFilteringCustomAttribute() throws Exception {
        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(SpiFlyConstants.REQUIRE_CAPABILITY, SpiFlyConstants.CLIENT_REQUIREMENT + ", " +
            SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE + "; filter:=\"(approval=global)\"");

        Bundle providerBundle1 = mockProviderBundle("impl1", 1);
        Bundle providerBundle2 = mockProviderBundle("impl2", 2);
        Map<String, Object> attrs1 = new HashMap<String, Object>();
        attrs1.put("approval", "local");
        activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle1, attrs1);

        Map<String, Object> attrs2 = new HashMap<String, Object>();
        attrs2.put("approval", "global");
        attrs2.put("other", "attribute");
        activator.registerProviderBundle("org.apache.aries.mytest.MySPI", providerBundle2, attrs2);

        Bundle consumerBundle = mockConsumerBundle(headers, providerBundle1, providerBundle2);
        activator.addConsumerWeavingData(consumerBundle, SpiFlyConstants.REQUIRE_CAPABILITY);

        Bundle spiFlyBundle = mockSpiFlyBundle(consumerBundle, providerBundle1, providerBundle2);
        WeavingHook wh = new ClientWeavingHook(spiFlyBundle.getBundleContext(), activator);

        // Weave the TestClient class.
        URL clsUrl = getClass().getResource("TestClient.class");
        WovenClass wc = new MyWovenClass(clsUrl, "org.apache.aries.spifly.dynamic.TestClient", consumerBundle);
        wh.weave(wc);

        // Invoke the woven class and check that it propertly sets the TCCL so that the
        // META-INF/services/org.apache.aries.mytest.MySPI file from impl2 is visible.
        Class<?> cls = wc.getDefinedClass();
        Method method = cls.getMethod("test", new Class [] {String.class});
        Object result = method.invoke(cls.newInstance(), "hello");
        Assert.assertEquals("Only the services from bundle impl2 should be selected", "HELLO5", result);
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
        EasyMock.expect(spiFlyBundle.getBundleId()).andReturn(Long.MAX_VALUE).anyTimes();
        EasyMock.expect(spiFlyBundle.getBundleContext()).andReturn(spiFlyBundleContext).anyTimes();
        EasyMock.replay(spiFlyBundle);

        // Set the bundle context for testing purposes
        Field bcField = BaseActivator.class.getDeclaredField("bundleContext");
        bcField.setAccessible(true);
        bcField.set(activator, spiFlyBundle.getBundleContext());

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
        final List<String> classNames = new ArrayList<String>();

        // Do a directory listing of the applicable META-INF/services directory
        List<String> resources = new ArrayList<String>();
        for (File f : directory.listFiles()) {
            String fileName = f.getName();
            if (fileName.startsWith(".") || fileName.endsWith("."))
                continue;

            classNames.addAll(getClassNames(f));

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
        final ClassLoader cl = new TestProviderBundleClassLoader(subdir, resources.toArray(new String [] {}));

        final List<String> classResources = new ArrayList<String>();
        for(String className : classNames) {
            classResources.add("/" + className.replace('.', '/') + ".class");
        }

        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(bc);

        Bundle providerBundle = EasyMock.createMock(Bundle.class);
        String bsn = subdir;
        int idx = bsn.indexOf('_');
        if (idx > 0) {
            bsn = bsn.substring(0, idx);
        }
        EasyMock.expect(providerBundle.getSymbolicName()).andReturn(bsn).anyTimes();
        EasyMock.expect(providerBundle.getBundleId()).andReturn(id).anyTimes();
        EasyMock.expect(providerBundle.getBundleContext()).andReturn(bc).anyTimes();
        EasyMock.expect(providerBundle.getVersion()).andReturn(version).anyTimes();
        EasyMock.expect(providerBundle.getEntryPaths("/")).andAnswer(new IAnswer<Enumeration<String>>() {
            @Override
            public Enumeration<String> answer() throws Throwable {
                return Collections.enumeration(classResources);
            }
        }).anyTimes();
        EasyMock.<Class<?>>expect(providerBundle.loadClass(EasyMock.anyObject(String.class))).andAnswer(new IAnswer<Class<?>>() {
            @Override
            public Class<?> answer() throws Throwable {
                String  name = (String) EasyMock.getCurrentArguments()[0];
                if (!classNames.contains(name)) {
                    throw new ClassCastException(name);
                }
                return cl.loadClass(name);
            }
        }).anyTimes();
        EasyMock.replay(providerBundle);
        return providerBundle;
    }

    private Collection<String> getClassNames(File f) throws IOException {
        List<String> names = new ArrayList<String>();

        BufferedReader br = new BufferedReader(new FileReader(f));
        try {
            String line = null;
            while((line = br.readLine()) != null) {
                names.add(line.trim());
            }
        } finally {
            br.close();
        }
        return names;
    }

    private Bundle mockConsumerBundle(Dictionary<String, String> headers, Bundle ... otherBundles) {
        // Create a mock object for the client bundle which holds the code that uses ServiceLoader.load()
        // or another SPI invocation.
        BundleContext bc = EasyMock.createMock(BundleContext.class);

        Bundle consumerBundle = EasyMock.createMock(Bundle.class);
        EasyMock.expect(consumerBundle.getSymbolicName()).andReturn("testConsumer").anyTimes();
        EasyMock.expect(consumerBundle.getVersion()).andReturn(new Version(1, 2, 3)).anyTimes();
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

    // A classloader that loads anything starting with org.apache.aries.spifly.dynamic.impl1 from it
    // and the rest from the parent. This is to mimic a bundle that holds a specific SPI implementation.
    public static class TestProviderBundleClassLoader extends URLClassLoader {
        private final List<String> resources;
        private final String prefix;
        private final String classPrefix;
        private final Map<String, Class<?>> loadedClasses = new ConcurrentHashMap<String, Class<?>>();

        public TestProviderBundleClassLoader(String subdir, String ... resources) {
            super(new URL [] {}, TestProviderBundleClassLoader.class.getClassLoader());

            this.prefix = TestProviderBundleClassLoader.class.getPackage().getName().replace('.', '/') + "/" + subdir + "/";
            this.classPrefix = prefix.replace('/', '.');
            this.resources = Arrays.asList(resources);
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (name.startsWith(classPrefix))
                return loadClassLocal(name);

            return super.loadClass(name);
        }

        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith(classPrefix)) {
                Class<?> cls = loadClassLocal(name);
                if (resolve)
                    resolveClass(cls);

                return cls;
            }

            return super.loadClass(name, resolve);
        }

        protected Class<?> loadClassLocal(String name) throws ClassNotFoundException {
            Class<?> prevLoaded = loadedClasses.get(name);
            if (prevLoaded != null)
                return prevLoaded;

            URL res = TestProviderBundleClassLoader.class.getClassLoader().getResource(name.replace('.', '/') + ".class");
            try {
                byte[] bytes = Streams.suck(res.openStream());
                Class<?> cls = defineClass(name, bytes, 0, bytes.length);
                loadedClasses.put(name, cls);
                return cls;
            } catch (Exception e) {
                throw new ClassNotFoundException(name, e);
            }
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
            EasyMock.expect(bw.getClassLoader()).andReturn(getClass().getClassLoader());
            EasyMock.replay(bw);
            return bw;
        }
    }

    private static class MyWovenClassClassLoader extends ClassLoader implements BundleReference {
        private final String className;
        private final Bundle bundle;
        private final byte [] bytes;
        private Class<?> wovenClass;

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
                if (wovenClass == null)
                    wovenClass = defineClass(className, bytes, 0, bytes.length);

                return wovenClass;
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
