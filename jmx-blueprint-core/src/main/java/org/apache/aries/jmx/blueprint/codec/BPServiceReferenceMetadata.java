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

import java.util.Map;

import javax.management.openmbean.CompositeData;

import org.apache.aries.jmx.blueprint.BlueprintMetadataMBean;
import org.osgi.service.blueprint.reflect.ReferenceListener;
import org.osgi.service.blueprint.reflect.ServiceReferenceMetadata;

public abstract class BPServiceReferenceMetadata extends BPComponentMetadata {

    private int availability;

    private String componentName;

    private String filter;

    private String $interface;

    private BPReferenceListener[] listeners;

    protected BPServiceReferenceMetadata(CompositeData reference) {
        super(reference);
        availability = (Integer) reference.get(BlueprintMetadataMBean.AVAILABILITY);
        componentName = (String) reference.get(BlueprintMetadataMBean.COMPONENT_NAME);
        filter = (String) reference.get(BlueprintMetadataMBean.FILTER);
        $interface = (String) reference.get(BlueprintMetadataMBean.INTERFACE);

        CompositeData[] cd_listeners = (CompositeData[]) reference.get(BlueprintMetadataMBean.REFERENCE_LISTENERS);
        listeners = new BPReferenceListener[cd_listeners.length];
        for (int i = 0; i < listeners.length; i++) {
            listeners[i] = new BPReferenceListener(cd_listeners[i]);
        }
    }

    protected BPServiceReferenceMetadata(ServiceReferenceMetadata reference) {
        super(reference);
        availability = reference.getAvailability();
        componentName = reference.getComponentName();
        filter = reference.getFilter();
        $interface = reference.getInterface();

        listeners = new BPReferenceListener[reference.getReferenceListeners().size()];
        int i = 0;
        for (Object listener : reference.getReferenceListeners()) {
            listeners[i++] = new BPReferenceListener((ReferenceListener) listener);
        }
    }

    protected Map<String, Object> getItemsMap() {
        Map<String, Object> items = super.getItemsMap();

        // itself
        items.put(BlueprintMetadataMBean.AVAILABILITY, availability);
        items.put(BlueprintMetadataMBean.COMPONENT_NAME, componentName);
        items.put(BlueprintMetadataMBean.FILTER, filter);
        items.put(BlueprintMetadataMBean.INTERFACE, $interface);

        CompositeData[] cd_listeners = new CompositeData[listeners.length];
        for (int i = 0; i < listeners.length; i++) {
            cd_listeners[i] = listeners[i].asCompositeData();
        }
        items.put(BlueprintMetadataMBean.REFERENCE_LISTENERS, cd_listeners);

        return items;
    }

    public int getAvailability() {
        return availability;
    }

    public String getComponentName() {
        return componentName;
    }

    public String getFilter() {
        return filter;
    }

    public String getInterface() {
        return $interface;
    }

    public BPReferenceListener[] getReferenceListeners() {
        return listeners;
    }
}
