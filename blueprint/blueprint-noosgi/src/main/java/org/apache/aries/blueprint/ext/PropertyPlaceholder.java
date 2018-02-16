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
package org.apache.aries.blueprint.ext;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.aries.blueprint.ext.evaluator.PropertyEvaluator;
import org.apache.aries.blueprint.ext.evaluator.PropertyEvaluatorExt;
import org.apache.felix.utils.properties.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Property placeholder that looks for properties in the System properties.
 *
 * @version $Rev$, $Date$
 */
public class PropertyPlaceholder extends AbstractPropertyPlaceholder {

    public enum SystemProperties {
        never,
        fallback,
        override
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyPlaceholder.class);

    private Map<String, Object> defaultProperties;
    private Properties properties;
    private List<String> locations;
    private boolean ignoreMissingLocations;
    private SystemProperties systemProperties = SystemProperties.override;
    private PropertyEvaluatorExt evaluator = null;

    public Map getDefaultProperties() {
        return defaultProperties;
    }

    public void setDefaultProperties(Map defaultProperties) {
        this.defaultProperties = defaultProperties;
    }

    public List<String> getLocations() {
        return locations;
    }

    public void setLocations(List<String> locations) {
        this.locations = locations;
    }

    public boolean isIgnoreMissingLocations() {
        return ignoreMissingLocations;
    }

    public void setIgnoreMissingLocations(boolean ignoreMissingLocations) {
        this.ignoreMissingLocations = ignoreMissingLocations;
    }

    public SystemProperties getSystemProperties() {
        return systemProperties;
    }

    public void setSystemProperties(SystemProperties systemProperties) {
        this.systemProperties = systemProperties;
    }

    public PropertyEvaluatorExt getEvaluator() {
        return evaluator;
    }

    public void setEvaluator(PropertyEvaluatorExt evaluator) {
        this.evaluator = evaluator;
    }

    public void init() throws Exception {
        properties = new Properties();
        if (locations != null) {
            for (String url : locations) {
                InputStream is = null;
                try {
                    if (url.startsWith("classpath:")) {
                        is = getBlueprintContainer().getResource(url.substring("classpath:".length())).openStream();
                    } else {
                        is = new URL(url).openStream();
                    }
                } catch (IOException e) {
                    if (ignoreMissingLocations) {
                        LOGGER.debug("Unable to load properties from url " + url + " while ignoreMissingLocations is set to true");
                    } else {
                        throw e;
                    }
                }
                if (is != null) {
                    try {
                        properties.load(is);
                    } finally {
                        is.close();
                    }
                }
            }
        }
    }

    protected Object getProperty(String val) {
        LOGGER.debug("Retrieving property {}", val);
        Object v = null;
        if (v == null && systemProperties == SystemProperties.override) {
            v = getBlueprintContainer().getProperty(val);
            if (v != null) {
                LOGGER.debug("Found system property {} with value {}", val, v);
            }
        }
        if (v == null && properties != null) {
            v = properties.getProperty(val);
            if (v != null) {
                LOGGER.debug("Found property {} from locations with value {}", val, v);
            }
        }
        if (v == null && systemProperties == SystemProperties.fallback) {
            v = getBlueprintContainer().getProperty(val);
            if (v != null) {
                LOGGER.debug("Found system property {} with value {}", val, v);
            }
        }
        if (v == null && defaultProperties != null) {
            v = defaultProperties.get(val);
            if (v != null) {
                LOGGER.debug("Retrieved property {} value from defaults {}", val, v);
            }
        }
        if (v == null) {
            LOGGER.debug("Property {} not found", val);
        }
        return v;
    }

    @Override
    protected Object retrieveValue(String expression) {
        LOGGER.debug("Retrieving Value from expression: {}", expression);

        if (evaluator == null) {
            return super.retrieveValue(expression);
        } else {
            return evaluator.evaluate(expression, new AbstractMap<String, Object>() {
                @Override
                public Object get(Object key) {
                    return getProperty((String) key);
                }
                @Override
                public Set<Entry<String, Object>> entrySet() {
                    throw new UnsupportedOperationException();
                }
            });
        }

    }


}
