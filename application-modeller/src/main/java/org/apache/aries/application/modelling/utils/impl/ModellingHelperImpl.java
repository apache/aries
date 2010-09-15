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
package org.apache.aries.application.modelling.utils.impl;

import static org.apache.aries.application.modelling.ModellingConstants.OPTIONAL_KEY;
import static org.apache.aries.application.utils.AppConstants.LOG_ENTRY;
import static org.apache.aries.application.utils.AppConstants.LOG_EXIT;
import static org.osgi.framework.Constants.BUNDLE_VERSION_ATTRIBUTE;
import static org.osgi.framework.Constants.VERSION_ATTRIBUTE;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.aries.application.InvalidAttributeException;
import org.apache.aries.application.VersionRange;
import org.apache.aries.application.modelling.DeployedBundles;
import org.apache.aries.application.modelling.ImportedBundle;
import org.apache.aries.application.modelling.ImportedPackage;
import org.apache.aries.application.modelling.ModelledResource;
import org.apache.aries.application.modelling.ModellingConstants;
import org.apache.aries.application.modelling.Provider;
import org.apache.aries.application.modelling.impl.DeployedBundlesImpl;
import org.apache.aries.application.modelling.impl.ImportedBundleImpl;
import org.apache.aries.application.modelling.impl.ImportedPackageImpl;
import org.apache.aries.application.modelling.internal.MessageUtil;
import org.apache.aries.application.modelling.utils.ModellingHelper;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor.NameValueMap;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ModellingHelperImpl implements ModellingHelper
{
  private static final  Logger logger = LoggerFactory.getLogger(ModellingHelperImpl.class);

  
  @Override
  public boolean areMandatoryAttributesPresent(
      Map<String, String> consumerAttributes, Provider p) {
    return areMandatoryAttributesPresent_(consumerAttributes, p);
  }


  @Override
  public ImportedBundle buildFragmentHost(String fragmentHostHeader)
      throws InvalidAttributeException {
    return buildFragmentHost_(fragmentHostHeader);
  }


  @Override
  public ImportedPackage intersectPackage(ImportedPackage p1, ImportedPackage p2) {
    return intersectPackage_(p1, p2);
  }
  
  @Override
  public DeployedBundles createDeployedBundles(String assetName,
      Collection<ImportedBundle> appContentNames,
      Collection<ImportedBundle> appUseBundleNames,
      Collection<ModelledResource> fakeServiceProvidingBundles) 
  {
    logger.debug(LOG_ENTRY, "createDeployedBundles", new Object[]{assetName, 
        appContentNames, appUseBundleNames, fakeServiceProvidingBundles});  
    DeployedBundles result = new DeployedBundlesImpl (assetName, 
        appContentNames, appUseBundleNames, fakeServiceProvidingBundles);
    logger.debug(LOG_EXIT, "createDeployedBundles", result);
    return result;
  }
  
  // These underlying static methods are directly accessible 
  // from other classes within the bundle
  public static boolean areMandatoryAttributesPresent_(Map<String,String> consumerAttributes, Provider p) {
    logger.debug(LOG_ENTRY, "areMandatoryAttributesPresent_", new Object[]{consumerAttributes, p});
    boolean allPresent = true;
    String mandatory = (String) p.getAttributes().get(Constants.MANDATORY_DIRECTIVE + ":");
    
    if(mandatory != null && !mandatory.equals("")) {
      List<String> attributeNames = ManifestHeaderProcessor.split(mandatory, ",");
      
      for(String name : attributeNames) {
        allPresent = consumerAttributes.containsKey(name);
        if(!allPresent)
          break;
      }
    }
    logger.debug(LOG_EXIT, "areMandatoryAttributesPresent_", allPresent);
    return allPresent;
  }
  
  
  
  public static ImportedBundle buildFragmentHost_(String fragmentHostHeader) throws InvalidAttributeException {
    logger.debug(LOG_ENTRY, "buildFragmentHost_", new Object[]{fragmentHostHeader});
    if(fragmentHostHeader == null) { 
      
      return null;
    }
    Map<String, NameValueMap<String, String>> parsedFragHost = ManifestHeaderProcessor.parseImportString(fragmentHostHeader);
    if(parsedFragHost.size() != 1)
      throw new InvalidAttributeException(MessageUtil.getMessage("APPUTILS0001W",
          new Object[] {fragmentHostHeader}, 
          "An internal error occurred. A bundle fragment manifest must define exactly one Fragment-Host entry. The following entry was found" + fragmentHostHeader + "."));
    
    String hostName = parsedFragHost.keySet().iterator().next();
    Map<String, String> attribs = parsedFragHost.get(hostName);
    
    String bundleVersion = attribs.remove(BUNDLE_VERSION_ATTRIBUTE);
    if (bundleVersion != null && attribs.get(VERSION_ATTRIBUTE) == null) { 
      attribs.put (VERSION_ATTRIBUTE, bundleVersion);
    }
    attribs.put(ModellingConstants.OBR_SYMBOLIC_NAME, hostName);  
    
    String filter = ManifestHeaderProcessor.generateFilter(attribs);
    
    ImportedBundle result = new ImportedBundleImpl(filter, attribs);
    logger.debug(LOG_EXIT, "buildFragmentHost_", result);
    return result;
  }
  
  public static ImportedPackage intersectPackage_ (ImportedPackage p1, ImportedPackage p2) { 
    
    logger.debug(LOG_ENTRY, "intersectPackage_", new Object[]{p1, p2});
    ImportedPackage result = null;
    if (p1.getPackageName().equals(p2.getPackageName()))
    {
      Map<String,String> att1 = new HashMap<String, String>(p1.getAttributes());
      Map<String,String> att2 = new HashMap<String, String>(p2.getAttributes());
      
      // Get the versions, we remove them so that the remaining attributes can be matched.
      String rangeStr1 = att1.remove(Constants.VERSION_ATTRIBUTE);
      String rangeStr2 = att2.remove(Constants.VERSION_ATTRIBUTE);

      //Also remove the optional directive as we don't care about that either
      att1.remove(OPTIONAL_KEY);
      att2.remove(OPTIONAL_KEY);
      
      //If identical take either, otherwise null!
      Map<String, String> mergedAttribs = (att1.equals(att2) ? att1 : null);
      if (mergedAttribs == null)
      {
        // Cannot intersect requirements if attributes are not identical.
        result = null;
      }
      else
      {
        boolean isIntersectSuccessful = true;
        
        if (rangeStr1 != null && rangeStr2 != null)
        {
          // Both requirements have a version constraint so check for an intersection between them.
          VersionRange range1 = ManifestHeaderProcessor.parseVersionRange(rangeStr1);
          VersionRange range2 = ManifestHeaderProcessor.parseVersionRange(rangeStr2);
          VersionRange intersectRange = range1.intersect(range2);
          
          if (intersectRange == null)
          {
            // No intersection possible.
            isIntersectSuccessful = false;
          }
          else
          {
            // Use the intersected version range.
            mergedAttribs.put(Constants.VERSION_ATTRIBUTE, intersectRange.toString());
          }
        }
        else if (rangeStr1 != null)
        {
          mergedAttribs.put(Constants.VERSION_ATTRIBUTE, rangeStr1);
        }
        else if (rangeStr2 != null)
        {
          mergedAttribs.put(Constants.VERSION_ATTRIBUTE, rangeStr2);
        }
        
        //If both optional, we are optional, otherwise use the default
        if(p1.isOptional() && p2.isOptional()) 
        {
          mergedAttribs.put(OPTIONAL_KEY, Constants.RESOLUTION_OPTIONAL);
        } 
        
        try { 
          result = (isIntersectSuccessful ? new ImportedPackageImpl(p1.getPackageName(), mergedAttribs) : null);
        } catch (InvalidAttributeException iax) { 
          logger.error(iax.getMessage());
        }
      }
    } 
    logger.debug(LOG_EXIT, "intersectPackage_", result);
    return result;
  }
  
}
