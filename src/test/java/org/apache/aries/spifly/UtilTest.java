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

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.ServiceLoader;

import org.apache.aries.mytest.MySPI;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;

public class UtilTest {
    private ClassLoader storedTCCL;

    @Before
    public void setup() {
        storedTCCL = Thread.currentThread().getContextClassLoader();
    }

    @After
    public void tearDown() {
        Thread.currentThread().setContextClassLoader(storedTCCL);
        storedTCCL = null;
    }

    @Test
    public void testSetRestoreTCCL() {
        ClassLoader cl = new URLClassLoader(new URL[] {});
        Thread.currentThread().setContextClassLoader(cl);
        Util.storeContextClassloader();

        Thread.currentThread().setContextClassLoader(null);

        Util.restoreContextClassloader();
        assertSame(cl, Thread.currentThread().getContextClassLoader());
    }

    @Test
    public void testFixContextClassLoaderSimpleViaEmbeddedJar() throws Exception {
        BaseActivator activator = new BaseActivator() {
            public void start(BundleContext context) throws Exception {
            }
        };
        BaseActivator.activator = activator;

        URL url = getClass().getResource("/embedded3.jar");
        assertNotNull("precondition", url);

        Bundle providerBundle = EasyMock.createMock(Bundle.class);
        final ClassLoader providerCL = new TestBundleClassLoader(new URL [] {url}, getClass().getClassLoader(), providerBundle);
        EasyMock.expect(providerBundle.getBundleId()).andReturn(42L).anyTimes();
        EasyMock.expect(providerBundle.getEntryPaths((String) EasyMock.anyObject())).andReturn(null).anyTimes();
        Dictionary<String, String> providerHeaders = new Hashtable<String, String>();
        providerHeaders.put(Constants.BUNDLE_CLASSPATH, ".,embedded3.jar");
        EasyMock.expect(providerBundle.getHeaders()).andReturn(providerHeaders).anyTimes();
        EasyMock.expect(providerBundle.getResource("embedded3.jar")).andReturn(url).anyTimes();
        providerBundle.loadClass((String) EasyMock.anyObject());
        EasyMock.expectLastCall().andAnswer(new IAnswer<Class<?>>() {
            @Override
            public Class<?> answer() throws Throwable {
                return providerCL.loadClass((String) EasyMock.getCurrentArguments()[0]);
            }
        }).anyTimes();
        EasyMock.replay(providerBundle);
        activator.registerProviderBundle(MySPI.class.getName(), providerBundle, new HashMap<String, Object>());

        Bundle clientBundle = EasyMock.createMock(Bundle.class);
        EasyMock.replay(clientBundle);
        ClassLoader clientCL = new TestBundleClassLoader(new URL [] {}, getClass().getClassLoader(), clientBundle);

        Thread.currentThread().setContextClassLoader(null);
        Util.fixContextClassloader(ServiceLoader.class.getName(), "load", MySPI.class, clientCL);
        assertSame(providerCL, Thread.currentThread().getContextClassLoader());
    }

    @Test
    public void testNotInitialized() throws Exception {
        BaseActivator.activator = null;

        URL url = getClass().getResource("/embedded3.jar");
        assertNotNull("precondition", url);

        Bundle providerBundle = EasyMock.createMock(Bundle.class);
        final ClassLoader providerCL = new TestBundleClassLoader(new URL [] {url}, getClass().getClassLoader(), providerBundle);
        EasyMock.expect(providerBundle.getBundleId()).andReturn(42L).anyTimes();
        EasyMock.expect(providerBundle.getEntryPaths((String) EasyMock.anyObject())).andReturn(null).anyTimes();
        Dictionary<String, String> providerHeaders = new Hashtable<String, String>();
        providerHeaders.put(Constants.BUNDLE_CLASSPATH, ".,embedded3.jar");
        EasyMock.expect(providerBundle.getHeaders()).andReturn(providerHeaders).anyTimes();
        EasyMock.expect(providerBundle.getResource("embedded3.jar")).andReturn(url).anyTimes();
        providerBundle.loadClass((String) EasyMock.anyObject());
        EasyMock.expectLastCall().andAnswer(new IAnswer<Class<?>>() {
            @Override
            public Class<?> answer() throws Throwable {
                return providerCL.loadClass((String) EasyMock.getCurrentArguments()[0]);
            }
        }).anyTimes();
        EasyMock.replay(providerBundle);

        Bundle clientBundle = EasyMock.createMock(Bundle.class);
        EasyMock.replay(clientBundle);
        ClassLoader clientCL = new TestBundleClassLoader(new URL [] {}, getClass().getClassLoader(), clientBundle);

        Thread.currentThread().setContextClassLoader(null);
        Util.fixContextClassloader(ServiceLoader.class.getName(), "load", MySPI.class, clientCL);
        assertSame("The system is not yet initialized, so the TCCL should not be set",
                null, Thread.currentThread().getContextClassLoader());
    }

    private static class TestBundleClassLoader extends URLClassLoader implements BundleReference {
        private final Bundle bundle;

        public TestBundleClassLoader(URL[] urls, ClassLoader parent, Bundle bundle) {
            super(urls, parent);
            this.bundle = bundle;
        }

        @Override
        public Bundle getBundle() {
            return bundle;
        }
    }
}
