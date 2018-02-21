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
package org.apache.aries.jndi.tracker;

import org.apache.aries.jndi.Utils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import java.util.*;
import java.util.function.Function;

public class CachingServiceTracker<S> extends ServiceTracker<S, ServiceReference<S>> {

    /** The cached references */
    private volatile Map<String, ServiceReference<S>> cache;
    /** The funtion to obtain the identifiers */
    private final Function<ServiceReference<S>, Iterable<String>> properties;

    public CachingServiceTracker(BundleContext context, Class<S> clazz) {
        this(context, clazz, ref -> Collections.emptyList());
    }

    public CachingServiceTracker(BundleContext context, Class<S> clazz, Function<ServiceReference<S>, Iterable<String>> properties) {
        super(context, clazz, null);
        this.properties = properties;
        open();
    }

    public ServiceReference<S> find(String identifier) {
        Map<String, ServiceReference<S>> c = cache;
        if (c == null) {
            synchronized (this) {
                if (cache == null) {
                    cache = new HashMap<>();
                    for (ServiceReference<S> ref : getReferences()) {
                        for (String key : properties.apply(ref)) {
                            cache.putIfAbsent(key, ref);
                        }
                    }
                }
                c = cache;
            }
        }
        return c.get(identifier);
    }

    public List<ServiceReference<S>> getReferences() {
        ServiceReference<S>[] refs = Utils.doPrivileged(this::getServiceReferences);
        if (refs != null) {
            Arrays.sort(refs, Comparator.reverseOrder());
            return Arrays.asList(refs);
        } else {
            return Collections.emptyList();
        }
    }

    public synchronized ServiceReference<S> addingService(ServiceReference<S> reference) {
        cache = null;
        return reference;
    }

    public synchronized void removedService(ServiceReference<S> reference, ServiceReference<S> service) {
        cache = null;
    }

    public void modifiedService(ServiceReference<S> reference, ServiceReference<S> service) {
        cache = null;
    }
}
