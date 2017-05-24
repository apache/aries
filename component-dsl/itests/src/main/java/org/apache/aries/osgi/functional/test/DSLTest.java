/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.osgi.functional.test;

import org.apache.aries.osgi.functional.OSGi;
import org.apache.aries.osgi.functional.OSGiResult;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.aries.osgi.functional.OSGi.configuration;
import static org.apache.aries.osgi.functional.OSGi.configurations;
import static org.apache.aries.osgi.functional.OSGi.just;
import static org.apache.aries.osgi.functional.OSGi.onClose;
import static org.apache.aries.osgi.functional.OSGi.register;
import static org.apache.aries.osgi.functional.OSGi.serviceReferences;
import static org.apache.aries.osgi.functional.OSGi.services;
import static org.apache.aries.osgi.functional.test.HighestRankingRouter.highest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DSLTest {

    static BundleContext bundleContext = FrameworkUtil.getBundle(
        DSLTest.class).getBundleContext();

    @Test
    public void testJust() {
        AtomicInteger atomicInteger = new AtomicInteger(0);

        OSGi<Integer> just = just(25);

        assertEquals(0, atomicInteger.get());

        try (OSGiResult<Integer> result = just.run(
            bundleContext, atomicInteger::set))
        {
            assertEquals(25, atomicInteger.get());
        }

        atomicInteger.set(0);

        OSGi<Integer> map = just(25).map(s -> s + 5);

        try (OSGiResult<Integer> result = map.run(
            bundleContext, atomicInteger::set))
        {
            assertEquals(30, atomicInteger.get());
        }

        atomicInteger.set(0);

        OSGi<Integer> flatMap = just(25).flatMap(s -> just(s + 10));

        try (OSGiResult<Integer> result = flatMap.run(
            bundleContext, atomicInteger::set))
        {
            assertEquals(35, atomicInteger.get());
        }

        atomicInteger.set(0);

        OSGi<Integer> filter = just(25).filter(s -> s % 2 == 0);

        try (OSGiResult<Integer> result = filter.run(
            bundleContext, atomicInteger::set))
        {
            assertEquals(0, atomicInteger.get());
        }

        atomicInteger.set(0);

        filter = just(25).filter(s -> s % 2 != 0);

        try (OSGiResult<Integer> result = filter.run(
            bundleContext, atomicInteger::set))
        {
            assertEquals(25, atomicInteger.get());
        }

    }

    @Test
    public void testServiceReferences() {
        AtomicReference<ServiceReference<Service>> atomicReference =
            new AtomicReference<>();

        ServiceRegistration<Service> serviceRegistration = null;

        try(
            OSGiResult<ServiceReference<Service>> osGiResult =
                serviceReferences(Service.class).
                run(bundleContext, atomicReference::set)
        ) {
            assertNull(atomicReference.get());

            serviceRegistration = bundleContext.registerService(
                Service.class, new Service(), new Hashtable<>());

            assertEquals(
                serviceRegistration.getReference(), atomicReference.get());
        }
        finally {
            if (serviceRegistration != null) {
                serviceRegistration.unregister();
            }
        }
    }

    @Test
    public void testServiceReferencesAndClose() {
        AtomicReference<ServiceReference<Service>> atomicReference =
            new AtomicReference<>();

        OSGi<ServiceReference<Service>> program =
            serviceReferences(Service.class).flatMap(ref ->
            onClose(() -> atomicReference.set(null)).
            then(just(ref))
        );

        ServiceRegistration<Service> serviceRegistration = null;

        try(
            OSGiResult<?> osGiResult = program.run(
            bundleContext, atomicReference::set)
        ) {
            assertNull(atomicReference.get());

            serviceRegistration = bundleContext.registerService(
                Service.class, new Service(), new Hashtable<>());

            assertEquals(
                serviceRegistration.getReference(), atomicReference.get());
        }
        finally {
            if (serviceRegistration != null) {
                serviceRegistration.unregister();
            }
        }

        assertNull(atomicReference.get());
    }

    @Test
    public void testConfiguration() throws IOException, InterruptedException {
        ServiceReference<ConfigurationAdmin> serviceReference =
            bundleContext.getServiceReference(ConfigurationAdmin.class);

        ConfigurationAdmin configurationAdmin = bundleContext.getService(
            serviceReference);

        AtomicReference<Dictionary<?,?>> atomicReference =
            new AtomicReference<>(null);

        Configuration configuration = null;

        CountDownLatch countDownLatch = new CountDownLatch(1);

        try(OSGiResult<Dictionary<String, ?>> result =
            configuration("test.configuration").run(
                bundleContext,
                x -> {
                    atomicReference.set(x);

                    countDownLatch.countDown();
                }))
        {
            assertNull(atomicReference.get());

            configuration = configurationAdmin.getConfiguration(
                "test.configuration");

            configuration.update(new Hashtable<>());

            countDownLatch.await(10, TimeUnit.SECONDS);

            assertNotNull(atomicReference.get());
        }
        finally {
            bundleContext.ungetService(serviceReference);

            if (configuration != null) {
                configuration.delete();
            }
        }
    }


    @Test
    public void testConfigurations() throws IOException, InterruptedException {
        ServiceReference<ConfigurationAdmin> serviceReference =
            bundleContext.getServiceReference(ConfigurationAdmin.class);

        ConfigurationAdmin configurationAdmin = bundleContext.getService(
            serviceReference);

        AtomicReference<Dictionary<?,?>> atomicReference =
            new AtomicReference<>(null);

        CountDownLatch countDownLatch = new CountDownLatch(1);

        Configuration configuration = null;

        try(OSGiResult<Dictionary<String, ?>> result =
            configurations("test.configuration").run(
                bundleContext,
                x -> {
                    atomicReference.set(x);

                    countDownLatch.countDown();
                }))
        {
            assertNull(atomicReference.get());

            configuration =
                configurationAdmin.createFactoryConfiguration(
                    "test.configuration");

            configuration.update(new Hashtable<>());

            countDownLatch.await(10, TimeUnit.SECONDS);

            assertNotNull(atomicReference.get());
        }
        finally {
            bundleContext.ungetService(serviceReference);

            if (configuration != null) {
                configuration.delete();
            }
        }
    }

    @Test
    public void testRegister() {
        assertNull(bundleContext.getServiceReference(Service.class));

        Service service = new Service();

        OSGiResult<ServiceRegistration<Service>> result = register(
            Service.class, service, new HashMap<>()).
            run(bundleContext);

        ServiceReference<Service> serviceReference =
            bundleContext.getServiceReference(Service.class);

        assertEquals(service, bundleContext.getService(serviceReference));

        result.close();

        assertNull(bundleContext.getServiceReference(Service.class));
    }

    @Test
    public void testConfigurationsAndRegistrations()
        throws InvalidSyntaxException, IOException, InterruptedException {

        ServiceReference<ConfigurationAdmin> serviceReference =
            bundleContext.getServiceReference(ConfigurationAdmin.class);

        ConfigurationAdmin configurationAdmin = bundleContext.getService(
            serviceReference);

        /*  For each factory configuration register a service with the property
            key set to the value of the property key that comes with the
            configuration */
        OSGi<ServiceRegistration<Service>> program =
            configurations("test.configuration").
                map(d -> d.get("key")).flatMap(key ->
            register(
                Service.class, new Service(),
                new HashMap<String, Object>() {{
                    put("key", key);
                }})
            );

        OSGiResult<ServiceRegistration<Service>> result = program.run(
            bundleContext);

        assertEquals(
            0,
            bundleContext.getServiceReferences(
                Service.class, "(test.configuration=*)").size());

        CountDownLatch addedLatch = new CountDownLatch(3);

        ServiceRegistration<?> addedServiceRegistration =
            bundleContext.registerService(
                ManagedServiceFactory.class,
                new ManagedServiceFactory() {
                    @Override
                    public String getName() {
                        return "";
                    }

                    @Override
                    public void updated(
                        String s, Dictionary<String, ?> dictionary)
                        throws ConfigurationException {

                        addedLatch.countDown();
                    }

                    @Override
                    public void deleted(String s) {

                    }
                },
                new Hashtable<String, Object>() {{
                    put("service.pid", "test.configuration");
                }});

        CountDownLatch deletedLatch = new CountDownLatch(3);

        ServiceRegistration<?> deletedServiceRegistration =
            bundleContext.registerService(
                ManagedServiceFactory.class,
                new ManagedServiceFactory() {
                    @Override
                    public String getName() {
                        return "";
                    }

                    @Override
                    public void updated(
                        String s, Dictionary<String, ?> dictionary)
                        throws ConfigurationException {

                    }

                    @Override
                    public void deleted(String s) {
                        deletedLatch.countDown();
                    }
                },
                new Hashtable<String, Object>() {{
                    put("service.pid", "test.configuration");
                }});

        Configuration configuration =
            configurationAdmin.createFactoryConfiguration("test.configuration");

        configuration.update(new Hashtable<String, Object>(){{
            put("key", "service one");
        }});

        Configuration configuration2 =
            configurationAdmin.createFactoryConfiguration("test.configuration");

        configuration2.update(new Hashtable<String, Object>(){{
            put("key", "service two");
        }});

        Configuration configuration3 =
            configurationAdmin.createFactoryConfiguration("test.configuration");

        configuration3.update(new Hashtable<String, Object>(){{
            put("key", "service three");
        }});

        assertTrue(addedLatch.await(10, TimeUnit.SECONDS));

        assertEquals(
            1,
            bundleContext.getServiceReferences(
                Service.class, "(key=service one)").size());
        assertEquals(
            1,
            bundleContext.getServiceReferences(
                Service.class, "(key=service two)").size());
        assertEquals(
            1,
            bundleContext.getServiceReferences(
                Service.class, "(key=service three)").size());

        configuration3.delete();

        configuration2.delete();

        configuration.delete();

        assertTrue(deletedLatch.await(10, TimeUnit.SECONDS));

        assertEquals(
            0,
            bundleContext.getServiceReferences(
                Service.class, "(test.configuration=*)").size());

        addedServiceRegistration.unregister();

        deletedServiceRegistration.unregister();

        result.close();

        bundleContext.ungetService(serviceReference);
    }

    @Test
    public void testProgrammaticDependencies() {
        AtomicBoolean executed = new AtomicBoolean(false);
        AtomicBoolean closed = new AtomicBoolean(false);

        String[] filters = {
            "(key=service one)",
            "(key=service two)",
            "(key=service three)"
        };

        OSGi<?> program =
            onClose(() -> closed.set(true)).foreach(
            ign -> executed.set(true)
        );

        for (String filter : filters) {
            program = services(filter).then(program);
        }

        try (OSGiResult<?> result = program.run(bundleContext)) {
            assertFalse(closed.get());
            assertFalse(executed.get());

            ServiceRegistration<Service> serviceRegistrationOne =
                bundleContext.registerService(
                    Service.class, new Service(),
                    new Hashtable<String, Object>() {{
                        put("key", "service one");
                    }});

            assertFalse(closed.get());
            assertFalse(executed.get());

            ServiceRegistration<Service> serviceRegistrationTwo =
                bundleContext.registerService(
                    Service.class, new Service(),
                    new Hashtable<String, Object>() {{
                        put("key", "service two");
                    }});

            assertFalse(closed.get());
            assertFalse(executed.get());

            ServiceRegistration<Service> serviceRegistrationThree =
                bundleContext.registerService(
                    Service.class, new Service(),
                    new Hashtable<String, Object>() {{
                        put("key", "service three");
                    }});

            assertFalse(closed.get());
            assertTrue(executed.get());

            serviceRegistrationOne.unregister();

            assertTrue(closed.get());

            serviceRegistrationTwo.unregister();
            serviceRegistrationThree.unregister();
        }

    }

    @Test
    public void testHighestRankingOnly() {
        AtomicReference<ServiceReference<Service>> current =
            new AtomicReference<>();

        OSGi<Void> program =
            highest(Service.class).
            foreach(current::set, sr -> current.set(null));

        assertNull(current.get());

        try (OSGiResult<Void> result = program.run(bundleContext)) {
            ServiceRegistration<Service> serviceRegistrationOne =
                bundleContext.registerService(
                    Service.class, new Service(),
                    new Hashtable<String, Object>() {{
                        put("service.ranking", 0);
                    }});

            assertEquals(serviceRegistrationOne.getReference(), current.get());

            ServiceRegistration<Service> serviceRegistrationTwo =
                bundleContext.registerService(
                    Service.class, new Service(),
                    new Hashtable<String, Object>() {{
                        put("service.ranking", 1);
                    }});

            assertEquals(serviceRegistrationTwo.getReference(), current.get());

            ServiceRegistration<Service> serviceRegistrationMinusOne =
                bundleContext.registerService(
                    Service.class, new Service(),
                    new Hashtable<String, Object>() {{
                        put("service.ranking", -1);
                    }});

            assertEquals(serviceRegistrationTwo.getReference(), current.get());

            serviceRegistrationTwo.unregister();

            assertEquals(serviceRegistrationOne.getReference(), current.get());

            serviceRegistrationOne.unregister();

            assertEquals(
                serviceRegistrationMinusOne.getReference(), current.get());

            serviceRegistrationMinusOne.unregister();
        }

    }

    private class Service {}

}
