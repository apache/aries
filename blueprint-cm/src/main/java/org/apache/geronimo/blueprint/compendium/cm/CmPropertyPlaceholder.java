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

import java.util.Map;
import java.util.Dictionary;

import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.reflect.ValueMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.Configuration;
import org.apache.geronimo.blueprint.beans.AbstractPropertyPlaceholder;
import org.apache.geronimo.blueprint.mutable.MutableValueMetadata;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 766508 $, $Date: 2009-04-19 22:09:27 +0200 (Sun, 19 Apr 2009) $
 */
public class CmPropertyPlaceholder extends AbstractPropertyPlaceholder {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmPropertyPlaceholder.class);

    private BlueprintContainer blueprintContainer;
    private ConfigurationAdmin configAdmin; 
    private String persistentId;
    private Map defaultProperties;

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

    public Map getDefaultProperties() {
        return defaultProperties;
    }

    public void setDefaultProperties(Map defaultProperties) {
        this.defaultProperties = defaultProperties;
    }

    @Override
    protected Metadata processValueMetadata(ValueMetadata metadata) {
        return new LateBindingValueMetadata(metadata.getStringValue(), metadata.getTypeName());
    }

    protected String getProperty(String val) {
        LOGGER.debug("Retrieving property value {} from configuration with pid {}", val, persistentId);
        Object v = null;
        try {
            Configuration config = configAdmin.getConfiguration(persistentId);
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
            } else {
                LOGGER.debug("No configuration available");
            }
        } catch (Throwable t) {
            LOGGER.info("Unable to retrieve property value " + val + " from configuration with pid " + persistentId, t);
        }
        if (v == null && defaultProperties != null && defaultProperties.containsKey(val)) {
            v = defaultProperties.get(val);
            if (v != null) {
                LOGGER.debug("Retrieved value from defaults {}", v);
            }
        }
        return v != null ? v.toString() : null;
    }

    public class LateBindingValueMetadata implements MutableValueMetadata {

        private String stringValue;
        private String typeName;
        private boolean retrieved;
        private String retrievedValue;

        public LateBindingValueMetadata(String stringValue, String typeName) {
            this.stringValue = stringValue;
            this.typeName = typeName;
        }

        public String getStringValue() {
            if (!retrieved) {
                retrieved = true;
                retrievedValue = processString(stringValue);
            }
            return retrievedValue;
        }

        public void setStringValue(String stringValue) {
            this.stringValue = stringValue;
        }

        public String getTypeName() {
            return typeName;
        }

        public void setTypeName(String typeName) {
            this.typeName = typeName;
        }
    }

}
