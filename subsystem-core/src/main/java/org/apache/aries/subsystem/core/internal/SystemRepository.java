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
package org.apache.aries.subsystem.core.internal;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.aries.subsystem.AriesSubsystem;
import org.apache.aries.subsystem.core.capabilityset.CapabilitySetRepository;
import org.apache.aries.subsystem.core.repository.Repository;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class SystemRepository 
        implements 
                Repository,
                BundleTrackerCustomizer<AtomicReference<BundleRevisionResource>>,
                ServiceTrackerCustomizer<AriesSubsystem, BasicSubsystem> {
    
    private final BundleContext bundleContext;
    private final CapabilitySetRepository repository;

    public SystemRepository(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        repository = new CapabilitySetRepository();
    }

    @Override
    public AtomicReference<BundleRevisionResource> addingBundle(Bundle bundle, BundleEvent event) {
        // The state mask must guarantee this will only be called when the bundle is in the INSTALLED state.
        BundleRevision revision = bundle.adapt(BundleRevision.class);
        BundleRevisionResource resource = new BundleRevisionResource(revision);
        if (ThreadLocalSubsystem.get() == null) {
        	// This is an explicitly installed bundle. It must be prevented
        	// from resolving as part of adding it to the repository. Searching
        	// for service requirements and capabilities will result in a call
        	// to findEntries which will cause the framework to attempt a
        	// resolution.
        	ThreadLocalBundleRevision.set(revision);
        	try {
        		repository.addResource(resource);
        	}
        	finally {
        		ThreadLocalBundleRevision.remove();
        	}
        }
        else {
        	// If this is a bundle being installed as part of a subsystem
        	// installation, it is already protected.
        	repository.addResource(resource);
        }
        return new AtomicReference<BundleRevisionResource>(resource);
    }

    @Override
    public BasicSubsystem addingService(ServiceReference<AriesSubsystem> reference) {
        // Intentionally letting the ClassCastException propagate. Everything received should be a BasicSubsystem.
        BasicSubsystem subsystem = (BasicSubsystem)bundleContext.getService(reference);
        repository.addResource(subsystem);
        return subsystem;
    }

    @Override
    public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
        return repository.findProviders(requirements);
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, AtomicReference<BundleRevisionResource> object) {
        if (BundleEvent.UPDATED == event.getType()) {
            BundleRevision revision = bundle.adapt(BundleRevision.class);
            BundleRevisionResource resource = new BundleRevisionResource(revision);
            repository.removeResource(object.getAndSet(resource));
            repository.addResource(resource);
        }
    }

    @Override
    public void modifiedService(ServiceReference<AriesSubsystem> reference, BasicSubsystem service) {
        // Nothing.
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, AtomicReference<BundleRevisionResource> object) {
        // The state mask must guarantee this will only be called when the bundle is in the UNINSTALLED state.
        repository.removeResource(object.get());
    }

    @Override
    public void removedService(ServiceReference<AriesSubsystem> reference, BasicSubsystem service) {
        repository.removeResource(service);
    }
}
