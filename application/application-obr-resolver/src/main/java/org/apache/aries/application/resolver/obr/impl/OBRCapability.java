/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.aries.application.resolver.obr.impl;


import static org.apache.aries.application.utils.AppConstants.LOG_ENTRY;
import static org.apache.aries.application.utils.AppConstants.LOG_EXIT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.aries.application.modelling.Provider;
import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.DataModelHelper;
import org.apache.felix.bundlerepository.Property;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common code for handling OBR Capabilities. 
 */
public class OBRCapability implements Capability
{

  private Logger logger = LoggerFactory.getLogger(OBRCapability.class);
  private final Provider _provider;

  private final RepositoryAdmin repositoryAdmin;
  /**
   * Property map for this Capability.
   */
  private final Map<String, Object> _props;
  
  /**
   * Construct a Capability specifying the OBR name to use.
   * @param type the value to be used for the Capability name in OBR.
   */
  public OBRCapability(Provider provider, RepositoryAdmin repositoryAdmin){

    logger.debug(LOG_ENTRY, "OBRCapability", provider);
    _provider = provider;
    _props = new HashMap<String, Object>(provider.getAttributes());
    this.repositoryAdmin = repositoryAdmin;
    logger.debug(LOG_EXIT, "OBRCapability");
    
  }
  
 
  public String getName()
  {
    logger.debug(LOG_ENTRY, "getName");
    String name = _provider.getType().toString();  
    logger.debug(LOG_EXIT, "getName", name);
    return name;
  }

 
  public Property[] getProperties()
  {
    logger.debug(LOG_ENTRY, "getProperties");
    DataModelHelper helper = repositoryAdmin.getHelper();
    
    List<Property> properties = new ArrayList<Property>();
    
    // Felix BundleRepository doesn't appear to correctly cope with String[] value properties
    // as a result we can't do multi value service properties. OBR doesn't really like implementations
    // of its interfaces that it didn't generate, but it is a little weird when it does and doesn't.
    // so we create a Property implemenation which we use to generate the OBR xml for a property which
    // we then get OBR to parse. This is really convoluted and nasty.
    for (final Map.Entry<String, Object> entry : _props.entrySet()) {
      String propXML = helper.writeProperty(new Property() {
        @Override
        public String getValue()
        {
          Object value = entry.getValue();
          
          if (value instanceof String[]) {
            String newValue = Arrays.toString((String[])value);
            value = newValue.substring(1, newValue.length() - 1);
          }
          
          return String.valueOf(value);
        }
        
        @Override
        public String getType()
        {
          String name = entry.getKey();
          String type = null;
          if (Constants.VERSION_ATTRIBUTE.equals(name) || (Constants.BUNDLE_VERSION_ATTRIBUTE.equals(name))) {
            type =  "version";
          } else if (Constants.OBJECTCLASS.equals(name) || (Constants.MANDATORY_DIRECTIVE + ":").equals(name) ||
              entry.getValue() instanceof String[])
            type = "set";
          return type;
        }
        
        @Override
        public String getName()
        {
          return entry.getKey();
        }
        
        @Override
        public Object getConvertedValue()
        {
          return null;
        }
      });
      
      try {
        properties.add(helper.readProperty(propXML));
      } catch (Exception e) {
        // Do nothing and hope it OBR doesn't generate XML it can't parse.
      }
    }
    
    
    logger.debug(LOG_EXIT, "getProperties", properties);
    return properties.toArray(new Property[properties.size()]);
  }

  public Map getPropertiesAsMap()
  {
    logger.debug(LOG_ENTRY, "getPropertiesAsMap");
    logger.debug(LOG_ENTRY, "getPropertiesAsMap", new Object[]{_props});
    return _props;
  }

}
