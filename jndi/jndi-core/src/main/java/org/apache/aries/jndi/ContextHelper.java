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

import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.ObjectFactory;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jndi.JNDIConstants;

/**
 * Provides helper methods for the DelegateContext. This provides the methods so
 * there can be many DelegateContexts, but few service trackers.
 */
public final class ContextHelper {

    /** Ensure no one constructs us */
    private ContextHelper() {
        throw new RuntimeException();
    }

    public static Context createURLContext(final BundleContext context,
                                           final String urlScheme, 
                                           final Hashtable<?, ?> env)
        throws NamingException {
        return Utils.doPrivilegedNaming(new PrivilegedExceptionAction<Context>() {
            public Context run() throws Exception {
                return doCreateURLContext(context, urlScheme, env);
            }
        });
    }
    
    /**
     * This method is used to create a URL Context. It does this by looking for
     * the URL context's ObjectFactory in the service registry.
     * 
     * @param urlScheme
     * @param env
     * @return a Context
     * @throws NamingException
     */
    private static Context doCreateURLContext(BundleContext context, String urlScheme, Hashtable<?, ?> env)
        throws NamingException {
        ServiceReference ref = null;
        try {
            ServiceReference[] services = context.getServiceReferences(ObjectFactory.class.getName(), 
                                                                       "(" + JNDIConstants.JNDI_URLSCHEME + "=" + urlScheme.trim() + ")");

            if (services != null) {
                ref = services[0];
            }
        } catch (InvalidSyntaxException e1) {
            NamingException e = new NamingException("Argh this should never happen :)");
            e.initCause(e1);
            throw e;
        }

        Context ctx = null; 
        
        if (ref != null) {
            ObjectFactory factory = (ObjectFactory) context.getService(ref);
            try {
                return (Context) factory.getObjectInstance(null, null, null, env);
            } catch (Exception e) {
                NamingException e2 = new NamingException();
                e2.initCause(e);
                throw e2;
            } finally {
                if (ref != null) {
                    context.ungetService(ref);
                }
            }
        }

        return ctx;
    }
        
    public static Context getInitialContext(BundleContext context, Hashtable<?, ?> environment)
        throws NamingException {
        ContextProvider provider = getContextProvider(context, environment);
        String contextFactoryClass = (String) environment.get(Context.INITIAL_CONTEXT_FACTORY);
        if (contextFactoryClass == null) {
            if (provider == null) {
                return new DelegateContext(context, environment);
            } else {
                return new DelegateContext(context, provider);
            }
        } else {
            if (provider == null) {
                throw new NoInitialContextException("We could not find an InitialContextFactory to use");
            } else {
                return new DelegateContext(context, provider);
            }
        }
    }

    public static ContextProvider getContextProvider(final BundleContext context,
                                                     final Hashtable<?, ?> environment)
        throws NamingException {
        return Utils.doPrivilegedNaming(new PrivilegedExceptionAction<ContextProvider>() {
            public ContextProvider run() throws Exception {
                return doGetContextProvider(context, environment);
            }
        });
    }
    
    private static ContextProvider doGetContextProvider(BundleContext context,
                                                        Hashtable<?, ?> environment)
        throws NamingException {
        ContextProvider provider = null;
        String contextFactoryClass = (String) environment.get(Context.INITIAL_CONTEXT_FACTORY);
        if (contextFactoryClass == null) {
            // 1. get ContextFactory using builder
            provider = getInitialContextUsingBuilder(context, environment);

            // 2. lookup all ContextFactory services
            if (provider == null) {
                String filter = "(&(objectClass=javax.naming.spi.InitialContextFactory))";
                ServiceReference[] references = null;
                try {
                    references = context.getAllServiceReferences(InitialContextFactory.class.getName(), filter);
                } catch (InvalidSyntaxException e) {
                    NamingException ex = new NamingException("Bad filter: " + filter);
                    ex.initCause(e);
                    throw ex;
                }
                if (references != null) {
                    Context initialContext = null;
                    Arrays.sort(references, Utils.SERVICE_REFERENCE_COMPARATOR);
                    for (ServiceReference reference : references) {
                        InitialContextFactory factory = (InitialContextFactory) context.getService(reference);
                        try {
                            initialContext = factory.getInitialContext(environment);
                        } finally {
                            context.ungetService(reference);
                        }
                        if (initialContext != null) {
                            provider = new ContextProvider(reference, initialContext);
                            break;
                        }
                    }
                }
            }
        } else {
            // 1. lookup ContextFactory using the factory class
            String filter = "(&(objectClass=javax.naming.spi.InitialContextFactory)(objectClass="+ contextFactoryClass + "))";
            ServiceReference[] references = null;
            try {
                references = context.getServiceReferences(InitialContextFactory.class.getName(), filter);
            } catch (InvalidSyntaxException e) {
                NamingException ex = new NamingException("Bad filter: " + filter);
                ex.initCause(e);
                throw ex;
            }

            if (references != null && references.length > 0) {
                Context initialContext = null;
                Arrays.sort(references, Utils.SERVICE_REFERENCE_COMPARATOR);
                ServiceReference reference = references[0];
                InitialContextFactory factory = (InitialContextFactory) context.getService(reference);
                try {
                    initialContext = factory.getInitialContext(environment);
                } finally {
                    context.ungetService(reference);
                }
                if (initialContext != null) {
                    provider = new ContextProvider(reference, initialContext);                    
                }
            }

            // 2. get ContextFactory using builder
            if (provider == null) {
                provider = getInitialContextUsingBuilder(context, environment);
            }
        }
        
        return provider;
    }

    private static ContextProvider getInitialContextUsingBuilder(BundleContext context,
                                                                 Hashtable<?, ?> environment)
            throws NamingException {
        ContextProvider provider = null;
        try {
            ServiceReference[] refs = context.getAllServiceReferences(InitialContextFactoryBuilder.class.getName(), null);
            if (refs != null) {
                InitialContextFactory factory = null;
                Arrays.sort(refs, Utils.SERVICE_REFERENCE_COMPARATOR);
                for (ServiceReference ref : refs) {                    
                    InitialContextFactoryBuilder builder = (InitialContextFactoryBuilder) context.getService(ref);
                    try {
                        factory = builder.createInitialContextFactory(environment);
                    } catch (NamingException e) {
                        // TODO: log
                        // ignore
                    } finally {
                        context.ungetService(ref);
                    }
                    if (factory != null) {
                        provider = new ContextProvider(ref, factory.getInitialContext(environment));
                        break;
                    }
                }
            }
        } catch (InvalidSyntaxException e) {
            // ignore - should never happen
        }
        return provider;
    }
    
    public static class ContextProvider {
        
        ServiceReference reference;
        Context context;
        
        public ContextProvider(ServiceReference reference, Context context) {
            this.reference = reference;
            this.context = context;
        }        
        
        public boolean isValid() {
            return (reference.getBundle() != null);
        }
    }

}
