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

import org.apache.aries.util.service.registry.ServicePair;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import javax.naming.*;
import javax.naming.directory.Attributes;
import javax.naming.spi.DirObjectFactory;
import javax.naming.spi.ObjectFactory;
import javax.naming.spi.ObjectFactoryBuilder;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ObjectFactoryHelper implements ObjectFactory {

    private static final Logger logger = Logger.getLogger(ObjectFactoryHelper.class.getName());
    protected BundleContext defaultContext;
    protected BundleContext callerContext;

    public ObjectFactoryHelper(BundleContext defaultContext, BundleContext callerContext) {
        this.defaultContext = defaultContext;
        this.callerContext = callerContext;
    }

    protected static String getUrlScheme(String name) {
        String scheme = name;
        int index = name.indexOf(':');
        if (index != -1) {
            scheme = name.substring(0, index);
        }
        return scheme;
    }

    static Tuple<ServiceReference<ObjectFactory>, ObjectFactory> findObjectFactoryByClassName(final BundleContext ctx, final String className) {
        return AccessController.doPrivileged(new PrivilegedAction<Tuple<ServiceReference<ObjectFactory>, ObjectFactory>>() {
            public Tuple<ServiceReference<ObjectFactory>, ObjectFactory> run() {
                ServiceReference<ObjectFactory> serviceReference = null;

                try {
                    ServiceReference<?>[] refs = ctx.getServiceReferences(className, null);
                    if (refs != null && refs.length > 0) {
                        serviceReference = (ServiceReference<ObjectFactory>) refs[0];
                    }
                } catch (InvalidSyntaxException e) {
                    // should not happen
                    throw new RuntimeException(Utils.MESSAGES.getMessage("null.is.invalid.filter"), e);
                }

                ObjectFactory factory = null;

                if (serviceReference != null) {
                    factory = ctx.getService(serviceReference);
                }

                return new Tuple<ServiceReference<ObjectFactory>, ObjectFactory>(serviceReference, factory);
            }
        });
    }

    public Object getObjectInstance(Object obj,
                                    Name name,
                                    Context nameCtx,
                                    Hashtable<?, ?> environment) throws Exception {

        // Step 1 ensure we have a reference rather than a referenceable
        if (obj instanceof Referenceable) {
            obj = ((Referenceable) obj).getReference();
        }

        if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "obj = " + obj);

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

        if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "Step 4: result = " + result);

        // Step 5 - if we still don't have a resolved object goto the object factory builds in the SR.
        if (result == null || result == obj) {
            result = getObjectInstanceUsingObjectFactoryBuilders(obj, name, nameCtx, environment);
        }

        if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "Step 5: result = " + result);

        // Step 6 - Attempt to use all the registered ObjectFactories in the SR.
        if (result == null || result == obj) {
            if (!(obj instanceof Reference) || ((Reference) obj).getFactoryClassName() == null) {
                result = getObjectInstanceUsingObjectFactories(obj, name, nameCtx, environment);
            }
        }

        if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "Step 6: result = " + result);

        // Extra, non-standard, bonus step 7. If javax.naming.OBJECT_FACTORIES is set as
        // a property in the environment, use its value to construct additional object factories.
        // Added under Aries-822, with reference
        // to https://www.osgi.org/bugzilla/show_bug.cgi?id=138
        if (result == null || result == obj) {
            result = getObjectInstanceViaContextDotObjectFactories(obj, name, nameCtx, environment);
        }

        if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "Step 7: result = " + result);

        return (result == null) ? obj : result;
    }

    /*
     * Attempt to obtain an Object instance via the java.naming.factory.object property
     */
    protected Object getObjectInstanceViaContextDotObjectFactories(Object obj,
                                                                   Name name,
                                                                   Context nameCtx,
                                                                   Hashtable<?, ?> environment) throws Exception {
        return getObjectInstanceViaContextDotObjectFactories(obj, name, nameCtx, environment, null);
    }

    /*
     * Attempt to obtain an Object instance via the java.naming.factory.object property
     */
    protected Object getObjectInstanceViaContextDotObjectFactories(Object obj,
                                                                   Name name,
                                                                   Context nameCtx,
                                                                   Hashtable<?, ?> environment,
                                                                   Attributes attrs) throws Exception {
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
                ObjectFactory factory;
                try {
                    @SuppressWarnings("unchecked")
                    Class<ObjectFactory> clz = (Class<ObjectFactory>) cl.loadClass(cand);
                    factory = clz.newInstance();
                } catch (Exception e) {
                    if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "Exception instantiating factory: " + e);
                    continue;
                }
                if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "cand=" + cand + " factory=" + factory);
                if (factory != null) {
                    if (factory instanceof DirObjectFactory) {
                        if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "its a DirObjectFactory");
                        final DirObjectFactory dirFactory = (DirObjectFactory) factory;
                        result = dirFactory.getObjectInstance(obj, name, nameCtx, environment, attrs);
                    } else {
                        if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "its an ObjectFactory");
                        result = factory.getObjectInstance(obj, name, nameCtx, environment);
                    }
                }
                if (result != null && result != obj) break;
            }
        }
        if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "result = " + result);
        return (result == null) ? obj : result;
    }

    protected Object getObjectInstanceUsingObjectFactories(Object obj,
                                                           Name name,
                                                           Context nameCtx,
                                                           Hashtable<?, ?> environment)
            throws Exception {
        Object result = null;
        Collection<ServiceReference<ObjectFactory>> refs = Utils.getReferencesPrivileged(callerContext, ObjectFactory.class);
        for (ServiceReference<ObjectFactory> ref : refs) {
            if (canCallObjectFactory(obj, ref)) {
                ObjectFactory factory = Utils.getServicePrivileged(callerContext, ref);

                try {
                    result = factory.getObjectInstance(obj, name, nameCtx, environment);
                } catch (NamingException ne) {
                    // Ignore this since we are doing last ditch finding, another OF might work.
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

    private boolean canCallObjectFactory(Object obj, ServiceReference ref) {
        if (obj instanceof Reference) return true;

        Object prop = ref.getProperty("aries.object.factory.requires.reference");

        if (prop == null) return true;

        if (prop instanceof Boolean) return !(Boolean) prop; // if set to true we don't call.

        return true;
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
                String urlScheme = getUrlScheme((String) address.getContent());

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

    private Object getObjectInstanceUsingClassName(Object reference,
                                                   String className,
                                                   Object obj,
                                                   Name name,
                                                   Context nameCtx,
                                                   Hashtable<?, ?> environment)
            throws Exception {

        Tuple<ServiceReference<ObjectFactory>, ObjectFactory> tuple = findObjectFactoryByClassName(defaultContext, className);
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

        Collection<ServiceReference<ObjectFactoryBuilder>> refs = Utils.getReferencesPrivileged(callerContext, ObjectFactoryBuilder.class);
        for (ServiceReference<ObjectFactoryBuilder> ref : refs) {
            ObjectFactoryBuilder builder = Utils.getServicePrivileged(callerContext, ref);
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

        Object result = null;

        if (factory != null) {
            result = factory.getObjectInstance(obj, name, nameCtx, environment);
        }

        return (result == null) ? obj : result;
    }

}