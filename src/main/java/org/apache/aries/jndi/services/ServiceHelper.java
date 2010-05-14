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
package org.apache.aries.jndi.services;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.aries.jndi.url.OsgiName;
import org.apache.aries.util.BundleToClassLoaderAdapter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jndi.JNDIConstants;

/**
 * This helper provides access to services registered in the OSGi service registry.
 * If a matching service cannot be located null may be returned. A caller should not
 * expect to get the same object if multiple requests are made to this API. A caller
 * should not expect to get a different object if multiple requests are made to this API.
 * A caller should avoid caching the returned service. OSGi is a dynamic environment and
 * the service may become unavailable while a reference to it is held. To minimize this
 * risk the caller should hold onto the service for the minimum length of time.
 * 
 * <p>This API should not be used from within an OSGi bundle. When in an OSGi environment
 *   the BundleContext for the bundle should be used to obtain the service.
 * </p>
 */
public final class ServiceHelper {
    
    private static class JNDIServiceDamper implements InvocationHandler {
        private BundleContext ctx;

        private ServicePair pair;

        private String interfaceName;

        private String filter;

        private boolean dynamic;

        public JNDIServiceDamper(BundleContext context,
                                 String i,
                                 String f,
                                 ServicePair service,
                                 boolean d) {
            ctx = context;
            pair = service;
            interfaceName = i;
            filter = f;
            dynamic = d;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (pair == null || pair.ref.getBundle() == null) {
                if (dynamic) {
                    pair = findService(ctx, interfaceName, filter);
                } else {
                    pair = null;
                }
            }

            if (pair == null) {
                throw new ServiceException(interfaceName, ServiceException.UNREGISTERED);
            }

            try {
                return method.invoke(pair.service, args);
            } catch (InvocationTargetException ite) {
                throw ite.getTargetException();
            }
        }
    }

    private static class ServicePair {
        private ServiceReference ref;
        private Object service;
    }

    public static Object getService(BundleContext ctx,
                                    OsgiName lookupName,
                                    String id,
                                    boolean dynamicRebind,
                                    Map<String, Object> env) throws NamingException {
        Object result = null;

        String interfaceName = lookupName.getInterface();
        String filter = lookupName.getFilter();
        String serviceName = lookupName.getServiceName();

        if (id != null) {
            if (filter == null) {
                filter = '(' + Constants.SERVICE_ID + '=' + id + ')';
            } else {
                filter = "(&(" + Constants.SERVICE_ID + '=' + id + ')' + filter + ')';
            }
        }

        ServicePair pair = null;

        if (!!!lookupName.isServiceNameBased()) {
            pair = findService(ctx, interfaceName, filter);
        }
        
        if (pair == null) {
            interfaceName = null;
            if (id == null) {
                filter = "(" + JNDIConstants.JNDI_SERVICENAME + "=" + serviceName + ')';
            } else {
                filter = "(&(" + Constants.SERVICE_ID + '=' + id + ")(" + JNDIConstants.JNDI_SERVICENAME + "=" + serviceName + "))";
            }
            pair = findService(ctx, interfaceName, filter);
        }

        if (pair != null) {
            result = proxy(interfaceName, filter, dynamicRebind, ctx, pair);
        }

        return result;
    }

    private static Object proxy(String interface1,
                                String filter,
                                boolean dynamicRebind,
                                BundleContext ctx,
                                ServicePair pair) {
        String[] interfaces = null;   
        if (interface1 != null) {
            interfaces = new String [] { interface1 };
        } else {
            interfaces = (String[]) pair.ref.getProperty(Constants.OBJECTCLASS);
        }

        List<Class<?>> clazz = new ArrayList<Class<?>>(interfaces.length);

        // We load the interface classes the service is registered under using
        // the defining
        // bundle. This is ok because the service must be able to see the
        // classes to be
        // registered using them. We then check to see if isAssignableTo on the
        // reference
        // works for the owning bundle and the interface name and only use the
        // interface if
        // true is returned there.

        // This might seem odd, but equinox and felix return true for
        // isAssignableTo if the
        // Bundle provided does not import the package. This is under the
        // assumption the
        // caller will then use reflection. The upshot of doing it this way is
        // that a utility
        // bundle can be created which centralizes JNDI lookups, but the service
        // will be used
        // by another bundle. It is true that class space consistency is less
        // safe, but we
        // are enabling a slightly odd use case anyway.

        Bundle serviceProviderBundle = pair.ref.getBundle();
        Bundle owningBundle = ctx.getBundle();

        for (String interfaceName : interfaces) {
            try {
                Class<?> potentialClass = serviceProviderBundle.loadClass(interfaceName);

                if (pair.ref.isAssignableTo(owningBundle, interfaceName)) {
                    clazz.add(potentialClass);
                }
            } catch (ClassNotFoundException e) {
            }
        }

        if (clazz.isEmpty()) {
            throw new IllegalArgumentException(Arrays.asList(interfaces).toString());
        }

        InvocationHandler ih = new JNDIServiceDamper(ctx, interface1, filter, pair, dynamicRebind);

        // The ClassLoader needs to be able to load the service interface
        // classes so it needs to be
        // wrapping the service provider bundle. The class is actually defined
        // on this adapter.

        return Proxy.newProxyInstance(new BundleToClassLoaderAdapter(serviceProviderBundle),
                                      clazz.toArray(new Class<?>[clazz.size()]), 
                                      ih);
    }

    private static ServicePair findService(BundleContext ctx, String interface1, String filter)
            throws NamingException {
        ServicePair p = null;

        try {
            ServiceReference[] refs = ctx.getServiceReferences(interface1, filter);

            if (refs != null) {
                // natural order is the exact opposite of the order we desire.
                Arrays.sort(refs, new Comparator<ServiceReference>() {
                    public int compare(ServiceReference o1, ServiceReference o2) {
                        return o2.compareTo(o1);
                    }
                });

                for (ServiceReference ref : refs) {
                    Object service = ctx.getService(ref);

                    if (service != null) {
                        p = new ServicePair();
                        p.ref = ref;
                        p.service = service;
                        break;
                    }
                }
            }

        } catch (InvalidSyntaxException e) {
            // If we get an invalid syntax exception we just ignore it. Null
            // will be returned which
            // is valid and that may result in a NameNotFoundException if that
            // is the right thing to do
        }

        return p;
    }

    public static ServiceReference[] getServiceReferences(BundleContext ctx,
                                                          String interface1,
                                                          String filter,
                                                          String serviceName,
                                                          Map<String, Object> env)
            throws NamingException {
        ServiceReference[] refs = null;

        try {
            refs = ctx.getServiceReferences(interface1, filter);

            if (refs == null || refs.length == 0) {
                refs = ctx.getServiceReferences(null, "(" + JNDIConstants.JNDI_SERVICENAME + "=" + serviceName + ')');
            }
        } catch (InvalidSyntaxException e) {
            throw (NamingException) new NamingException(e.getFilter()).initCause(e);
        }

        if (refs != null) {
            // natural order is the exact opposite of the order we desire.
            Arrays.sort(refs, new Comparator<ServiceReference>() {
                public int compare(ServiceReference o1, ServiceReference o2) {
                    return o2.compareTo(o1);
                }
            });
        }

        return refs;
    }

    public static Object getService(BundleContext ctx, ServiceReference ref) {
        Object service = ctx.getService(ref);

        Object result = null;

        if (service != null) {
            ServicePair pair = new ServicePair();
            pair.ref = ref;
            pair.service = service;

            result = proxy(null, null, false, ctx, pair);
        }

        return result;
    }

}
