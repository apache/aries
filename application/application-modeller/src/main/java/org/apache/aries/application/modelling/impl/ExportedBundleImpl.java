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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;

import org.apache.aries.application.InvalidAttributeException;
import org.apache.aries.application.modelling.ImportedBundle;
import org.apache.aries.application.modelling.internal.MessageUtil;
import org.apache.aries.application.modelling.utils.ModellingConstants;
import org.apache.aries.application.modelling.utils.ModellingUtils;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor.NameValueMap;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An exported bundle: one that I have and make available.  
 */
public class ExportedBundleImpl extends AbstractExportedBundle
{
  private static final Logger logger = LoggerFactory.getLogger(ExportedBundleImpl.class);
  private final Map<String, Object> _attributes;
  private final ImportedBundle _fragHost;
    
  /**
   * Construct an ExportedBundleImpl from a processed Manifest
   * @param attrs
   * @throws InvalidAttributeException
   */
  public ExportedBundleImpl (Attributes attrs) throws InvalidAttributeException { 
    logger.debug(LOG_ENTRY, "ExportedBundleImpl", attrs);
    String symbolicName = attrs.getValue(Constants.BUNDLE_SYMBOLICNAME);
    
    Map<String,NameValueMap<String, String>> map = ManifestHeaderProcessor.parseImportString(symbolicName);
    
    //This should have one entry, which is keyed on the symbolicName
    
    if(map.size() != 1) {
      InvalidAttributeException iax = new InvalidAttributeException (MessageUtil.getMessage(
          "TOO_MANY_SYM_NAMES", new Object[] {symbolicName}));
      logger.debug(LOG_EXIT, "ExportedBundleImpl", iax);
      throw iax;
    }
    
    Map.Entry<String, NameValueMap<String, String>> entry =  map.entrySet().iterator().next();
    
    symbolicName = entry.getKey();
    
    Map<String, String> bundleAttrs = entry.getValue();
    
    String displayName = attrs.getValue(Constants.BUNDLE_NAME);
    String version = attrs.getValue(Constants.BUNDLE_VERSION);
    if (version == null) { 
      version = Version.emptyVersion.toString();
    }
    String bmVersion = attrs.getValue(Constants.BUNDLE_MANIFESTVERSION);
    if (symbolicName == null || bmVersion == null) { 
      InvalidAttributeException iax = new InvalidAttributeException(MessageUtil.getMessage("INCORRECT_MANDATORY_HEADERS", 
          new Object[] {symbolicName, bmVersion}));
      logger.debug(LOG_EXIT, "ExportedBundleImpl", iax);
      throw iax;
    }

    if(bundleAttrs != null)
      _attributes = new HashMap<String, Object>(entry.getValue());
    else
      _attributes = new HashMap<String, Object>();

    _attributes.put (Constants.BUNDLE_MANIFESTVERSION, bmVersion);
    _attributes.put(ModellingConstants.OBR_SYMBOLIC_NAME, symbolicName);
    _attributes.put (Constants.VERSION_ATTRIBUTE, version);
    
    if(displayName != null)
      _attributes.put(ModellingConstants.OBR_PRESENTATION_NAME, displayName);
    
    String fragmentHost = attrs.getValue(Constants.FRAGMENT_HOST);
    if (fragmentHost != null) { 
      _fragHost = ModellingUtils.buildFragmentHost(fragmentHost);
      _attributes.put(Constants.FRAGMENT_HOST, fragmentHost);
    } else { 
      _fragHost = null;
    }
    logger.debug(LOG_EXIT, "ExportedBundleImpl");
  }
  
  /**
   * Construct a bundle from attributes and a fragment host
   * @param attributes attributes describing the bundle
   * @param fragHost may be null if this bundle is not a fragment
   */
  public ExportedBundleImpl(Map<String, String> attributes, ImportedBundle fragHost) {
    logger.debug(LOG_ENTRY, "ExportedBundleImpl", new Object[]{attributes, fragHost});
    _attributes = new HashMap<String, Object>(attributes);
    _fragHost = fragHost;
    logger.debug(LOG_EXIT, "ExportedBundleImpl", new Object[]{attributes, fragHost});
  }


  public Map<String, Object> getAttributes() {
    logger.debug(LOG_ENTRY, "getAttributes");
    logger.debug(LOG_EXIT, "getAttributes", new Object[]{_attributes});
    return Collections.unmodifiableMap(_attributes);
  }
  

  public String toString() {
    return  _attributes.toString();
  }

 
  public ImportedBundle getFragmentHost() {
    logger.debug(LOG_ENTRY, "getFragmentHost");
    logger.debug(LOG_EXIT, "getFragmentHost", new Object[]{_fragHost});
    return _fragHost;
  }


  public boolean isFragment() {
    logger.debug(LOG_ENTRY, "isFragment");
    boolean result = _fragHost != null;
    logger.debug(LOG_EXIT, "isFragment", new Object[]{result});
    return result;
  }
}
