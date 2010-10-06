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

import java.lang.reflect.Field;
import java.util.Arrays;

import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.NamingManager;
import javax.naming.spi.ObjectFactory;
import javax.naming.spi.ObjectFactoryBuilder;

import org.apache.aries.jndi.ContextManagerServiceFactory;
import org.apache.aries.jndi.JREInitialContextFactoryBuilder;
import org.apache.aries.jndi.OSGiInitialContextFactoryBuilder;
import org.apache.aries.jndi.OSGiObjectFactoryBuilder;
import org.apache.aries.jndi.ProviderAdminServiceFactory;
import org.apache.aries.jndi.Utils;
import org.apache.aries.jndi.tracker.ServiceTrackerCustomizers;
import org.apache.aries.jndi.urls.URLObjectFactoryFinder;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jndi.JNDIContextManager;
import org.osgi.service.jndi.JNDIProviderAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * The activator for this bundle makes sure the static classes in it are
 * driven so they can do their magic stuff properly.
 */
public class Activator implements BundleActivator {
    
    private OSGiInitialContextFactoryBuilder icfBuilder;
    private OSGiObjectFactoryBuilder ofBuilder;
    private static ServiceTracker icfBuilders;
    private static ServiceTracker urlObjectFactoryFinders;
    private static ServiceTracker initialContextFactories;
    private static ServiceTracker objectFactories;
    
    public void start(BundleContext context) {
        
        initialContextFactories = initServiceTracker(context, InitialContextFactory.class, ServiceTrackerCustomizers.ICF_CACHE);
        objectFactories = initServiceTracker(context, ObjectFactory.class, ServiceTrackerCustomizers.URL_FACTORY_CACHE);
        icfBuilders = initServiceTracker(context, InitialContextFactoryBuilder.class, ServiceTrackerCustomizers.LAZY);
        urlObjectFactoryFinders = initServiceTracker(context, URLObjectFactoryFinder.class, ServiceTrackerCustomizers.LAZY);
        
        try {
            OSGiInitialContextFactoryBuilder builder = new OSGiInitialContextFactoryBuilder();
            NamingManager.setInitialContextFactoryBuilder(builder);
            icfBuilder = builder;
        } catch (NamingException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            System.err.println("Cannot set the InitialContextFactoryBuilder. Another builder is already installed");
        }
    
        try {
            OSGiObjectFactoryBuilder builder = new OSGiObjectFactoryBuilder(context);
            NamingManager.setObjectFactoryBuilder(builder);
            ofBuilder = builder;
        } catch (NamingException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            System.err.println("Cannot set the ObjectFactoryBuilder. Another builder is already installed");
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
    }

    private ServiceTracker initServiceTracker(BundleContext context,
        Class<?> type, ServiceTrackerCustomizer custom) 
    {
      ServiceTracker t = new ServiceTracker(context, type.getName(), custom);
      t.open();
      return t;
    }

    public void stop(BundleContext context) {
        /*
         * Try to reset the InitialContextFactoryBuilder and ObjectFactoryBuilder
         * on the NamingManager. 
         */
        if (icfBuilder != null) {
            unsetField(InitialContextFactoryBuilder.class);
        }
        if (ofBuilder != null) {
            unsetField(ObjectFactoryBuilder.class);
        }
        
        icfBuilders.close();
        urlObjectFactoryFinders.close();
        objectFactories.close();
        initialContextFactories.close();
        
    }
    
    /*
     * There are no public API to reset the InitialContextFactoryBuilder or
     * ObjectFactoryBuilder on the NamingManager so try to use reflection. 
     */
    private static void unsetField(Class<?> expectedType) {
        try {
            for (Field field : NamingManager.class.getDeclaredFields()) {
                if (expectedType.equals(field.getType())) {
                    field.setAccessible(true);
                    field.set(null, null);
                }
            }
        } catch (Throwable t) {
            // Ignore
          t.printStackTrace();
        }
    }

    public static ServiceReference[] getInitialContextFactoryBuilderServices()
    {
      ServiceReference[] refs = icfBuilders.getServiceReferences();
      
      if (refs != null) Arrays.sort(refs, Utils.SERVICE_REFERENCE_COMPARATOR);
      
      return refs;
    }
    
    public static ServiceReference[] getInitialContextFactoryServices() 
    {
      ServiceReference[] refs = initialContextFactories.getServiceReferences();
      
      if (refs != null) Arrays.sort(refs, Utils.SERVICE_REFERENCE_COMPARATOR);
      
      return refs;
    }

    public static ServiceReference[] getURLObectFactoryFinderServices() 
    {
      ServiceReference[] refs = urlObjectFactoryFinders.getServiceReferences();
      
      if (refs != null) Arrays.sort(refs, Utils.SERVICE_REFERENCE_COMPARATOR);
      
      return refs;
    }
}
