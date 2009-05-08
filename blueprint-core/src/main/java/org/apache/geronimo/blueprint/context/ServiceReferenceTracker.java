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
package org.apache.geronimo.blueprint.context;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev$, $Date$
 */
public class ServiceReferenceTracker implements ServiceListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceReferenceTracker.class);
    
    private BundleContext context;
    private String filter;
    private boolean optional;
    private boolean satisfied;
    private List<ServiceReference> referenceSet;
    private boolean started;   
    private List<ServiceListener> serviceListeners = new CopyOnWriteArrayList<ServiceListener>();
    private List<SatisfactionListener> satisfactionListeners = new CopyOnWriteArrayList<SatisfactionListener>();
    
    public ServiceReferenceTracker(BundleContext context, String filter, boolean optional) {   
        this.context = context;
        this.filter = filter;
        this.optional = optional;
        
        this.started = false;
        this.satisfied = false;
        this.referenceSet = new ArrayList<ServiceReference>();
    }
    
    public synchronized void start() throws InvalidSyntaxException {
        if (started) {
            return;
        }
        context.addServiceListener(this, filter);
        ServiceReference[] references = context.getServiceReferences(null, filter);
        if (references != null) {
            for (ServiceReference reference : references) {
                referenceSet.add(reference);
            }
        }
        satisfied = (optional) ? true : !referenceSet.isEmpty();   
        started = true;
    }
    
    public synchronized void stop() {
        context.removeServiceListener(this);
        referenceSet.clear();
        satisfactionListeners.clear();
        started = false;
    }
    
    public synchronized boolean isStarted() {
        return started;
    }
    
    public boolean isSatisfied() {
        return satisfied;
    }
    
    public int size() {
        return referenceSet.size();
    }
    
    public void serviceChanged(ServiceEvent event) {
        int eventType = event.getType();
        switch (eventType) {
            case ServiceEvent.REGISTERED:
                serviceAdded(event);
                break;
            case ServiceEvent.MODIFIED:
                serviceModified(event);
                break;
            case ServiceEvent.UNREGISTERING:
                serviceRemoved(event);
                break;
        }
    }
    
    protected synchronized void serviceAdded(ServiceEvent event) {
        ServiceReference ref = event.getServiceReference();
        referenceSet.add(ref);
        notifyServiceListeners(event);
        if (!optional) {
            setSatisfied(true);
        }
    }
    
    protected synchronized void serviceRemoved(ServiceEvent event) {
        ServiceReference ref = event.getServiceReference();
        referenceSet.remove(ref);
        notifyServiceListeners(event);
        if (!optional && referenceSet.isEmpty()) {
            setSatisfied(false);
        }
    }
    
    protected synchronized void serviceModified(ServiceEvent event) { 
        ServiceReference ref = event.getServiceReference();
        notifyServiceListeners(event);
    }
       
    protected void setSatisfied(boolean satisfied) {
        if (this.satisfied != satisfied) {
            this.satisfied = satisfied;
            for (SatisfactionListener listener : satisfactionListeners) {
                listener.notifySatisfaction(this);
                LOGGER.debug("Service reference with filter {} satisfied {}", filter, this.satisfied);
            }
        }
    }
    
    protected void notifyServiceListeners(ServiceEvent event) {
        for (ServiceListener listener : serviceListeners) {
            listener.serviceChanged(event);
        }
    }
    
    public synchronized List<ServiceReference> getServiceReferences() {
        return new ArrayList<ServiceReference>(referenceSet);
    }
    
    public synchronized ServiceReference getBestServiceReference() {
        int length = (referenceSet == null) ? 0 : referenceSet.size();
        if (length == 0) { /* if no service is being tracked */
            return null;
        }
        int index = 0;
        if (length > 1) { /* if more than one service, select highest ranking */
            int rankings[] = new int[length];
            int count = 0;
            int maxRanking = Integer.MIN_VALUE;
            for (int i = 0; i < length; i++) {
                Object property = referenceSet.get(i).getProperty(Constants.SERVICE_RANKING);
                int ranking = (property instanceof Integer) ? ((Integer) property).intValue() : 0;
                rankings[i] = ranking;
                if (ranking > maxRanking) {
                    index = i;
                    maxRanking = ranking;
                    count = 1;
                } else {
                    if (ranking == maxRanking) {
                        count++;
                    }
                }
            }
            if (count > 1) { /* if still more than one service, select lowest id */
                long minId = Long.MAX_VALUE;
                for (int i = 0; i < length; i++) {
                    if (rankings[i] == maxRanking) {
                        long id = ((Long) (referenceSet.get(i).getProperty(Constants.SERVICE_ID))).longValue();
                        if (id < minId) {
                            index = i;
                            minId = id;
                        }
                    }
                }
            }
        }
        return referenceSet.get(index);
    }
    
    public void registerServiceListener(ServiceListener listener) {
        serviceListeners.add(listener);
    }
    
    public void unregisterServiceListener(ServiceListener listener) {
        serviceListeners.remove(listener);
    }
    
    public void registerListener(SatisfactionListener listener) {
        satisfactionListeners.add(listener);
    }
    
    public void unregisterListener(SatisfactionListener listener) {
        satisfactionListeners.remove(listener);
    }
    
    
    public interface SatisfactionListener {

        void notifySatisfaction(ServiceReferenceTracker satisfiable);

    }
}
