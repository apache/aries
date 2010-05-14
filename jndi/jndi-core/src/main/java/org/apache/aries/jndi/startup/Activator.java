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

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.NamingManager;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.jndi.JNDIContextManager;
import org.osgi.service.jndi.JNDIProviderAdmin;

import org.apache.aries.jndi.ContextHelper;
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
    
    public void start(BundleContext context) {
          
        try {
            if (!!!NamingManager.hasInitialContextFactoryBuilder()) {
                NamingManager.setInitialContextFactoryBuilder(new OSGiInitialContextFactoryBuilder(context));
            }
        } catch (NamingException e) {
            //    TODO Auto-generated catch block
            e.printStackTrace();
        }
    
        try {
            NamingManager.setObjectFactoryBuilder(new OSGiObjectFactoryBuilder(context));
        } catch (NamingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
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
        for (ServiceRegistration registration : registrations) {
            registration.unregister();
        }
        registrations.clear();
    }
}
