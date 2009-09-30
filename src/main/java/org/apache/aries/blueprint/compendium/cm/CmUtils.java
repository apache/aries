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

import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

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
  
}
