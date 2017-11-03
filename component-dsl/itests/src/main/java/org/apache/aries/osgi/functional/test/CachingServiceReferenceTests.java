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

import org.apache.aries.osgi.functional.CachingServiceReference;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Carlos Sierra Andrés
 */
public class CachingServiceReferenceTests {
    private static BundleContext bundleContext = FrameworkUtil.getBundle(
        CachingServiceReferenceTests.class).getBundleContext();

    @Test
    public void testComparable() {
        ServiceRegistration<Service> serviceRegistrationNoRanking =
            bundleContext.registerService(
                Service.class, new Service(),
                new Hashtable<String, Object>() {{
                }});

        ServiceRegistration<Service> serviceRegistrationOne =
            bundleContext.registerService(
                Service.class, new Service(),
                new Hashtable<String, Object>() {{
                    put("service.ranking", 1);
                }});

        ServiceRegistration<Service> serviceRegistrationMinusOne =
            bundleContext.registerService(
                Service.class, new Service(),
                new Hashtable<String, Object>() {{
                    put("service.ranking", -1);
                }});

        try {
            TreeSet<CachingServiceReference<?>> set = new TreeSet<>();

            CachingServiceReference<Service> noRanking =
                new CachingServiceReference<>(
                    serviceRegistrationNoRanking.getReference());

            set.add(noRanking);

            CachingServiceReference<Service> rankingOne =
                new CachingServiceReference<>(
                    serviceRegistrationOne.getReference());

            set.add(rankingOne);

            CachingServiceReference<Service> minusOne =
                new CachingServiceReference<>(
                    serviceRegistrationMinusOne.getReference());

            set.add(minusOne);

            CachingServiceReference<?> first = set.first();

            assertTrue(first.equals(minusOne));
            CachingServiceReference<?> second = set.higher(first);
            assertTrue(second.equals(noRanking));
            CachingServiceReference<?> third = set.higher(second);
            assertTrue(third.equals(rankingOne));
        }
        finally {
            serviceRegistrationMinusOne.unregister();
            serviceRegistrationNoRanking.unregister();
            serviceRegistrationOne.unregister();
        }
    }

    @Test
    public void testCachingNullProperties() {
        ServiceRegistration<Service> serviceRegistration =
            bundleContext.registerService(
                Service.class, new Service(),
                new Hashtable<String, Object>() {{
                }});

        try {
            ServiceReference<Service> serviceReference =
                serviceRegistration.getReference();

            CachingServiceReference<Service> cachingServiceReference =
                new CachingServiceReference<>(serviceReference);

            assertEquals(
                serviceReference.getProperty("property"),
                cachingServiceReference.getProperty("property"));

            serviceRegistration.setProperties(
                new Hashtable<String, Object>() {{
                    put("property", "value2");
                }}
            );

            assertEquals(serviceReference.getProperty("property"), "value2");

            assertNull(cachingServiceReference.getProperty("property"));

            assertTrue(cachingServiceReference.isDirty());
        }
        finally {
            serviceRegistration.unregister();
        }
    }

    @Test
    public void testNullPropertiesKeysAreNotReturned() {
        ServiceRegistration<Service> serviceRegistration =
            bundleContext.registerService(
                Service.class, new Service(),
                new Hashtable<String, Object>() {{
                }});

        try {
            ServiceReference<Service> serviceReference =
                serviceRegistration.getReference();

            CachingServiceReference<Service> cachingServiceReference =
                new CachingServiceReference<>(serviceReference);

            assertNull(serviceReference.getProperty("property"));

            assertEquals(
                serviceReference.getProperty("property"),
                cachingServiceReference.getProperty("property"));

            List<String> properties = Arrays.asList(
                cachingServiceReference.getPropertyKeys());

            assertFalse(properties.contains("property"));

            serviceRegistration.setProperties(
                new Hashtable<String, Object>() {{
                    put("property", "value2");
                }}
            );

            assertEquals(serviceReference.getProperty("property"), "value2");

            properties = Arrays.asList(cachingServiceReference.getPropertyKeys());

            assertFalse(properties.contains("property"));
        }
        finally {
            serviceRegistration.unregister();
        }
    }

    @Test
    public void testCachingProperties() {
        ServiceRegistration<Service> serviceRegistration =
            bundleContext.registerService(
                Service.class, new Service(),
                new Hashtable<String, Object>() {{
                    put("property", "value");
                }});

        try {
            ServiceReference<Service> serviceReference =
                serviceRegistration.getReference();

            CachingServiceReference<Service> cachingServiceReference =
                new CachingServiceReference<>(serviceReference);

            assertEquals(
                serviceReference.getProperty("property"),
                cachingServiceReference.getProperty("property"));

            serviceRegistration.setProperties(
                new Hashtable<String, Object>() {{
                    put("property", "value2");
                }}
            );

            assertEquals(serviceReference.getProperty("property"), "value2");

            assertEquals(cachingServiceReference.getProperty("property"), "value");
        }
        finally {
            serviceRegistration.unregister();
        }

    }

