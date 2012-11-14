/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.jmx.blueprint.codec;

import java.util.HashMap;
import java.util.Map;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.OpenDataException;

import org.apache.aries.jmx.blueprint.BlueprintStateMBean;
import org.osgi.service.blueprint.container.BlueprintEvent;

/**
 * <p>
 * This class represents the CODEC for the composite data representing a OSGi
 * <link>BlueprintEvent</link>
 * <p>
 * It serves as both the documentation of the type structure and as the
 * codification of the mechanism to convert to/from the CompositeData.
 * <p>
 * The structure of the composite data is:
 * <table border="1">
 * <tr>
 * <td>bundleId</td>
 * <td>long</td>
 * </tr>
 * <tr>
 * <td>extenderBundleId</td>
 * <td>long</td>
 * </tr>
 * <tr>
 * <td>eventType</td>
 * <td>int</td>
 * </tr>
 * <tr>
 * <td>replay</td>
 * <td>boolean</td>
 * </tr>
 * <tr>
 * <td>timestamp</td>
 * <td>long</td>
 * </tr>
 * <tr>
 * <td>dependencies</td>
 * <td>String[]</td>
 * </tr>
 * <tr>
 * <td>exceptionMessage</td>
 * <td>String</td>
 * </tr>
 * </table>
 */

public class OSGiBlueprintEvent implements TransferObject{

    private long bundleId;

    private long extenderBundleId;

    private int eventType;

    private boolean replay;

    private long timestamp;

    private String[] dependencies;

    private String exceptionMessage;


    /**
     * Construct an OSGiBlueprintEvent from the supplied <link>BlueprintEvent</link>
     *
     * @param event
     *            - the event to represent
     */
    public OSGiBlueprintEvent(BlueprintEvent event) {
        this(event.getBundle().getBundleId(),
                event.getExtenderBundle().getBundleId(),
                event.getType(),
                event.isReplay(),
                event.getTimestamp(),
                event.getDependencies(),
                (event.getCause() == null) ? null : event.getCause().getMessage());
    }

    /**
     * Construct an OSGiBlueprintEvent from the CompositeData representing the
     * event
     *
     * @param data
     *            - the CompositeData representing the event.
     */
    @SuppressWarnings("boxing")
    public OSGiBlueprintEvent(CompositeData data) {
        this((Long) data.get(BlueprintStateMBean.BUNDLE_ID),
                (Long) data.get(BlueprintStateMBean.EXTENDER_BUNDLE_ID),
                (Integer) data.get(BlueprintStateMBean.EVENT_TYPE),
                (Boolean) data.get(BlueprintStateMBean.REPLAY),
                (Long) data.get(BlueprintStateMBean.TIMESTAMP),
                (String[]) data.get(BlueprintStateMBean.DEPENDENCIES),
                (String) data.get(BlueprintStateMBean.EXCEPTION_MESSAGE));
    }

    /**
     * Construct the OSGiBlueprintEvent
     *
     * @param bundleId
     * @param extenderBundleId
     * @param eventType
     * @param replay
     * @param timestamp
     * @param dependencies
     * @param exceptionMessage
     */
    public OSGiBlueprintEvent(long bundleId, long extenderBundleId, int eventType, boolean replay, long timestamp, String[] dependencies, String exceptionMessage){
        this.bundleId = bundleId;
        this.extenderBundleId = extenderBundleId;
        this.eventType = eventType;
        this.replay = replay;
        this.timestamp = timestamp;
        this.dependencies = dependencies;
        this.exceptionMessage = exceptionMessage;
    }

    /**
     * Answer the receiver encoded as CompositeData
     *
     * @return the CompositeData encoding of the receiver.
     */
    @SuppressWarnings("boxing")
    public CompositeData asCompositeData() {
        Map<String, Object> items = new HashMap<String, Object>();
        items.put(BlueprintStateMBean.BUNDLE_ID, bundleId);
        items.put(BlueprintStateMBean.EXTENDER_BUNDLE_ID, extenderBundleId);
        items.put(BlueprintStateMBean.EVENT_TYPE, eventType);
        items.put(BlueprintStateMBean.REPLAY, replay);
        items.put(BlueprintStateMBean.TIMESTAMP, timestamp);
        items.put(BlueprintStateMBean.DEPENDENCIES, dependencies);
        items.put(BlueprintStateMBean.EXCEPTION_MESSAGE, exceptionMessage);
        try {
            return new CompositeDataSupport(BlueprintStateMBean.OSGI_BLUEPRINT_EVENT_TYPE, items);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Cannot form blueprint event open data", e);
        }
    }

    public long getBundleId() {
        return bundleId;
    }

    public long getExtenderBundleId() {
        return extenderBundleId;
    }

    public int getEventType() {
        return eventType;
    }

    public boolean isReplay() {
        return replay;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String[] getDependencies() {
        return dependencies;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }


}
