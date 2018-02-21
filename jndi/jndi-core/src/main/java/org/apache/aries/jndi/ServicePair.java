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

import java.util.function.Supplier;

public class ServicePair<T> implements Supplier<T> {

    private BundleContext ctx;
    private ServiceReference<?> ref;
    private T svc;

    public ServicePair(BundleContext context, ServiceReference<T> serviceRef) {
        this.ctx = context;
        this.ref = serviceRef;
    }

    public ServicePair(BundleContext context, ServiceReference<?> serviceRef, T service) {
        this.ctx = context;
        this.ref = serviceRef;
        this.svc = service;
    }

    @SuppressWarnings("unchecked")
    public T get() {
        return svc != null ? svc : ref != null ? (T) Activator.getService(ctx, ref) : null;
    }

    public boolean isValid() {
        return ref.getBundle() != null;
    }

    public ServiceReference<?> getReference() {
        return ref;
    }

}
