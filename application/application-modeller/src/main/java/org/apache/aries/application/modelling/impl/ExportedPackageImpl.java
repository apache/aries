/*
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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.application.modelling.impl;
import static org.apache.aries.application.utils.AppConstants.LOG_ENTRY;
import static org.apache.aries.application.utils.AppConstants.LOG_EXIT;
import static org.apache.aries.application.modelling.ResourceType.PACKAGE;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.application.modelling.ExportedPackage;
import org.apache.aries.application.modelling.ModelledResource;
import org.apache.aries.application.modelling.ResourceType;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




public class ExportedPackageImpl implements ExportedPackage
{
  
  @SuppressWarnings("deprecation")
  private static final String PACKAGE_SPECIFICATION_VERSION = Constants.PACKAGE_SPECIFICATION_VERSION;
  private final Map<String, Object> _attributes;
  private final String _packageName;
  private final String _version;
  private final ModelledResource _bundle;
  private final Logger logger = LoggerFactory.getLogger(ExportedPackageImpl.class);
  /**
   * 
   * @param mr                 The {@link ModelledResource} exporting this package. Never null.  
   * @param pkg                The fully qualified name of the package being exported
   * @param attributes         The package attributes. If no version is present, will be defaulted to 0.0.0. 
   *                           
   */
  public ExportedPackageImpl (ModelledResource mr, String pkg, Map<String, Object> attributes) {
    logger.debug(LOG_ENTRY, "ExportedPackageImpl", new Object[]{mr, pkg, attributes});
    _attributes = new HashMap<String, Object> (attributes);
    _packageName = pkg;
    _attributes.put (PACKAGE.toString(), _packageName);
    String version = (String) attributes.get(Constants.VERSION_ATTRIBUTE);
    if (version == null || "".equals(version)) { 
      _version = Version.emptyVersion.toString();
    } else { 
      _version = version;
    }
    _attributes.put(Constants.VERSION_ATTRIBUTE, _version); 
    _attributes.put (Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE, mr.getSymbolicName());
    _attributes.put (Constants.BUNDLE_VERSION_ATTRIBUTE, mr.getVersion());
    _bundle = mr;
    logger.debug(LOG_EXIT, "ExportedPackageImpl");
  }


  public Map<String, Object> getAttributes() {
    logger.debug(LOG_ENTRY, "getAttributes");
    logger.debug(LOG_EXIT, "getAttributes", _attributes);
    return Collections.unmodifiableMap(_attributes);
  }


  public ResourceType getType() {
    logger.debug(LOG_ENTRY, "getType");
    logger.debug(LOG_EXIT, "getType", PACKAGE);
    return PACKAGE;
  }

  /**
   * Get the name of the exported package
   * @return package name
   */
  public String getPackageName() { 
    logger.debug(LOG_ENTRY, "getPackageName");
    logger.debug(LOG_EXIT, "getPackageName", _packageName);
    return _packageName;
  }

  /**
   * This will never be null. 
   * @return Version as String, or 0.0.0
   */
  public String getVersion() {
    logger.debug(LOG_ENTRY, "getVersion");
    logger.debug(LOG_EXIT, "getVersion", _version);
    return _version;
  }
  
  /**
   * This method turns an {@link ExportedPackageImpl} into a string suitable for a 
   * Use-Bundle style package import. We do NOT lock down package versions, only bundle versions. 
   */
  public String toDeploymentString() {
    logger.debug(LOG_ENTRY, "toDeploymentString");
    StringBuilder sb = new StringBuilder(_packageName);
    for (Map.Entry<String, Object> entry : _attributes.entrySet()) {
      String key = entry.getKey();
      Object objectValue = entry.getValue(); 
      // While attributes can be arrays, especially for services, they should never be arrays for packages
      // If the values are not arrays, they are Strings
      if (!objectValue.getClass().isArray()) {  
        String value = String.valueOf(objectValue);
        if (key != null && !key.equals(PACKAGE.toString()) 
            && !key.equals(PACKAGE_SPECIFICATION_VERSION))
        {
          if (key.equals(Constants.BUNDLE_VERSION_ATTRIBUTE)) { 
            value = "[" + value + "," + value + "]";
          }
          // No Export-Package directives are valid on Import-Package, so strip out all 
          // directives. Never print out a null or empty key or value. 
          if (key.equals("") || key.endsWith(":") || value==null || value.equals("")) {
            
            logger.debug("ExportedPackageImpl.toDeploymentString ignored " + key + "=" + value);
          } else { 
            sb.append (";").append (key).append("=\"").append(value).append('"');
          }
        } else { 
          logger.debug("ExportedPackageImpl.toDeploymentString() ignoring attribute " + key + "->" + value);
        }
      }
    }
    String result = sb.toString();
    logger.debug(LOG_EXIT, "toDeploymentString", result);
    return result;
  }

  public ModelledResource getBundle() {
    logger.debug(LOG_ENTRY, "getBundle");
    logger.debug(LOG_EXIT, "getBundle", _bundle);
    return _bundle;
  }
  
  @Override
  public String toString() {
    return toDeploymentString();
  }

}
