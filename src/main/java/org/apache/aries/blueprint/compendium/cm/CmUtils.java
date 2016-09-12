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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationPlugin;

public class CmUtils  {

    private CmUtils() {        
    }
    
    public static Configuration getConfiguration(ConfigurationAdmin configAdmin, String persistentId) throws IOException {
        String filter = '(' + Constants.SERVICE_PID + '=' + persistentId + ')';
        Configuration[] configs;
        try {
            configs = configAdmin.listConfigurations(filter);
        } catch (InvalidSyntaxException e) {
            // this should not happen
            throw new RuntimeException("Invalid filter: " + filter);
        }
        if (configs != null && configs.length > 0) {
            return configs[0];
        } else {
            // TODO: what should we do?
            // throw new RuntimeException("No configuration object for pid=" + persistentId);
            return null;
        }
    }

    public static Dictionary<String, Object> getProperties(ServiceReference service, String persistentId) throws IOException {
        BundleContext bc = service.getBundle().getBundleContext();
        ServiceReference<ConfigurationAdmin> caRef = bc.getServiceReference(ConfigurationAdmin.class);
        try {
            ConfigurationAdmin ca = bc.getService(caRef);
            Configuration config = getConfiguration(ca, persistentId);
            if (config != null) {
                Dictionary<String, Object> props = new CaseInsensitiveDictionary(config.getProperties());
                BundleContext caBc = caRef.getBundle().getBundleContext();
                callPlugins(caBc, props, service, persistentId, null);
                return props;
            } else {
                return null;
            }
        } finally {
            bc.ungetService(caRef);
        }
    }

    private static void callPlugins(final BundleContext bundleContext,
                                    final Dictionary<String, Object> props,
                                    final ServiceReference sr,
                                    final String configPid,
                                    final String factoryPid) {
        ServiceReference[] plugins = null;
        try {
            final String targetPid = (factoryPid == null) ? configPid : factoryPid;
            String filter = "(|(!(cm.target=*))(cm.target=" + targetPid + "))";
            plugins = bundleContext.getServiceReferences(ConfigurationPlugin.class.getName(), filter);
        } catch (InvalidSyntaxException ise) {
            // no filter, no exception ...
        }

        // abort early if there are no plugins
        if (plugins == null || plugins.length == 0) {
            return;
        }

        // sort the plugins by their service.cmRanking
        if (plugins.length > 1) {
            Arrays.sort(plugins, CM_RANKING);
        }

        // call the plugins in order
        for (ServiceReference pluginRef : plugins) {
            ConfigurationPlugin plugin = (ConfigurationPlugin) bundleContext.getService(pluginRef);
            if (plugin != null) {
                try {
                    plugin.modifyConfiguration(sr, props);
                } catch (Throwable t) {
                    // Ignore
                } finally {
                    // ensure ungetting the plugin
                    bundleContext.ungetService(pluginRef);
                }
                setAutoProperties(props, configPid, factoryPid);
            }
        }
    }

    private static void setAutoProperties( Dictionary<String, Object> properties, String pid, String factoryPid )
    {
        replaceProperty(properties, Constants.SERVICE_PID, pid);
        replaceProperty(properties, ConfigurationAdmin.SERVICE_FACTORYPID, factoryPid);
        properties.remove(ConfigurationAdmin.SERVICE_BUNDLELOCATION);
    }

    private static void replaceProperty(Dictionary<String, Object> properties, String key, String value) {
        if (value == null) {
            properties.remove(key);
        } else {
            properties.put(key, value);
        }
    }

    private static Comparator<ServiceReference> CM_RANKING = new Comparator<ServiceReference>() {
        @Override
        public int compare(ServiceReference sr1, ServiceReference sr2) {
            final long rank1 = getLong(sr1, ConfigurationPlugin.CM_RANKING);
            final long rank2 = getLong(sr2, ConfigurationPlugin.CM_RANKING);
            if (rank1 == rank2) {
                return 0;
            }
            return (rank1 < rank2) ? -1 : 1;
        }

        protected long getLong(ServiceReference sr, String property) {
            Object rankObj = sr.getProperty(property);
            if (rankObj instanceof Number) {
                return ((Number) rankObj).longValue();
            }
            return 0;
        }
    };

    private static class CaseInsensitiveDictionary extends Dictionary<String, Object> {
        private final Hashtable<String, Object> internalMap = new Hashtable<String, Object>();
        private final Hashtable<String, String> originalKeys = new Hashtable<String, String>();

        public CaseInsensitiveDictionary(Dictionary<String, Object> props) {
            if (props != null) {
                Enumeration<String> keys = props.keys();
                while (keys.hasMoreElements()) {
                    // check the correct syntax of the key
                    String key = checkKey(keys.nextElement());
                    // check uniqueness of key
                    String lowerCase = key.toLowerCase();
                    if (internalMap.containsKey(lowerCase)) {
                        throw new IllegalArgumentException("Key [" + key + "] already present in different case");
                    }
                    // check the value
                    Object value = props.get(key);
                    checkValue(value);
                    // add the key/value pair
                    internalMap.put(lowerCase, value);
                    originalKeys.put(lowerCase, key);
                }
            }
        }

        public Enumeration<Object> elements() {
            return Collections.enumeration(internalMap.values());
        }

        public Object get(Object keyObj) {
            String lowerCase = checkKey(keyObj == null ? null : keyObj.toString()).toLowerCase();
            return internalMap.get(lowerCase);
        }

        public boolean isEmpty() {
            return internalMap.isEmpty();
        }

        public Enumeration<String> keys() {
            return Collections.enumeration(originalKeys.values());
        }

        public Object put(String key, Object value) {
            String lowerCase = checkKey(key).toLowerCase();
            checkValue(value);
            originalKeys.put(lowerCase, key);
            return internalMap.put(lowerCase, value);
        }

        public Object remove(Object keyObj) {
            String lowerCase = checkKey(keyObj == null ? null : keyObj.toString()).toLowerCase();
            originalKeys.remove(lowerCase);
            return internalMap.remove(lowerCase);
        }

        public int size() {
            return internalMap.size();
        }

        static String checkKey(String key) {
            if (key == null || key.length() == 0) {
                throw new IllegalArgumentException("Key must not be null nor an empty string");
            }
            return key;
        }

        static Object checkValue(Object value) {
            if (value == null) {
                throw new IllegalArgumentException("Value must not be null");
            }
            return value;
        }

        public String toString() {
            return internalMap.toString();
        }

    }

}
