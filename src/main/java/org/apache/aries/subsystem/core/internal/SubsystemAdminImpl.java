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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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
import org.apache.aries.subsystem.SubsystemListener;
import org.apache.aries.subsystem.scope.ScopeAdmin;
import org.apache.aries.subsystem.spi.Resource;
import org.apache.aries.subsystem.spi.ResourceResolver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubsystemAdminImpl implements SubsystemAdmin {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubsystemAdminImpl.class);
    private static final Version SUBSYSTEM_MANIFEST_VERSION = new Version("1.0");

    final BundleContext context;
    final ScopeAdmin scopeAdmin;
    final Map<Long, Subsystem> subsystems = new HashMap<Long, Subsystem>();
    final ServiceTracker resourceResolverTracker;
    final SubsystemEventDispatcher eventDispatcher;
    final ServiceTracker listenersTracker;
    final Subsystem subsystem;
    final Subsystem parentSubsystem;
    
    public SubsystemAdminImpl(ScopeAdmin scopeAdmin, Subsystem subsystem, Subsystem parentSubsystem) {
        context = Activator.getBundleContext();
        this.eventDispatcher = Activator.getEventDispatcher();
        this.scopeAdmin = scopeAdmin;
        this.subsystem = subsystem;
        this.parentSubsystem = parentSubsystem;
        this.resourceResolverTracker = new ServiceTracker(context, ResourceResolver.class.getName(), null);
        this.resourceResolverTracker.open();
        this.listenersTracker = new ServiceTracker(context, SubsystemListener.class.getName(), null);
        this.listenersTracker.open();
    }

    public void dispose() {
        resourceResolverTracker.close();
        listenersTracker.close();
    }
    
    private synchronized void refreshSubsystems() {
        subsystems.clear();
        /*for (Subsystem sub : subsystem.getChildrenSubsystems()) {
            subsystems.put(sub.getSubsystemId(), sub);
        }*/
        
        final String filter = "(SubsystemParentId=" + subsystem.getSubsystemId() + ")";
                  
        try {
            ServiceReference[] srs = context.getServiceReferences(SubsystemAdmin.class.getName(), filter);
            if (srs != null) {
                for (ServiceReference sr : srs) {
                    SubsystemAdmin childSubAdmin = (SubsystemAdmin)context.getService(sr);
                    Subsystem childSub = childSubAdmin.getSubsystem();
                    subsystems.put(childSub.getSubsystemId(), childSub);
                    context.ungetService(sr);
                }
                
            }
        } catch (InvalidSyntaxException e) {
            // ignore
        }
    }
    
    public Subsystem getSubsystem(long id) {
        refreshSubsystems();
        synchronized (subsystems) {
            for (Subsystem s : subsystems.values()) {
                if (s.getSubsystemId() == id) {
                    return s;
                }
            }
            return null;
        }
    }

    public Subsystem getSubsystem(String symbolicName, Version version) {
        refreshSubsystems();
        synchronized (subsystems) {
            for (Subsystem s : subsystems.values()) {
                if (s.getSymbolicName().equals(symbolicName) && s.getVersion().equals(version)) {
                    return s;
                }
            }
            return null;
        }
    }
    
    public Collection<Subsystem> getSubsystems() {
        refreshSubsystems();
        synchronized (subsystems) {
            return Collections.unmodifiableCollection(new ArrayList(subsystems.values()));
        }
    }

    public Subsystem install(String url) {
        return install(url, null);
    }

    public synchronized Subsystem install(String url, final InputStream is) throws SubsystemException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Installing subsystem url {}", url);
        }
        // let's check if the subsystem has been installed or not first before proceed installation
        Subsystem toReturn = getInstalledSubsytem(url);      
        if (toReturn != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("A subsystem containing the same location identifier {} is already installed", url);
            }
            return toReturn;
        }
        
        Resource subsystemResource = new ResourceImpl(null, null, SubsystemConstants.RESOURCE_TYPE_SUBSYSTEM, url, Collections.<String, String>emptyMap()) {
            @Override
            public InputStream open() throws IOException {
                if (is != null) {
                    return is;
                }
                return super.open();
            }
        };
        SubsystemResourceProcessor processor = new SubsystemResourceProcessor();
        SubsystemResourceProcessor.SubsystemSession session = processor.createSession(this);
        boolean success = false;
        try {
            session.process(subsystemResource);
            session.prepare();
            session.commit();
            success = true;
        } finally {
            if (!success) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Installing subsystem url {} is not successful, rollback now", url);
                }
                session.rollback();
            }
        }

        // let's get the one we just installed
        if (success) {
            toReturn = getInstalledSubsytem(url);       
            if (toReturn != null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Installing subsystem url {} is successful", url);
                }
                
                // emit the subsystem event
                eventDispatcher.subsystemEvent(new SubsystemEvent(SubsystemEvent.Type.INSTALLED, System.currentTimeMillis(), toReturn));

                return toReturn;
            }
        }
        
        throw new IllegalStateException();
    }

    public void update(Subsystem subsystem) throws SubsystemException {
        update(subsystem, null);
    }

    public void update(final Subsystem subsystem, final InputStream is) throws SubsystemException {
        if (subsystem.getState().equals(Subsystem.State.UNINSTALLED)) {
            throw new IllegalStateException("Unable to update subsystem as subsystem is already uninstalled");
        }
        
        if (subsystem.getState().equals(Subsystem.State.ACTIVE) 
                || subsystem.getState().equals(Subsystem.State.STARTING) 
                || subsystem.getState().equals(Subsystem.State.STOPPING)) {
            subsystem.stop();
        }
        
        Resource subsystemResource = new ResourceImpl(subsystem.getSymbolicName(), subsystem.getVersion(), SubsystemConstants.RESOURCE_TYPE_SUBSYSTEM, subsystem.getLocation(), Collections.<String, String>emptyMap()) {
            @Override
            public InputStream open() throws IOException {
                if (is != null) {
                    return is;
                }
                // subsystem-updatelocation specified the manifest has higher priority than subsystem original location
                String subsystemLoc = subsystem.getHeaders().get(SubsystemConstants.SUBSYSTEM_UPDATELOCATION);
                if (subsystemLoc != null && subsystemLoc.length() > 0) {
                    // we have a subsystem location let us use it
                    return new URL(subsystemLoc).openStream();
                }
                return super.open();
            }
        };
        SubsystemResourceProcessor processor = new SubsystemResourceProcessor();
        SubsystemResourceProcessor.SubsystemSession session = processor.createSession(this);
        boolean success = false;
        try {
            session.process(subsystemResource);
            session.prepare();
            session.commit();
            success = true;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Updating subsystem {} is successful", subsystem.getSymbolicName());
            }
 
            // emit the subsystem event
            eventDispatcher.subsystemEvent(new SubsystemEvent(SubsystemEvent.Type.UPDATED, System.currentTimeMillis(), subsystem));
 
        } finally {
            if (!success) {
                session.rollback();
            }
        }
    }

    public void uninstall(Subsystem subsystem) {
        if (subsystem.getState().equals(Subsystem.State.UNINSTALLED)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to uninstall subsystem {} as subsystem is already uninstalled", subsystem.getSymbolicName());
            }
            return;
        }
        
        Resource subsystemResource = new ResourceImpl(subsystem.getSymbolicName(), subsystem.getVersion(), SubsystemConstants.RESOURCE_TYPE_SUBSYSTEM, subsystem.getLocation(), Collections.<String, String>emptyMap());
        SubsystemResourceProcessor processor = new SubsystemResourceProcessor();
        SubsystemResourceProcessor.SubsystemSession session = processor.createSession(this);
        boolean success = false;
        try {
            session.dropped(subsystemResource);
            session.prepare();
            session.commit();
            success = true;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Uninstalling subsystem {} is successful", subsystem.getSymbolicName());
            }
            
            // emit the subsystem event
            eventDispatcher.subsystemEvent(new SubsystemEvent(SubsystemEvent.Type.UNINSTALLED, System.currentTimeMillis(), subsystem));
 
        } finally {
            if (!success) {
                session.rollback();
            }
        }
    }

    public boolean cancel() {
        // TODO
        return false;
    }

    private Subsystem getInstalledSubsytem(String url) {
        for (Subsystem ss : getSubsystems()) {
            if (url.equals(ss.getLocation())) {
                return ss;
            }
        }
        return null;
    }
    
    // return the scope admin associated with the subsystemadmin.
    protected ScopeAdmin getScopeAdmin() {
        return this.scopeAdmin;
    }
    
    public Subsystem getSubsystem() {
        return this.subsystem;
    }

    public Subsystem getParentSubsystem() {
        return this.parentSubsystem;
    }
}

