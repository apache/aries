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
package org.apache.aries.jmx.cm;

import static org.osgi.jmx.JmxConstants.PROPERTIES_TYPE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;

import org.apache.aries.jmx.codec.PropertyData;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.jmx.service.cm.ConfigurationAdminMBean;
import org.osgi.service.cm.Configuration;

/**
 * Implementation of <code>ConfigurationAdminMBean</code> 
 *
 * @version $Rev$ $Date$
 */
public class ConfigurationAdmin implements ConfigurationAdminMBean {

    private org.osgi.service.cm.ConfigurationAdmin configurationAdmin;
    
    /**
     * Constructs a ConfigurationAdmin implementation
     * @param configurationAdmin instance of org.osgi.service.cm.ConfigurationAdmin service
     */
    public ConfigurationAdmin(org.osgi.service.cm.ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }
    
    
    /**
     * @see org.osgi.jmx.service.cm.ConfigurationAdminMBean#createFactoryConfiguration(java.lang.String)
     */
    public String createFactoryConfiguration(String factoryPid) throws IOException {
        return createFactoryConfigurationForLocation(factoryPid, null); 
    }

    /**
     * @see org.osgi.jmx.service.cm.ConfigurationAdminMBean#createFactoryConfigurationForLocation(java.lang.String, java.lang.String)
     */
    public String createFactoryConfigurationForLocation(String factoryPid, String location) throws IOException {
        if (factoryPid == null || factoryPid.length() < 1) {
            throw new IOException("Argument factoryPid cannot be null or empty");
        }
        Configuration config = configurationAdmin.createFactoryConfiguration(factoryPid);
        config.setBundleLocation(location);
        return config.getPid();
    }

    /**
     * @see org.osgi.jmx.service.cm.ConfigurationAdminMBean#delete(java.lang.String)
     */
    public void delete(String pid) throws IOException {
       deleteForLocation(pid, null);
    }

    /**
     * @see org.osgi.jmx.service.cm.ConfigurationAdminMBean#deleteForLocation(java.lang.String, java.lang.String)
     */
    public void deleteForLocation(String pid, String location) throws IOException {
        if (pid == null || pid.length() < 1) {
            throw new IOException("Argument pid cannot be null or empty");
        }
        Configuration config = configurationAdmin.getConfiguration(pid, location);
        config.delete();
    }

    /**
     * @see org.osgi.jmx.service.cm.ConfigurationAdminMBean#deleteConfigurations(java.lang.String)
     */
    public void deleteConfigurations(String filter) throws IOException {
        if (filter == null || filter.length() < 1) {
            throw new IOException("Argument filter cannot be null or empty");
        }
        Configuration[] configuations = null;
        try {
            configuations = configurationAdmin.listConfigurations(filter);
        } catch (InvalidSyntaxException e) {
            throw new IOException("Invalid filter [" + filter + "] : " + e);
        }
        if (configuations != null) {
            for (Configuration config : configuations) {
                config.delete();
            }
        }
    }

    /**
     * @see org.osgi.jmx.service.cm.ConfigurationAdminMBean#getBundleLocation(java.lang.String)
     */
    public String getBundleLocation(String pid) throws IOException {
        if (pid == null || pid.length() < 1) {
            throw new IOException("Argument pid cannot be null or empty");
        }
        Configuration config = configurationAdmin.getConfiguration(pid, null);
        String bundleLocation = (config.getBundleLocation() == null) ? "Configuration is not yet bound to a bundle location" : config.getBundleLocation();
        return bundleLocation;
    }

    /**
     * @see org.osgi.jmx.service.cm.ConfigurationAdminMBean#getConfigurations(java.lang.String)
     */
    public String[][] getConfigurations(String filter) throws IOException {
        if (filter == null || filter.length() < 1) {
            throw new IOException("Argument filter cannot be null or empty");
        }
        List<String[]> result = new ArrayList<String[]>();
        Configuration[] configurations = null;
        try {
            configurations = configurationAdmin.listConfigurations(filter);
        } catch (InvalidSyntaxException e) {
            throw new IOException("Invalid filter [" + filter + "] : " + e);
        }
        if (configurations != null) {
            for (Configuration config : configurations) {
                result.add(new String[] { config.getPid(), config.getBundleLocation() });
            }
        }
        return result.toArray(new String[result.size()][]);
    }

