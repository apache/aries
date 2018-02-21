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

import org.apache.aries.jndi.startup.Activator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import javax.naming.*;
import javax.naming.directory.Attributes;
import javax.naming.spi.DirObjectFactory;
import javax.naming.spi.ObjectFactory;
import javax.naming.spi.ObjectFactoryBuilder;
import java.security.AccessController;
import java.security.PrivilegedAction;
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

    public Object getObjectInstance(Object obj,
                                    Name name,
                                    Context nameCtx,
                                    Hashtable<?, ?> environment) throws Exception {

        return getObjectInstance(obj, name, nameCtx, environment, null);
    }

    public Object getObjectInstance(Object obj,
                                    Name name,
                                    Context nameCtx,
                                    Hashtable<?, ?> environment,
                                    Attributes attrs) throws Exception {
        return Utils.doPrivilegedE(() -> doGetObjectInstance(obj, name, nameCtx, environment, attrs));
    }

    private Object doGetObjectInstance(Object obj,
                                    Name name,
                                    Context nameCtx,
                                    Hashtable<?, ?> environment,
                                    Attributes attrs) throws Exception {
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
                result = getObjectInstanceUsingClassName(obj, className, obj, name, nameCtx, environment, attrs);
            } else {
                // Step 4 - look, assuming url string ref addrs, for a url context object factory.
                result = getObjectInstanceUsingRefAddress(ref.getAll(), obj, name, nameCtx, environment, attrs);
            }
        }

        // Step 4
        if (result == null || result == obj) {
            result = getObjectInstanceUsingObjectFactoryBuilders(obj, name, nameCtx, environment, attrs);
        }

        // Step 5
        if (result == null || result == obj) {
            if (!(obj instanceof Reference) || ((Reference) obj).getFactoryClassName() == null) {
                result = getObjectInstanceUsingObjectFactories(obj, name, nameCtx, environment, attrs);
            }
        }

        // Extra, non-standard, bonus step. If javax.naming.OBJECT_FACTORIES is set as
        // a property in the environment, use its value to construct additional object factories.
        // Added under Aries-822, with reference
        // to https://www.osgi.org/bugzilla/show_bug.cgi?id=138
        if (result == null || result == obj) {
            result = getObjectInstanceViaContextDotObjectFactories(obj, name, nameCtx, environment, attrs);
        }

        return (result == null) ? obj : result;
    }

    private Object getObjectInstanceUsingObjectFactories(Object obj,
                                                         Name name,
                                                         Context nameCtx,
                                                         Hashtable<?, ?> environment,
                                                         Attributes attrs) throws Exception {
        for (ServiceReference<ObjectFactory> ref : Activator.getReferences(callerContext, ObjectFactory.class)) {
            if (canCallObjectFactory(obj, ref)) {
                ObjectFactory factory = Activator.getService(callerContext, ref);
                if (factory != null) {
                    Object result = getObjectFromFactory(obj, name, nameCtx, environment, attrs, factory);
                    // if the result comes back and is not null and not the reference
                    // object then we should return the result, so break out of the
                    // loop we are in.
                    if (result != null && result != obj) {
                        return result;
                    }
                }
            }
        }
        return obj;
    }

    private boolean canCallObjectFactory(Object obj, ServiceReference ref) {
        if (obj instanceof Reference) return true;
        Object prop = ref.getProperty("aries.object.factory.requires.reference");
        return (prop == null) || !(prop instanceof Boolean) || !(Boolean) prop;
    }

    private Object getObjectInstanceUsingClassName(Object reference,
                                                   String className,
                                                   Object obj,
                                                   Name name,
                                                   Context nameCtx,
                                                   Hashtable<?, ?> environment,
                                                   Attributes attrs)
            throws Exception {

        Object result = null;

        ObjectFactory factory = ObjectFactoryHelper.findObjectFactoryByClassName(defaultContext, className);
        if (factory != null) {
            result = getObjectFromFactory(reference, name, nameCtx, environment, attrs, factory);
        }

        return (result == null) ? obj : result;
    }

    private Object getObjectInstanceUsingObjectFactoryBuilders(Object obj,
                                                               Name name,
                                                               Context nameCtx,
                                                               Hashtable<?, ?> environment,
                                                               Attributes attrs)
            throws Exception {
        ObjectFactory factory = null;
        for (ObjectFactoryBuilder ofb : Activator.getServices(callerContext, ObjectFactoryBuilder.class)) {
            try {
                factory = ofb.createObjectFactory(obj, environment);
            } catch (NamingException e) {
                // TODO: log it
            }
            if (factory != null) {
                break;
            }
        }

        Object result = null;

        if (factory != null) {
            result = getObjectFromFactory(obj, name, nameCtx, environment, attrs, factory);
        }

        return (result == null) ? obj : result;
    }

    /*
     * Attempt to obtain an Object instance via the java.naming.factory.object property
     */
    private Object getObjectInstanceViaContextDotObjectFactories(Object obj,
                                                                 Name name,
                                                                 Context nameCtx,
                                                                 Hashtable<?, ?> environment,
                                                                 Attributes attrs) throws Exception {
        Object result = null;
        String factories = (String) environment.get(Context.OBJECT_FACTORIES);
        if (factories != null && factories.length() > 0) {
            String[] candidates = factories.split(":");
            ClassLoader cl = Utils.doPrivileged(Thread.currentThread()::getContextClassLoader);
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
                    result = getObjectFromFactory(obj, name, nameCtx, environment, attrs, factory);
                }
                if (result != null && result != obj) break;
            }
        }
        if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "result = " + result);
        return (result == null) ? obj : result;
    }

    private Object getObjectInstanceUsingRefAddress(Enumeration<RefAddr> addresses,
                                                    Object obj,
                                                    Name name,
                                                    Context nameCtx,
                                                    Hashtable<?, ?> environment,
                                                    Attributes attrs)
            throws Exception {
        while (addresses.hasMoreElements()) {
            RefAddr address = addresses.nextElement();
            if (address instanceof StringRefAddr && "URL".equals(address.getType())) {
                String urlScheme = getUrlScheme((String) address.getContent());

                ServicePair<ObjectFactory> factoryService = ContextHelper.getURLObjectFactory(callerContext, urlScheme, environment);

                if (factoryService != null) {
                    ObjectFactory factory = factoryService.get();

                    String value = (String) address.getContent();
                    Object result = getObjectFromFactory(value, name, nameCtx, environment, attrs, factory);

                    // if the result comes back and is not null and not the reference
                    // object then we should return the result, so break out of the
                    // loop we are in.
                    if (result != null && result != obj) {
                        return result;
                    }
                }
            }
        }

        return obj;
    }

    private Object getObjectFromFactory(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment, Attributes attrs, ObjectFactory factory) throws Exception {
        Object result;
        if (factory instanceof DirObjectFactory) {
            result = ((DirObjectFactory) factory).getObjectInstance(obj, name, nameCtx, environment, attrs);
        } else {
            result = factory.getObjectInstance(obj, name, nameCtx, environment);
        }
        return result;
    }

    private static String getUrlScheme(String name) {
        String scheme = name;
        int index = name.indexOf(':');
        if (index != -1) {
            scheme = name.substring(0, index);
        }
        return scheme;
    }

    private static ObjectFactory findObjectFactoryByClassName(final BundleContext ctx, final String className) {
        return Utils.doPrivileged(() -> {
            ServiceReference<?> ref = ctx.getServiceReference(className);
            if (ref != null) {
                return (ObjectFactory) Activator.getService(ctx, ref);
            }
            return null;
        });
    }

}