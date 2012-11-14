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

import static org.apache.aries.jmx.util.FrameworkUtils.getBundleIds;
import static org.apache.aries.jmx.util.TypeUtils.toLong;
import static org.apache.aries.jmx.util.TypeUtils.toPrimitive;
import static org.osgi.jmx.framework.ServiceStateMBean.BUNDLE_IDENTIFIER;
import static org.osgi.jmx.framework.ServiceStateMBean.IDENTIFIER;
import static org.osgi.jmx.framework.ServiceStateMBean.OBJECT_CLASS;
import static org.osgi.jmx.framework.ServiceStateMBean.PROPERTIES;
import static org.osgi.jmx.framework.ServiceStateMBean.SERVICE_TYPE;
import static org.osgi.jmx.framework.ServiceStateMBean.USING_BUNDLES;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.jmx.JmxConstants;
import org.osgi.jmx.framework.ServiceStateMBean;

/**
 * <p>
 * <tt>ServiceData</tt> represents Service Type @see {@link ServiceStateMBean#SERVICE_TYPE}. It is a codec for the
 * <code>CompositeData</code> representing an OSGi <code>ServiceReference</code>.
 * </p>
 *
 * @version $Rev$ $Date$
 */
public class ServiceData {

    /**
     * @see ServiceStateMBean#IDENTIFIER_ITEM
     */
    private long serviceId;

    /**
     * @see ServiceStateMBean#BUNDLE_IDENTIFIER_ITEM
     */
    private long bundleId;

    /**
     * @see ServiceStateMBean#OBJECT_CLASS_ITEM
     */
    private String[] serviceInterfaces;

    /**
     * @see ServiceStateMBean#PROPERTIES_ITEM
     */
    private List<PropertyData<? extends Object>> properties = new ArrayList<PropertyData<? extends Object>>();

    /**
     * @see ServiceStateMBean#USING_BUNDLES_ITEM
     */
    private long[] usingBundles;

    private ServiceData() {
        super();
    }

    public ServiceData(ServiceReference<?> serviceReference) throws IllegalArgumentException {
        if (serviceReference == null) {
            throw new IllegalArgumentException("Argument serviceReference cannot be null");
        }
        this.serviceId = (Long) serviceReference.getProperty(Constants.SERVICE_ID);
        this.bundleId = serviceReference.getBundle().getBundleId();
        this.serviceInterfaces = (String[]) serviceReference.getProperty(Constants.OBJECTCLASS);
        this.usingBundles = getBundleIds(serviceReference.getUsingBundles());
        for (String propertyKey: serviceReference.getPropertyKeys()) {
            this.properties.add(PropertyData.newInstance(propertyKey, serviceReference.getProperty(propertyKey)));
        }
    }

    /**
     * Returns CompositeData representing a ServiceReference typed by {@link ServiceStateMBean#SERVICE_TYPE}.
     * @return
     */
    public CompositeData toCompositeData() {
        return toCompositeData(ServiceStateMBean.SERVICE_TYPE.keySet());
    }

    public CompositeData toCompositeData(Collection<String> itemNames) {
        Map<String, Object> items = new HashMap<String, Object>();

        items.put(IDENTIFIER, this.serviceId);

        if (itemNames.contains(BUNDLE_IDENTIFIER))
            items.put(BUNDLE_IDENTIFIER, this.bundleId);

        if (itemNames.contains(OBJECT_CLASS))
            items.put(OBJECT_CLASS, this.serviceInterfaces);

        TabularData propertiesTable = new TabularDataSupport(JmxConstants.PROPERTIES_TYPE);
        for (PropertyData<? extends Object> propertyData : this.properties) {
            propertiesTable.put(propertyData.toCompositeData());
        }
        items.put(PROPERTIES, propertiesTable);


        if (itemNames.contains(USING_BUNDLES))
            items.put(USING_BUNDLES, toLong(this.usingBundles));

        String[] allItemNames = SERVICE_TYPE.keySet().toArray(new String [] {});
        Object[] itemValues = new Object[allItemNames.length];
        for (int i=0; i < allItemNames.length; i++) {
            itemValues[i] = items.get(allItemNames[i]);
        }

        try {
            return new CompositeDataSupport(SERVICE_TYPE, allItemNames, itemValues);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Failed to create CompositeData for ServiceReference with "
                    + Constants.SERVICE_ID + " [" + this.serviceId + "]", e);
        }
    }

    /**
     * Constructs a <code>ServiceData</code> object from the given <code>CompositeData</code>
     *
     * @param compositeData
     * @return
     * @throws IlleglArugmentException
     *             if compositeData is null or not of type {@link ServiceStateMBean#SERVICE_TYPE}.
     */
    public static ServiceData from(CompositeData compositeData) {
        if (compositeData == null) {
            throw new IllegalArgumentException("Argument compositeData cannot be null");
        }
        if (!compositeData.getCompositeType().equals(SERVICE_TYPE)) {
            throw new IllegalArgumentException("Invalid CompositeType [" + compositeData.getCompositeType() + "]");
        }
        ServiceData serviceData = new ServiceData();
        serviceData.serviceId = (Long) compositeData.get(IDENTIFIER);
        serviceData.bundleId = (Long) compositeData.get(BUNDLE_IDENTIFIER);
        serviceData.serviceInterfaces = (String[]) compositeData.get(OBJECT_CLASS);
        serviceData.usingBundles = toPrimitive((Long[]) compositeData.get(USING_BUNDLES));
        TabularData propertiesTable = (TabularData) compositeData.get(PROPERTIES);
        Collection<CompositeData> propertyData = (Collection<CompositeData>) propertiesTable.values();
        for (CompositeData propertyRow: propertyData) {
            serviceData.properties.add(PropertyData.from(propertyRow));
        }
        return serviceData;
    }

    public long getServiceId() {
        return serviceId;
    }

    public long getBundleId() {
        return bundleId;
    }

    public String[] getServiceInterfaces() {
        return serviceInterfaces;
    }

    public List<PropertyData<? extends Object>> getProperties() {
        return properties;
    }

    public long[] getUsingBundles() {
        return usingBundles;
    }
}
