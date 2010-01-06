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

import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;

import org.apache.aries.blueprint.ext.AbstractPropertyPlaceholder;
import org.apache.aries.blueprint.ext.PropertyPlaceholder;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: javadoc
 *
 * @version $Rev$, $Date$
 */
public class CmPropertyPlaceholder extends PropertyPlaceholder {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmPropertyPlaceholder.class);

    private BlueprintContainer blueprintContainer;
    private ConfigurationAdmin configAdmin; 
    private String persistentId;
    private transient Configuration config;

    public BlueprintContainer getBlueprintContainer() {
        return blueprintContainer;
    }

    public void setBlueprintContainer(BlueprintContainer blueprintContainer) {
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

    protected String getProperty(String val) {
        LOGGER.debug("Retrieving property value {} from configuration with pid {}", val, persistentId);
        Configuration config = getConfig();
        Object v = null;
        if (config != null) {
            Dictionary props = config.getProperties();
            if (props != null) {
                v = props.get(val);
                if (v != null) {
                    LOGGER.debug("Found property value {}", v);
                } else {
                    LOGGER.debug("Property not found in configuration");
                }
            } else {
                LOGGER.debug("No dictionary available from configuration");
            }
        }
        if (v == null) {
            v = super.getProperty(val);
        }
        return v != null ? v.toString() : null;
    }

    protected synchronized Configuration getConfig() {
        if (config == null) {
            try {
                config = CmUtils.getConfiguration(configAdmin, persistentId);
            } catch (IOException e) {
                // ignore
            }
        }
        return config;
    }

}
