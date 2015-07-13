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

import static org.osgi.service.jpa.EntityManagerFactoryBuilder.JPA_UNIT_NAME;

import java.io.Closeable;

import javax.persistence.EntityManager;

import org.apache.aries.jpa.supplier.EmSupplier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

public class EmSupplierProxy implements EmSupplier, Closeable {
    private ServiceTracker<EmSupplier, EmSupplier> tracker;
    private Filter filter;

    public EmSupplierProxy(BundleContext context, String unitName) {
        String filterS = String.format("(&(objectClass=%s)(%s=%s))", EmSupplier.class.getName(),
                                       JPA_UNIT_NAME,
                                       unitName);
        try {
            filter = FrameworkUtil.createFilter(filterS);
        } catch (InvalidSyntaxException e) {
            throw new IllegalStateException(e);
        }
        tracker = new ServiceTracker<>(context, filter, null);
        tracker.open();
    }

    @Override
    public EntityManager get() {
        return getEmSupplier().get();
    }

    @Override
    public void close() {
        tracker.close();
    }

    @Override
    public void preCall() {
        getEmSupplier().preCall();
    }

    @Override
    public void postCall() {
        getEmSupplier().postCall();
    }

    private EmSupplier getEmSupplier() {
        try {
            EmSupplier emSupplier = tracker.waitForService(10000);
            if (emSupplier == null) {
                throw new IllegalStateException("EmSupplier service not available with filter " + filter);
            }
            return emSupplier;
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

}
