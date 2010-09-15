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
package org.apache.aries.application.resolver.obr.impl;

import static org.apache.aries.application.utils.AppConstants.LOG_ENTRY;
import static org.apache.aries.application.utils.AppConstants.LOG_EXIT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.aries.application.InvalidAttributeException;
import org.apache.aries.application.modelling.ExportedBundle;
import org.apache.aries.application.modelling.ExportedPackage;
import org.apache.aries.application.modelling.ExportedService;
import org.apache.aries.application.modelling.ImportedBundle;
import org.apache.aries.application.modelling.ImportedPackage;
import org.apache.aries.application.modelling.ImportedService;
import org.apache.aries.application.modelling.ModelledResource;
import org.apache.aries.application.modelling.ModellingManager;
import org.apache.aries.application.modelling.ResourceType;
import org.apache.aries.application.modelling.utils.ModellingHelper;
import org.apache.aries.application.resolver.internal.MessageUtil;
import org.apache.aries.application.utils.AppConstants;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor;
import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Property;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resource;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelledBundleResource implements ModelledResource {  
  private final Resource resource;
  private final ExportedBundle exportedBundle;
  private final Collection<ImportedPackage> packageRequirements;
  private final Collection<ImportedService> serviceRequirements;
  private final Collection<ExportedPackage> packageCapabilities;
  private final Collection<ExportedService> serviceCapabilties;
  private final Collection<ImportedBundle> bundleRequirements;
  private final ResourceType type;
  private final ModellingManager modellingManager;
  private final ModellingHelper modellingHelper;
  private final Logger logger = LoggerFactory.getLogger(ModelledBundleResource.class);

  public ModelledBundleResource (Resource r, ModellingManager mm, ModellingHelper mh) throws InvalidAttributeException { 

    logger.debug(LOG_ENTRY, "ModelledBundleResource", new Object[]{r, mm, mh});
    resource = r;
    modellingManager = mm;
    modellingHelper = mh;
    List<ExportedBundle> exportedBundles = new ArrayList<ExportedBundle>();
    ResourceType thisResourceType = ResourceType.BUNDLE;

    // We'll iterate through our Capabilities a second time below. We do this since we later
    // build an ExportedPackageImpl for which 'this' is the ModelledResource. 
    for (Capability cap : r.getCapabilities()) { 
      String capName = cap.getName();
      if (capName.equals(ResourceType.BUNDLE.toString())) { 
        @SuppressWarnings("unchecked")
        Property[] props = cap.getProperties();

        Map<String,String> sanitizedMap = new HashMap<String, String>();
        for(Property entry : props) {
          sanitizedMap.put(entry.getName(), entry.getValue());
        }
        exportedBundles.add (modellingManager.getExportedBundle(sanitizedMap, modellingHelper.buildFragmentHost(
            sanitizedMap.get(Constants.FRAGMENT_HOST))));
      } else if (cap.getName().equals(ResourceType.COMPOSITE.toString())) { 
        thisResourceType = ResourceType.COMPOSITE;
      }
    }
    type = thisResourceType;


    if (exportedBundles.size() == 0) {
      throw new InvalidAttributeException(MessageUtil.getMessage("NO_EXPORTED_BUNDLE", new Object[0]));
    } else if (exportedBundles.size() == 1) { 
      exportedBundle = exportedBundles.get(0);
    } else {  
      throw new InvalidAttributeException(MessageUtil.getMessage("TOO_MANY_EXPORTED_BUNDLES",
          new Object[0]));
    }   

    packageCapabilities = new HashSet<ExportedPackage>();
    packageRequirements = new HashSet<ImportedPackage>();
    serviceCapabilties = new HashSet<ExportedService>();
    serviceRequirements = new HashSet<ImportedService>();
    bundleRequirements = new HashSet<ImportedBundle>();

    for (Requirement requirement : r.getRequirements())
    {
      String reqName = requirement.getName();
      // Build ImportedPackageImpl, ImportedServiceImpl objects from the Resource's requirments. 
      // Parse the Requirement's filter and remove from the parsed Map all the entries
      // that we will pass in as explicit parameters to the ImportedServiceImpl or ImportedPackageImpl
      // constructor. 
      // (This does mean that we remove 'package=package.name' entries but not 'service=service' - 
      // the latter is not very useful :)
      if (ResourceType.PACKAGE.toString().equals(reqName))
      {
        Map<String, String> filter = ManifestHeaderProcessor.parseFilter(requirement.getFilter());
        // Grab and remove the package name, leaving only additional attributes.
        String name = filter.remove(ResourceType.PACKAGE.toString());
        if (requirement.isOptional()) { 
          filter.put(Constants.RESOLUTION_DIRECTIVE + ":", Constants.RESOLUTION_OPTIONAL);
        }
        ImportedPackage info = modellingManager.getImportedPackage(name, filter);
        packageRequirements.add(info);
      } else if (ResourceType.SERVICE.toString().equals(requirement.getName())) {
        boolean optional = requirement.isOptional();
        String iface;
        String componentName;
        String blueprintFilter;
        String id = null;
        boolean isMultiple = requirement.isMultiple();

        /* It would be much better to pull these keys out of ImportedServiceImpl, 
         * or even values via static methods
         */
        Map<String, String> attrs = ManifestHeaderProcessor.parseFilter(requirement.getFilter());
        iface = attrs.get(Constants.OBJECTCLASS);
        componentName = attrs.get ("osgi.service.blueprint.compname");
        blueprintFilter = requirement.getFilter();

        ImportedService svc = modellingManager.getImportedService(optional, iface, componentName,
            blueprintFilter, id, isMultiple);
        serviceRequirements.add(svc);
      } else if (ResourceType.BUNDLE.toString().equals(requirement.getName())) {
        String filter =requirement.getFilter();
        Map<String,String> atts = ManifestHeaderProcessor.parseFilter(filter);
        if (requirement.isOptional()) { 
          atts.put(Constants.RESOLUTION_DIRECTIVE + ":", Constants.RESOLUTION_OPTIONAL);
        }
        bundleRequirements.add(modellingManager.getImportedBundle(filter, atts));
      }
    }

    for (Capability capability : r.getCapabilities())
    {
      Map<String, Object> props = new HashMap<String, Object>();
      Property[] properties = capability.getProperties();
      for (Property prop : properties) {
        props.put(prop.getName(), prop.getValue());
      }
      if (ResourceType.PACKAGE.toString().equals(capability.getName())) 
      {
        // Grab and remove the package name, leaving only additional attributes.
        Object pkg = props.remove(ResourceType.PACKAGE.toString());
        // bundle symbolic name and version will be in additionalProps, so do not 
        // need to be passed in separately. 
        ExportedPackage info = modellingManager.getExportedPackage(this, pkg.toString(), props);
        packageCapabilities.add(info);
      } else if (ResourceType.SERVICE.toString().equals(capability.getName())) { 
        String name = null;   // we've lost this irretrievably
        int ranking = 0;
        Collection<String> ifaces;
        String rankingText = (String) props.remove(Constants.SERVICE_RANKING);
        if (rankingText != null) ranking = Integer.parseInt(rankingText);
        // objectClass may come out as a String or String[]
        Object rawObjectClass = props.remove (Constants.OBJECTCLASS);
        if (rawObjectClass.getClass().isArray()) { 
          ifaces = Arrays.asList((String[])rawObjectClass);
        } else { 
          ifaces = Arrays.asList((String)rawObjectClass);
        }

        ExportedService svc = modellingManager.getExportedService(name, ranking, ifaces, props);
        serviceCapabilties.add(svc);

      }
    }
    logger.debug(LOG_EXIT, "ModelledBundleResource");

  }


  public ExportedBundle getExportedBundle() {
    logger.debug(LOG_ENTRY, "AbstractExportedBundle");
    logger.debug(LOG_EXIT, "AbstractExportedBundle",exportedBundle );
    return exportedBundle;
  }


  public Collection<? extends ExportedPackage> getExportedPackages() {
    logger.debug(LOG_ENTRY, "getExportedPackages");
    logger.debug(LOG_EXIT,  "getExportedPackages", packageCapabilities );
    return Collections.unmodifiableCollection(packageCapabilities);
  }


  public Collection<? extends ExportedService> getExportedServices() {
    logger.debug(LOG_ENTRY, "getExportedServices");
    logger.debug(LOG_EXIT,  "getExportedServices", serviceCapabilties );

    return Collections.unmodifiableCollection(serviceCapabilties);
  }


  public Collection<? extends ImportedPackage> getImportedPackages() {
    logger.debug(LOG_ENTRY, "getImportedPackages");
    logger.debug(LOG_EXIT,  "getImportedPackages", packageRequirements );
    return Collections.unmodifiableCollection(packageRequirements);
  }


  public Collection<? extends ImportedService> getImportedServices() {
    logger.debug(LOG_ENTRY, "getImportedServices");
    logger.debug(LOG_EXIT,  "getImportedServices", serviceRequirements );    
    return Collections.unmodifiableCollection(serviceRequirements);
  }


  public Collection<? extends ImportedBundle> getRequiredBundles() {
    logger.debug(LOG_ENTRY, "getRequiredBundles");
    logger.debug(LOG_EXIT,  "getRequiredBundles", bundleRequirements );    
    return Collections.unmodifiableCollection(bundleRequirements);
  }


  public String getSymbolicName() {
    logger.debug(LOG_ENTRY, "getSymbolicName");   
    String result = resource.getSymbolicName();
    logger.debug(LOG_EXIT,  "getSymbolicName", result );

    return result;
  }


  public String getLocation() {
    logger.debug(LOG_ENTRY, "getLocation");   
    logger.debug(LOG_EXIT,  "getLocation", resource.getURI());
    return resource.getURI();

  }


  public String getVersion() {
    logger.debug(LOG_ENTRY, "getVersion");
    String result = resource.getVersion().toString();
    logger.debug(LOG_EXIT,  "getVersion", result);
    return result;
  }


  public String toDeploymentString() {
    logger.debug(LOG_ENTRY, "toDeploymentString");   
    String result = getSymbolicName() + ";" + AppConstants.DEPLOYMENT_BUNDLE_VERSION + "=" + getVersion();
    logger.debug(LOG_EXIT,  "toDeploymentString", result);
    return result;
  }


  public String toString() { 
    logger.debug(LOG_ENTRY, "toString");
    String result = toDeploymentString() + " uri=" + getLocation();
    logger.debug(LOG_EXIT,  "toString", result);
    return result;
  }


  public ResourceType getType() {
    logger.debug(LOG_ENTRY, "getType");   
    logger.debug(LOG_EXIT,  "getType", type);
    return type;
  }


  public ImportedBundle getFragmentHost() {
    logger.debug(LOG_ENTRY, "getFragmentHost");
    ImportedBundle result = exportedBundle.getFragmentHost();
    logger.debug(LOG_EXIT,  "getFragmentHost", result);
    return result;
  }


  public boolean isFragment() {
    logger.debug(LOG_ENTRY, "isFragment");
    boolean result = exportedBundle.isFragment();
    logger.debug(LOG_EXIT,  "isFragment", result);
    return result;
  }


}