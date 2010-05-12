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
public final class ContextHelper
{
	/** The bundle context we use for accessing the SR */
  private static BundleContext context;
  
  /** Ensure no one constructs us */
  private ContextHelper() { throw new RuntimeException(); }
  
  public static void setBundleContext(BundleContext ctx)
  {
  	context = ctx;
  }
  
  /**
   * This class creates a Context from an InitialContextFactory that may be
   * named in the provided env. If no name is set the first InitialContextFactory
   * returned from the service registry is used.
   * 
   * @param env
   * @return the context.
   * @throws NamingException
   */
  public static Context createContext(Hashtable<?,?> env)
    throws NamingException
  {
  	
    InitialContextFactory icf = null;
    ServiceReference ref = null;

    String icfFactory = (String) env.get(Context.INITIAL_CONTEXT_FACTORY);
    
    boolean icfFactorySet = true;

    if (icfFactory == null) {
      icfFactory = InitialContextFactory.class.getName();
      icfFactorySet = false;
    }
    
    try {
      ServiceReference[] refs = context.getAllServiceReferences(icfFactory, null);
      if (refs != null) {
        ref = refs[0];
        icf = (InitialContextFactory) context.getService(ref);
      }
    } catch (InvalidSyntaxException e) {
      // TODO nls enable this.
      NamingException e4 = new NamingException("Argh this should never happen :)");
      e4.initCause(e);
      
      throw e4;
    }

    if (icf == null) {
      try {
        ServiceReference[] refs = context.getAllServiceReferences(InitialContextFactoryBuilder.class.getName(), null);

        if (refs != null) {
          for (ServiceReference icfbRef : refs) {
            InitialContextFactoryBuilder builder = (InitialContextFactoryBuilder) context.getService(icfbRef);

            icf = builder.createInitialContextFactory(env);
            
            context.ungetService(icfbRef);
            if (icf != null) {
              break;
            }
          }
        }
      } catch (InvalidSyntaxException e) {
        // TODO nls enable this.
        NamingException e4 = new NamingException("Argh this should never happen :)");
        e4.initCause(e);    
        throw e4;
      }
    }

    if (icf == null && icfFactorySet) {
      try {
        Class<?> clazz = Class.forName(icfFactory, true, null);
        icf = (InitialContextFactory) clazz.newInstance();
      } catch (ClassNotFoundException e11) {
        // TODO nls enable this.
        NamingException e = new NamingException("Argh this should never happen :)");
        e.initCause(e11);    
        throw e;
      } catch (InstantiationException e2) {
        // TODO nls enable this.
        NamingException e4 = new NamingException("Argh this should never happen :)");
        e4.initCause(e2);    
        throw e4;
      } catch (IllegalAccessException e1) {
        // TODO nls enable this.
        NamingException e4 = new NamingException("Argh this should never happen :)");
        e4.initCause(e1);    
        throw e4;
      }
    }

    if (icf == null) {

      // TODO nls enable this.
      NamingException e3 = new NoInitialContextException("We could not find an InitialContextFactory to use");
      
      throw e3;
    }

    Context ctx = icf.getInitialContext(env);

    if (ref != null) context.ungetService(ref);

    if (ctx == null) {
      // TODO nls enable this
      NamingException e = new NamingException("The ICF returned a null context");
      throw e;
    }

    return ctx;
  }
  
  
  private static Context createIcfContext(Hashtable<?,?> env) throws NamingException
  {
    String icfFactory = (String) env.get(Context.INITIAL_CONTEXT_FACTORY);
    InitialContextFactory icf = null;

    if (icfFactory != null) {
      try {
        Class<?> clazz = Class.forName(icfFactory, true, Thread.currentThread()
            .getContextClassLoader());
        icf = (InitialContextFactory) clazz.newInstance();

      } catch (ClassNotFoundException e11) {
        NamingException e4 = new NamingException("Argh this should never happen :)");
        e4.initCause(e11);
        throw e4;
      } catch (InstantiationException e2) {
        NamingException e4 = new NamingException("Argh this should never happen :)");
        e4.initCause(e2);
        throw e4;
      } catch (IllegalAccessException e1) {
        NamingException e4 = new NamingException("Argh this should never happen :)");
        e4.initCause(e1);
        throw e4;
      }
    }
    Context ctx = null;

    if (icf != null) {
      ctx = icf.getInitialContext(env);
    }    
    
    return ctx;
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
  public static Context createURLContext(String urlScheme, Hashtable<?, ?> env)
      throws NamingException
  {
    ObjectFactory factory = null;
    ServiceReference ref = null;

    Context ctx = null;

    try {
      ServiceReference[] services = context.getServiceReferences(ObjectFactory.class.getName(),
                                                                 "(" + JNDIConstants.JNDI_URLSCHEME + "=" + urlScheme + ")");

      if (services != null) {
        ref = services[0];
        factory = (ObjectFactory) context.getService(ref);
      }
    } catch (InvalidSyntaxException e1) {
      // TODO nls enable this.
      NamingException e = new NamingException("Argh this should never happen :)");
      e.initCause(e1);
      throw e;
    }

    if (factory != null) {
      try {
        ctx = (Context) factory.getObjectInstance(null, null, null, env);
      } catch (Exception e) {
        NamingException e2 = new NamingException();
        e2.initCause(e);
        throw e2;
      } finally {
        if (ref != null) context.ungetService(ref);
      }
    }

    // TODO: This works for WAS - we believe - but is incorrect behaviour. We should not use an icf to generate the URLContext.
    // Rather, the missing URLContext factories should be exported on behalf of WAS.
    if (ctx == null) {
      ctx = createIcfContext(env);
    }
    
    if (ctx == null && factory == null) {
      NamingException e = new NamingException("We could not find an ObjectFactory to use");
      throw e;
    } else if (ctx == null && factory != null) {
      NamingException e = new NamingException("The ICF returned a null context");
      throw e;
    }

    return ctx;
  }
}