    /**
     * @see org.osgi.jmx.service.cm.ConfigurationAdminMBean#getFactoryPid(java.lang.String)
     */
    public String getFactoryPid(String pid) throws IOException {
       return getFactoryPidForLocation(pid, null);
    }

    /**
     * @see org.osgi.jmx.service.cm.ConfigurationAdminMBean#getFactoryPidForLocation(java.lang.String, java.lang.String)
     */
    public String getFactoryPidForLocation(String pid, String location) throws IOException {
        if (pid == null || pid.length() < 1) {
            throw new IOException("Argument pid cannot be null or empty");
        }
        Configuration config = configurationAdmin.getConfiguration(pid, location);
        return config.getFactoryPid();
    }

    /**
     * @see org.osgi.jmx.service.cm.ConfigurationAdminMBean#getProperties(java.lang.String)
     */
    public TabularData getProperties(String pid) throws IOException {
       return getPropertiesForLocation(pid, null);
    }

    /**
     * @see org.osgi.jmx.service.cm.ConfigurationAdminMBean#getPropertiesForLocation(java.lang.String, java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public TabularData getPropertiesForLocation(String pid, String location) throws IOException {
        if (pid == null || pid.length() < 1) {
            throw new IOException("Argument pid cannot be null or empty");
        }
        TabularData propertiesTable = null;
        Configuration config = configurationAdmin.getConfiguration(pid, location);
        Dictionary<String, Object> properties = config.getProperties();
        if (properties != null) {
            propertiesTable = new TabularDataSupport(PROPERTIES_TYPE);
            Enumeration<String> keys = properties.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                propertiesTable.put(PropertyData.newInstance(key, properties.get(key)).toCompositeData());
            }
        }
        return propertiesTable;
    }

    /**
     * @see org.osgi.jmx.service.cm.ConfigurationAdminMBean#setBundleLocation(java.lang.String, java.lang.String)
     */
    public void setBundleLocation(String pid, String location) throws IOException {
        if (pid == null || pid.length() < 1) {
            throw new IOException("Argument factoryPid cannot be null or empty");
        }
        Configuration config = configurationAdmin.getConfiguration(pid, null);
        config.setBundleLocation(location);
    }

    /**
     * @see org.osgi.jmx.service.cm.ConfigurationAdminMBean#update(java.lang.String, javax.management.openmbean.TabularData)
     */
    public void update(String pid, TabularData configurationTable) throws IOException {
        updateForLocation(pid, null, configurationTable);
    }

    /**
     * @see org.osgi.jmx.service.cm.ConfigurationAdminMBean#updateForLocation(java.lang.String, java.lang.String, javax.management.openmbean.TabularData)
     */
    @SuppressWarnings("unchecked")
    public void updateForLocation(String pid, String location, TabularData configurationTable) throws IOException {
        if (pid == null || pid.length() < 1) {
            throw new IOException("Argument pid cannot be null or empty");
        }
        if (configurationTable == null) {
            throw new IOException("Argument configurationTable cannot be null");
        }
                
        if (!PROPERTIES_TYPE.equals(configurationTable.getTabularType())) {
            throw new IOException("Invalid TabularType ["  + configurationTable.getTabularType() + "]");
        }
        Dictionary<String, Object> configurationProperties = new Hashtable<String, Object>();
        Collection<CompositeData> compositeData = (Collection<CompositeData>) configurationTable.values();
        for (CompositeData row: compositeData) {
            PropertyData<? extends Class> propertyData = PropertyData.from(row);
            configurationProperties.put(propertyData.getKey(), propertyData.getValue());
        }
        Configuration config = configurationAdmin.getConfiguration(pid, location);
        config.update(configurationProperties);
    }

}
