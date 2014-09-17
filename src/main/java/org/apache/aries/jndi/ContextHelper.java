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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.ObjectFactory;

import org.apache.aries.jndi.startup.Activator;
import org.apache.aries.jndi.tracker.ServiceTrackerCustomizers;
import org.apache.aries.jndi.urls.URLObjectFactoryFinder;
import org.apache.aries.util.service.registry.ServicePair;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
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

    /**
     * This method is used to create a URL Context. It does this by looking for
     * the URL context's ObjectFactory in the service registry.
     * 
     * @param context
     * @param urlScheme
     * @param env
     * @return a Context
     * @throws NamingException
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
    
    public static final ServicePair<ObjectFactory> getURLObjectFactory(final BundleContext ctx, String urlScheme, Hashtable<?, ?> environment)
      throws NamingException
    {
      ServicePair<ObjectFactory> result = null;
      
      ServiceReference ref = ServiceTrackerCustomizers.URL_FACTORY_CACHE.find(urlScheme);
      
      if (ref == null) {
        ServiceReference[] refs = AccessController.doPrivileged(new PrivilegedAction<ServiceReference[]>() {
            public ServiceReference[] run() {
                return Activator.getURLObectFactoryFinderServices();
            }
        });        
        
        if (refs != null) {
          for (final ServiceReference finderRef : refs) {
            URLObjectFactoryFinder finder = (URLObjectFactoryFinder) Utils.getServicePrivileged(ctx, finderRef);
                
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
      
      final Bundle jndiBundle = FrameworkUtil.getBundle(ContextHelper.class);
      // if we are outside OSGi (like in our unittests) then we would get Null back here, so just make sure we don't.
      if (jndiBundle != null) {
        
        BundleContext jndiBundleContext = AccessController.doPrivileged(new PrivilegedAction<BundleContext>() {
          public BundleContext run()
          {
            return jndiBundle.getBundleContext();
          }
        });
        
        if (!!!jndiBundleContext.getClass().equals(context.getClass())){
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
            throw new NoInitialContextException(Utils.MESSAGES.getMessage("no.initial.context.factory", contextFactoryClass));
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
            provider = getInitialContextUsingBuilder(context, environment);

            // 2. lookup all ContextFactory services
            if (provider == null) {
                
                ServiceReference[] references = AccessController.doPrivileged(new PrivilegedAction<ServiceReference[]>() {
                    public ServiceReference[] run() {
                        return Activator.getInitialContextFactoryServices();
                    }
                });
                
                if (references != null) {
                    Context initialContext = null;
                    for (ServiceReference reference : references) {
                        InitialContextFactory factory = (InitialContextFactory) Utils.getServicePrivileged(context, reference);
                        try {
                            initialContext = factory.getInitialContext(environment);
                            if (initialContext != null) {
                              provider = new SingleContextProvider(context, reference, initialContext);
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
              InitialContextFactory factory = (InitialContextFactory) Utils.getServicePrivileged(context, ref);
              if (factory != null) {
                try {
                    initialContext = factory.getInitialContext(environment);
                    provider = new SingleContextProvider(context, ref, initialContext);
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

    private static final Logger logger = Logger.getLogger(ContextHelper.class.getName());
    
    private static ContextProvider getInitialContextUsingBuilder(BundleContext context,
                                                                 Hashtable<?, ?> environment)
            throws NamingException {
        
        ContextProvider provider = null;
        ServiceReference[] refs = AccessController.doPrivileged(new PrivilegedAction<ServiceReference[]>() {
            public ServiceReference[] run() {
                return Activator.getInitialContextFactoryBuilderServices();
            }            
        });
            
        if (refs != null) {
            InitialContextFactory factory = null;
            for (ServiceReference ref : refs) {                    
                InitialContextFactoryBuilder builder = (InitialContextFactoryBuilder) Utils.getServicePrivileged(context, ref);
                try {
                  factory = builder.createInitialContextFactory(environment);
                } catch (NamingException ne) {
                  // TODO: log
                  // ignore this, if the builder fails we want to move onto the next one
                } catch (NullPointerException npe) { 
                	logger.log(Level.SEVERE,  "NPE caught in ContextHelper.getInitialContextUsingBuilder. context=" + context + " ref=" + ref);
                	throw npe;
                }
                
                if (factory != null) {
                  try {
                    provider = new SingleContextProvider(context, ref, factory.getInitialContext(environment));
                  } finally {
                    if (provider == null) context.ungetService(ref); // we didn't get something back, so this was no good.
                  }
                  break;
                } else {
                  context.ungetService(ref); // we didn't get something back, so this was no good.
                }
            }
        }
        return provider;
    }

}
