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
package org.apache.aries.itest;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

/**
 * {@link BundleContext} wrapper that adds a couple of additional utilities
 *
 */
public class RichBundleContext implements BundleContext {
    public static final long DEFAULT_TIMEOUT = 15000;

    private final BundleContext delegate;

    public RichBundleContext(BundleContext delegate) {
        this.delegate = delegate;
    }


    public <T> T getService(Class<T> type) {
        return getService(type, null, DEFAULT_TIMEOUT);
    }

    public <T> T getService(Class<T> type, long timeout) {
        return getService(type, null, timeout);
    }

    public <T> T getService(Class<T> type, String filter) {
        return getService(type, filter, DEFAULT_TIMEOUT);
    }

    public <T> T getService(Class<T> type, String filter, long timeout) {
        ServiceTracker tracker = null;
        try {
            String flt;
            if (filter != null) {
                if (filter.startsWith("(")) {
                    flt = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")" + filter + ")";
                } else {
                    flt = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")(" + filter + "))";
                }
            } else {
                flt = "(" + Constants.OBJECTCLASS + "=" + type.getName() + ")";
            }
            Filter osgiFilter = FrameworkUtil.createFilter(flt);
            tracker = new ServiceTracker(delegate, osgiFilter, null);
            tracker.open();

            Object svc = type.cast(tracker.waitForService(timeout));
            if (svc == null) {
                System.out.println("Could not obtain a service in time, service-ref="+
                  tracker.getServiceReference()+
                  ", time="+System.currentTimeMillis());
                throw new RuntimeException("Gave up waiting for service " + flt);
            }
            return type.cast(svc);
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Invalid filter", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    public Bundle getBundleByName(String symbolicName) {
        for (Bundle b : delegate.getBundles()) {
            if (b.getSymbolicName().equals(symbolicName)) {
                return b;
            }
        }
        return null;
    }

    public String getProperty(String key) {
        return delegate.getProperty(key);
    }

    public Bundle getBundle() {
        return delegate.getBundle();
    }

    public Bundle getBundle(String filter) { return delegate.getBundle(filter); }

    public Bundle installBundle(String location, InputStream input)
            throws BundleException {
        return delegate.installBundle(location, input);
    }

    public Bundle installBundle(String location) throws BundleException {
        return delegate.installBundle(location);
    }

    public Bundle getBundle(long id) {
        return delegate.getBundle(id);
    }

    public Bundle[] getBundles() {
        return delegate.getBundles();
    }

    public void addServiceListener(ServiceListener listener, String filter)
            throws InvalidSyntaxException {
        delegate.addServiceListener(listener, filter);
    }

    public void addServiceListener(ServiceListener listener) {
        delegate.addServiceListener(listener);
    }

    public void removeServiceListener(ServiceListener listener) {
        delegate.removeServiceListener(listener);
    }

    public void addBundleListener(BundleListener listener) {
        delegate.addBundleListener(listener);
    }

    public void removeBundleListener(BundleListener listener) {
        delegate.removeBundleListener(listener);
    }

    public void addFrameworkListener(FrameworkListener listener) {
        delegate.addFrameworkListener(listener);
    }

    public void removeFrameworkListener(FrameworkListener listener) {
        delegate.removeFrameworkListener(listener);
    }

    @SuppressWarnings("rawtypes")
    public ServiceRegistration registerService(String[] clazzes,
            Object service, Dictionary properties) {
        return delegate.registerService(clazzes, service, properties);
    }

    @SuppressWarnings("rawtypes")
    public ServiceRegistration registerService(String clazz, Object service,
            Dictionary properties) {
        return delegate.registerService(clazz, service, properties);
    }

    public ServiceRegistration registerService(Class clazz, Object service, Dictionary props) {
        return delegate.registerService(clazz, service, props);
    }

    public ServiceReference[] getServiceReferences(String clazz, String filter)
            throws InvalidSyntaxException {
        return delegate.getServiceReferences(clazz, filter);
    }

    public Collection getServiceReferences(Class clazz, String filter) throws InvalidSyntaxException {
        return delegate.getServiceReferences(clazz, filter);
    }

    public ServiceReference[] getAllServiceReferences(String clazz,
            String filter) throws InvalidSyntaxException {
        return delegate.getAllServiceReferences(clazz, filter);
    }

    public ServiceReference getServiceReference(String clazz) {
        return delegate.getServiceReference(clazz);
    }

    public ServiceReference getServiceReference(Class clazz) { return delegate.getServiceReference(clazz); }

    public Object getService(ServiceReference reference) {
        return delegate.getService(reference);
    }

    public boolean ungetService(ServiceReference reference) {
        return delegate.ungetService(reference);
    }

    public File getDataFile(String filename) {
        return delegate.getDataFile(filename);
    }

    public Filter createFilter(String filter) throws InvalidSyntaxException {
        return delegate.createFilter(filter);
    }
    
    
}
