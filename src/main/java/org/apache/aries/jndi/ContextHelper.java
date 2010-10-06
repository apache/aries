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
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.ObjectFactory;

import org.apache.aries.jndi.startup.Activator;
import org.apache.aries.jndi.tracker.ServiceTrackerCustomizers;
import org.apache.aries.jndi.urls.URLObjectFactoryFinder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Provides helper methods for the DelegateContext. This provides the methods so
 * there can be many DelegateContexts, but few service trackers.
 */
public final class ContextHelper {

    /** Ensure no one constructs us */
    private ContextHelper() {
        throw new RuntimeException();
    }

    public static ContextProvider createURLContext(final BundleContext context,
                                           final String urlScheme, 
                                           final Hashtable<?, ?> env)
        throws NamingException {
        return Utils.doPrivilegedNaming(new PrivilegedExceptionAction<ContextProvider>() {
            public ContextProvider run() throws Exception {
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
    private static ContextProvider doCreateURLContext(BundleContext context, String urlScheme, Hashtable<?, ?> env)
        throws NamingException {
      
        ServicePair<ObjectFactory> urlObjectFactory = getURLObjectFactory(context, urlScheme, env);
        
        if (urlObjectFactory != null) {
            ObjectFactory factory = urlObjectFactory.get();
            
            if (factory != null) {
                try {
                    Context ctx = (Context) factory.getObjectInstance(null, null, null, env);
                    
                    return new ContextProvider(context, urlObjectFactory.getReference(), ctx);
                } catch (Exception e) {
                    urlObjectFactory.unget();
                    NamingException e2 = new NamingException();
                    e2.initCause(e);
                    throw e2;
                }
            }
        }

        // if we got here then we couldn't find a URL context factory so return null.
        return null;
    }
    
    public static final ServicePair<ObjectFactory> getURLObjectFactory(BundleContext ctx, String urlScheme, Hashtable<?, ?> environment)
      throws NamingException
    {
      ServicePair<ObjectFactory> result = null;
      
      ServiceReference ref = ServiceTrackerCustomizers.URL_FACTORY_CACHE.find(urlScheme);
      
      if (ref == null) {
        ServiceReference[] refs = Activator.getURLObectFactoryFinderServices();
        
        if (refs != null) {
          for (ServiceReference finderRef : refs) {
            URLObjectFactoryFinder finder = (URLObjectFactoryFinder) ctx.getService(finderRef);
            if (finder != null) {
              ObjectFactory f = finder.findFactory(urlScheme, environment);
              
              if (f != null) {
                result = new ServicePair<ObjectFactory>(ctx, finderRef, f);
                break;
              } else {
                ctx.ungetService(finderRef);
              }
            }
          }
        }
      } else {
        result = new ServicePair<ObjectFactory>(ctx, ref);
      }
      
      return result;
    }
        
    public static Context getInitialContext(BundleContext context, Hashtable<?, ?> environment)
        throws NamingException {
        ContextProvider provider = getContextProvider(context, environment);
        
        if (provider != null) {
          return new DelegateContext(context, provider);
        } else {
          String contextFactoryClass = (String) environment.get(Context.INITIAL_CONTEXT_FACTORY);
          if (contextFactoryClass == null) {
            return new DelegateContext(context, environment);
          } else {
            throw new NoInitialContextException("We could not find an InitialContextFactory to use");
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
                ServiceReference[] references = Activator.getInitialContextFactoryServices();
                if (references != null) {
                    Context initialContext = null;
                    for (ServiceReference reference : references) {
                        InitialContextFactory factory = (InitialContextFactory) context.getService(reference);
                        try {
                            initialContext = factory.getInitialContext(environment);
                            if (initialContext != null) {
                              provider = new ContextProvider(context, reference, initialContext);
                              break;
                          }
                        } finally {
                            if (provider == null) context.ungetService(reference);
                        }
                    }
                }
            }
        } else {
            ServiceReference ref = ServiceTrackerCustomizers.ICF_CACHE.find(contextFactoryClass);
            
            if (ref != null) {
              Context initialContext = null;
              InitialContextFactory factory = (InitialContextFactory) context.getService(ref);
              if (factory != null) {
                try {
                    initialContext = factory.getInitialContext(environment);
                    provider = new ContextProvider(context, ref, initialContext);
                } finally {
                    if (provider == null) context.ungetService(ref);
                }
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
        ServiceReference[] refs = Activator.getInitialContextFactoryBuilderServices();
        if (refs != null) {
            InitialContextFactory factory = null;
            for (ServiceReference ref : refs) {                    
                InitialContextFactoryBuilder builder = (InitialContextFactoryBuilder) context.getService(ref);
                try {
                    factory = builder.createInitialContextFactory(environment);
                    if (factory != null) {
                      provider = new ContextProvider(context, ref, factory.getInitialContext(environment));
                      break;
                    } else {
                      context.ungetService(ref); // we didn't get something back, so this was no good.
                    }
                } catch (NamingException e) {
                    // TODO: log
                    // ignore
                    context.ungetService(ref);
                }
            }
        }
        return provider;
    }

}
