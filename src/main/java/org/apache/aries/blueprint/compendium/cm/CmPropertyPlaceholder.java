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
import java.util.Enumeration;
import java.util.Properties;

import org.apache.aries.blueprint.ext.PropertyPlaceholder;
import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: javadoc
 *
 * @version $Rev$, $Date$
 */
public class CmPropertyPlaceholder extends PropertyPlaceholder implements ManagedObject {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmPropertyPlaceholder.class);

    private ExtendedBlueprintContainer blueprintContainer;
    private ConfigurationAdmin configAdmin; 
    private String persistentId;
    private String updateStrategy;
    private ManagedObjectManager managedObjectManager;
    private Dictionary<String, Object> properties;
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

    public String getPersistentId() {
        return persistentId;
    }

    public void setPersistentId(String persistentId) {
        this.persistentId = persistentId;
    }

    public String getUpdateStrategy() {
        return updateStrategy;
    }

    public void setUpdateStrategy(String updateStrategy) {
        this.updateStrategy = updateStrategy;
    }

    public ManagedObjectManager getManagedObjectManager() {
        return managedObjectManager;
    }

    public void setManagedObjectManager(ManagedObjectManager managedObjectManager) {
        this.managedObjectManager = managedObjectManager;
    }

    public void init() throws Exception {
        LOGGER.debug("Initializing CmPropertyPlaceholder");
        Properties props = new Properties();
        props.put(Constants.SERVICE_PID, persistentId);
        Bundle bundle = blueprintContainer.getBundleContext().getBundle();
        props.put(Constants.BUNDLE_SYMBOLICNAME, bundle.getSymbolicName());
        props.put(Constants.BUNDLE_VERSION, bundle.getHeaders().get(Constants.BUNDLE_VERSION));
        managedObjectManager.register(this, props);
    }

    public void destroy() {
        LOGGER.debug("Destroying CmPropertyPlaceholder");
        managedObjectManager.unregister(this);
    }

    protected String getProperty(String val) {
        LOGGER.debug("Retrieving property value {} from configuration with pid {}", val, persistentId);
        Object v = null;
        if (properties != null) {
            v = properties.get(val);
            if (v != null) {
                LOGGER.debug("Found property value {}", v);
            } else {
                LOGGER.debug("Property not found in configuration");
            }
        }
        if (v == null) {
            v = super.getProperty(val);
        }
        return v != null ? v.toString() : null;
    }

    public Bundle getBundle() {
        return blueprintContainer.getBundleContext().getBundle();
    }

    public void updated(Dictionary props) {
        if (!initialized) {
            properties = props;
            initialized = true;
            return;
        }
        if ("reload".equalsIgnoreCase(updateStrategy) && !equals(properties, props)) {
            LOGGER.debug("Configuration updated for pid={}", persistentId);
            // Run in a separate thread to avoid re-entrance
            new Thread() {
                public void run() {
                    blueprintContainer.reload();
                }
            }.start();
        }
    }

    private <T, U> boolean equals(Dictionary<T, U> d1, Dictionary<T, U> d2) {
        if (d1 == null || d1.isEmpty()) {
            return d2 == null || d2.isEmpty();
        } else if (d2 == null || d1.size() != d2.size()) {
            return false;
        } else {
            for (Enumeration<T> e = d1.keys(); e.hasMoreElements();) {
                T k = e.nextElement();
                U v1 = d1.get(k);
                U v2 = d2.get(k);
                if (v1 == null) {
                    if (v2 != null) {
                        return false;
                    }
                } else {
                    if (!v1.equals(v2)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

}
