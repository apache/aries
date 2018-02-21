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
package org.apache.aries.jndi.startup;

import org.apache.aries.jndi.*;
import org.apache.aries.jndi.spi.AugmenterInvoker;
import org.apache.aries.jndi.tracker.CachingServiceTracker;
import org.apache.aries.jndi.urls.URLObjectFactoryFinder;
import org.osgi.framework.*;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.service.jndi.JNDIConstants;
import org.osgi.service.jndi.JNDIContextManager;
import org.osgi.service.jndi.JNDIProviderAdmin;
import org.osgi.util.tracker.BundleTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.spi.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The activator for this bundle makes sure the static classes in it are
 * driven so they can do their magic stuff properly.
 */
public class Activator implements BundleActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class.getName());

    private static final String DISABLE_BUILDER = "org.apache.aries.jndi.disable.builder";
    private static final String FORCE_BUILDER = "org.apache.aries.jndi.force.builder";

    private static volatile Activator instance;

    private BundleTracker<ServiceCache> bundleServiceCaches;

    private CachingServiceTracker<InitialContextFactoryBuilder> icfBuilders;
    private CachingServiceTracker<URLObjectFactoryFinder> urlObjectFactoryFinders;
    private CachingServiceTracker<InitialContextFactory> initialContextFactories;
    private CachingServiceTracker<ObjectFactory> objectFactories;

    private AugmenterInvoker augmenterInvoker;

    private InitialContextFactoryBuilder originalICFBuilder;
    private OSGiInitialContextFactoryBuilder icfBuilder;

    private ObjectFactoryBuilder originalOFBuilder;
    private OSGiObjectFactoryBuilder ofBuilder;

    public static Collection<ServiceReference<InitialContextFactoryBuilder>> getInitialContextFactoryBuilderServices() {
        return instance.icfBuilders.getReferences();
    }

    public static Collection<ServiceReference<InitialContextFactory>> getInitialContextFactoryServices() {
        return instance.initialContextFactories.getReferences();
    }

    public static Collection<ServiceReference<URLObjectFactoryFinder>> getURLObjectFactoryFinderServices() {
        return instance.urlObjectFactoryFinders.getReferences();
    }

    public static ServiceReference<ObjectFactory> getUrlFactory(String scheme) {
        return instance.objectFactories.find(scheme);
    }

    public static ServiceReference<InitialContextFactory> getInitialContextFactory(String interfaceName) {
        return instance.initialContextFactories.find(interfaceName);
    }

    public static AugmenterInvoker getAugmenterInvoker() {
        return instance.augmenterInvoker;
    }

    public static <T> T getService(BundleContext context, ServiceReference<T> ref) {
        ServiceCache cache = getServiceCache(context);
        return cache.getService(ref);
    }

    public static <T> Collection<ServiceReference<T>> getReferences(BundleContext context, Class<T> clazz) {
        ServiceCache cache = getServiceCache(context);
        return cache.getReferences(clazz);
    }

    public static <T> Iterable<T> getServices(BundleContext context, Class<T> clazz) {
        ServiceCache cache = getServiceCache(context);
        if (cache == null) {
            cache = new ServiceCache(context);
        }
        Collection<ServiceReference<T>> refs = cache.getReferences(clazz);
        return () -> Utils.map(refs.iterator(), ref -> Activator.getService(context, ref));
    }

    private static ServiceCache getServiceCache(BundleContext context) {
        ServiceCache cache = instance.bundleServiceCaches.getObject(context.getBundle());
        if (cache == null) {
            cache = new ServiceCache(context);
        }
        return cache;
    }


    public void start(BundleContext context) {
        instance = this;

        bundleServiceCaches = new BundleTracker<ServiceCache>(context, Bundle.ACTIVE, null) {
            @Override
            public ServiceCache addingBundle(Bundle bundle, BundleEvent event) {
                return new ServiceCache(bundle.getBundleContext());
            }
            @Override
            public void modifiedBundle(Bundle bundle, BundleEvent event, ServiceCache object) {
            }
            @Override
            public void removedBundle(Bundle bundle, BundleEvent event, ServiceCache object) {
                object.close();
            }
        };
        bundleServiceCaches.open();

        initialContextFactories = new CachingServiceTracker<>(context, InitialContextFactory.class, Activator::getInitialContextFactoryInterfaces);
        objectFactories = new CachingServiceTracker<>(context, ObjectFactory.class, Activator::getObjectFactorySchemes);
        icfBuilders = new CachingServiceTracker<>(context, InitialContextFactoryBuilder.class);
        urlObjectFactoryFinders = new CachingServiceTracker<>(context, URLObjectFactoryFinder.class);

        if (!disableBuilder(context)) {
            try {
                OSGiInitialContextFactoryBuilder builder = new OSGiInitialContextFactoryBuilder();
                try {
                    NamingManager.setInitialContextFactoryBuilder(builder);
                } catch (IllegalStateException e) {
                    // use reflection to force the builder to be used
                    if (forceBuilder(context)) {
                        originalICFBuilder = swapStaticField(InitialContextFactoryBuilder.class, builder);
                    }
                }
                icfBuilder = builder;
            } catch (NamingException e) {
                LOGGER.debug("A failure occurred when attempting to register an InitialContextFactoryBuilder with the NamingManager. " +
                        "Support for calling new InitialContext() will not be enabled.", e);
            } catch (IllegalStateException e) {
                // Log the problem at info level, but only log the exception at debug level, as in many cases this is not a real issue and people
                // don't want to see stack traces at info level when everything it working as expected.
                String msg = "It was not possible to register an InitialContextFactoryBuilder with the NamingManager because " +
                        "another builder called " + getClassName(InitialContextFactoryBuilder.class) + " was already registered. Support for calling new InitialContext() will not be enabled.";
                LOGGER.info(msg);
                LOGGER.debug(msg, e);
            }

            try {
                OSGiObjectFactoryBuilder builder = new OSGiObjectFactoryBuilder(context);
                try {
                    NamingManager.setObjectFactoryBuilder(builder);
                } catch (IllegalStateException e) {
                    // use reflection to force the builder to be used
                    if (forceBuilder(context)) {
                        originalOFBuilder = swapStaticField(ObjectFactoryBuilder.class, builder);
                    }
                }
                ofBuilder = builder;
            } catch (NamingException e) {
                LOGGER.info("A failure occurred when attempting to register an ObjectFactoryBuilder with the NamingManager. " +
                        "Looking up certain objects may not work correctly.", e);
            } catch (IllegalStateException e) {
                // Log the problem at info level, but only log the exception at debug level, as in many cases this is not a real issue and people
                // don't want to see stack traces at info level when everything it working as expected.
                String msg = "It was not possible to register an ObjectFactoryBuilder with the NamingManager because " +
                        "another builder called " + getClassName(ObjectFactoryBuilder.class) + " was already registered. Looking up certain objects may not work correctly.";
                LOGGER.info(msg);
                LOGGER.debug(msg, e);
            }
        }

        context.registerService(JNDIProviderAdmin.class.getName(),
                new ProviderAdminServiceFactory(context),
                null);

        context.registerService(InitialContextFactoryBuilder.class.getName(),
                new JREInitialContextFactoryBuilder(),
                null);

        context.registerService(JNDIContextManager.class.getName(),
                new ContextManagerServiceFactory(),
                null);

        context.registerService(AugmenterInvoker.class.getName(),
                augmenterInvoker = new AugmenterInvokerImpl(context),
                null);
    }

    public void stop(BundleContext context) {
        bundleServiceCaches.close();

        /*
         * Try to reset the InitialContextFactoryBuilder and ObjectFactoryBuilder
         * on the NamingManager.
         */
        if (icfBuilder != null) {
            swapStaticField(InitialContextFactoryBuilder.class, originalICFBuilder);
        }
        if (ofBuilder != null) {
            swapStaticField(ObjectFactoryBuilder.class, originalOFBuilder);
        }

        icfBuilders.close();
        urlObjectFactoryFinders.close();
        objectFactories.close();
        initialContextFactories.close();

        instance = null;
    }

    private boolean forceBuilder(BundleContext context) {
        String forceBuilderProp = context.getProperty(FORCE_BUILDER);
        if (forceBuilderProp != null) {
            return !"false".equals(forceBuilderProp) && !"no".equals(forceBuilderProp);
        }
        BundleRevision revision = context.getBundle().adapt(BundleRevision.class);
        return !(revision.getDeclaredCapabilities(FORCE_BUILDER).isEmpty());
    }

    private boolean disableBuilder(BundleContext context) {
        String disableBuilder = context.getProperty(DISABLE_BUILDER);
        if (disableBuilder != null) {
            return !"false".equals(disableBuilder) && !"no".equals(disableBuilder);
        }
        BundleRevision revision = context.getBundle().adapt(BundleRevision.class);
        return !(revision.getDeclaredCapabilities(DISABLE_BUILDER).isEmpty());
    }

    private String getClassName(Class<?> expectedType) {
        try {
            for (Field field : NamingManager.class.getDeclaredFields()) {
                if (expectedType.equals(field.getType())) {
                    field.setAccessible(true);
                    Object icf = field.get(null);
                    return icf.getClass().getName();
                }
            }
        } catch (Throwable t) {
            // Ignore
        }
        return "";
    }

    /*
     * There are no public API to reset the InitialContextFactoryBuilder or
     * ObjectFactoryBuilder on the NamingManager so try to use reflection.
     */
    private static <T> T swapStaticField(Class<T> expectedType, Object value) throws IllegalStateException {
        try {
            for (Field field : NamingManager.class.getDeclaredFields()) {
                if (expectedType.equals(field.getType())) {
                    field.setAccessible(true);
                    T original = expectedType.cast(field.get(null));
                    field.set(null, value);
                    return original;
                }
            }
        } catch (Throwable t) {
            // Ignore
            LOGGER.debug("Error setting field.", t);
            throw new IllegalStateException(t);
        }
        throw new IllegalStateException("Error setting field: no field found for type " + expectedType);
    }

    private static List<String> getInitialContextFactoryInterfaces(ServiceReference<InitialContextFactory> ref) {
        String[] interfaces = (String[]) ref.getProperty(Constants.OBJECTCLASS);
        List<String> resultList = new ArrayList<>();
        for (String interfaceName : interfaces) {
            if (!InitialContextFactory.class.getName().equals(interfaceName)) {
                resultList.add(interfaceName);
            }
        }

        return resultList;
    }

    private static List<String> getObjectFactorySchemes(ServiceReference<ObjectFactory> reference) {
        Object scheme = reference.getProperty(JNDIConstants.JNDI_URLSCHEME);
        List<String> result;

        if (scheme instanceof String) {
            result = new ArrayList<>();
            result.add((String) scheme);
        } else if (scheme instanceof String[]) {
            result = Arrays.asList((String[]) scheme);
        } else {
            result = Collections.emptyList();
        }

        return result;
    }

    private static class ServiceCache {

        private final BundleContext context;
        private final Map<ServiceReference<?>, Object> cache = new ConcurrentHashMap<>();
        private final Map<Class<?>, CachingServiceTracker<?>> trackers = new ConcurrentHashMap<>();

        ServiceCache(BundleContext context) {
            this.context = context;
        }

        @SuppressWarnings("unchecked")
        <T> T getService(ServiceReference<T> ref) {
            return (T) cache.computeIfAbsent(ref, this::doGetService);
        }

        @SuppressWarnings("unchecked")
        <T> Collection<ServiceReference<T>> getReferences(Class<T> clazz) {
            return (List) trackers.computeIfAbsent(clazz, c -> new CachingServiceTracker<>(context, c)).getReferences();
        }

        void close() {
            cache.forEach(this::doUngetService);
        }

        Object doGetService(ServiceReference<?> ref) {
            return Utils.doPrivileged(() -> context.getService(ref));
        }

        void doUngetService(ServiceReference<?> ref, Object svc) {
            Utils.doPrivileged(() -> context.ungetService(ref));
        }
    }

}
