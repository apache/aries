/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.jmx.codec;

import static org.osgi.jmx.framework.ServiceStateMBean.BUNDLE_IDENTIFIER;
import static org.osgi.jmx.framework.ServiceStateMBean.BUNDLE_LOCATION;
import static org.osgi.jmx.framework.ServiceStateMBean.BUNDLE_SYMBOLIC_NAME;
import static org.osgi.jmx.framework.ServiceStateMBean.EVENT;
import static org.osgi.jmx.framework.ServiceStateMBean.IDENTIFIER;
import static org.osgi.jmx.framework.ServiceStateMBean.OBJECT_CLASS;
import static org.osgi.jmx.framework.ServiceStateMBean.SERVICE_EVENT_TYPE;

import java.util.HashMap;
import java.util.Map;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.OpenDataException;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.jmx.framework.ServiceStateMBean;

/**
 <p>
 * <tt>ServiceEventData</tt> represents ServiceEvent Type @see {@link ServiceStateMBean#SERVICE_EVENT_TYPE}.
 * It is a codec for the <code>CompositeData</code> representing an OSGi ServiceEvent.
 * </p>
 *
 * @version $Rev$ $Date$
 */
public class ServiceEventData {

    /**
     * @see ServiceStateMBean#IDENTIFIER_ITEM
     */
    private long serviceId;
    
    /**
     * @see ServiceStateMBean#OBJECT_CLASS_ITEM
     */
    private String[] serviceInterfaces;
    
    /**
     * @see ServiceStateMBean#BUNDLE_IDENTIFIER_ITEM
     */
    private long bundleId;
    
    /**
     * @see ServiceStateMBean#BUNDLE_LOCATION_ITEM
     */
    private String bundleLocation;
    
    /**
     * @see ServiceStateMBean#BUNDLE_SYMBOLIC_NAME_ITEM
     */
    private String bundleSymbolicName;
    
    /**
     * @see ServiceStateMBean#EVENT_ITEM
     */
    private int eventType;
    
    
    private ServiceEventData(){
        super();
    }
    
    public ServiceEventData(ServiceEvent serviceEvent) {
        @SuppressWarnings("rawtypes")
        ServiceReference serviceReference = serviceEvent.getServiceReference();
        this.serviceId = (Long) serviceReference.getProperty(Constants.SERVICE_ID);
        this.serviceInterfaces = (String[]) serviceReference.getProperty(Constants.OBJECTCLASS);
        this.eventType = serviceEvent.getType();
        Bundle bundle = serviceReference.getBundle();
        if (bundle != null) {
            this.bundleId = bundle.getBundleId();
            this.bundleLocation = bundle.getLocation();
            this.bundleSymbolicName = bundle.getSymbolicName();
        }
    }
    
    /**
     * Returns CompositeData representing a ServiceEvent typed by {@link ServiceStateMBean#SERVICE_EVENT_TYPE}.
     * @return
     */
    public CompositeData toCompositeData() {
        CompositeData result = null;
        Map<String, Object> items = new HashMap<String, Object>();
        items.put(IDENTIFIER, this.serviceId);
        items.put(OBJECT_CLASS, this.serviceInterfaces);
        items.put(BUNDLE_IDENTIFIER, this.bundleId);
        items.put(BUNDLE_LOCATION, this.bundleLocation);
        items.put(BUNDLE_SYMBOLIC_NAME, this.bundleSymbolicName);
        items.put(EVENT, this.eventType);
        try {
            result = new CompositeDataSupport(SERVICE_EVENT_TYPE, items);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Failed to create CompositeData for ServiceEvent for Service [" + this.serviceId + "]", e);
        }
        return result;
    }
    
    /**
     * Returns a <code>ServiceEventData</code> representation of the given compositeData
     * @param compositeData
     * @return
     * @throws IllegalArgumentException if the compositeData is null or incorrect type
     */
    public static ServiceEventData from(CompositeData compositeData) throws IllegalArgumentException {
        ServiceEventData serviceEventData = new ServiceEventData();
        if ( compositeData == null ) {
            throw new IllegalArgumentException("Argument compositeData cannot be null");
        }
        if (!compositeData.getCompositeType().equals(SERVICE_EVENT_TYPE)) {
            throw new IllegalArgumentException("Invalid CompositeType [" + compositeData.getCompositeType() + "]");
        }
        serviceEventData.serviceId = (Long) compositeData.get(IDENTIFIER);
        serviceEventData.serviceInterfaces = (String[]) compositeData.get(OBJECT_CLASS);
        serviceEventData.bundleId = (Long) compositeData.get(BUNDLE_IDENTIFIER);
        serviceEventData.bundleLocation = (String) compositeData.get(BUNDLE_LOCATION);
        serviceEventData.bundleSymbolicName = (String) compositeData.get(BUNDLE_SYMBOLIC_NAME);
        serviceEventData.eventType = (Integer) compositeData.get(EVENT);
        return serviceEventData;
    }
    
    public long getServiceId() {
        return serviceId;
    }
    
    public String[] getServiceInterfaces() {
        return serviceInterfaces;
    }
    
    public long getBundleId() {
        return bundleId;
    }
    
    public String getBundleLocation() {
        return bundleLocation;
    }
    
    public String getBundleSymbolicName() {
        return bundleSymbolicName;
    }
    
    public int getEventType() {
        return eventType;
    }
    
}
