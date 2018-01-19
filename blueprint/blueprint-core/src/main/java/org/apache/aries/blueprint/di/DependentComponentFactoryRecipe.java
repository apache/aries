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
package org.apache.aries.blueprint.di;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.apache.aries.blueprint.container.SatisfiableRecipe;
import org.apache.aries.blueprint.ext.DependentComponentFactoryMetadata;

/**
 * Extends ComponentFactoryRecipe to support the dependency management (SatisfiableRecipe) for custom
 * bean managers (DependentComponentFactoryMetadata instances in this case).
 */
public class DependentComponentFactoryRecipe extends ComponentFactoryRecipe<DependentComponentFactoryMetadata> 
    implements SatisfiableRecipe, DependentComponentFactoryMetadata.SatisfactionCallback {

    private SatisfactionListener listener;
    private AtomicBoolean started = new AtomicBoolean(false);
    
    public DependentComponentFactoryRecipe(
            String name, DependentComponentFactoryMetadata metadata, 
            ExtendedBlueprintContainer container, List<Recipe> dependencies) {
        super(name, metadata, container, dependencies);
    }

    @Override
    public boolean isStaticLifecycle() {
        return false;
    }

    public String getOsgiFilter() {
        return getMetadata().getDependencyDescriptor();
    }

    public boolean isSatisfied() {
        return getMetadata().isSatisfied();
    }

    public void start(SatisfactionListener listener) {
        if (started.compareAndSet(false, true)) {
            this.listener = listener;
            getMetadata().startTracking(this);
        }
    }

    public void stop() {
        if (started.compareAndSet(true, false)) {
            listener = null;
            getMetadata().stopTracking();
        }
    }

    public void notifyChanged() {
        if (listener != null) {
            listener.notifySatisfaction(this);
        }
    }

}
