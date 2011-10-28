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

import static org.osgi.jmx.framework.BundleStateMBean.BUNDLE_EVENT_TYPE;
import static org.osgi.jmx.framework.BundleStateMBean.EVENT;
import static org.osgi.jmx.framework.BundleStateMBean.IDENTIFIER;
import static org.osgi.jmx.framework.BundleStateMBean.LOCATION;
import static org.osgi.jmx.framework.BundleStateMBean.SYMBOLIC_NAME;

import java.util.HashMap;
import java.util.Map;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.OpenDataException;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.jmx.framework.BundleStateMBean;

/**
 * <p>
 * <tt>BundleEventData</tt> represents BundleEvent Type @see {@link BundleStateMBean#BUNDLE_EVENT_TYPE}. It is a codec
 * for the <code>CompositeData</code> representing an OSGi BundleEvent.
 * </p>
 * 
 * @version $Rev$ $Date$
 */
public class BundleEventData {

    /**
     * @see BundleStateMBean#IDENTIFIER_ITEM
     */
    private long bundleId;

    /**
     * @see BundleStateMBean#LOCATION_ITEM
     */
    private String location;

    /**
     * @see BundleStateMBean#SYMBOLIC_NAME_ITEM
     */
    private String bundleSymbolicName;

    /**
     * @see BundleStateMBean#EVENT_ITEM
     */
    private int eventType;

    private BundleEventData() {
        super();
    }

    public BundleEventData(BundleEvent bundleEvent) {
        this.eventType = bundleEvent.getType();
        Bundle bundle = bundleEvent.getBundle();
        this.bundleId = bundle.getBundleId();
        this.location = bundle.getLocation();
        this.bundleSymbolicName = bundle.getSymbolicName();
    }

    /**
     * Returns CompositeData representing a BundleEvent typed by {@link BundleStateMBean#BUNDLE_EVENT_TYPE}
     * 
     * @return
     */
    public CompositeData toCompositeData() {
        CompositeData result = null;
        Map<String, Object> items = new HashMap<String, Object>();
        items.put(IDENTIFIER, this.bundleId);
        items.put(SYMBOLIC_NAME, this.bundleSymbolicName);
        items.put(LOCATION, this.location);
        items.put(EVENT, this.eventType);
        try {
            result = new CompositeDataSupport(BUNDLE_EVENT_TYPE, items);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Failed to create CompositeData for BundleEvent for Bundle ["
                    + this.bundleId + "]", e);
        }
        return result;
    }

    /**
     * Returns a <code>BundleEventData</code> representation of the given compositeData
     * 
     * @param compositeData
     * @return
     * @throws IllegalArgumentException
     *             if the compositeData is null or incorrect type
     */
    public static BundleEventData from(CompositeData compositeData) throws IllegalArgumentException {
        BundleEventData eventData = new BundleEventData();
        if (compositeData == null) {
            throw new IllegalArgumentException("Argument compositeData cannot be null");
        }
        if (!compositeData.getCompositeType().equals(BUNDLE_EVENT_TYPE)) {
            throw new IllegalArgumentException("Invalid CompositeType [" + compositeData.getCompositeType() + "]");
        }
        eventData.bundleId = (Long) compositeData.get(IDENTIFIER);
        eventData.bundleSymbolicName = (String) compositeData.get(SYMBOLIC_NAME);
        eventData.eventType = (Integer) compositeData.get(EVENT);
        eventData.location = (String) compositeData.get(LOCATION);
        return eventData;
    }

    public long getBundleId() {
        return bundleId;
    }

    public String getLocation() {
        return location;
    }

    public String getBundleSymbolicName() {
        return bundleSymbolicName;
    }

    public int getEventType() {
        return eventType;
    }
}
