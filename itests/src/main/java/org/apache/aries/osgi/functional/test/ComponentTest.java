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
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.osgi.util.tracker.ServiceTracker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.apache.aries.osgi.functional.OSGi.bundleContext;
import static org.apache.aries.osgi.functional.OSGi.configurations;
import static org.apache.aries.osgi.functional.OSGi.just;
import static org.apache.aries.osgi.functional.OSGi.onClose;
import static org.apache.aries.osgi.functional.OSGi.register;
import static org.apache.aries.osgi.functional.OSGi.services;
import static org.apache.aries.osgi.functional.test.HighestRankingRouter.highest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Carlos Sierra Andrés
 */
public class ComponentTest {

    static BundleContext _bundleContext =
        FrameworkUtil.getBundle(DSLTest.class).getBundleContext();
    private static ConfigurationAdmin _configurationAdmin;
    private static ServiceReference<ConfigurationAdmin> _configAdminServiceReference;

    @BeforeClass
    public static void setupClass() {
        _configAdminServiceReference = _bundleContext.getServiceReference(
            ConfigurationAdmin.class);

        _configurationAdmin = _bundleContext.getService(
            _configAdminServiceReference);
    }

    @AfterClass
    public static void tearDownClass() {
        _bundleContext.ungetService(_configAdminServiceReference);
    }

    @Test
    public void testComponent() {
        OSGi<?> program =
            configurations("org.components.MyComponent").flatMap(props ->
            services(Service.class).flatMap(ms ->
            just(new Component(props, ms)).flatMap(component ->
            register(Component.class, component, new HashMap<>()).distribute(
            ign -> dynamic(
                highestService(ServiceOptional.class),
                component::setOptional, c -> component.setOptional(null)),
            ign -> dynamic(
                services(ServiceForList.class),
                component::addService, component::removeService)
        ))));

        ServiceTracker<Component, Component> serviceTracker =
            new ServiceTracker<>(_bundleContext, Component.class, null);

        serviceTracker.open();

        CountDownLatch countDownLatch = new CountDownLatch(1);

        _bundleContext.registerService(
            ManagedService.class, dictionary -> countDownLatch.countDown(),
            new Hashtable<String, Object>() {{
                put("service.pid", "org.components.MyComponent");
            }});

        try (OSGiResult<?> run = program.run(_bundleContext)) {
            Configuration factoryConfiguration =
                _configurationAdmin.createFactoryConfiguration(
                    "org.components.MyComponent");
            factoryConfiguration.update(new Hashtable<>());

            countDownLatch.await(10, TimeUnit.SECONDS);

            assertNull(serviceTracker.getService());

            ServiceRegistration<Service> serviceRegistration =
                _bundleContext.registerService(
                    Service.class, new Service(), new Hashtable<>());

            Component component = serviceTracker.waitForService(10 * 1000);

            assertNotNull(component);

            assertNull(component.getOptional());

            ServiceRegistration<ServiceOptional> serviceRegistration2 =
                _bundleContext.registerService(
                    ServiceOptional.class, new ServiceOptional(),
                    new Hashtable<>());

            Thread.sleep(1000L);

            assertNotNull(component.getOptional());

            ServiceOptional serviceOptional = new ServiceOptional();

            ServiceRegistration<ServiceOptional> serviceRegistration3 =
                _bundleContext.registerService(
                    ServiceOptional.class, serviceOptional,
                    new Hashtable<String, Object>() {{
                        put("service.ranking", 1);
                    }});

            assertEquals(serviceOptional, component.getOptional());

            serviceRegistration3.unregister();

            assertNotNull(component.getOptional());

            serviceRegistration2.unregister();

            assertNull(component.getOptional());

            ServiceRegistration<ServiceForList> serviceRegistration4 =
                _bundleContext.registerService(
                    ServiceForList.class, new ServiceForList(),
                    new Hashtable<>());

            ServiceRegistration<ServiceForList> serviceRegistration5 =
                _bundleContext.registerService(
                    ServiceForList.class, new ServiceForList(),
                    new Hashtable<>());

            assertEquals(2, component.getServiceForLists().size());

            serviceRegistration4.unregister();

            assertEquals(1, component.getServiceForLists().size());

            serviceRegistration5.unregister();

            assertEquals(0, component.getServiceForLists().size());

            serviceRegistration.unregister();

            assertNull(serviceTracker.getService());
        }
        catch (IOException ioe) {

        }
        catch (InterruptedException e) {
            Assert.fail("Timeout waiting for configuration");
        }
        finally {
            serviceTracker.close();
        }
    }

    private static <T> OSGi<T> highestService(Class<T> clazz) {
        return highest(clazz).flatMap(sr ->
        bundleContext().flatMap(bc ->
        onClose(() -> bc.ungetService(sr)).then(
        just(bc.getService(sr))
        )));
    }

    public static <T> OSGi<Void> dynamic(
        OSGi<T> program, Consumer<T> bind, Consumer<T> unbind) {

        return program.foreach(bind, unbind);
    }

    private class Component {

        final Dictionary<String, ?> configuration;
        final Service mandatory;
        private final ArrayList<ServiceForList> _serviceForLists;
        ServiceOptional optional = null;

        public Component(Dictionary<String, ?> configuration, Service mandatory) {
            this.configuration = configuration;
            this.mandatory = mandatory;

            _serviceForLists = new ArrayList<>();
        }

        public Dictionary<String, ?> getConfiguration() {
            return configuration;
        }

        public Service getMandatory() {
            return mandatory;
        }

        public ServiceOptional getOptional() {
            return optional;
        }

        public void setOptional(ServiceOptional optional) {
            this.optional = optional;
        }

        public void addService(ServiceForList serviceForList) {
            _serviceForLists.add(serviceForList);
        }

        public void removeService(ServiceForList serviceForList) {
            _serviceForLists.remove(serviceForList);
        }

        public ArrayList<ServiceForList> getServiceForLists() {
            return _serviceForLists;
        }

    }

    private class Service {}

    private class ServiceOptional {}

    private class ServiceForList {}
}
