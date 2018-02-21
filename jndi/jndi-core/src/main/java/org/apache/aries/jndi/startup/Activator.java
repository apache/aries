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
import org.apache.aries.jndi.spi.EnvironmentAugmentation;
import org.apache.aries.jndi.spi.EnvironmentUnaugmentation;
import org.apache.aries.jndi.tracker.ServiceTrackerCustomizers;
import org.apache.aries.jndi.urls.URLObjectFactoryFinder;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.service.jndi.JNDIContextManager;
import org.osgi.service.jndi.JNDIProviderAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.spi.*;
import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * The activator for this bundle makes sure the static classes in it are
 * driven so they can do their magic stuff properly.
 */
public class Activator implements BundleActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class.getName());

    private static String FORCE_BUILDER = "org.apache.aries.jndi.force.builder";
    private static InitialContextFactoryBuilder originalICFBuilder;
    private static ObjectFactoryBuilder originalOFBuilder;
    private static volatile ServiceTracker<InitialContextFactoryBuilder, ServiceReference<InitialContextFactoryBuilder>> icfBuilders;
    private static volatile ServiceTracker<URLObjectFactoryFinder, ServiceReference<URLObjectFactoryFinder>> urlObjectFactoryFinders;
    private static volatile ServiceTracker<InitialContextFactory, ServiceReference<InitialContextFactory>> initialContextFactories;
    private static volatile ServiceTracker<ObjectFactory, ServiceReference<ObjectFactory>> objectFactories;
    private static volatile ServiceTracker<EnvironmentAugmentation, ServiceReference<EnvironmentAugmentation>> environmentAugmentors;
    private static volatile ServiceTracker<EnvironmentUnaugmentation, ServiceReference<EnvironmentUnaugmentation>> environmentUnaugmentors;
    private OSGiInitialContextFactoryBuilder icfBuilder;
    private OSGiObjectFactoryBuilder ofBuilder;

    /*
     * There are no public API to reset the InitialContextFactoryBuilder or
     * ObjectFactoryBuilder on the NamingManager so try to use reflection.
     */
    private static void setField(Class<?> expectedType, Object value, boolean saveOriginal) throws IllegalStateException {
        try {
            for (Field field : NamingManager.class.getDeclaredFields()) {
                if (expectedType.equals(field.getType())) {
                    field.setAccessible(true);
                    if (saveOriginal) {
                        if (expectedType.equals(InitialContextFactoryBuilder.class)) {
                            originalICFBuilder = (InitialContextFactoryBuilder) field.get(null);
                        } else {
                            originalOFBuilder = (ObjectFactoryBuilder) field.get(null);
                        }
                    }

                    field.set(null, value);
                }
            }
        } catch (Throwable t) {
            // Ignore
            LOGGER.debug("Error setting field.", t);
            throw new IllegalStateException(t);
        }
    }

    public static ServiceReference<InitialContextFactoryBuilder>[] getInitialContextFactoryBuilderServices() {
        ServiceReference<InitialContextFactoryBuilder>[] refs = icfBuilders.getServiceReferences();

        if (refs != null) {
            Arrays.sort(refs, Utils.SERVICE_REFERENCE_COMPARATOR);
        }

        return refs;
    }

    public static ServiceReference<InitialContextFactory>[] getInitialContextFactoryServices() {
        ServiceReference<InitialContextFactory>[] refs = initialContextFactories.getServiceReferences();

        if (refs != null) {
            Arrays.sort(refs, Utils.SERVICE_REFERENCE_COMPARATOR);
        }

        return refs;
    }

    public static ServiceReference<URLObjectFactoryFinder>[] getURLObjectFactoryFinderServices() {
        ServiceReference<URLObjectFactoryFinder>[] refs = urlObjectFactoryFinders.getServiceReferences();

        if (refs != null) {
            Arrays.sort(refs, Utils.SERVICE_REFERENCE_COMPARATOR);
        }
        return refs;
    }

    public static Object[] getEnvironmentAugmentors() {
        return environmentAugmentors.getServices();
    }

    public static Object[] getEnvironmentUnaugmentors() {
        return environmentUnaugmentors.getServices();
    }

    public void start(BundleContext context) {

        initialContextFactories = initServiceTracker(context, InitialContextFactory.class, ServiceTrackerCustomizers.ICF_CACHE);
        objectFactories = initServiceTracker(context, ObjectFactory.class, ServiceTrackerCustomizers.URL_FACTORY_CACHE);
        icfBuilders = initServiceTracker(context, InitialContextFactoryBuilder.class, ServiceTrackerCustomizers.<InitialContextFactoryBuilder>LAZY());
        urlObjectFactoryFinders = initServiceTracker(context, URLObjectFactoryFinder.class, ServiceTrackerCustomizers.<URLObjectFactoryFinder>LAZY());
        environmentAugmentors = initServiceTracker(context, EnvironmentAugmentation.class, null);
        environmentUnaugmentors = initServiceTracker(context, EnvironmentUnaugmentation.class, null);

        try {
            OSGiInitialContextFactoryBuilder builder = new OSGiInitialContextFactoryBuilder();
            try {
                NamingManager.setInitialContextFactoryBuilder(builder);
            } catch (IllegalStateException e) {
                // use reflection to force the builder to be used
                if (forceBuilder(context)) {
                    setField(InitialContextFactoryBuilder.class, builder, true);
                }
            }
            icfBuilder = builder;
        } catch (NamingException e) {
            LOGGER.debug(Utils.MESSAGES.getMessage("unable.to.set.static.ICFB"), e);
        } catch (IllegalStateException e) {
            // Log the problem at info level, but only log the exception at debug level, as in many cases this is not a real issue and people
            // don't want to see stack traces at info level when everything it working as expected.
            LOGGER.info(Utils.MESSAGES.getMessage("unable.to.set.static.ICFB.already.exists", getClassName(InitialContextFactoryBuilder.class)));
            LOGGER.debug(Utils.MESSAGES.getMessage("unable.to.set.static.ICFB.already.exists", getClassName(InitialContextFactoryBuilder.class)), e);
        }

        try {
            OSGiObjectFactoryBuilder builder = new OSGiObjectFactoryBuilder(context);
            try {
                NamingManager.setObjectFactoryBuilder(builder);
            } catch (IllegalStateException e) {
                // use reflection to force the builder to be used
                if (forceBuilder(context)) {
                    setField(ObjectFactoryBuilder.class, builder, true);
                }
            }
            ofBuilder = builder;
        } catch (NamingException e) {
            LOGGER.info(Utils.MESSAGES.getMessage("unable.to.set.static.OFB"), e);
        } catch (IllegalStateException e) {
            // Log the problem at info level, but only log the exception at debug level, as in many cases this is not a real issue and people
            // don't want to see stack traces at info level when everything it working as expected.
            LOGGER.info(Utils.MESSAGES.getMessage("unable.to.set.static.OFB.already.exists", getClassName(ObjectFactoryBuilder.class)));
            LOGGER.debug(Utils.MESSAGES.getMessage("unable.to.set.static.OFB.already.exists", getClassName(ObjectFactoryBuilder.class)), e);
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
                AugmenterInvokerImpl.getInstance(),
                null);
    }

    private boolean forceBuilder(BundleContext context) {
        String forceBuilderProp = context.getProperty(FORCE_BUILDER);
        if (forceBuilderProp != null) {
            return true;
        }
        BundleRevision revision = context.getBundle().adapt(BundleRevision.class);
        return !(revision.getDeclaredCapabilities(FORCE_BUILDER).isEmpty());
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

    private <S, T> ServiceTracker<S, T> initServiceTracker(BundleContext context,
                                                           Class<S> type, ServiceTrackerCustomizer<S, T> custom) {
        ServiceTracker<S, T> t = new ServiceTracker<S, T>(context, type, custom);
        t.open();
        return t;
    }

    public void stop(BundleContext context) {
        /*
         * Try to reset the InitialContextFactoryBuilder and ObjectFactoryBuilder
         * on the NamingManager.
         */
        if (icfBuilder != null) {
            setField(InitialContextFactoryBuilder.class, originalICFBuilder, false);
        }
        if (ofBuilder != null) {
            setField(ObjectFactoryBuilder.class, originalOFBuilder, false);
        }

        icfBuilders.close();
        urlObjectFactoryFinders.close();
        objectFactories.close();
        initialContextFactories.close();
        environmentAugmentors.close();
        environmentUnaugmentors.close();
    }
}
