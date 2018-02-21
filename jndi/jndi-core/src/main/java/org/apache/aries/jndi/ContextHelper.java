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
package org.apache.aries.jndi;

import org.apache.aries.jndi.startup.Activator;
import org.apache.aries.jndi.urls.URLObjectFactoryFinder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.ObjectFactory;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides helper methods for the DelegateContext. This provides the methods so
 * there can be many DelegateContexts, but few service trackers.
 */
public final class ContextHelper {

    private static final Logger logger = Logger.getLogger(ContextHelper.class.getName());

    /**
     * Ensure no one constructs us
     */
    private ContextHelper() {
        throw new RuntimeException();
    }

    /**
     * This method is used to create a URL Context. It does this by looking for
     * the URL context's ObjectFactory in the service registry.
     */
    public static ContextProvider createURLContext(final BundleContext context,
                                                   final String urlScheme,
                                                   final Hashtable<?, ?> env)
            throws NamingException {

        ServicePair<ObjectFactory> urlObjectFactory = getURLObjectFactory(context, urlScheme, env);

        if (urlObjectFactory != null) {
            ObjectFactory factory = urlObjectFactory.get();

            if (factory != null) {
                return new URLContextProvider(context, urlObjectFactory.getReference(), factory, env);
            }
        }

        // if we got here then we couldn't find a URL context factory so return null.
        return null;
    }

    public static ServicePair<ObjectFactory> getURLObjectFactory(final BundleContext ctx, String urlScheme, Hashtable<?, ?> environment)
            throws NamingException {
        ServicePair<ObjectFactory> result = null;

        ServiceReference<ObjectFactory> ref = Activator.getUrlFactory(urlScheme);

        if (ref == null) {

            Collection<ServiceReference<URLObjectFactoryFinder>> refs = Activator.getURLObjectFactoryFinderServices();
            for (final ServiceReference<URLObjectFactoryFinder> finderRef : refs) {
                URLObjectFactoryFinder finder = Activator.getService(ctx, finderRef);

                if (finder != null) {
                    ObjectFactory f = finder.findFactory(urlScheme, environment);

                    if (f != null) {
                        result = new ServicePair<>(ctx, finderRef, f);
                        break;
                    }
                }
            }
        } else {
            result = new ServicePair<>(ctx, ref);
        }

        return result;
    }

    public static Context getInitialContext(BundleContext context, Hashtable<?, ?> environment)
            throws NamingException {

        final Bundle jndiBundle = FrameworkUtil.getBundle(ContextHelper.class);
        // if we are outside OSGi (like in our unittests) then we would get Null back here, so just make sure we don't.
        if (jndiBundle != null) {
            BundleContext jndiBundleContext = Utils.doPrivileged(jndiBundle::getBundleContext);
            if (!jndiBundleContext.getClass().equals(context.getClass())) {
                //the context passed in must have come from a child framework
                //use the parent context instead
                context = jndiBundleContext;
            }
        }

        ContextProvider provider = getContextProvider(context, environment);

        if (provider != null) {
            return new DelegateContext(context, provider);
        } else {
            String contextFactoryClass = (String) environment.get(Context.INITIAL_CONTEXT_FACTORY);
            if (contextFactoryClass == null) {
                return new DelegateContext(context, environment);
            } else {
                throw new NoInitialContextException("Unable to find the InitialContextFactory " + contextFactoryClass + ".");
            }
        }
    }

    public static ContextProvider getContextProvider(BundleContext context,
                                                     Hashtable<?, ?> environment)
            throws NamingException {

        ContextProvider provider = null;
        String contextFactoryClass = (String) environment.get(Context.INITIAL_CONTEXT_FACTORY);
        if (contextFactoryClass == null) {
            // 1. get ContextFactory using builder
            provider = getInitialContextUsingBuilder(context, environment)
            // 2. lookup all ContextFactory services
                    .orElseGet(() -> getInitialContextUsingFactoryServices(context, environment)
                            .orElse(null));

        } else {
            // 1. lookup using specified InitialContextFactory
            ServiceReference<InitialContextFactory> ref = Activator.getInitialContextFactory(contextFactoryClass);
            if (ref != null) {
                InitialContextFactory factory = Activator.getService(context, ref);
                if (factory != null) {
                    Context initialContext = factory.getInitialContext(environment);
                    provider = new SingleContextProvider(context, ref, initialContext);
                }
            }

            // 2. get ContextFactory using builder
            if (provider == null) {
                provider = getInitialContextUsingBuilder(context, environment).orElse(null);
            }
        }

        return provider;
    }

    private static Optional<ContextProvider> getInitialContextUsingFactoryServices(BundleContext context, Hashtable<?, ?> environment) {
        for (ServiceReference<InitialContextFactory> reference : Activator.getInitialContextFactoryServices()) {
            try {
                InitialContextFactory factory = Activator.getService(context, reference);
                Context initialContext = factory.getInitialContext(environment);
                if (initialContext != null) {
                    return Optional.of(new SingleContextProvider(context, reference, initialContext));
                }
            } catch (NamingException e) {
                // ignore this, if the builder fails we want to move onto the next one
                logger.log(Level.FINE, "Exception caught", e);
            }
        }
        return Optional.empty();
    }

    private static Optional<ContextProvider> getInitialContextUsingBuilder(BundleContext context,
                                                                           Hashtable<?, ?> environment) {
        for (ServiceReference<InitialContextFactoryBuilder> ref : Activator.getInitialContextFactoryBuilderServices()) {
            InitialContextFactoryBuilder builder = Activator.getService(context, ref);
            try {
                InitialContextFactory factory = builder.createInitialContextFactory(environment);
                if (factory != null) {
                    return Optional.of(new SingleContextProvider(context, ref, factory.getInitialContext(environment)));
                }
            } catch (NamingException ne) {
                // ignore this, if the builder fails we want to move onto the next one
                logger.log(Level.FINE, "Exception caught", ne);
            } catch (NullPointerException npe) {
                logger.log(Level.SEVERE, "NPE caught in ContextHelper.getInitialContextUsingBuilder. context=" + context + " ref=" + ref);
                throw npe;
            }
        }
        return Optional.empty();
    }

}
