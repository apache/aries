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
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import org.apache.aries.subsystem.Subsystem;
import org.apache.aries.subsystem.SubsystemAdmin;
import org.apache.aries.subsystem.SubsystemConstants;
import org.apache.aries.subsystem.SubsystemEvent;
import org.apache.aries.subsystem.SubsystemException;
import org.apache.aries.subsystem.spi.Resource;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.Version;
import org.osgi.service.composite.CompositeBundle;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubsystemAdminImpl implements SubsystemAdmin {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubsystemAdminImpl.class);
    private static final Version SUBSYSTEM_MANIFEST_VERSION = new Version("1.0");

    final BundleContext context;
    final Map<Long, Subsystem> subsystems = new HashMap<Long, Subsystem>();
    final SubsystemEventDispatcher eventDispatcher;
    final ServiceTracker executorTracker;
    final HashMap<String, SubsystemFutureTask> installInProgress = new HashMap<String, SubsystemFutureTask>();
    final HashMap<String, SubsystemFutureTask> updateInProgress = new HashMap<String, SubsystemFutureTask>();
    
    public SubsystemAdminImpl(BundleContext context, SubsystemEventDispatcher eventDispatcher) {
        this.context = context;
        this.eventDispatcher = eventDispatcher;
        this.executorTracker = new ServiceTracker(context, Executor.class.getName(), null);
        this.executorTracker.open();
        // Track subsystems
        synchronized (subsystems) {
            this.context.addBundleListener(new SynchronousBundleListener() {
                public void bundleChanged(BundleEvent event) {
                    SubsystemAdminImpl.this.bundleChanged(event);
                }
            });
            loadSubsystems();
        }
    }

    public void dispose() {
        executorTracker.close();
    }

    public void bundleChanged(BundleEvent event) {
        synchronized (subsystems) {
            Bundle bundle = event.getBundle();
            if (event.getType() == BundleEvent.UNINSTALLED) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Removing bundle symbolic name {} version {} from subsystems map being tracked", bundle.getSymbolicName(), bundle.getVersion());
                }
                subsystems.remove(bundle.getBundleId());
            }
            if (event.getType() == BundleEvent.UPDATED) {
                Subsystem s = isSubsystem(bundle);

                // If this is a subsystem we have in progress, then set the result on the SubsystemFutureTask
                if (updateInProgress.containsKey(s.getLocation())) {
                    SubsystemFutureTask task = updateInProgress.remove(s.getLocation());
                    task.set(s);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(
                                "Update of subsystem url {} is successful",
                                s.getLocation());
                    }

                    // emit the subsystem event
                    eventDispatcher.subsystemEvent(new SubsystemEvent(
                            SubsystemEvent.Type.UPDATED, System.currentTimeMillis(), s));

                }

            }
            if (event.getType() == BundleEvent.INSTALLED) {
                Subsystem s = isSubsystem(bundle);
                if (s != null) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Adding bundle symbolic name {} version {} to subsystems map being tracked", bundle.getSymbolicName(), bundle.getVersion());
                    }
                    subsystems.put(s.getSubsystemId(), s);
                    
                    // If this is a subsystem we have in progress, then set the result on the SubsystemFutureTask
                    if (installInProgress.containsKey(s.getLocation())) {
                        SubsystemFutureTask task = installInProgress.remove(s
                                .getLocation());
                        task.set(s);
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug(
                                    "Install of subsystem url {} is successful",
                                    s.getLocation());
                        }

                        // emit the subsystem event
                        eventDispatcher.subsystemEvent(new SubsystemEvent(
                                SubsystemEvent.Type.INSTALLED, System.currentTimeMillis(), s));

                    }
                }
            }
            if (event.getType() == BundleEvent.RESOLVED) {
                Subsystem s = isSubsystem(bundle);
                if (s != null) {
                    // emit the subsystem resolved event
                    eventDispatcher.subsystemEvent(new SubsystemEvent(SubsystemEvent.Type.RESOLVED, System.currentTimeMillis(), s));
                }
            }
        }
    }

    protected void loadSubsystems() {
        synchronized (subsystems) {
            subsystems.clear();
            for (Bundle bundle : context.getBundles()) {
                Subsystem s = isSubsystem(bundle);
                if (s != null) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Adding bundle symbolic name {} version {} to subsystems map being tracked", bundle.getSymbolicName(), bundle.getVersion());
                    }
                    subsystems.put(s.getSubsystemId(), s);
                }
            }
        }
    }

    protected Subsystem isSubsystem(Bundle bundle) {
        if (bundle instanceof CompositeBundle) {
            // it is important not to use bundle.getSymbolicName() here as that would not contain the directives we need.
            String bsn = (String) bundle.getHeaders().get(Constants.BUNDLE_SYMBOLICNAME);
            Clause[] bsnClauses = Parser.parseHeader(bsn);
            if ("true".equals(bsnClauses[0].getDirective(SubsystemConstants.SUBSYSTEM_DIRECTIVE))) {
                return new SubsystemImpl(this, (CompositeBundle) bundle, eventDispatcher);
            }
        }
        return null;
    }

    public Subsystem getSubsystem(long id) {
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
        synchronized (subsystems) {
            return Collections.unmodifiableCollection(new ArrayList(subsystems.values()));
        }
    }

    public Future<Subsystem> install(String url) {
        return install(url, null);
    }

    public synchronized Future<Subsystem> install(final String url, final InputStream is) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Installing subsystem url {}", url);
        }
        
        // check if the subsystem install is already in progress
        Future<Subsystem> futureToReturn = installInProgress.get(url);
        if (futureToReturn != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("A Future<Subsystem> containing the same location identifier {} is already in installing.", url);
            }       
            return futureToReturn;
        }

        // check if the subsystem has already been installed before proceeding with the installation
        Subsystem toReturn = getInstalledSubsytem(url);      
        if (toReturn != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("A subsystem containing the same location identifier {} is already installed", url);
            }
            return new ImmediateFuture<Subsystem>(toReturn);
        }
        
        // Create a new Future to handle the installation of the Subsystem
        SubsystemFutureTask installTask = new SubsystemFutureTask(new Runnable() {
            public void run() {
                Resource subsystemResource = new ResourceImpl(null, null,
                        SubsystemConstants.RESOURCE_TYPE_SUBSYSTEM, url,
                        Collections.<String, String> emptyMap()) {
                    @Override
                    public InputStream open() throws IOException {
                        if (is != null) {
                            return is;
                        }
                        return super.open();
                    }
                };
                SubsystemResourceProcessor processor = new SubsystemResourceProcessor();
                SubsystemResourceProcessor.SubsystemSession session = processor
                        .createSession(context);
                boolean success = false;
                try {
                    session.process(subsystemResource);
                    session.prepare();
                    session.commit();
                    success = true;
                    
                } finally {
                    if (!success) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug(
                                    "Installing subsystem url {} is not successful, rollback now",
                                    url);
                        }
                        session.rollback();
                    }
                }                
            }
        }, context, url, is);
        
        // Kick off the future and return it
        installInProgress.put(url, installTask);

        execute(installTask);

        return installTask;
    }

    /**
     * Execute the Future either using an Executor from the service registry or
     * a new Thread.
     * 
     * @param installTask
     */
    private void execute(SubsystemFutureTask installTask) {
        Executor ex = (Executor) executorTracker.getService();
        if (ex != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Using Executor to execute Subsystem Install");
            }
            ex.execute(installTask);
        } else {
            // Create own thread if no executor is available
            Thread thread = new Thread(installTask);
            thread.start();
        }
    }

    public Future<Subsystem> update(Subsystem subsystem) throws SubsystemException {
        return update(subsystem, null);
    }

    public Future<Subsystem> update(final Subsystem subsystem, final InputStream is) throws SubsystemException {
        if (subsystem.getState().equals(Subsystem.State.UNINSTALLED)) {
            throw new IllegalStateException("Unable to update subsystem as subsystem is already uninstalled");
        }
        
        // check if the subsystem install is already in progress
        Future<Subsystem> futureToReturn = updateInProgress.get(subsystem.getLocation());
        if (futureToReturn != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("A Future<Subsystem> containing the same location identifier {} is already updating.", subsystem.getLocation());
            }       
            return futureToReturn;
        }        

        // Create a new Future to handle the update of the Subsystem
        SubsystemFutureTask updateTask = new SubsystemFutureTask(
                new Runnable() {
                    public void run() {
                        if (subsystem.getState().equals(Subsystem.State.ACTIVE)
                                || subsystem.getState().equals(
                                        Subsystem.State.STARTING)
                                || subsystem.getState().equals(
                                        Subsystem.State.STOPPING)) {
                            subsystem.stop();
                        }
                        Resource subsystemResource = new ResourceImpl(
                                subsystem.getSymbolicName(),
                                subsystem.getVersion(),
                                SubsystemConstants.RESOURCE_TYPE_SUBSYSTEM,
                                subsystem.getLocation(),
                                Collections.<String, String> emptyMap()) {
                            @Override
                            public InputStream open() throws IOException {
                                if (is != null) {
                                    return is;
                                }
                                // subsystem-updatelocation specified the
                                // manifest has higher priority than subsystem
                                // original location
                                String subsystemLoc = subsystem
                                        .getHeaders()
                                        .get(SubsystemConstants.SUBSYSTEM_UPDATELOCATION);
                                if (subsystemLoc != null
                                        && subsystemLoc.length() > 0) {
                                    // we have a subsystem location lets us use it
                                    return new URL(subsystemLoc).openStream();
                                }
                                return super.open();
                            }
                        };
                        SubsystemResourceProcessor processor = new SubsystemResourceProcessor();
                        SubsystemResourceProcessor.SubsystemSession session = processor
                                .createSession(context);
                        boolean success = false;
                        try {
                            session.process(subsystemResource);
                            session.prepare();
                            session.commit();
                            success = true;
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug(
                                        "Updating subsystem {} is successful",
                                        subsystem.getSymbolicName());
                            }

                        } finally {
                            if (!success) {
                                session.rollback();
                            }
                        }
                    }
                }, context, subsystem.getLocation(), is);

        // Kick off the future and return it
        updateInProgress.put(subsystem.getLocation(), updateTask);

        execute(updateTask);

        return updateTask;
        
    }

    public void uninstall(Subsystem subsystem) {
        if (subsystem.getState().equals(Subsystem.State.UNINSTALLED)) {
            throw new IllegalStateException("Unable to uninstall subsystem as subsystem is already uninstalled");
        }
        
        Resource subsystemResource = new ResourceImpl(subsystem.getSymbolicName(), subsystem.getVersion(), SubsystemConstants.RESOURCE_TYPE_SUBSYSTEM, subsystem.getLocation(), Collections.<String, String>emptyMap());
        SubsystemResourceProcessor processor = new SubsystemResourceProcessor();
        SubsystemResourceProcessor.SubsystemSession session = processor.createSession(context);
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

    private Subsystem getInstalledSubsytem(String url) {
        for (Subsystem ss : getSubsystems()) {
            if (url.equals(ss.getLocation())) {
                return ss;
            }
        }
        return null;
    }
        
}
