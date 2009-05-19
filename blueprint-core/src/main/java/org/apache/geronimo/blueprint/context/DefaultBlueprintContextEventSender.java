/**
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
package org.apache.geronimo.blueprint.context;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.geronimo.blueprint.BlueprintConstants;
import org.apache.geronimo.blueprint.BlueprintContextEventSender;
import org.apache.geronimo.blueprint.BlueprintStateManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.context.BlueprintContextListener;
import org.osgi.service.blueprint.context.EventConstants;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class DefaultBlueprintContextEventSender implements BlueprintContextEventSender, EventConstants, BlueprintStateManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBlueprintContextEventSender.class);

    private final ServiceRegistration registration;
    private final Bundle extenderBundle;
    private final ServiceTracker eventAdminServiceTracker;
    private final ServiceTracker contextListenerTracker;
    private final Map<Bundle, Object> states;

    public DefaultBlueprintContextEventSender(final BundleContext bundleContext) {
        this.extenderBundle = bundleContext.getBundle();
        this.eventAdminServiceTracker = new ServiceTracker(bundleContext, EventAdmin.class.getName(), null);
        this.eventAdminServiceTracker.open();
        this.contextListenerTracker = new ServiceTracker(bundleContext, BlueprintContextListener.class.getName(), new ServiceTrackerCustomizer() {
            public Object addingService(ServiceReference reference) {
                BlueprintContextListener listener = (BlueprintContextListener) bundleContext.getService(reference);
                sendInitialEvents(listener);
                return listener;
            }
            public void modifiedService(ServiceReference reference, Object service) {
            }
            public void removedService(ServiceReference reference, Object service) {
                bundleContext.ungetService(reference);
            }
        });
        this.contextListenerTracker.open();
        this.states = new ConcurrentHashMap<Bundle, Object>();
        this.registration = bundleContext.registerService(BlueprintStateManager.class.getName(), this, null);
    }

    protected void sendInitialEvents(BlueprintContextListener listener) {
        for (Map.Entry<Bundle, Object> entry : states.entrySet()) {
            callListener(listener, entry.getKey(), entry.getValue());
        }
    }

    public int getState(Bundle bundle) {
        Object obj = states.get(bundle);
        if (obj instanceof Integer) {
            return (Integer) obj;
        } else if (obj instanceof Throwable) {
            return FAILED;
        } else {
            return UNKNOWN;
        }
    }

    public Throwable getFailure(Bundle bundle) {
        Object obj = states.get(bundle);
        if (obj instanceof Throwable) {
            return (Throwable) obj;
        } else {
            return null;
        }
    }

    public void sendCreating(Bundle bundle) {
        states.put(bundle,  CREATING);
        sendEvent(bundle, TOPIC_CREATING, null, null, null);
    }

    public void sendCreated(Bundle bundle) {
        states.put(bundle,  CREATED);
        sendEvent(bundle, TOPIC_CREATED, null, null, null);
    }

    public void sendDestroying(Bundle bundle) {
        states.put(bundle,  DESTROYING);
        sendEvent(bundle, TOPIC_DESTROYING, null, null, null);
    }

    public void sendDestroyed(Bundle bundle) {
        states.put(bundle,  DESTROYED);
        sendEvent(bundle, TOPIC_DESTROYED, null, null, null);
    }

    public void sendWaiting(Bundle bundle, String[] serviceObjectClass, String serviceFilter) {
        states.put(bundle,  WAITING);
        sendEvent(bundle, TOPIC_WAITING, null, serviceObjectClass, serviceFilter);
    }

    public void sendFailure(Bundle bundle, Throwable cause) {
        states.put(bundle,  cause != null ? cause : FAILED);
        sendEvent(bundle, TOPIC_FAILURE, cause, null, null);
    }

    public void sendFailure(Bundle bundle, Throwable cause, String[] serviceObjectClass, String serviceFilter) {
        states.put(bundle,  cause != null ? cause : FAILED);
        sendEvent(bundle, TOPIC_FAILURE, cause, serviceObjectClass, serviceFilter);
    }

    public void sendEvent(Bundle bundle, String topic, Throwable cause, String[] serviceObjectClass, String serviceFilter) {

        LOGGER.debug("Sending blueprint context event {} for bundle {}", topic, bundle.getSymbolicName());

        callListeners(bundle, topic, cause);

        EventAdmin eventAdmin = getEventAdmin();
        if (eventAdmin == null) {
            return;
        }

        Dictionary<String,Object> props = new Hashtable<String,Object>();
        props.put(org.osgi.service.event.EventConstants.BUNDLE_SYMBOLICNAME, bundle.getSymbolicName());
        props.put(org.osgi.service.event.EventConstants.BUNDLE_ID, bundle.getBundleId());
        props.put(org.osgi.service.event.EventConstants.BUNDLE, bundle);
        Version version = getBundleVersion(bundle);
        if (version != null) {
            props.put(BlueprintConstants.BUNDLE_VERSION, version);
        }
        props.put(org.osgi.service.event.EventConstants.TIMESTAMP, System.currentTimeMillis());
        props.put(EventConstants.EXTENDER_BUNDLE, extenderBundle);
        props.put(EventConstants.EXTENDER_ID, extenderBundle.getBundleId());
        props.put(EventConstants.EXTENDER_SYMBOLICNAME, extenderBundle.getSymbolicName());
        
        if (cause != null) {
            props.put(org.osgi.service.event.EventConstants.EXCEPTION, cause);
        }
        if (serviceObjectClass != null) {
            props.put(org.osgi.service.event.EventConstants.SERVICE_OBJECTCLASS, serviceObjectClass);
        }
        if (serviceFilter != null) {
            props.put(BlueprintConstants.SERVICE_FILTER, serviceFilter);
        }

        Event event = new Event(topic, props);
        eventAdmin.postEvent(event);
    }

    private void callListeners(Bundle bundle, String topic, Throwable cause) {
        boolean created = TOPIC_CREATED.equals(topic);
        boolean failure = TOPIC_FAILURE.equals(topic);
        if (created || failure) {
            Object[] listeners = contextListenerTracker.getServices();
            if (listeners != null) {
                for (Object listener : listeners) {
                    callListener((BlueprintContextListener) listener, bundle, created ? CREATED : cause != null ? cause : FAILED);
                }
            }
        }
    }

    private void callListener(BlueprintContextListener listener, Bundle bundle, Object state) {
        // TODO: listener is missing a few methods to replay the state (at least WAITING events)
        try {
            if (state instanceof Integer) {
                if ((Integer) state == CREATED) {
                    listener.contextCreated(bundle);
                } else if ((Integer) state == FAILED) {
                    listener.contextCreationFailed(bundle, null);
                }
            } else if (state instanceof Throwable) {
                listener.contextCreationFailed(bundle, (Throwable) state);
            }
        } catch (Throwable t) {
            LOGGER.info("Error calling blueprint context listener", t);
        }
    }

    private static Version getBundleVersion(Bundle bundle) {
        Dictionary headers = bundle.getHeaders();
        String version = (String)headers.get(Constants.BUNDLE_VERSION);
        return (version != null) ? Version.parseVersion(version) : null;
    }
    
    private EventAdmin getEventAdmin() {
        return (EventAdmin)this.eventAdminServiceTracker.getService();
    }

    public void destroy() {
        this.registration.unregister();
        this.eventAdminServiceTracker.close();
        this.contextListenerTracker.close();
    }
}
