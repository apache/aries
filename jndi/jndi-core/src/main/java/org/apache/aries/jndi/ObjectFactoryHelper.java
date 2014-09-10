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
import java.util.concurrent.ConcurrentHashMap;
import java.util.Arrays;
import java.util.Vector;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import javax.naming.directory.Attributes;
import javax.naming.spi.DirObjectFactory;
import javax.naming.spi.ObjectFactory;
import javax.naming.spi.ObjectFactoryBuilder;

import org.apache.aries.util.service.registry.ServicePair;
import org.apache.aries.jndi.tracker.ServiceTrackerCustomizers;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;


public class ObjectFactoryHelper implements ObjectFactory {
    
    protected BundleContext defaultContext;
    protected BundleContext callerContext;
    
    protected ServiceTrackerCustomizers.ContextServiceTrackerCustomizer objFactoryBuilderStC = null;
    protected ServiceTrackerCustomizers.ContextServiceTrackerCustomizer objFactoryStC = null;
    protected ServiceTrackerCustomizers.ContextServiceTrackerCustomizer defaultStC = null;
    
    private static final Logger logger = Logger.getLogger(ObjectFactoryHelper.class.getName());

    public ObjectFactoryHelper(BundleContext defaultContext, BundleContext callerContext) {
        this.defaultContext = defaultContext;
        this.callerContext = callerContext;

        //Create service trackers for the contexts to allow caching of services
        objFactoryBuilderStC = ServiceTrackerCustomizers.getOrRegisterServiceTracker(callerContext, ObjectFactoryBuilder.class.getName());
        
        objFactoryStC = ServiceTrackerCustomizers.getOrRegisterServiceTracker(callerContext, ObjectFactory.class.getName());
        
        defaultStC = ServiceTrackerCustomizers.getOrRegisterServiceTracker(defaultContext, ObjectFactory.class.getName());
      
    }

    public Object getObjectInstance(Object obj,
                                    Name name,
                                    Context nameCtx,
                                    Hashtable<?, ?> environment) throws Exception {

        // Step 1 ensure we have a reference rather than a referenceable
        if (obj instanceof Referenceable) {
            obj = ((Referenceable) obj).getReference();
        }
        
        logger.log(Level.FINE, "obj = " + obj);

        Object result = obj;

        // Step 2 - if we have a reference process it as a reference
        if (obj instanceof Reference) {
            Reference ref = (Reference) obj;
            String className = ref.getFactoryClassName();

            if (className != null) {
                // Step 3 - use the class name in the reference to get the factory class name
                result = getObjectInstanceUsingClassName(obj, className, obj, name, nameCtx, environment);
            } else {
                // Step 4 - look, assuming url string ref addrs, for a url context object factory.
                result = getObjectInstanceUsingRefAddress(ref.getAll(), obj, name, nameCtx, environment);
            }
        }
        
		logger.log(Level.FINE, "Step 4: result = " + result);

        // Step 5 - if we still don't have a resolved object goto the object factory builds in the SR.
        if (result == null || result == obj) {
            result = getObjectInstanceUsingObjectFactoryBuilders(obj, name, nameCtx, environment);
        }

		logger.log(Level.FINE, "Step 5: result = " + result);

        // Step 6 - Attempt to use all the registered ObjectFactories in the SR.
        if (result == null || result == obj) {                
            if ((obj instanceof Reference && ((Reference) obj).getFactoryClassName() == null) ||
                !(obj instanceof Reference)) {
                result = getObjectInstanceUsingObjectFactories(obj, name, nameCtx, environment);
            }
        }
 
		logger.log(Level.FINE, "Step 6: result = " + result);

		// Extra, non-standard, bonus step 7. If javax.naming.OBJECT_FACTORIES is set as 
		// a property in the environment, use its value to construct additional object factories. 
		// Added under Aries-822, with reference 
		// to https://www.osgi.org/bugzilla/show_bug.cgi?id=138 
		if (result == null || result == obj) {
			result = getObjectInstanceViaContextDotObjectFactories(obj, name, nameCtx, environment);
		} 
		
		logger.log(Level.FINE, "Step 7: result = " + result);

        return (result == null) ? obj : result;
    }
 
    /*
     * Attempt to obtain an Object instance via the java.naming.factory.object property
     */
    protected Object getObjectInstanceViaContextDotObjectFactories(Object obj,
            Name name,
            Context nameCtx,
            Hashtable<?, ?> environment) throws Exception
    {
    	return getObjectInstanceViaContextDotObjectFactories(obj, name, nameCtx, environment, null);
    }
    
