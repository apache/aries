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

import java.util.concurrent.atomic.AtomicReference;

import org.apache.aries.subsystem.AriesSubsystem;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.ServiceTracker;

public class SystemRepositoryManager {
    private static final Filter filter = createFilter();
    
    private static Filter createFilter() {
        try {
            return FrameworkUtil.createFilter(new StringBuilder()
                    .append("(&(!(|(")
                    .append(SubsystemConstants.SUBSYSTEM_STATE_PROPERTY)
                    .append('=')
                    .append(Subsystem.State.INSTALL_FAILED)
                    .append(")(")
                    .append(SubsystemConstants.SUBSYSTEM_STATE_PROPERTY)
                    .append('=')
                    .append(Subsystem.State.UNINSTALLING)
                    .append(")(")
                    .append(SubsystemConstants.SUBSYSTEM_STATE_PROPERTY)
                    .append('=')
                    .append(Subsystem.State.UNINSTALLED)
                    .append(")))(")
                    .append(org.osgi.framework.Constants.OBJECTCLASS)
                    .append('=')
                    .append(AriesSubsystem.class.getName())
                    .append("))")
                    .toString());
        } 
        catch (InvalidSyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private final BundleTracker<AtomicReference<BundleRevisionResource>> bundleTracker;
    private final ServiceTracker<AriesSubsystem, BasicSubsystem> serviceTracker;
    private final SystemRepository systemRepository;
    
    public SystemRepositoryManager(BundleContext bundleContext) {
        systemRepository = new SystemRepository(bundleContext);
        bundleTracker = new BundleTracker<AtomicReference<BundleRevisionResource>>(
                bundleContext, ~Bundle.UNINSTALLED, systemRepository);
        serviceTracker = new ServiceTracker<AriesSubsystem, BasicSubsystem>(
                bundleContext, filter, systemRepository);
    }
    
    public SystemRepository getSystemRepository() {
        return systemRepository;
    }

    public void open() {
        bundleTracker.open();
        serviceTracker.open();
    }
    
    public void close() {
        serviceTracker.close();
        bundleTracker.close();
    }
}