    @Test
    public void testCachingPropertiesBecomingNull() {
        ServiceRegistration<Service> serviceRegistration =
            bundleContext.registerService(
                Service.class, new Service(),
                new Hashtable<String, Object>() {{
                    put("property", "value");
                }});

        try {
            ServiceReference<Service> serviceReference =
                serviceRegistration.getReference();

            String[] propertyKeys = serviceReference.getPropertyKeys();

            Arrays.sort(propertyKeys);

            CachingServiceReference<Service> cachingServiceReference =
                new CachingServiceReference<>(serviceReference);

            assertEquals(
                serviceReference.getProperty("property"),
                cachingServiceReference.getProperty("property"));

            serviceRegistration.setProperties(
                new Hashtable<String, Object>() {{
                }}
            );

            assertNull(serviceReference.getProperty("property"));

            assertEquals("value", cachingServiceReference.getProperty("property"));

            assertTrue(cachingServiceReference.isDirty());

            String[] cachedPropertyKeys = cachingServiceReference.getPropertyKeys();

            Arrays.sort(cachedPropertyKeys);

            assertTrue(Arrays.equals(propertyKeys, cachedPropertyKeys));
        }
        finally {
            serviceRegistration.unregister();
        }
    }

    @Test
    public void testIsDirty() {
        ServiceRegistration<Service> serviceRegistration =
            bundleContext.registerService(
                Service.class, new Service(),
                new Hashtable<String, Object>() {{
                    put("property", "value");
                }});

        try {
            ServiceReference<Service> serviceReference =
                serviceRegistration.getReference();

            CachingServiceReference<Service> cachingServiceReference =
                new CachingServiceReference<>(serviceReference);

            assertEquals(
                serviceReference.getProperty("property"),
                cachingServiceReference.getProperty("property"));

            serviceRegistration.setProperties(
                new Hashtable<String, Object>() {{
                    put("property", "value2");
                }}
            );

            assertTrue(cachingServiceReference.isDirty());
        }
        finally {
            serviceRegistration.unregister();
        }
    }

    @Test
    public void testIsNotDirty() {
        ServiceRegistration<Service> serviceRegistration =
            bundleContext.registerService(
                Service.class, new Service(),
                new Hashtable<String, Object>() {{
                    put("property", "value");
                }});
        try {
            ServiceReference<Service> serviceReference =
                serviceRegistration.getReference();

            CachingServiceReference<Service> cachingServiceReference =
                new CachingServiceReference<>(serviceReference);

            serviceRegistration.setProperties(
                new Hashtable<String, Object>() {{
                    put("property", "value2");
                }}
            );

            assertFalse(cachingServiceReference.isDirty());
        }
        finally {
            serviceRegistration.unregister();
        }
    }

    @Test
    public void testIsDirtyDoesNotCache() {
        ServiceRegistration<Service> serviceRegistration =
            bundleContext.registerService(
                Service.class, new Service(),
                new Hashtable<String, Object>() {{
                    put("property", "value");
                }});
        try {
            ServiceReference<Service> serviceReference =
                serviceRegistration.getReference();

            CachingServiceReference<Service> cachingServiceReference =
                new CachingServiceReference<>(serviceReference);

            serviceRegistration.setProperties(
                new Hashtable<String, Object>() {{
                    put("property", "value2");
                }}
            );

            assertFalse(cachingServiceReference.isDirty("property"));

            assertEquals(
                "value2", cachingServiceReference.getProperty("property"));

            assertFalse(cachingServiceReference.isDirty("property"));

            serviceRegistration.setProperties(
                new Hashtable<String, Object>() {{
                    put("property", "value3");
                }}
            );

            assertTrue(cachingServiceReference.isDirty("property"));
        }
        finally {
            serviceRegistration.unregister();
        }
    }

    @Test
    public void testIsDirtyNullProperty() {
        ServiceRegistration<Service> serviceRegistration =
            bundleContext.registerService(
                Service.class, new Service(),
                new Hashtable<String, Object>() {{
                }});
        try {
            ServiceReference<Service> serviceReference =
                serviceRegistration.getReference();

            CachingServiceReference<Service> cachingServiceReference =
                new CachingServiceReference<>(serviceReference);

            assertFalse(cachingServiceReference.isDirty("nonExisting"));
        }
        finally {
            serviceRegistration.unregister();
        }
    }


    @Test
    public void testPropertiesAreCachedWhenQueried() {
        ServiceRegistration<Service> serviceRegistration =
            bundleContext.registerService(
                Service.class, new Service(),
                new Hashtable<String, Object>() {{
                    put("property", "value");
                }});
        try {
            ServiceReference<Service> serviceReference =
                serviceRegistration.getReference();

            CachingServiceReference<Service> cachingServiceReference =
                new CachingServiceReference<>(serviceReference);

            serviceRegistration.setProperties(
                new Hashtable<String, Object>() {{
                    put("property", "value2");
                }}
            );

            assertEquals(
                serviceReference.getProperty("property"),
                cachingServiceReference.getProperty("property"));
        }
        finally {
            serviceRegistration.unregister();
        }
    }



    private class Service {}

}
