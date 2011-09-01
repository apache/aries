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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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
    private transient JexlExpressionParser jexlParser;
    
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
            v = System.getProperty(val);
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
            v = System.getProperty(val);
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

    @Override
    protected String retrieveValue(String expression) {
        LOGGER.debug("Retrieving Value from expression: {}", expression);
        String result = super.retrieveValue(expression);
        
        if (result == null){
            try {
                Class.forName("org.apache.commons.jexl2.JexlEngine");
                JexlExpressionParser parser = getJexlParser();
                try {
                    Object obj = parser.evaluate(expression);
                    if (obj!=null) {
                        result = obj.toString();
                    }
                } catch (Exception e) {
                    LOGGER.info("Could not evaluate expression: {}", expression);
                    LOGGER.info("Exception:", e);
                }
            } catch (ClassNotFoundException e) {
                LOGGER.info("Could not evaluate expression: {}", expression);
                LOGGER.info("Exception:", e);
            }
        }
        return result;
    }
    
    private synchronized JexlExpressionParser getJexlParser() {
        if (jexlParser == null) {
            jexlParser = new JexlExpressionParser(toMap());
        }
        return jexlParser;
    }

    private Map<String, Object> toMap() {
        return new Map<String, Object>() {
            @Override
            public boolean containsKey(Object o) {
                return getProperty((String) o) != null;
            }
            
            @Override
            public Object get(Object o) {
                return getProperty((String) o);
            }
            
            // following are not important
            @Override
            public Object put(String s, Object o) {
                throw new UnsupportedOperationException();
            }
            
            @Override
            public int size() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isEmpty() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean containsValue(Object o) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Object remove(Object o) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void putAll(Map<? extends String, ? extends Object> map) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Set<String> keySet() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Collection<Object> values() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Set<Entry<String, Object>> entrySet() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
