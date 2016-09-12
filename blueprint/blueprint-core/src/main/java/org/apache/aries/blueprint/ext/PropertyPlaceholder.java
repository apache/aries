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
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.aries.blueprint.ext.evaluator.PropertyEvaluator;
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

    private Map defaultProperties;
    private Properties properties;
    private List<URL> locations;
    private boolean ignoreMissingLocations;
    private SystemProperties systemProperties = SystemProperties.fallback;
    private PropertyEvaluator evaluator = null;

    public Map getDefaultProperties() {
        return defaultProperties;
    }

    public void setDefaultProperties(Map defaultProperties) {
        this.defaultProperties = defaultProperties;
    }

    public List<URL> getLocations() {
        return locations;
    }

    public void setLocations(List<URL> locations) {
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

    public PropertyEvaluator getEvaluator() {
        return evaluator;
    }

    public void setEvaluator(PropertyEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    public void init() throws Exception {
        properties = new Properties();
        if (locations != null) {
            for (URL url : locations) {
                InputStream is = null;
                try {
                    is = url.openStream();
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

    protected String getProperty(String val) {
        LOGGER.debug("Retrieving property {}", val);
        Object v = null;
        if (v == null && systemProperties == SystemProperties.override) {
            v = getSystemProperty(val);
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
            v = getSystemProperty(val);
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
        return v != null ? v.toString() : null;
    }

    protected String getSystemProperty(String val) {
        if (val.startsWith("env:")) {
            return System.getenv(val.substring("env:".length()));
        }
        return System.getProperty(val);
    }

    @Override
    protected String retrieveValue(String expression) {
        LOGGER.debug("Retrieving Value from expression: {}", expression);
        
        if (evaluator == null) {
            return super.retrieveValue(expression);
        } else {
            return evaluator.evaluate(expression, new Dictionary<String, String>(){
                @Override
                public String get(Object key) {
                    return getProperty((String) key);
                }

                // following are not important
                @Override
                public String put(String key, String value) {
                    throw new UnsupportedOperationException();
                }
                
                @Override
                public Enumeration<String> elements() {
                    throw new UnsupportedOperationException();
                }
                
                @Override
                public boolean isEmpty() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Enumeration<String> keys() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public String remove(Object key) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public int size() {
                    throw new UnsupportedOperationException();
                }
                
            });
        }

    }
    
   
}
