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
package org.apache.aries.jmx.blueprint.impl;

import java.io.IOException;
import java.util.Collection;

import javax.management.openmbean.CompositeData;

import org.apache.aries.jmx.blueprint.BlueprintMetadataMBean;
import org.apache.aries.jmx.blueprint.codec.BPMetadata;
import org.apache.aries.jmx.blueprint.codec.Util;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.ServiceMetadata;
import org.osgi.service.blueprint.reflect.ServiceReferenceMetadata;

public class BlueprintMetadata implements BlueprintMetadataMBean {

    BundleContext bundleContext;

    public BlueprintMetadata(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.aries.jmx.blueprint.BlueprintMetadataMBean#getBlueprintContainerServiceId(long)
     */
    public long getBlueprintContainerServiceId(long bundleId) throws IOException {
        Bundle bpBundle = bundleContext.getBundle(bundleId);
        if (null == bpBundle)
            throw new IllegalArgumentException("Invalid bundle id " + bundleId);

        String filter = "(&(osgi.blueprint.container.symbolicname=" // no similar one in interfaces
                + bpBundle.getSymbolicName() + ")(osgi.blueprint.container.version=" + bpBundle.getVersion() + "))";
        ServiceReference[] serviceReferences = null;
        try {
            serviceReferences = bundleContext.getServiceReferences(BlueprintContainer.class.getName(), filter);
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException(e);
        }
        if (serviceReferences == null || serviceReferences.length < 1)
            return -1;
        else
            return (Long) serviceReferences[0].getProperty(Constants.SERVICE_ID);
    }

    public long[] getBlueprintContainerServiceIds() throws IOException {
        ServiceReference[] serviceReferences = null;
        try {
            serviceReferences = bundleContext.getServiceReferences(BlueprintContainer.class.getName(), null);
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException(e);
        }
        if (serviceReferences == null || serviceReferences.length < 1)
            return null;
        
        long[] serviceIds = new long[serviceReferences.length];
        for (int i = 0; i < serviceReferences.length; i++) {
            serviceIds[i] = (Long) serviceReferences[i].getProperty(Constants.SERVICE_ID);
        }
        return serviceIds;
    }

    public String[] getComponentIds(long containerServiceId) {
        BlueprintContainer container = getBlueprintContainer(containerServiceId);
        return (String[]) container.getComponentIds().toArray(new String[0]);
    }

    /*
     * 
     * type could be Bean, Service, serviceReference (non-Javadoc)
     * 
     * @see org.apache.aries.jmx.blueprint.BlueprintMetadataMBean#getComponentIdsByType(long, java.lang.String)
     */
    public String[] getComponentIdsByType(long containerServiceId, String type) {
        BlueprintContainer container = getBlueprintContainer(containerServiceId);
        Collection<? extends ComponentMetadata> components;
        if (type.equals(BlueprintMetadataMBean.SERVICE_METADATA)) {
            components = container.getMetadata(ServiceMetadata.class);
        } else if (type.equals(BlueprintMetadataMBean.BEAN_METADATA)) {
            components = container.getMetadata(BeanMetadata.class);
        } else if (type.equals(BlueprintMetadataMBean.SERVICE_REFERENCE_METADATA)) {
            components = container.getMetadata(ServiceReferenceMetadata.class);
        } else {
            throw new IllegalArgumentException("Unrecognized component type: " + type);
        }
        String ids[] = new String[components.size()];
        int i = 0;
        for (ComponentMetadata component : components) {
            // from compendium 121.4.8, in-line managers can not be retrieved by getMetadata, which will return null.
            // Because in-line managers are actually the object values.
            // Here we ignore it.
            if(null == component)
                continue;
            ids[i++] = component.getId();
        }
        return ids;
    }

    public CompositeData getComponentMetadata(long containerServiceId, String componentId) {
        BlueprintContainer container = getBlueprintContainer(containerServiceId);
        ComponentMetadata componentMetadata = container.getComponentMetadata(componentId);
        BPMetadata metadata = Util.metadata2BPMetadata(componentMetadata);
        return metadata.asCompositeData();
    }

    private BlueprintContainer getBlueprintContainer(long containerServiceId) {
        String filter = "(" + Constants.SERVICE_ID + "=" + containerServiceId + ")";
        ServiceReference[] serviceReferences = null;
        try {
            serviceReferences = bundleContext.getServiceReferences(BlueprintContainer.class.getName(), filter);
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException(e);
        }

        if (serviceReferences == null || serviceReferences.length <1) {
            throw new IllegalArgumentException("Invalid BlueprintContainer service id: " + containerServiceId);
        }
        return (BlueprintContainer) bundleContext.getService(serviceReferences[0]);
    }

}
