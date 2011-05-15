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

import org.apache.aries.util.service.registry.ServicePair;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class ObjectFactoryHelper implements ObjectFactory {
    
    protected BundleContext defaultContext;
    protected BundleContext callerContext;

    public ObjectFactoryHelper(BundleContext defaultContext, BundleContext callerContext) {
        this.defaultContext = defaultContext;
        this.callerContext = callerContext;
    }

    public Object getObjectInstance(Object obj,
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
        ServiceReference[] refs = Utils.getReferencesPrivileged(callerContext, ObjectFactory.class);
            
        if (refs != null) {
        	Arrays.sort(refs, Utils.SERVICE_REFERENCE_COMPARATOR);
        	
        	for (ServiceReference ref : refs) {
        		ObjectFactory factory = (ObjectFactory) Utils.getServicePrivileged(callerContext, ref);

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
                
                ServicePair<ObjectFactory> factoryService = ContextHelper.getURLObjectFactory(callerContext, urlScheme, environment);
                
                if (factoryService != null) {
                    ObjectFactory factory = factoryService.get();
                    
                    String value = (String) address.getContent();
                    try {
                        result = factory.getObjectInstance(value, name, nameCtx, environment);
                    } finally {
                        factoryService.unget();
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

    static Tuple<ServiceReference,ObjectFactory> findObjectFactoryByClassName(final BundleContext ctx, final String className) {
    	return AccessController.doPrivileged(new PrivilegedAction<Tuple<ServiceReference,ObjectFactory>>() {
			public Tuple<ServiceReference,ObjectFactory> run() {
		        ServiceReference serviceReference = null;
		        
		        try {
		            ServiceReference[] refs = ctx.getServiceReferences(className, null);
		            if (refs != null && refs.length > 0) {
		                serviceReference = refs[0];
		            }
		        } catch (InvalidSyntaxException e) {
		            // should not happen
                    throw new RuntimeException(Utils.MESSAGES.getMessage("null.is.invalid.filter"), e);
		        }

		        ObjectFactory factory = null;
		        
		        if (serviceReference != null) {
		            factory = (ObjectFactory) ctx.getService(serviceReference);			
		        }
		        
		        return new Tuple<ServiceReference, ObjectFactory>(serviceReference, factory);
			}
		});    	
    }
    
    private Object getObjectInstanceUsingClassName(Object reference,
                                                   String className,
                                                   Object obj,
                                                   Name name,
                                                   Context nameCtx,
                                                   Hashtable<?, ?> environment) 
        throws Exception {
    	
    	Tuple<ServiceReference,ObjectFactory> tuple = findObjectFactoryByClassName(defaultContext, className);
    	Object result = null;
    	
    	if (tuple.second != null) {
            try {
                result = tuple.second.getObjectInstance(reference, name, nameCtx, environment);
            } finally {
                defaultContext.ungetService(tuple.first);
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
        
        ServiceReference[] refs = Utils.getReferencesPrivileged(callerContext, ObjectFactoryBuilder.class);
        if (refs != null) {
        	Arrays.sort(refs, Utils.SERVICE_REFERENCE_COMPARATOR);
        	for (ServiceReference ref : refs) {
        		ObjectFactoryBuilder builder = (ObjectFactoryBuilder) Utils.getServicePrivileged(callerContext, ref);
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

        Object result = null;
        
        if (factory != null) {
            result = factory.getObjectInstance(obj, name, nameCtx, environment);
        }
        
        return (result == null) ? obj : result;
    }

}