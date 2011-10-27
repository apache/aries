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
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.OpenDataException;

import org.apache.aries.jmx.blueprint.BlueprintMetadataMBean;
import org.osgi.service.blueprint.reflect.MapEntry;
import org.osgi.service.blueprint.reflect.RegistrationListener;
import org.osgi.service.blueprint.reflect.ServiceMetadata;

public class BPServiceMetadata extends BPComponentMetadata {

    private int autoExport;

    private String[] interfaces;

    private int ranking;

    private BPRegistrationListener[] listeners;

    private BPMapEntry[] properties;

    private BPTarget serviceComponent;

    public BPServiceMetadata(CompositeData service) {
        super(service);
        autoExport = (Integer) service.get(BlueprintMetadataMBean.AUTO_EXPORT);
        interfaces = (String[]) service.get(BlueprintMetadataMBean.INTERFACES);
        ranking = (Integer) service.get(BlueprintMetadataMBean.RANKING);

        CompositeData[] cd_listeners = (CompositeData[]) service.get(BlueprintMetadataMBean.REGISTRATION_LISTENERS);
        listeners = new BPRegistrationListener[cd_listeners.length];
        for (int i = 0; i < listeners.length; i++) {
            listeners[i] = new BPRegistrationListener(cd_listeners[i]);
        }

        CompositeData[] cd_props = (CompositeData[]) service.get(BlueprintMetadataMBean.SERVICE_PROPERTIES);
        properties = new BPMapEntry[cd_props.length];
        for (int i = 0; i < properties.length; i++) {
            properties[i] = new BPMapEntry(cd_props[i]);
        }

        Byte[] buf = (Byte[]) service.get(BlueprintMetadataMBean.SERVICE_COMPONENT);
        serviceComponent = (BPTarget) Util.boxedBinary2BPMetadata(buf);
    }

    public BPServiceMetadata(ServiceMetadata service) {
        super(service);
        autoExport = service.getAutoExport();
        interfaces = (String[])service.getInterfaces().toArray(new String[0]);
        ranking = service.getRanking();

        listeners = new BPRegistrationListener[service.getRegistrationListeners().size()];
        int i = 0;
        for (Object listener : service.getRegistrationListeners()) {
            listeners[i++] = new BPRegistrationListener((RegistrationListener) listener);
        }

        properties = new BPMapEntry[service.getServiceProperties().size()];
        i = 0;
        for (Object prop : service.getServiceProperties()) {
            properties[i++] = new BPMapEntry((MapEntry) prop);
        }

        serviceComponent = (BPTarget) Util.metadata2BPMetadata(service.getServiceComponent());
    }

    protected Map<String, Object> getItemsMap() {
        Map<String, Object> items = super.getItemsMap();
        items.put(BlueprintMetadataMBean.AUTO_EXPORT, autoExport);
        items.put(BlueprintMetadataMBean.INTERFACES, interfaces);
        items.put(BlueprintMetadataMBean.RANKING, ranking);

        CompositeData[] cd_listeners = new CompositeData[listeners.length];
        for (int i = 0; i < listeners.length; i++) {
            cd_listeners[i] = listeners[i].asCompositeData();
        }
        items.put(BlueprintMetadataMBean.REGISTRATION_LISTENERS, cd_listeners);

        CompositeData[] cd_props = new CompositeData[properties.length];
        for (int i = 0; i < properties.length; i++) {
            cd_props[i] = properties[i].asCompositeData();
        }
        items.put(BlueprintMetadataMBean.SERVICE_PROPERTIES, cd_props);

        items.put(BlueprintMetadataMBean.SERVICE_COMPONENT, Util.bpMetadata2BoxedBinary(serviceComponent));

        return items;
    }

    public CompositeData asCompositeData() {
        try {
            return new CompositeDataSupport(BlueprintMetadataMBean.SERVICE_METADATA_TYPE, getItemsMap());
        } catch (OpenDataException e) {
            throw new RuntimeException(e);
        }
    }

    public int getAutoExport() {
        return autoExport;
    }

    public String[] getInterfaces() {
        return interfaces;
    }

    public int getRanking() {
        return ranking;
    }

    public BPRegistrationListener[] getRegistrationListeners() {
        return listeners;
    }

    public BPTarget getServiceComponent() {
        return serviceComponent;
    }

    public BPMapEntry[] getServiceProperties() {
        return properties;
    }
}
