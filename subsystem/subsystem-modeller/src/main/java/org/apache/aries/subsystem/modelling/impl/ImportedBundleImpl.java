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
package org.apache.aries.subsystem.modelling.impl;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.aries.subsystem.modelling.ImportedBundle;
import org.apache.aries.subsystem.modelling.InvalidAttributeException;
import org.apache.aries.subsystem.modelling.ModellingConstants;
import org.apache.aries.subsystem.modelling.Provider;
import org.apache.aries.subsystem.modelling.ResourceType;
import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A model of a Bundle imported, or required, by something. For example, an entry in an APPLICATION.MF. 
 */
public class ImportedBundleImpl implements ImportedBundle {
 
  private final Map<String, String> _attributes;
  private final String _filterString;
  private final Filter _filter;
  private final Logger logger = LoggerFactory.getLogger(ImportedBundleImpl.class);
  /**
   * Build an ImportedBundleImpl from filter string and a set of attributes. The filter string is 
   * most easily obtained ManifestHeaderProcessor.generateFilter() or Requirement.getFilter() - 
   * be careful if building your own. 
   * @param filterString For example as obtained from Requirement.getFilter()
   * @param attributes   
   * @throws InvalidAttributeException 
   */
  public ImportedBundleImpl(String filterString, Map<String, String> attributes) throws InvalidAttributeException
  {
    logger.debug("Method entry: {}, args {}", "ImportedBundleImpl", new Object[]{filterString, attributes});
    _attributes = new HashMap<String, String> (attributes);
    String versionRange = _attributes.remove(Constants.BUNDLE_VERSION_ATTRIBUTE);
    if(versionRange == null) {
      versionRange = Version.emptyVersion.toString();
    }
    if(_attributes.get(Constants.VERSION_ATTRIBUTE) == null) { 
       _attributes.put(Constants.VERSION_ATTRIBUTE, versionRange);
    }
    _filterString = filterString;
    try { 
      _filter = FrameworkUtil.createFilter(FilterUtils.removeMandatoryFilterToken(_filterString));
    } catch (InvalidSyntaxException isx) {
      InvalidAttributeException iax = new InvalidAttributeException(isx);
      logger.debug("Method exit: {}, returning {}", "ImportedBundleImpl", new Object[]{iax});
      throw iax;
    }
    logger.debug("Method exit: {}, returning {}", "ImportedBundleImpl");
  }
  
  /**
   * Build an ImportedBundleImpl from a bundle name and version range.  
   * @param bundleName   Bundle symbolic name
   * @param versionRange Bundle version range
   * @throws InvalidAttributeException
   */
  public ImportedBundleImpl (String bundleName, String versionRange) throws InvalidAttributeException { 
    logger.debug("Method entry: {}, args {}", "ImportedBundleImpl", new Object[] {bundleName, versionRange});
    _attributes = new HashMap<String, String> ();
    _attributes.put (ModellingConstants.OBR_SYMBOLIC_NAME, bundleName);
    _attributes.put (Constants.VERSION_ATTRIBUTE, versionRange);
    _filterString = ManifestHeaderProcessor.generateFilter(_attributes);
    try { 
      _filter = FrameworkUtil.createFilter(FilterUtils.removeMandatoryFilterToken(_filterString));
    } catch (InvalidSyntaxException isx) { 
      InvalidAttributeException iax = new InvalidAttributeException(isx);
      logger.debug("Method entry: {}, args {}", "ImportedBundleImpl", new Object[] {iax});
      throw iax;
    }
    logger.debug("Method exit: {}, returning {}", "ImportedBundleImpl");
  }
 

  public String getAttributeFilter() {
    logger.debug("Method entry: {}, args {}", "getAttributeFilter");
    logger.debug("Method exit: {}, returning {}", "getAttributeFilter", new Object[] {_filterString});
    return _filterString;
  }


  public ResourceType getType() {

    logger.debug("Method entry: {}, args {}", "getType");
    logger.debug("Method exit: {}, returning {}", "getType", new Object[] {ResourceType.BUNDLE});
    return ResourceType.BUNDLE;
  }


  public boolean isMultiple() {
    logger.debug("Method entry: {}, args {}", "isMultiple");
    logger.debug("Method exit: {}, returning {}", "isMultiple", new Object[] {false});
    return false;
  }


  public boolean isOptional() {
    logger.debug("Method entry: {}, args {}", "isOptional");
    boolean optional = false;
    if (_attributes.containsKey(Constants.RESOLUTION_DIRECTIVE + ":")) {
      if (Constants.RESOLUTION_OPTIONAL.equals(_attributes.get(Constants.RESOLUTION_DIRECTIVE + ":"))) {
        optional = true;
      }
    }
    logger.debug("Method exit: {}, returning {}", "isOptional", optional);
    return optional;
  }

  public boolean isSatisfied(Provider capability) {
    logger.debug("Method entry: {}, args {}", "isSatisfied", capability);
    if (capability.getType() != ResourceType.BUNDLE 
        && capability.getType() != ResourceType.COMPOSITE) { 
      logger.debug("Method exit: {}, returning {}", "isSatisfied", false);
      return false;
    }
    Dictionary<String, Object> dict = new Hashtable<String, Object> (capability.getAttributes());
    String version = (String) dict.get(Constants.VERSION_ATTRIBUTE);
    if (version != null) { 
      dict.put(Constants.VERSION_ATTRIBUTE, Version.parseVersion(version));
    }
    boolean allPresent = ModellingHelperImpl.areMandatoryAttributesPresent_(_attributes, capability);
    boolean result = allPresent && _filter.match(dict);
    logger.debug("Method exit: {}, returning {}", "isSatisfied", result);
    return result;
  }
  
  /**
   * Get the version range on this bundle import
   * @return Imported version range, as a string
   */
  public String getVersionRange() {
    logger.debug("Method entry: {}, args {}", "getVersionRange");
    String range = _attributes.get(Constants.VERSION_ATTRIBUTE);
    String result = (range == null) ? Version.emptyVersion.toString() : range;
    logger.debug("Method exit: {}, returning {}", "getVersionRange", result);
    return result;
  }
  
  /**
   * Get the symbolic name of the imported bundle
   * @return symbolic name
   */
  public String getSymbolicName() {
    logger.debug("Method entry: {}, args {}", "getSymbolicName");
    String result = _attributes.get(ModellingConstants.OBR_SYMBOLIC_NAME);
    logger.debug("Method exit: {}, returning {}", "getSymbolicName", result);
    return result;
  }
  
  /**
   * Equal if symbolic names match and version strings match
   */
  @Override
  public boolean equals(Object o)
  {
    logger.debug("Method entry: {}, args {}", "equals", o);
    boolean result = false;
    if (o == this)
    {
      result = true;
    }
    else if (o instanceof ImportedBundleImpl)
    {
      ImportedBundleImpl ib = (ImportedBundleImpl)o;
      result = (getSymbolicName().equals(ib.getSymbolicName())
          && getVersionRange().equals(ib.getVersionRange()));
    }
    logger.debug("Method exit: {}, returning {}", "equals", result);
    return result;
  }
  
  @Override
  public int hashCode()
  {
    logger.debug("Method entry: {}, args {}", "hashCode");
    int hashCode = getSymbolicName().hashCode() + 31 * getVersionRange().hashCode();
    logger.debug("Method entry: {}, args {}", "hashCode", hashCode);
    return hashCode;
  }
  
  @Override
  public String toString() {
    return _filterString;
  }
}
