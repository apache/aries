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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;

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
import org.osgi.framework.ServicePermission;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;

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
        BaseActivator.activator = null;
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
        EasyMock.expect(providerBundle.getBundleContext()).andThrow(new IllegalStateException("Disable getBundleClassLoaderViaAdapt"));
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

    @Test
    public void standardConsumerWithNoProvidersHasClosedView() throws Exception {
        BaseActivator activator = newActivator();
        Bundle consumer = mockPermissionBundle(new AtomicBoolean(true));
        activator.registerStandardConsumer(consumer, null);

        URL forbidden = getClass().getResource("/embedded2.jar");
        assertNotNull("precondition", forbidden);
        Thread.currentThread().setContextClassLoader(
                new URLClassLoader(new URL[] {forbidden}, getClass().getClassLoader()));

        ServiceLoader<MySPI> loader = Util.serviceLoaderLoad(
                MySPI.class, callerClass(consumer));

        assertFalse(loader.iterator().hasNext());
    }

    @Test
    public void explicitLoaderCannotAddProviderConfigurations() throws Exception {
        BaseActivator activator = newActivator();
        Bundle consumer = mockPermissionBundle(new AtomicBoolean(true));
        BundleWiring consumerWiring = EasyMock.createNiceMock(BundleWiring.class);
        EasyMock.expect(consumerWiring.getRequirements(
                SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE))
                .andReturn(Collections.<BundleRequirement>emptyList()).anyTimes();
        EasyMock.replay(consumerWiring);
        activator.registerStandardConsumer(consumer, consumerWiring);

        URL selected = getClass().getResource("/embedded.jar");
        URL forbidden = getClass().getResource("/embedded2.jar");
        assertNotNull("precondition", selected);
        assertNotNull("precondition", forbidden);
        Bundle provider = mockProviderBundle(42L, selected);
        activator.registerProviderBundle(
                MySPI.class.getName(), provider, new HashMap<String, Object>());

        ClassLoader specified = new URLClassLoader(
                new URL[] {forbidden}, getClass().getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve)
                    throws ClassNotFoundException {
                if (name.startsWith("org.apache.aries.spifly.impl2.")) {
                    throw new AssertionError(
                            "selected provider classes must not come from the specified loader");
                }
                return super.loadClass(name, resolve);
            }
        };
        ServiceLoader<MySPI> loader = Util.serviceLoaderLoad(
                MySPI.class, specified, callerClass(consumer));

        List<String> providerTypes = new ArrayList<String>();
        for (MySPI providerInstance : loader) {
            providerTypes.add(providerInstance.getClass().getName());
        }
        assertEquals(2, providerTypes.size());
        assertTrue(providerTypes.contains(
                "org.apache.aries.spifly.impl2.MySPIImpl2a"));
        assertTrue(providerTypes.contains(
                "org.apache.aries.spifly.impl2.MySPIImpl2b"));
        assertFalse(providerTypes.contains(
                "org.apache.aries.spifly.impl3.MySPIImpl3"));
    }

    @Test
    public void unprocessedCallerRetainsOriginalLoaderFallback() throws Exception {
        newActivator();
        Bundle consumer = EasyMock.createNiceMock(Bundle.class);
        EasyMock.replay(consumer);
        URL providerConfig = getClass().getResource("/embedded2.jar");
        assertNotNull("precondition", providerConfig);
        Thread.currentThread().setContextClassLoader(new URLClassLoader(
                new URL[] {providerConfig}, getClass().getClassLoader()));

        ServiceLoader<MySPI> loader = Util.serviceLoaderLoad(
                MySPI.class, callerClass(consumer));

        assertEquals("org.apache.aries.spifly.impl3.MySPIImpl3",
                loader.iterator().next().getClass().getName());
    }

    @Test
    public void consumerPermissionIsCheckedAtLazyIteration() throws Exception {
        BaseActivator activator = newActivator();
        AtomicBoolean consumerPermission = new AtomicBoolean(true);
        Bundle consumer = mockPermissionBundle(consumerPermission);
        BundleWiring consumerWiring = EasyMock.createNiceMock(BundleWiring.class);
        EasyMock.expect(consumerWiring.getRequirements(
                SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE))
                .andReturn(Collections.<BundleRequirement>emptyList()).anyTimes();
        EasyMock.replay(consumerWiring);
        activator.registerStandardConsumer(consumer, consumerWiring);

        URL selected = getClass().getResource("/embedded2.jar");
        assertNotNull("precondition", selected);
        Bundle provider = mockProviderBundle(42L, selected);
        activator.registerProviderBundle(
                MySPI.class.getName(), provider, new HashMap<String, Object>());

        ServiceLoader<MySPI> loader = Util.serviceLoaderLoad(
                MySPI.class, callerClass(consumer));
        consumerPermission.set(false);
        assertFalse(loader.iterator().hasNext());

        consumerPermission.set(true);
        Iterator<MySPI> iterator = Util.serviceLoaderLoad(
                MySPI.class, callerClass(consumer)).iterator();
        assertTrue(iterator.hasNext());
        consumerPermission.set(false);
        assertThrows(ServiceConfigurationError.class, iterator::next);

        consumerPermission.set(true);
        assertTrue(Util.serviceLoaderLoad(MySPI.class, callerClass(consumer))
                .iterator().hasNext());
    }

    @Test
    public void providerPermissionChangesDoNotRequireReindexing() throws Exception {
        BaseActivator activator = newActivator();
        Bundle consumer = mockPermissionBundle(new AtomicBoolean(true));
        BundleWiring consumerWiring = EasyMock.createNiceMock(BundleWiring.class);
        EasyMock.expect(consumerWiring.getRequirements(
                SpiFlyConstants.SERVICELOADER_CAPABILITY_NAMESPACE))
                .andReturn(Collections.<BundleRequirement>emptyList()).anyTimes();
        EasyMock.replay(consumerWiring);
        activator.registerStandardConsumer(consumer, consumerWiring);

        AtomicBoolean providerPermission = new AtomicBoolean(false);
        URL selected = getClass().getResource("/embedded2.jar");
        assertNotNull("precondition", selected);
        Bundle provider = mockProviderBundle(42L, selected, providerPermission);
        activator.registerProviderBundle(
                MySPI.class.getName(), provider, new HashMap<String, Object>());

        assertFalse(Util.serviceLoaderLoad(MySPI.class, callerClass(consumer))
                .iterator().hasNext());
        providerPermission.set(true);
        Iterator<MySPI> iterator = Util.serviceLoaderLoad(
                MySPI.class, callerClass(consumer)).iterator();
        assertTrue(iterator.hasNext());
        providerPermission.set(false);
        assertThrows(ServiceConfigurationError.class, iterator::next);

        providerPermission.set(true);
        assertTrue(Util.serviceLoaderLoad(MySPI.class, callerClass(consumer))
                .iterator().hasNext());
    }

    @Test
    public void providerFactoryRechecksRegisterPermission() {
        AtomicBoolean providerPermission = new AtomicBoolean(true);
        Bundle provider = mockPermissionBundle(providerPermission);
        ProviderServiceFactory factory = new ProviderServiceFactory(
                org.apache.aries.spifly.impl3.MySPIImpl3.class,
                provider, MySPI.class.getName());

        assertNotNull(factory.getService(null, null));
        providerPermission.set(false);
        assertSame(null, factory.getService(null, null));
    }

    private BaseActivator newActivator() {
        BaseActivator activator = new BaseActivator() {
            @Override
            public void start(BundleContext context) throws Exception {
            }
        };
        BaseActivator.activator = activator;
        return activator;
    }

    @SuppressWarnings("unchecked")
    private Class<Object> callerClass(Bundle consumer) throws Exception {
        URL callerJar = getClass().getResource("/embedded3.jar");
        assertNotNull("precondition", callerJar);
        return (Class<Object>) new TestBundleClassLoader(
                new URL[] {callerJar}, getClass().getClassLoader(), consumer)
                .loadClass("org.apache.aries.spifly.testpkg.TestClass");
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Bundle mockProviderBundle(long bundleId, URL providerJar) throws Exception {
        return mockProviderBundle(bundleId, providerJar, new AtomicBoolean(true));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Bundle mockProviderBundle(long bundleId, URL providerJar,
            final AtomicBoolean permission) throws Exception {
        Bundle providerBundle = EasyMock.createMock(Bundle.class);
        final ClassLoader providerCL = new TestBundleClassLoader(
                new URL[] {providerJar}, getClass().getClassLoader(), providerBundle);
        BundleWiring providerWiring = EasyMock.createNiceMock(BundleWiring.class);
        EasyMock.expect(providerWiring.getClassLoader()).andReturn(providerCL).anyTimes();
        EasyMock.replay(providerWiring);
        BundleRevision providerRevision = EasyMock.createNiceMock(BundleRevision.class);
        EasyMock.expect(providerRevision.getWiring()).andReturn(providerWiring).anyTimes();
        EasyMock.replay(providerRevision);
        Bundle systemBundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(systemBundle.loadClass(BundleRevision.class.getName()))
                .andReturn((Class) BundleRevision.class).anyTimes();
        EasyMock.expect(systemBundle.loadClass(BundleWiring.class.getName()))
                .andReturn((Class) BundleWiring.class).anyTimes();
        EasyMock.replay(systemBundle);
        BundleContext providerContext = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(providerContext.getBundle(0)).andReturn(systemBundle).anyTimes();
        EasyMock.replay(providerContext);
        EasyMock.expect(providerBundle.getBundleContext())
                .andReturn(providerContext).anyTimes();
        EasyMock.expect(providerBundle.adapt(BundleRevision.class))
                .andReturn(providerRevision).anyTimes();
        EasyMock.expect(providerBundle.getBundleId()).andReturn(bundleId).anyTimes();
        EasyMock.expect(providerBundle.hasPermission(
                EasyMock.isA(ServicePermission.class))).andAnswer(new IAnswer<Boolean>() {
                    @Override
                    public Boolean answer() throws Throwable {
                        return permission.get();
                    }
                }).anyTimes();
        EasyMock.expect(providerBundle.getEntryPaths((String) EasyMock.anyObject()))
                .andReturn(null).anyTimes();
        Dictionary<String, String> providerHeaders = new Hashtable<String, String>();
        providerHeaders.put(Constants.BUNDLE_CLASSPATH, ".,provider.jar");
        EasyMock.expect(providerBundle.getHeaders()).andReturn(providerHeaders).anyTimes();
        EasyMock.expect(providerBundle.getResource("provider.jar"))
                .andReturn(providerJar).anyTimes();
        providerBundle.loadClass((String) EasyMock.anyObject());
        EasyMock.expectLastCall().andAnswer(new IAnswer<Class<?>>() {
            @Override
            public Class<?> answer() throws Throwable {
                return providerCL.loadClass((String) EasyMock.getCurrentArguments()[0]);
            }
        }).anyTimes();
        EasyMock.replay(providerBundle);
        return providerBundle;
    }

    private Bundle mockPermissionBundle(final AtomicBoolean permission) {
        Bundle bundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(bundle.hasPermission(EasyMock.isA(ServicePermission.class)))
                .andAnswer(new IAnswer<Boolean>() {
                    @Override
                    public Boolean answer() throws Throwable {
                        return permission.get();
                    }
                }).anyTimes();
        EasyMock.replay(bundle);
        return bundle;
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
