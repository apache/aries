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
package org.apache.aries.blueprint.container;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.aries.blueprint.utils.JavaUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.blueprint.container.BlueprintEvent;
import org.osgi.service.blueprint.container.BlueprintListener;
import org.osgi.service.blueprint.container.EventConstants;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: javadoc
 *
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class BlueprintEventDispatcher implements BlueprintListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintEventDispatcher.class);

    private final BlueprintListener eventAdminListener;
    private final ServiceTracker containerListenerTracker;
    private final Map<Bundle, BlueprintEvent> states;
    private final ExecutorService executor;

    public BlueprintEventDispatcher(final BundleContext bundleContext) {
        this.states = new ConcurrentHashMap<Bundle, BlueprintEvent>();
        this.executor = Executors.newSingleThreadExecutor();
        BlueprintListener listener = null;
        try {
            getClass().getClassLoader().loadClass("org.osgi.service.event.EventAdmin");
            listener = new EventAdminListener(bundleContext);
        } catch (Throwable t) {
            // Ignore, if the EventAdmin package is not available, just don't use it
        }
        this.eventAdminListener = listener;
        this.containerListenerTracker = new ServiceTracker(bundleContext, BlueprintListener.class.getName(), new ServiceTrackerCustomizer() {
            public Object addingService(ServiceReference reference) {
                BlueprintListener listener = (BlueprintListener) bundleContext.getService(reference);
                sendInitialEvents(listener);
                return listener;
            }
            public void modifiedService(ServiceReference reference, Object service) {
            }
            public void removedService(ServiceReference reference, Object service) {
                bundleContext.ungetService(reference);
            }
        });
        this.containerListenerTracker.open();
    }

    protected void sendInitialEvents(BlueprintListener listener) {
        if (states != null) {
            for (Map.Entry<Bundle, BlueprintEvent> entry : states.entrySet()) {
                listener.blueprintEvent(new BlueprintEvent(entry.getValue(), true));
            }
        }
    }

    public void blueprintEvent(final BlueprintEvent event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Sending blueprint container event {} for bundle {}", toString(event), event.getBundle().getSymbolicName());
        }
        states.put(event.getBundle(), event);
        executor.submit(new Runnable() {
            public void run() {
                callListeners(event);
                if (eventAdminListener != null) {
                    eventAdminListener.blueprintEvent(event);
                }
            }
        });
    }

    private String toString(BlueprintEvent event) {
        return "BlueprintEvent[type=" + getEventType(event.getType())
                        + (event.getDependencies() != null ? ", dependencies=" + Arrays.asList(event.getDependencies()) : "")
                        + (event.getCause() != null ? ", exception=" + event.getCause().getMessage() : "")
                        + "]";
    }

    private String getEventType(int type) {
        switch (type) {
            case BlueprintEvent.CREATING:
                return "CREATING";
            case BlueprintEvent.CREATED:
                return "CREATED";
            case BlueprintEvent.DESTROYING:
                return "DESTROYING";
            case BlueprintEvent.DESTROYED:
                return "DESTROYED";
            case BlueprintEvent.FAILURE:
                return "FAILURE";
            case BlueprintEvent.GRACE_PERIOD:
                return "GRACE_PERIOD";
            case BlueprintEvent.WAITING:
                return "WAITING";
            default:
                return "UNKNOWN";
        }
    }

    private void callListeners(BlueprintEvent event) {
        Object[] listeners = containerListenerTracker.getServices();
        if (listeners != null) {
            for (Object listener : listeners) {
                try {
                    ((BlueprintListener) listener).blueprintEvent(event);
                } catch (Throwable e) {
                    // an exception in a BlueprintListener should not terminate the
                    // dispatch chain to other listeners
                }
            }
        }
    }

    protected void contextDestroyed(Bundle bundle) {
        states.remove(bundle);
    }

    public void destroy() {
        this.executor.shutdown();
        // wait for the queued tasks to execute
        try {
            this.executor.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // ignore
        }
        this.containerListenerTracker.close();
    }

    static class EventAdminListener implements BlueprintListener {

        private ServiceTracker tracker;

        public EventAdminListener(BundleContext context) {
            tracker = new ServiceTracker(context, EventAdmin.class.getName(), null);
            tracker.open();
        }

        public void blueprintEvent(BlueprintEvent event) {
            EventAdmin eventAdmin = (EventAdmin) tracker.getService();
            if (eventAdmin == null) {
                return;
            }

            Dictionary<String,Object> props = new Hashtable<String,Object>();
            props.put(EventConstants.TYPE, event.getType());
            props.put(EventConstants.EVENT, event);
            props.put(EventConstants.TIMESTAMP, event.getTimestamp());
            props.put(EventConstants.BUNDLE, event.getBundle());
            props.put(EventConstants.BUNDLE_SYMBOLICNAME, event.getBundle().getSymbolicName());
            props.put(EventConstants.BUNDLE_ID, event.getBundle().getBundleId());
            props.put(EventConstants.BUNDLE_VERSION, JavaUtils.getBundleVersion(event.getBundle()));
            props.put(EventConstants.EXTENDER_BUNDLE, event.getExtenderBundle());
            props.put(EventConstants.EXTENDER_BUNDLE_ID, event.getExtenderBundle().getBundleId());
            props.put(EventConstants.EXTENDER_BUNDLE_SYMBOLICNAME, event.getExtenderBundle().getSymbolicName());
            props.put(EventConstants.EXTENDER_BUNDLE_VERSION, JavaUtils.getBundleVersion(event.getExtenderBundle()));

            if (event.getCause() != null) {
                props.put(EventConstants.CAUSE, event.getCause());
            }
            if (event.getDependencies() != null) {
                props.put(EventConstants.DEPENDENCIES, event.getDependencies());
            }
            String topic;
            switch (event.getType()) {
                case BlueprintEvent.CREATING:
                    topic = EventConstants.TOPIC_CREATING;
                    break;
                case BlueprintEvent.CREATED:
                    topic = EventConstants.TOPIC_CREATED;
                    break;
                case BlueprintEvent.DESTROYING:
                    topic = EventConstants.TOPIC_DESTROYING;
                    break;
                case BlueprintEvent.DESTROYED:
                    topic = EventConstants.TOPIC_DESTROYED;
                    break;
                case BlueprintEvent.FAILURE:
                    topic = EventConstants.TOPIC_FAILURE;
                    break;
                case BlueprintEvent.GRACE_PERIOD:
                    topic = EventConstants.TOPIC_GRACE_PERIOD;
                    break;
                case BlueprintEvent.WAITING:
                    topic = EventConstants.TOPIC_WAITING;
                    break;
                default:
                    throw new IllegalStateException("Unknown blueprint event type: " + event.getType());
            }
            eventAdmin.postEvent(new Event(topic, props));
        }

    }

}
