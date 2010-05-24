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
import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.NamingManager;
import javax.naming.spi.ObjectFactoryBuilder;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.jndi.JNDIContextManager;
import org.osgi.service.jndi.JNDIProviderAdmin;

import org.apache.aries.jndi.ContextManagerServiceFactory;
import org.apache.aries.jndi.JREInitialContextFactoryBuilder;
import org.apache.aries.jndi.OSGiInitialContextFactoryBuilder;
import org.apache.aries.jndi.OSGiObjectFactoryBuilder;
import org.apache.aries.jndi.ProviderAdminServiceFactory;

/**
 * The activator for this bundle makes sure the static classes in it are
 * driven so they can do their magic stuff properly.
 */
public class Activator implements BundleActivator {
    
    private List<ServiceRegistration> registrations = new ArrayList<ServiceRegistration>();
    private OSGiInitialContextFactoryBuilder icfBuilder;
    private OSGiObjectFactoryBuilder ofBuilder;
    
    public void start(BundleContext context) {
          
        try {
            OSGiInitialContextFactoryBuilder builder = new OSGiInitialContextFactoryBuilder(context);
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
        
        registrations.add(context.registerService(JNDIProviderAdmin.class.getName(), 
                          new ProviderAdminServiceFactory(context), 
                          null));

        registrations.add(context.registerService(InitialContextFactoryBuilder.class.getName(), 
                          new JREInitialContextFactoryBuilder(), 
                          null));

        registrations.add(context.registerService(JNDIContextManager.class.getName(), 
                          new ContextManagerServiceFactory(context),
                          null));
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
        
        for (ServiceRegistration registration : registrations) {
            registration.unregister();
        }
        registrations.clear();
    }
    
    /*
     * There are no public API to reset the InitialContextFactoryBuilder or
     * ObjectFactoryBuilder on the NamingManager so try to use reflection. 
     */
    private static void unsetField(Class<?> expectedType) {
        try {
            for (Field field : NamingManager.class.getDeclaredFields()) {
                if (expectedType.equals(field.getType())) {
                    boolean accessible = field.isAccessible();
                    field.setAccessible(true);
                    try {
                        field.set(null, null);
                    } finally {
                        field.setAccessible(accessible);
                    }
                }
            }
        } catch (Throwable t) {
            // Ignore
        }
    }
}
