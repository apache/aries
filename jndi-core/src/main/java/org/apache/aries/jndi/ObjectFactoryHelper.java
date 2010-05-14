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
import java.util.Enumeration;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import javax.naming.spi.ObjectFactoryBuilder;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jndi.JNDIConstants;

public class ObjectFactoryHelper implements ObjectFactory {
    
    protected BundleContext defaultContext;
    protected BundleContext callerContext;

    public ObjectFactoryHelper(BundleContext defaultContext, BundleContext callerContext) {
        this.defaultContext = defaultContext;
        this.callerContext = callerContext;
    }

    public Object getObjectInstance(final Object obj,
                                    final Name name,
                                    final Context nameCtx,
                                    final Hashtable<?, ?> environment) throws Exception {
        return Utils.doPrivileged(new PrivilegedExceptionAction<Object>() {
            public Object run() throws Exception {
                return doGetObjectInstance(obj, name, nameCtx, environment);
            }            
        });
    }
    
    private Object doGetObjectInstance(Object obj,
                                       Name name,
                                       Context nameCtx,
                                       Hashtable<?, ?> environment) throws Exception {

        // Step 1
        if (obj instanceof Referenceable) {
            obj = ((Referenceable) obj).getReference();
        }

        Object result = obj;

        // Step 2
        if (obj instanceof Reference) {
            Reference ref = (Reference) obj;
            String className = ref.getFactoryClassName();

            if (className != null) {
                // Step 3
                result = getObjectInstanceUsingClassName(obj, className, obj, name, nameCtx, environment);
            } else {
                // Step 4
                result = getObjectInstanceUsingRefAddress(ref.getAll(), obj, name, nameCtx, environment);
            }
        }

        // Step 5
        if (result == null || result == obj) {
            result = getObjectInstanceUsingObjectFactoryBuilders(obj, name, nameCtx, environment);
        }

        // Step 6
        if (result == null || result == obj) {                
            if ((obj instanceof Reference && ((Reference) obj).getFactoryClassName() == null) ||
                !(obj instanceof Reference)) {
                result = getObjectInstanceUsingObjectFactories(obj, name, nameCtx, environment);
            }
        }

        return (result == null) ? obj : result;
    }

    protected Object getObjectInstanceUsingObjectFactories(Object obj,
                                                           Name name,
                                                           Context nameCtx,
                                                           Hashtable<?, ?> environment) 
        throws Exception {
        Object result = null;
        try {
            ServiceReference[] refs = callerContext.getServiceReferences(ObjectFactory.class.getName(), null);
            if (refs != null) {
                Arrays.sort(refs, Utils.SERVICE_REFERENCE_COMPARATOR);
                for (ServiceReference ref : refs) {
                    ObjectFactory factory = (ObjectFactory) callerContext.getService(ref);

                    try {
                        result = factory.getObjectInstance(obj, name, nameCtx, environment);
                    } finally {
                        callerContext.ungetService(ref);
                    }

                    // if the result comes back and is not null and not the reference
                    // object then we should return the result, so break out of the
                    // loop we are in.
                    if (result != null && result != obj) {
                        break;
                    }
                }
            }
        } catch (InvalidSyntaxException e) {
            // should not happen
            throw new RuntimeException("Invalid filter", e);
        }

        return (result == null) ? obj : result;
    }

    protected static String getUrlScheme(String name) {
        String scheme = name;   
        int index = name.indexOf(':');
        if (index != -1) {
            scheme = name.substring(0, index);
        }
        return scheme;
    }
        
    private Object getObjectInstanceUsingRefAddress(Enumeration<RefAddr> addresses,
                                                    Object obj,
                                                    Name name,
                                                    Context nameCtx,
                                                    Hashtable<?, ?> environment)
        throws Exception {       
        Object result = null;
        while (addresses.hasMoreElements()) {
            RefAddr address = addresses.nextElement();
            if (address instanceof StringRefAddr && "URL".equals(address.getType())) {
                String urlScheme = getUrlScheme( (String) address.getContent() );
                ObjectFactory factory = null;
                ServiceReference ref = null;
                try {
                    ServiceReference[] services = callerContext.getServiceReferences(ObjectFactory.class.getName(), 
                            "(&(" + JNDIConstants.JNDI_URLSCHEME + "=" + urlScheme + "))");

                    if (services != null && services.length > 0) {
                        ref = services[0];
                    }
                } catch (InvalidSyntaxException e) {
                    // should not happen
                    throw new RuntimeException("Invalid filter", e);
                }

                if (ref != null) {
                    factory = (ObjectFactory) callerContext.getService(ref);
                    
                    String value = (String) address.getContent();
                    try {
                        result = factory.getObjectInstance(value, name, nameCtx, environment);
                    } finally {
                        callerContext.ungetService(ref);
                    }
                    
                    // if the result comes back and is not null and not the reference
                    // object then we should return the result, so break out of the
                    // loop we are in.
                    if (result != null && result != obj) {
                        break;
                    }
                }
            }
        }

        return (result == null) ? obj : result;
    }

    private Object getObjectInstanceUsingClassName(Object reference,
                                                   String className,
                                                   Object obj,
                                                   Name name,
                                                   Context nameCtx,
                                                   Hashtable<?, ?> environment) 
        throws Exception {
        ServiceReference serviceReference = null;

        try {
            ServiceReference[] refs = defaultContext.getServiceReferences(className, null);
            if (refs != null && refs.length > 0) {
                serviceReference = refs[0];
            }
        } catch (InvalidSyntaxException e) {
            // should not happen
            throw new RuntimeException("Invalid filter", e);
        }

        Object result = null;
        
        if (serviceReference != null) {
            ObjectFactory factory = (ObjectFactory) defaultContext.getService(serviceReference);
            try {
                result = factory.getObjectInstance(reference, name, nameCtx, environment);
            } finally {
                defaultContext.ungetService(serviceReference);
            }
        }

        return (result == null) ? obj : result;
    }
  
    private Object getObjectInstanceUsingObjectFactoryBuilders(Object obj,
                                                               Name name,
                                                               Context nameCtx,
                                                               Hashtable<?, ?> environment) 
        throws Exception {
        ObjectFactory factory = null;
        try {
            ServiceReference[] refs = callerContext.getServiceReferences(ObjectFactoryBuilder.class.getName(), null);
            if (refs != null) {
                Arrays.sort(refs, Utils.SERVICE_REFERENCE_COMPARATOR);
                for (ServiceReference ref : refs) {
                    ObjectFactoryBuilder builder = (ObjectFactoryBuilder) callerContext.getService(ref);
                    try {
                        factory = builder.createObjectFactory(obj, environment);
                    } catch (NamingException e) {
                        // TODO: log it
                    } finally {
                        callerContext.ungetService(ref);
                    }
                    if (factory != null) {
                        break;
                    }
                }
            }
        } catch (InvalidSyntaxException e) {
            // should not happen
            throw new RuntimeException("Invalid filter", e);
        }

        Object result = null;
        
        if (factory != null) {
            result = factory.getObjectInstance(obj, name, nameCtx, environment);
        }
        
        return (result == null) ? obj : result;
    }

}