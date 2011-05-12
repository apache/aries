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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.aries.subsystem.Subsystem;
import org.apache.aries.subsystem.SubsystemConstants;
import org.apache.aries.subsystem.SubsystemException;
import org.apache.aries.subsystem.core.SubsystemEvent;
import org.apache.aries.subsystem.spi.Resource;
import org.apache.aries.subsystem.spi.ResourceOperation;
import org.eclipse.equinox.region.Region;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.coordinator.Coordination;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubsystemImpl implements Subsystem {
	private static final Logger LOGGER = LoggerFactory.getLogger(SubsystemImpl.class);
	
	private static long lastId = -1;

	private synchronized static long getNextId() {
		if (Long.MAX_VALUE == lastId)
			throw new IllegalStateException("Next ID would exceed Long.MAX_VALUE");
		// First ID will be 1.
		return ++lastId;
	}
	
    final long id;
    final SubsystemEventDispatcher eventDispatcher;
    final Region region;
    Map<String, String> headers;
    private final ServiceTracker serviceTracker;
    private final BundleContext context;
    private final Map<Long, Subsystem> subsystems = new HashMap<Long, Subsystem>();
    private final Subsystem parent;
    private final String location;

    public SubsystemImpl(Region region, Map<String, String> headers, Subsystem parent, String location) {
        this.region = region;
        this.id = getNextId();
        this.eventDispatcher = Activator.getEventDispatcher();
        this.headers = headers;
        this.parent = parent;
        this.location = location;
        Filter filter = null;
        try {
            filter = FrameworkUtil.createFilter("(&("
                    + Constants.OBJECTCLASS + "=" + Subsystem.class.getName() + "))");
        } catch (InvalidSyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        this.context = Activator.getBundleContext();
        serviceTracker = new ServiceTracker(context, filter,
                new ServiceTrackerCustomizer() {

                    public Object addingService(ServiceReference reference) {
                        // adding new service, update admins map
                        Subsystem s = (Subsystem)context.getService(reference);
                        if (s.getParent() != null && s.getParent().getSubsystemId() == id) {
                            // it is the child subsystems for the current subsystem
                            synchronized (subsystems) {
                                subsystems.put(s.getSubsystemId(), s);
                            }
                            return s;
                        }

                        return null;
                    }

                    public void modifiedService(ServiceReference reference,
                            Object service) {
                        // TODO Auto-generated method stub

                    }

                    public void removedService(ServiceReference reference,
                            Object service) {
                        Subsystem s = (Subsystem)service;
                        if (s.getParent().getSubsystemId() == id) {
                            // it is the child subsystems for the current subsystem
                            synchronized (subsystems) {
                                subsystems.remove(s.getSubsystemId());
                            }
                        }
                    }

                });
        // TODO This needs to be closed somewhere.
        serviceTracker.open();
    }

    public State getState() {
        // check bundles status
        Collection<Bundle> bundles = getConstituents();
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
            Collection<Bundle> bundles = getBundles();
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
            Collection<Bundle> bundles = getBundles();
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
        return location;
    }

    public String getSymbolicName() {
        return region.getName();
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

    public Collection<Bundle> getConstituents() {
    	return getBundles();
    }
    
    protected Region getRegion() {
        return region;
    }

    public Collection<Subsystem> getChildren() {
        synchronized (subsystems) {
            return Collections.unmodifiableCollection(new ArrayList(subsystems.values()));
        }
    }

	public Subsystem getParent() {
		return parent;
	}

	public Subsystem install(String url) throws SubsystemException {
		return install(url, null);
	}

	public Subsystem install(final String url, final InputStream is) throws SubsystemException {
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
        final Resource subsystemResource = new ResourceImpl(null, null, SubsystemConstants.RESOURCE_TYPE_SUBSYSTEM, url, null) {
            @Override
            public InputStream open() throws IOException {
                if (is != null) {
                    return is;
                }
                return super.open();
            }
        };
        SubsystemResourceProcessor processor = new SubsystemResourceProcessor(context);
        final Coordination coordination = Activator.getCoordinator().create("subsystem", 0);
        try {
	        processor.process(new ResourceOperation() {
				public void completed() {
					eventDispatcher.subsystemEvent(new SubsystemEvent(SubsystemEvent.Type.INSTALLED, System.currentTimeMillis(), getInstalledSubsytem(url)));
				}
	
				public Coordination getCoordination() {
					return coordination;
				}
	
				public Resource getResource() {
					return subsystemResource;
				}
	
				public Map<String, Object> getContext() {
					Map<String, Object> c = new HashMap<String, Object>();
					c.put("subsystem", SubsystemImpl.this);
					return c;
				}
	
				public Type getType() {
					return Type.INSTALL;
				}
	        });
	        return getInstalledSubsytem(url); 
        }
        catch (Exception e) {
        	coordination.fail(e);
        }
        finally {
        	coordination.end();
        }
        throw new IllegalStateException();
	}

	public void uninstall() throws SubsystemException {
		if (getState().equals(Subsystem.State.UNINSTALLED)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to uninstall subsystem {} as subsystem is already uninstalled", getSymbolicName());
            }
            return;
        }
        final Resource subsystemResource = new ResourceImpl(getSymbolicName(), getVersion(), SubsystemConstants.RESOURCE_TYPE_SUBSYSTEM, getLocation(), Collections.<String, Object>emptyMap());
        SubsystemResourceProcessor processor = new SubsystemResourceProcessor(context);
        final Coordination coordination = Activator.getCoordinator().create("subsystem", 0);
        try {
	        processor.process(new ResourceOperation() {
				public void completed() {
					eventDispatcher.subsystemEvent(new SubsystemEvent(SubsystemEvent.Type.UNINSTALLED, System.currentTimeMillis(), SubsystemImpl.this));
				}
	
				public Coordination getCoordination() {
					return coordination;
				}
	
				public Resource getResource() {
					return subsystemResource;
				}
	
				public Map<String, Object> getContext() {
					Map<String, Object> c = new HashMap<String, Object>();
					c.put("subsystem", SubsystemImpl.this);
					return c;
				}
	
				public Type getType() {
					return Type.UNINSTALL;
				}
	        }); 
        }
        catch (Exception e) {
        	coordination.fail(e);
        }
        finally {
        	coordination.end();
        }
	}

	public void update() throws SubsystemException {
		update(null);
	}

	public void update(final InputStream is) throws SubsystemException {
		if (getState().equals(Subsystem.State.UNINSTALLED)) {
            throw new IllegalStateException("Unable to update subsystem as subsystem is already uninstalled");
        }
        if (getState().equals(Subsystem.State.ACTIVE) 
                || getState().equals(Subsystem.State.STARTING) 
                || getState().equals(Subsystem.State.STOPPING)) {
            stop();
        }
        final Resource subsystemResource = new ResourceImpl(getSymbolicName(), getVersion(), SubsystemConstants.RESOURCE_TYPE_SUBSYSTEM, getLocation(), Collections.<String, Object>emptyMap()) {
            @Override
            public InputStream open() throws IOException {
                if (is != null) {
                    return is;
                }
                // subsystem-updatelocation specified the manifest has higher priority than subsystem original location
                String subsystemLoc = getHeaders().get(SubsystemConstants.SUBSYSTEM_UPDATELOCATION);
                if (subsystemLoc != null && subsystemLoc.length() > 0) {
                    // we have a subsystem location let us use it
                    return new URL(subsystemLoc).openStream();
                }
                return super.open();
            }
        };
        SubsystemResourceProcessor processor = new SubsystemResourceProcessor(context);
        
        final Coordination coordination = Activator.getCoordinator().create("subsystem", 0);
        try {
	        processor.process(new ResourceOperation() {
				public void completed() {
					eventDispatcher.subsystemEvent(new SubsystemEvent(SubsystemEvent.Type.UPDATED, System.currentTimeMillis(), SubsystemImpl.this));
				}
	
				public Coordination getCoordination() {
					return coordination;
				}
	
				public Resource getResource() {
					return subsystemResource;
				}
	
				public Map<String, Object> getContext() {
					Map<String, Object> c = new HashMap<String, Object>();
					c.put("subsystem", SubsystemImpl.this);
					return c;
				}
	
				public Type getType() {
					return Type.INSTALL;
				}
	        }); 
        }
        catch (Exception e) {
        	coordination.fail(e);
        }
        finally {
        	coordination.end();
        }
	}
	
	private Collection<Bundle> getBundles() {
		Set<Long> bundleIds = region.getBundleIds();
    	Collection<Bundle> bundles = new HashSet<Bundle>(bundleIds.size());
    	for (Long bundleId : bundleIds) {
    		bundles.add(context.getBundle(bundleId));
    	}
        return bundles;
	}
	
	private Subsystem getInstalledSubsytem(String url) {
        for (Subsystem ss : subsystems.values()) {
            if (url.equals(ss.getLocation())) {
                return ss;
            }
        }
        return null;
    }
}