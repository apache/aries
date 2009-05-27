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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Since persistence id can only be associated with one ManagedService in a bundle
 * this class ensures only one ManagedService is registered per persistence id.
 */
public class ManagedObjectManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedObjectManager.class);

    private static HashMap<Key, ConfigurationWatcher> map = new HashMap<Key, ConfigurationWatcher>();
               
    public static synchronized void register(ManagedObject cm, Properties props) {
        Key key = new Key(cm);
        ConfigurationWatcher reg = map.get(key);
        if (reg == null) {
            reg = new ConfigurationWatcher(); 
            ServiceRegistration registration = cm.getBundle().getBundleContext().registerService(ManagedService.class.getName(), reg, props);
            reg.setRegistration(registration);            
            map.put(key, reg);
        }
        reg.add(cm);
    }

    public static synchronized void unregister(ManagedObject cm) {
        Key key = new Key(cm);
        ConfigurationWatcher reg = map.get(key);
        if (reg != null) {
            reg.remove(cm);
            if (reg.isEmpty()) {
                map.remove(key);
                reg.getRegistration().unregister();
            }
        }
    }
            
    private static class ConfigurationWatcher implements ManagedService {

        private ServiceRegistration registration;
        private List<ManagedObject> list = new ArrayList<ManagedObject>();
        
        public ConfigurationWatcher() {
        }
        
        public void updated(Dictionary props) throws ConfigurationException {
            for (ManagedObject cm : list) {
                cm.updated(props);
            }
        }
        
        private void setRegistration(ServiceRegistration registration) {
            this.registration = registration;
        }
        
        private ServiceRegistration getRegistration() {
            return registration;
        }
        
        private void add(ManagedObject cm) {
            list.add(cm);
        }
        
        private void remove(ManagedObject cm) {
            list.remove(cm);
        }
        
        private boolean isEmpty() {
            return list.isEmpty();
        }
    }
    
    private static class Key {
        
        private String persistanceId;
        private Bundle bundle;
        
        public Key(ManagedObject cm) {
            this.persistanceId = cm.getPersistentId();
            this.bundle = cm.getBundle();
        }

        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((bundle == null) ? 0 : bundle.hashCode());
            result = prime * result + ((persistanceId == null) ? 0 : persistanceId.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Key other = (Key) obj;
            if (bundle == null) {
                if (other.bundle != null) {
                    return false;
                }
            } else if (!bundle.equals(other.bundle)) {
                return false;
            }
            if (persistanceId == null) {
                if (other.persistanceId != null) {
                    return false;
                }
            } else if (!persistanceId.equals(other.persistanceId)) {
                return false;
            }
            return true;
        }
        
    }
}