    /*
     * Attempt to obtain an Object instance via the java.naming.factory.object property
     */
    protected Object getObjectInstanceViaContextDotObjectFactories(Object obj,
            Name name,
            Context nameCtx,
            Hashtable<?, ?> environment,
            Attributes attrs) throws Exception
    {
    	Object result = null;
    	String factories = (String) environment.get(Context.OBJECT_FACTORIES);
		if (factories != null && factories.length() > 0) {
			String[] candidates = factories.split(":");
			ClassLoader cl = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
				public ClassLoader run() {
					return Thread.currentThread().getContextClassLoader();
				}
			});
			for (String cand : candidates) {
				ObjectFactory factory = null;
				try {
					@SuppressWarnings("unchecked")
					Class<ObjectFactory> clz = (Class<ObjectFactory>) cl.loadClass(cand);
					factory = clz.newInstance();
				} catch (Exception e) {
					logger.log(Level.FINE, "Exception instantiating factory: " + e);
					continue;
				}
				logger.log(Level.FINE, "cand=" + cand + " factory=" + factory);
				if (factory != null) {
					if(factory instanceof DirObjectFactory)
					{
						logger.log(Level.FINE, "its a DirObjectFactory");
						final DirObjectFactory dirFactory = (DirObjectFactory) factory;
						result = dirFactory.getObjectInstance(obj, name, nameCtx, environment, attrs);
					}
					else
					{
						logger.log(Level.FINE, "its an ObjectFactory");
						result = factory.getObjectInstance(obj, name, nameCtx, environment);
					}
				}
				if (result != null && result != obj) break;
			}
		}
		logger.log(Level.FINE, "result = " + result);
		return (result == null) ? obj : result;
    }

    protected Object getObjectInstanceUsingObjectFactories(Object obj,
                                                           Name name,
                                                           Context nameCtx,
                                                           Hashtable<?, ?> environment) 
        throws Exception {
        Object result = null;
        ServiceReference[] refs = objFactoryStC.getServiceRefs();
            
        if (refs != null) {
            Arrays.sort(refs, Utils.SERVICE_REFERENCE_COMPARATOR);
            
            for (ServiceReference ref : refs) {
              if (canCallObjectFactory(obj, ref)) {
                ObjectFactory factory = (ObjectFactory) objFactoryStC.getService(ref);

                try {
                    result = factory.getObjectInstance(obj, name, nameCtx, environment);
                } catch (NamingException ne) {
                  // Ignore this since we are doing last ditch finding, another OF might work.
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

    private boolean canCallObjectFactory(Object obj, ServiceReference ref)
    {
      if (obj instanceof Reference) return true;
      
      Object prop = ref.getProperty("aries.object.factory.requires.reference");
      
      if (prop == null) return true;
      
      if (prop instanceof Boolean) return !!!(Boolean) prop; // if set to true we don't call.
      
      return true;
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

    static Tuple<ServiceReference,ObjectFactory> findObjectFactoryByClassName(final ServiceTrackerCustomizers.ContextServiceTrackerCustomizer ctxCache, final String className) {
        return AccessController.doPrivileged(new PrivilegedAction<Tuple<ServiceReference,ObjectFactory>>() {
            public Tuple<ServiceReference,ObjectFactory> run() {
                ServiceReference serviceReference = ctxCache.getServiceRef(className);

                ObjectFactory factory = null;
                
                if (serviceReference != null) {
                    factory = (ObjectFactory) ctxCache.getService(serviceReference);
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
        
        Tuple<ServiceReference,ObjectFactory> tuple = findObjectFactoryByClassName(defaultStC, className);
        Object result = null;
        
        if (tuple.second != null) {
            result = tuple.second.getObjectInstance(reference, name, nameCtx, environment);
        }

        return (result == null) ? obj : result;
    }
  
    private Object getObjectInstanceUsingObjectFactoryBuilders(Object obj,
                                                               Name name,
                                                               Context nameCtx,
                                                               Hashtable<?, ?> environment) 
        throws Exception {
        
        ObjectFactory factory = null;
        
        ServiceReference[] refs = objFactoryBuilderStC.getServiceRefs();
        if (refs != null) {
            Arrays.sort(refs, Utils.SERVICE_REFERENCE_COMPARATOR);
            for (ServiceReference ref : refs) {
                ObjectFactoryBuilder builder = (ObjectFactoryBuilder) objFactoryBuilderStC.getService(ref);
                try {
                    factory = builder.createObjectFactory(obj, environment);
                } catch (NamingException e) {
                    // TODO: log it
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