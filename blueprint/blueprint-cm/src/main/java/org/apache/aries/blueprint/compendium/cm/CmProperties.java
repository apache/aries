/**
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
package org.apache.aries.blueprint.compendium.cm;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.apache.aries.blueprint.ServiceProcessor;
import org.apache.aries.blueprint.utils.JavaUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Rev$, $Date$
 */
public class CmProperties implements ManagedObject, ServiceProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmProperties.class);

    private ExtendedBlueprintContainer blueprintContainer;
    private ConfigurationAdmin configAdmin;
    private ManagedObjectManager managedObjectManager;
    private String persistentId;
    private boolean update;
    private String serviceId;

    private final Object lock = new Object();
    private final Set<ServicePropertiesUpdater> services = new HashSet<ServicePropertiesUpdater>();
    private final Properties properties = new Properties();
    private boolean initialized;

    public ExtendedBlueprintContainer getBlueprintContainer() {
        return blueprintContainer;
    }

    public void setBlueprintContainer(ExtendedBlueprintContainer blueprintContainer) {
        this.blueprintContainer = blueprintContainer;
    }

    public ConfigurationAdmin getConfigAdmin() {
        return configAdmin;
    }

    public void setConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    public void setManagedObjectManager(ManagedObjectManager managedObjectManager) {
        this.managedObjectManager = managedObjectManager;
    }
    
    public ManagedObjectManager getManagedObjectManager() {
        return managedObjectManager;
    }
    
    public Bundle getBundle() {
        return blueprintContainer.getBundleContext().getBundle();
    }
    
    public String getPersistentId() {
        return persistentId;
    }

    public void setPersistentId(String persistentId) {
        this.persistentId = persistentId;
    }

    public boolean getUpdate() {
        return update;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }
    
    public void init() throws Exception {
        if (serviceId != null) {
            LOGGER.debug("Initializing CmProperties for service={} / pid={}", serviceId, persistentId);
        } else {
            LOGGER.debug("Initializing CmProperties for pid={}", persistentId);
        }
        
        Properties props = new Properties();
        props.put(Constants.SERVICE_PID, persistentId);
        Bundle bundle = blueprintContainer.getBundleContext().getBundle();
        props.put(Constants.BUNDLE_SYMBOLICNAME, bundle.getSymbolicName());
        props.put(Constants.BUNDLE_VERSION, bundle.getHeaders().get(Constants.BUNDLE_VERSION));
                
        synchronized (lock) {
            managedObjectManager.register(this, props);
        }
    }

    public void destroy() {
        managedObjectManager.unregister(this);
    }

    public Properties getProperties() {
        return properties;
    }

    public void updated(Dictionary props) {
        if (initialized) {
            if (serviceId != null) {
                LOGGER.debug("Service properties updated for service={} / pid={}, {}", new Object[]{serviceId, persistentId, props});
            } else {
                LOGGER.debug("Service properties updated for pid={}, {}", new Object[]{persistentId, props});
            }
        }
        
        synchronized (lock) {
            properties.clear();
            if (props != null) {
                JavaUtils.copy(properties, props);
            }
            if (!initialized) {
                initialized = true;
            } else if (update) {
                for (ServicePropertiesUpdater service : services) {
                    service.updateProperties(props);
                }
            }
        }
    }

    public void updateProperties(ServicePropertiesUpdater service, Dictionary props) {
        if (this.serviceId == null || !this.serviceId.equals(service.getId())) {
            return;
        }
                
        LOGGER.debug("Service properties initialized for service={} / pid={}, {}", new Object[] {serviceId, persistentId, props});
        
        synchronized (lock) {
            services.add(service);
            JavaUtils.copy(props, properties);
        }
    }

}
