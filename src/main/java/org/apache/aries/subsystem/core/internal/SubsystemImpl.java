/*
 /*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.subsystem.core.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.subsystem.Subsystem;
import org.apache.aries.subsystem.SubsystemAdmin;
import org.apache.aries.subsystem.SubsystemConstants;
import org.apache.aries.subsystem.SubsystemEvent;
import org.apache.aries.subsystem.SubsystemException;
import org.apache.aries.subsystem.scope.Scope;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class SubsystemImpl implements Subsystem {

    final long id;
    final SubsystemEventDispatcher eventDispatcher;
    final Scope scope;
    Map<String, String> headers;
    private final ServiceTracker serviceTracker;
    private final BundleContext context;
    private final Map<Long, Subsystem> subsystems = new HashMap<Long, Subsystem>();

    public SubsystemImpl(Scope scope, Map<String, String> headers) {
        this.scope = scope;
        this.id = this.scope.getId();
        this.eventDispatcher = Activator.getEventDispatcher();
        this.headers = headers;
        Filter filter = null;
        try {
            filter = FrameworkUtil.createFilter("(&("
                    + Constants.OBJECTCLASS + "=" + SubsystemAdmin.class.getName() + "))");
        } catch (InvalidSyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        this.context = Activator.getBundleContext();
        serviceTracker = new ServiceTracker(context, filter,
                new ServiceTrackerCustomizer() {

                    public Object addingService(ServiceReference reference) {
                        // adding new service, update admins map
                        SubsystemAdmin sa = (SubsystemAdmin)context.getService(reference);
                        if (sa.getParentSubsystem().getSubsystemId() == id) {
                            // it is the child subsystems for the current subsystem
                            synchronized (subsystems) {
                                subsystems.put(sa.getSubsystem().getSubsystemId(), sa.getSubsystem());
                            }
                        }

                        return sa;
                    }

                    public void modifiedService(ServiceReference reference,
                            Object service) {
                        // TODO Auto-generated method stub

                    }

                    public void removedService(ServiceReference reference,
                            Object service) {
                        SubsystemAdmin sa = (SubsystemAdmin) service;
                        if (sa.getParentSubsystem().getSubsystemId() == id) {
                            // it is the child subsystems for the current subsystem
                            synchronized (subsystems) {
                                subsystems.remove(sa.getSubsystem().getSubsystemId());
                            }
                        }
                    }

                });
    }

    public State getState() {
        // check bundles status
        Collection<Bundle> bundles = getBundles();
        if (checkBundlesStatus(bundles, Bundle.UNINSTALLED)) {
            return State.UNINSTALLED;
        } else if (checkBundlesStatus(bundles, Bundle.INSTALLED)) {
            return State.INSTALLED;
        } else if (checkBundlesStatus(bundles, Bundle.RESOLVED)) {
            return State.RESOLVED;
        } else if (checkBundlesStatus(bundles, Bundle.STARTING)) {
            return State.STARTING;
        } else if (checkBundlesStatus(bundles, Bundle.ACTIVE)) {
            return State.ACTIVE;
        } else if (checkBundlesStatus(bundles, Bundle.STOPPING)) {
            return State.STOPPING;
        } 
        
        throw new SubsystemException("Unable to retrieve subsystem state");
    }

    /**
     * check if all bundles in the collection has the state
     * 
     * @return
     */
    private boolean checkBundlesStatus(Collection<Bundle> bundles, int state) {
        for (Bundle b : bundles) {
            if (b.getState() != state) {
                return false;
            }
        }
     
        return true;
    }
    public void start() throws SubsystemException {
        try {
            eventDispatcher.subsystemEvent(new SubsystemEvent(SubsystemEvent.Type.STARTING, System.currentTimeMillis(), this));
            Collection<Bundle> bundles = this.scope.getBundles();
            for (Bundle b : bundles) {
                b.start();
            }
            eventDispatcher.subsystemEvent(new SubsystemEvent(SubsystemEvent.Type.STARTED, System.currentTimeMillis(), this));
        } catch (BundleException e) {
            throw new SubsystemException("Unable to start subsystem", e);
        }
    }

    public void stop() throws SubsystemException {
        try {
            eventDispatcher.subsystemEvent(new SubsystemEvent(SubsystemEvent.Type.STOPPING, System.currentTimeMillis(), this));
            Collection<Bundle> bundles = this.scope.getBundles();
            for (Bundle b : bundles) {
                b.start();
            }
            eventDispatcher.subsystemEvent(new SubsystemEvent(SubsystemEvent.Type.STOPPED, System.currentTimeMillis(), this));
        } catch (BundleException e) {
            throw new SubsystemException("Unable to stop subsystem", e);
        }
    }

    public long getSubsystemId() {
        return this.id;
    }

    public String getLocation() {
        return scope.getLocation();
    }

    public String getSymbolicName() {
        return scope.getName();
    }

    public Version getVersion() {
        return Version.parseVersion(headers.get(SubsystemConstants.SUBSYSTEM_VERSION));
    }

    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    public void updateHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
    
    public Map<String, String> getHeaders(String locale) {
        return null;
    }

    public Collection<Bundle> getBundles() {
        return this.scope.getBundles();
    }
    
    protected Scope getScope() {
        return scope;
    }

    public Collection<Subsystem> getChildrenSubsystems() {
        synchronized (subsystems) {
            return Collections.unmodifiableCollection(new ArrayList(subsystems.values()));
        }
    }
}