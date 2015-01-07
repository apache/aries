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
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.spi.DirObjectFactory;
import javax.naming.spi.DirectoryManager;
import javax.naming.spi.NamingManager;
import javax.naming.spi.ObjectFactory;
import javax.naming.spi.ObjectFactoryBuilder;

import org.osgi.framework.BundleContext;

public class OSGiObjectFactoryBuilder implements ObjectFactoryBuilder, ObjectFactory, DirObjectFactory {

    private BundleContext defaultContext;
    
    public OSGiObjectFactoryBuilder(BundleContext ctx) {
        defaultContext = ctx;
    }

    public ObjectFactory createObjectFactory(Object obj, Hashtable<?, ?> environment)
        throws NamingException {
        return this;
    }

    public Object getObjectInstance(Object obj,
                                    Name name,
                                    Context nameCtx,
                                    Hashtable<?, ?> environment) throws Exception {
        
        if (environment == null) {
            environment = new Hashtable();
        }
        
        BundleContext callerContext = getCallerBundleContext(environment);
        if (callerContext == null) {
            return obj;
        }
        DirObjectFactoryHelper helper = new DirObjectFactoryHelper(defaultContext, callerContext);
        return helper.getObjectInstance(obj, name, nameCtx, environment);
    }

    public Object getObjectInstance(Object obj,
                                    Name name,
                                    Context nameCtx,
                                    Hashtable<?, ?> environment,
                                    Attributes attrs) throws Exception {
        
        if (environment == null) {
            environment = new Hashtable();
        }
        
        BundleContext callerContext = getCallerBundleContext(environment);
        if (callerContext == null) {
            return obj;
        }
        DirObjectFactoryHelper helper = new DirObjectFactoryHelper(defaultContext, callerContext);
        return helper.getObjectInstance(obj, name, nameCtx, environment, attrs);
    }

    private BundleContext getCallerBundleContext(Hashtable<?, ?> environment) throws NamingException {
        AugmenterInvokerImpl.getInstance().augmentEnvironment(environment);
        BundleContext context = Utils.getBundleContext(environment, NamingManager.class);        
        if (context == null) {
            context = Utils.getBundleContext(environment, DirectoryManager.class);
        }
        AugmenterInvokerImpl.getInstance().unaugmentEnvironment(environment);
        return context;
    }
}
