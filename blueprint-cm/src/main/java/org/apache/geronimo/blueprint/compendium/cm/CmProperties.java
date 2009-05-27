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
package org.apache.geronimo.blueprint.compendium.cm;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.geronimo.blueprint.ExtendedBlueprintContainer;
import org.apache.geronimo.blueprint.ServiceProcessor;
import org.apache.geronimo.blueprint.utils.JavaUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev$, $Date$
 */
public class CmProperties implements ManagedObject, ServiceProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmProperties.class);

    private ExtendedBlueprintContainer blueprintContainer;
    private ConfigurationAdmin configAdmin;
    private String persistentId;
    private boolean update;
    private String serviceId;

    private final Object lock = new Object();
    private final Set<ServicePropertiesUpdater> services = new HashSet<ServicePropertiesUpdater>();
    private Dictionary<String,Object> properties;

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
    
    public void init() {
        LOGGER.debug("Initializing CmProperties for service={} / pid={}", serviceId, persistentId);
        Properties props = new Properties();
        props.put(Constants.SERVICE_PID, persistentId);
        Bundle bundle = blueprintContainer.getBundleContext().getBundle();
        props.put(Constants.BUNDLE_SYMBOLICNAME, bundle.getSymbolicName());
        props.put(Constants.BUNDLE_VERSION, bundle.getHeaders().get(Constants.BUNDLE_VERSION));
        synchronized (lock) {
            ManagedObjectManager.register(this, props);
            try {
                properties = configAdmin.getConfiguration(persistentId).getProperties();
            } catch (Exception e) {
                LOGGER.debug("Unable to retrieve initial configuration for service={} / pid={}", new Object [] { serviceId,  persistentId}, e);
            }
        }
    }

    public void destroy() {
        ManagedObjectManager.unregister(this);
    }

    public void updated(Dictionary props) {
        LOGGER.debug("Service properties updated for service={} / pid={}, {}", new Object[] {serviceId, persistentId, props});
        
        synchronized (lock) {
            this.properties = props;
            if (update) {
                for (ServicePropertiesUpdater service : services) {
                    service.updateProperties(props);
                }
            }
        }
    }

    public void updateProperties(ServicePropertiesUpdater service, Dictionary props) {
        if (!this.serviceId.equals(service.getId())) {
            return;
        }
                
        LOGGER.debug("Service properties initialized for service={} / pid={}, {}", new Object[] {serviceId, persistentId, props});
        
        services.add(service);
        
        synchronized (lock) {
            if (properties != null) {
                JavaUtils.copy(props, properties);
            }
        }
    }

}
