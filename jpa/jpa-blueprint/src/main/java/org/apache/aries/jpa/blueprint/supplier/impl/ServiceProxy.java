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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.jpa.blueprint.supplier.impl;

import java.io.Closeable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.blueprint.container.ServiceUnavailableException;
import org.osgi.util.tracker.ServiceTracker;

public class ServiceProxy implements InvocationHandler {
    private static final int SERVICE_TIMEOUT = 120000;

    @SuppressWarnings("rawtypes")
    private ServiceTracker tracker;

    private String filterS;

    public ServiceProxy(BundleContext context, String filterS) {
        this.filterS = filterS;
        tracker = new ServiceTracker<>(context, createFilter(filterS), null);
        tracker.open();
    }

    private Filter createFilter(String filterS) {
        try {
            return filterS == null ? null : FrameworkUtil.createFilter(filterS);
        } catch (InvalidSyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private Object getService() {
        try {
            Object serviceO = tracker.waitForService(SERVICE_TIMEOUT);
            if (serviceO == null) {
                throw new ServiceUnavailableException("No matching service found after timeout of " + SERVICE_TIMEOUT + " ms", filterS);
            }
            return serviceO;
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("close".equals(method.getName())) {
            tracker.close();
            return null;
        }
        try {
            return method.invoke(getService(), args);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }
    
    public static <T> T create(BundleContext context, Class<T> iface) {
        return  (T) create(context, iface, getFilter(iface));
    }
    
    private static String getFilter(Class<?> clazz) {
        return String.format("(objectClass=%s)", clazz.getName());
    }

    @SuppressWarnings("unchecked")
    public static <T> T create(BundleContext context, Class<T> iface, String filter) {
    	ClassLoader cl = iface.getClassLoader();
        Class<?>[] ifAr = new Class[] { Closeable.class, iface };
        return  (T) Proxy.newProxyInstance(cl, ifAr, new ServiceProxy(context, filter));
    }
}
