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

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.aries.application.InvalidAttributeException;
import org.apache.aries.application.modelling.ImportedBundle;
import org.apache.aries.application.modelling.ModellingConstants;
import org.apache.aries.application.modelling.Provider;
import org.apache.aries.application.modelling.ResourceType;
import org.apache.aries.application.modelling.utils.impl.ModellingHelperImpl;
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
 * A model of a Bundle imported, or required, by something. For example, an entry in an APPLICATION.MF. 
 */
public class ImportedBundleImpl implements ImportedBundle
{
 
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
    logger.debug(LOG_ENTRY, "ImportedBundleImpl", new Object[]{filterString, attributes});
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
      logger.debug(LOG_EXIT, "ImportedBundleImpl", new Object[]{iax});
      throw iax;
    }
    logger.debug(LOG_EXIT, "ImportedBundleImpl");
  }
  
  /**
   * Build an ImportedBundleImpl from a bundle name and version range.  
   * @param bundleName   Bundle symbolic name
   * @param versionRange Bundle version range
   * @throws InvalidAttributeException
   */
  public ImportedBundleImpl (String bundleName, String versionRange) throws InvalidAttributeException { 
    logger.debug(LOG_ENTRY, "ImportedBundleImpl", new Object[] {bundleName, versionRange});
    _attributes = new HashMap<String, String> ();
    _attributes.put (ModellingConstants.OBR_SYMBOLIC_NAME, bundleName);
    _attributes.put (Constants.VERSION_ATTRIBUTE, versionRange);
    _filterString = ManifestHeaderProcessor.generateFilter(_attributes);
    try { 
      _filter = FrameworkUtil.createFilter(FilterUtils.removeMandatoryFilterToken(_filterString));
    } catch (InvalidSyntaxException isx) { 
      InvalidAttributeException iax = new InvalidAttributeException(isx);
      logger.debug(LOG_ENTRY, "ImportedBundleImpl", new Object[] {iax});
      throw iax;
    }
    logger.debug(LOG_EXIT, "ImportedBundleImpl"); 
  }
 

  public String getAttributeFilter() {
    logger.debug(LOG_ENTRY, "getAttributeFilter");
    logger.debug(LOG_EXIT, "getAttributeFilter", new Object[] {_filterString});
    return _filterString;
  }


  public ResourceType getType() {

    logger.debug(LOG_ENTRY, "getType");
    logger.debug(LOG_EXIT, "getType", new Object[] {ResourceType.BUNDLE});
    return ResourceType.BUNDLE;
  }


  public boolean isMultiple() {
    logger.debug(LOG_ENTRY, "isMultiple");
    logger.debug(LOG_EXIT, "isMultiple", new Object[] {false});
    return false;
  }


  public boolean isOptional() {
    logger.debug(LOG_ENTRY, "isOptional");
    boolean optional = false;
    if (_attributes.containsKey(Constants.RESOLUTION_DIRECTIVE + ":")) {
      if ((Constants.RESOLUTION_OPTIONAL).equals
          (_attributes.get(Constants.RESOLUTION_DIRECTIVE + ":"))) { 
        optional = true;
      }
    }
    logger.debug(LOG_EXIT, "isOptional", optional);
    return optional;
  }

  public boolean isSatisfied(Provider capability) {
    logger.debug(LOG_ENTRY, "isSatisfied", capability);
    if (capability.getType() != ResourceType.BUNDLE 
        && capability.getType() != ResourceType.COMPOSITE) { 
      logger.debug(LOG_EXIT, "isSatisfied", false);
      return false;
    }
    Dictionary<String, Object> dict = new Hashtable<String, Object> (capability.getAttributes());
    String version = (String) dict.get(Constants.VERSION_ATTRIBUTE);
    if (version != null) { 
      dict.put(Constants.VERSION_ATTRIBUTE, Version.parseVersion(version));
    }
    boolean allPresent = ModellingHelperImpl.areMandatoryAttributesPresent_(_attributes, capability);
    boolean result = allPresent && _filter.match(dict);
    logger.debug(LOG_EXIT, "isSatisfied", result);
    return result;
  }
  
  /**
   * Get the version range on this bundle import
   * @return Imported version range, as a string
   */
  public String getVersionRange() {
    logger.debug(LOG_ENTRY, "getVersionRange");
    String range = _attributes.get(Constants.VERSION_ATTRIBUTE);
    String result = (range == null) ? Version.emptyVersion.toString() : range;
    logger.debug(LOG_EXIT, "getVersionRange", result);
    return result;
  }
  
  /**
   * Get the symbolic name of the imported bundle
   * @return symbolic name
   */
  public String getSymbolicName() {
    logger.debug(LOG_ENTRY, "getSymbolicName");
    String result = _attributes.get(ModellingConstants.OBR_SYMBOLIC_NAME);
    logger.debug(LOG_EXIT, "getSymbolicName", result);
    return result;
  }
  
  /**
   * Equal if symbolic names match and version strings match
   */
  @Override
  public boolean equals(Object o)
  {
    logger.debug(LOG_ENTRY, "equals", o);
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
    logger.debug(LOG_EXIT, "equals", result);
    return result;
  }
  
  @Override
  public int hashCode()
  {
    logger.debug(LOG_ENTRY, "hashCode");
    int hashCode = getSymbolicName().hashCode() + 31 * getVersionRange().hashCode();
    logger.debug(LOG_ENTRY, "hashCode", hashCode);
    return hashCode;
  }
  
  @Override
  public String toString() {
    return _filterString;
  }
}
