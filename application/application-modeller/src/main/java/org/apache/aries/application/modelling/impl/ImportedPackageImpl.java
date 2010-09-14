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

import static org.apache.aries.application.modelling.ResourceType.PACKAGE;
import static org.apache.aries.application.modelling.utils.ModellingConstants.OPTIONAL_KEY;
import static org.apache.aries.application.utils.AppConstants.LOG_ENTRY;
import static org.apache.aries.application.utils.AppConstants.LOG_EXIT;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.aries.application.InvalidAttributeException;
import org.apache.aries.application.modelling.ImportedPackage;
import org.apache.aries.application.modelling.Provider;
import org.apache.aries.application.modelling.ResourceType;
import org.apache.aries.application.modelling.utils.ModellingUtils;
import org.apache.aries.application.utils.FilterUtils;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * An imported, or required package. Capable of generating an entry in DEPLOYMENT.MF's Import-Package header. 
 */
public class ImportedPackageImpl implements ImportedPackage
{

  
  private final boolean _optional;
  private final String _filterString;
  private final Filter _filter;
  private final String _package;
  private final String _versionRange;
  private final Map<String,String> _attributes;
  private final Logger logger = LoggerFactory.getLogger(ImportedPackageImpl.class);
  /**
   * Construct a package requirement
   * @param pkg The name of the required package
   * @param attributes Other attributes - most commonly, version
   * @throws InvalidAttributeException
   */
  public ImportedPackageImpl (String pkg, Map<String, String> attributes) throws InvalidAttributeException {
    logger.debug(LOG_ENTRY, "ImportedPackageImpl", new Object[] {pkg, attributes});
    _package = pkg;
    String versionRange = null;
    if (attributes != null) {
      _optional = (Constants.RESOLUTION_OPTIONAL.equals(attributes.get(OPTIONAL_KEY)));
      versionRange = attributes.get(Constants.VERSION_ATTRIBUTE);
      _attributes = new HashMap<String, String>(attributes);
    } else { 
      _optional = false;
      _attributes = new HashMap<String, String>();
    }
    if (versionRange == null) {
      _versionRange = Version.emptyVersion.toString();
    } else { 
      _versionRange = versionRange;
    }
    
    _attributes.put(Constants.VERSION_ATTRIBUTE, _versionRange);
    _filterString = ManifestHeaderProcessor.generateFilter(PACKAGE.toString(), _package, _attributes);
    try { 
    _filter = FrameworkUtil.createFilter(FilterUtils.removeMandatoryFilterToken(_filterString));
    } catch (InvalidSyntaxException isx) { 
      logger.debug(LOG_EXIT, "ImportedPackageImpl", new Object[] {isx});
      throw new InvalidAttributeException(isx);
    }
    logger.debug(LOG_EXIT, "ImportedPackageImpl");
  }
  
  /**
   * Get this ImportedPackageImpl's attributes
   * @return attributes
   */
  public Map<String, String> getAttributes() { 
    logger.debug(LOG_ENTRY, "getAttributes");
    logger.debug(LOG_EXIT, "getAttributes", new Object[] {_attributes});
    return Collections.unmodifiableMap(_attributes);
  }
  
  /**
   * Get the package name
   * @return package name
   */
  public String getPackageName() { 
    logger.debug(LOG_ENTRY, "getPackageName");
    logger.debug(LOG_EXIT, "getPackageName", new Object[] {_package});
    return _package;
  }
  
  /**
   * Get the imported package's version range
   * @return version range
   */
  public String getVersionRange() {
    logger.debug(LOG_ENTRY, "getVersionRange");
    logger.debug(LOG_EXIT, "getVersionRange", new Object[] {_versionRange});
    return _versionRange;
  }
  

  public String getAttributeFilter() {
    logger.debug(LOG_ENTRY, "getAttributeFilter");
    logger.debug(LOG_EXIT, "getAttributeFilter", new Object[] {_filterString});
    return _filterString;
  }


  public ResourceType getType() {
    logger.debug(LOG_ENTRY, "getType");
    logger.debug(LOG_EXIT, "getType", new Object[] {PACKAGE});
    return PACKAGE;
  }


  public boolean isMultiple() {
    logger.debug(LOG_ENTRY, "isMultiple");
    logger.debug(LOG_EXIT, "isMultiple", new Object[] {false});
    return false;   // cannot import a given package more than once
  }


  public boolean isOptional() {
    logger.debug(LOG_ENTRY, "isOptional");
    logger.debug(LOG_EXIT, "isOptional", new Object[] {_optional});
    return _optional;
  }


  public boolean isSatisfied(Provider capability) {
    logger.debug(LOG_ENTRY, "isSatisfied", new Object[]{capability});
    if (capability.getType() != PACKAGE) { 
      logger.debug(LOG_EXIT, "isSatisfied", new Object[] {false});
      return false;
    }
    Dictionary<String, Object> dict = new Hashtable<String, Object> (capability.getAttributes());
    String version = (String) dict.get(Constants.VERSION_ATTRIBUTE);
    if (version != null) { 
      dict.put(Constants.VERSION_ATTRIBUTE, Version.parseVersion(version));
    }
    
    boolean allPresent = ModellingUtils.areMandatoryAttributesPresent(_attributes, capability);
    boolean result = allPresent && _filter.match(dict);
    logger.debug(LOG_EXIT, "isSatisfied", new Object[] {result});
    return result;
  }

  
  
  /**
  * This method turns an {@link ImportedPackageImpl} into a string suitable for a 
  * Provision-Bundle style package import.  
  * It will not include ;bundle-symbolic-name=bundleName;bundle-version=version attribute pairs
  * @return A String
  */
  @SuppressWarnings("deprecation")
  public String toDeploymentString() {
    logger.debug(LOG_ENTRY, "toDeploymentString");
    StringBuilder sb = new StringBuilder(_package);
    // Note that the RESOLUTION_DIRECTIVE is set in this map, so it will be
    // output automatically. p41 of the OSGi Core Spec v4.2 includes an example
    // Import-Package with a resolution:=mandatory directive on. We could choose to suppress
    // resolution:=mandatory on packages however, since mandatory is the default.
    for (Map.Entry<String, String> entry : _attributes.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      if (!key.equals(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE)
          && !key.equals(Constants.BUNDLE_VERSION_ATTRIBUTE)
          && !key.equals(PACKAGE.toString())
          && !key.equals(Constants.PACKAGE_SPECIFICATION_VERSION)) {
        sb.append(";").append(key).append("=\"").append(value).append('"');
      } else {
        logger.debug("ignoring attribute {" + key + "=" + value + "} in ImportedPackageImpl.toDeploymentString()");
      }
    }
    String result = sb.toString();
    logger.debug(LOG_EXIT, "toDeploymentString", new Object[]{result});
    return result;
  }
  
  @Override
  public String toString() {
    return toDeploymentString();
  }
}
