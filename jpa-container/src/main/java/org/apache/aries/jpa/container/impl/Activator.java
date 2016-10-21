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
package org.apache.aries.jpa.container.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.aries.jpa.container.weaving.impl.TransformerRegistry;
import org.apache.aries.jpa.container.weaving.impl.TransformerRegistrySingleton;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.util.tracker.BundleTracker;

public class Activator implements BundleActivator {

    private BundleTracker<Bundle> persistenceBundleManager;

    @Override
    public void start(BundleContext context) throws Exception {
        registerWeavingHook(context, TransformerRegistrySingleton.get());

        PersistenceBundleTracker customizer = new PersistenceBundleTracker();
        persistenceBundleManager = new BundleTracker<Bundle>(context, Bundle.STARTING | Bundle.ACTIVE, customizer);
        persistenceBundleManager.open();
    }

    /**
     * ARIES-1019: Register with the highest possible service ranking to
     * avoid ClassNotFoundException caused by interfaces added by earlier
     * weaving hooks that are not yet visible to the bundle class loader.
     */
    private void registerWeavingHook(BundleContext context, TransformerRegistry tr) {
        Dictionary<String, Object> props = new Hashtable<String, Object>(1); // NOSONAR
        props.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
        context.registerService(WeavingHook.class.getName(), tr, props);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        persistenceBundleManager.close();
    }

}
