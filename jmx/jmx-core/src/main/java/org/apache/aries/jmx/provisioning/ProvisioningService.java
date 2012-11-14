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
package org.apache.aries.jmx.provisioning;

import static org.osgi.jmx.JmxConstants.PROPERTIES_TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.zip.ZipInputStream;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;

import org.apache.aries.jmx.codec.PropertyData;
import org.osgi.jmx.service.provisioning.ProvisioningServiceMBean;

/**
 * Implementation of <code>ProvisioningServiceMBean</code> 
 *
 * @version $Rev$ $Date$
 */
public class ProvisioningService implements ProvisioningServiceMBean {

    private org.osgi.service.provisioning.ProvisioningService provisioningService;
    
    /**
     * Constructs new ProvisioningService instance
     * @param provisioningService instance of org.osgi.service.provisioning.ProvisioningService service
     */
    public ProvisioningService(org.osgi.service.provisioning.ProvisioningService provisioningService){
        this.provisioningService = provisioningService;
    }
    
    /**
     * @see org.osgi.jmx.service.provisioning.ProvisioningServiceMBean#addInformationFromZip(java.lang.String)
     */
    public void addInformationFromZip(String zipURL) throws IOException {
        if (zipURL == null || zipURL.length() < 1) {
            throw new IOException("Argument zipURL cannot be null or empty");
        }
        InputStream is = createStream(zipURL);
        ZipInputStream zis = new ZipInputStream(is);
        try {
            provisioningService.addInformation(zis);
        } finally {
            zis.close();
        }
    }

    /**
     * @see org.osgi.jmx.service.provisioning.ProvisioningServiceMBean#addInformation(javax.management.openmbean.TabularData)
     */
    public void addInformation(TabularData info) throws IOException {
        Dictionary<String, Object> provisioningInfo = extractProvisioningDictionary(info);
        provisioningService.addInformation(provisioningInfo);
    }

    /**
     * @see org.osgi.jmx.service.provisioning.ProvisioningServiceMBean#listInformation()
     */
    @SuppressWarnings("unchecked")
    public TabularData listInformation() throws IOException {
        TabularData propertiesTable = new TabularDataSupport(PROPERTIES_TYPE);
        Dictionary<String, Object> information = (Dictionary<String, Object>) provisioningService.getInformation();
        if (information != null) {
            Enumeration<String> keys = information.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                propertiesTable.put(PropertyData.newInstance(key, information.get(key)).toCompositeData());
            }
        }
        return propertiesTable;
    }

    /**
     * @see org.osgi.jmx.service.provisioning.ProvisioningServiceMBean#setInformation(javax.management.openmbean.TabularData)
     */
    public void setInformation(TabularData info) throws IOException {
        Dictionary<String, Object> provisioningInfo = extractProvisioningDictionary(info);
        provisioningService.setInformation(provisioningInfo);
    }
    
    
    @SuppressWarnings("unchecked")
    protected Dictionary<String, Object> extractProvisioningDictionary(TabularData info) {
        Dictionary<String, Object> provisioningInfo = new Hashtable<String, Object>();
        if (info != null) {
            Collection<CompositeData> compositeData = (Collection<CompositeData>) info.values();
            for (CompositeData row: compositeData) {
                PropertyData<? extends Class> propertyData = PropertyData.from(row);
                provisioningInfo.put(propertyData.getKey(), propertyData.getValue());
            }
        }
        return provisioningInfo;
    }

    protected InputStream createStream(String url) throws IOException {
        return new URL(url).openStream();
    }
    
}
