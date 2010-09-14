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
import static org.osgi.framework.Constants.BUNDLE_VERSION_ATTRIBUTE;
import static org.osgi.framework.Constants.DYNAMICIMPORT_PACKAGE;
import static org.osgi.framework.Constants.EXPORT_PACKAGE;
import static org.osgi.framework.Constants.EXPORT_SERVICE;
import static org.osgi.framework.Constants.IMPORT_PACKAGE;
import static org.osgi.framework.Constants.IMPORT_SERVICE;
import static org.osgi.framework.Constants.REQUIRE_BUNDLE;
import static org.osgi.framework.Constants.RESOLUTION_DIRECTIVE;
import static org.osgi.framework.Constants.RESOLUTION_OPTIONAL;
import static org.osgi.framework.Constants.VERSION_ATTRIBUTE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;

import org.apache.aries.application.InvalidAttributeException;
import org.apache.aries.application.management.BundleInfo;
import org.apache.aries.application.modelling.ExportedBundle;
import org.apache.aries.application.modelling.ExportedPackage;
import org.apache.aries.application.modelling.ExportedService;
import org.apache.aries.application.modelling.ImportedBundle;
import org.apache.aries.application.modelling.ImportedPackage;
import org.apache.aries.application.modelling.ImportedService;
import org.apache.aries.application.modelling.ModelledResource;
import org.apache.aries.application.modelling.ResourceType;
import org.apache.aries.application.modelling.utils.ModellingConstants;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor.NameValueMap;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor.NameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A model of a bundle or composite. Used for example to supply information to 
 * RepositoryGenerator.generateRepository()
 *
 */
public class ModelledResourceImpl implements ModelledResource
{
  private final Logger logger = LoggerFactory.getLogger(ModelledResourceImpl.class);
  private final String _fileURI;
  private final Collection<ImportedService> _importedServices;
  private final Collection<ExportedService> _exportedServices;
  private final Collection<ExportedPackage> _exportedPackages;
  private final Collection<ImportedPackage> _importedPackages;
  private final Collection<ImportedBundle> _requiredBundles;
  private final ExportedBundle _exportedBundle;
  private final ResourceType _resourceType;
  
  /**
   * Construct a new {@link ModelledResourceImpl} for the following manifest and services
   * @param fileURI The location of the bundle, may be null, which indicates a by value bundle
   * @param bundleInfo The bundle info object
   * @param importedServices The blueprint references defined by the bundle. May be null
   * @param exportedServices The blueprint services exported by the bundle. May be null
   * @throws InvalidAttributeException
   */
  public ModelledResourceImpl (String fileURI, BundleInfo bundleInfo, 
      Collection<ImportedService> importedServices, 
      Collection<ExportedService> exportedServices) throws InvalidAttributeException
  {
    this(fileURI, bundleInfo.getRawAttributes(), importedServices, exportedServices);
  }
  /**
   * Construct a new {@link ModelledResourceImpl} for the following manifest and services
   * @param fileURI The location of the bundle, may be null, which indicates a by value bundle
   * @param bundleAttributes The bundle manifest, must not be null
   * @param importedServices The blueprint references defined by the bundle. May be null
   * @param exportedServices The blueprint services exported by the bundle. May be null
   * @throws InvalidAttributeException
   */
  @SuppressWarnings("deprecation")
  public ModelledResourceImpl (String fileURI, Attributes bundleAttributes, 
      Collection<ImportedService> importedServices, 
      Collection<ExportedService> exportedServices) throws InvalidAttributeException
  { 
    logger.debug(LOG_ENTRY, "ModelledResourceImpl", new Object[]{fileURI, bundleAttributes, importedServices, exportedServices});
    _fileURI = fileURI;
    if(importedServices != null)
      _importedServices = new ArrayList<ImportedService> (importedServices);
    else
      _importedServices = new ArrayList<ImportedService>();
    
    if(exportedServices != null)
      _exportedServices = new ArrayList<ExportedService> (exportedServices);
    else
      _exportedServices = new ArrayList<ExportedService>();
    
    
      _resourceType = ResourceType.BUNDLE;
      _exportedBundle = new ExportedBundleImpl (bundleAttributes);
  

    
    _exportedPackages = new ArrayList<ExportedPackage>();
    String packageExports = bundleAttributes.getValue(EXPORT_PACKAGE);
    if (packageExports != null) {
      List<NameValuePair<String, NameValueMap<String, String>>> exportedPackages = ManifestHeaderProcessor
        .parseExportString(packageExports);
      for (NameValuePair<String, NameValueMap<String, String>> exportedPackage : exportedPackages) {
        _exportedPackages.add(new ExportedPackageImpl(this, exportedPackage.getName(), 
            new HashMap<String, Object>(exportedPackage.getValue())));
    
      }
    }

    _importedPackages = new ArrayList<ImportedPackage>();
    String packageImports = bundleAttributes.getValue(IMPORT_PACKAGE);
    if (packageImports != null) {
      Map<String, NameValueMap<String, String>> importedPackages = ManifestHeaderProcessor
          .parseImportString(packageImports);
      for (Map.Entry<String, NameValueMap<String, String>> importedPackage : importedPackages.entrySet()) {
        Map<String, String> atts = importedPackage.getValue();
        _importedPackages.add(new ImportedPackageImpl(importedPackage.getKey(), atts));
      }
    }
    
    // Use of Import-Service and Export-Service is deprecated in OSGi. We like Blueprint. 
    // Blueprint is good. 
    
    String serviceExports = null; 
    if (_resourceType == ResourceType.BUNDLE) { 
      serviceExports = bundleAttributes.getValue(EXPORT_SERVICE);
    } 
    if (serviceExports != null) {
      List<NameValuePair<String, NameValueMap<String, String>>> expServices = ManifestHeaderProcessor
          .parseExportString(serviceExports);
      for (NameValuePair<String, NameValueMap<String, String>> exportedService : expServices) {
        _exportedServices.add(new ExportedServiceImpl(exportedService.getName(), exportedService.getValue()));
      }
    }

    String serviceImports =null;
    if (_resourceType == ResourceType.BUNDLE) { 
      serviceImports = bundleAttributes.getValue(IMPORT_SERVICE);
    } 
    if (serviceImports != null) {
      Map<String, NameValueMap<String, String>> svcImports = ManifestHeaderProcessor
          .parseImportString(serviceImports);
      for (Map.Entry<String, NameValueMap<String, String>> importedService : svcImports.entrySet()) {
        _importedServices.add(new ImportedServiceImpl(importedService.getKey(), importedService.getValue()));
      }
    }
    
    _requiredBundles = new ArrayList<ImportedBundle>();
    // Require-Bundle and DynamicImport-Package relevant to Bundles but not Composites
    if (_resourceType == ResourceType.BUNDLE) { 
      String requireBundleHeader = bundleAttributes.getValue(REQUIRE_BUNDLE);
      if (requireBundleHeader != null) {
        Map<String, NameValueMap<String, String>> requiredBundles = ManifestHeaderProcessor
            .parseImportString(requireBundleHeader);
        for (Map.Entry<String, NameValueMap<String, String>> bundle : requiredBundles.entrySet()) {
          String type = bundle.getKey();
          Map<String, String> attribs = bundle.getValue();
          // We may parse a manifest with a header like Require-Bundle: bundle.a;bundle-version=3.0.0
          // The filter that we generate is intended for OBR in which case we need (version>=3.0.0) and not (bundle-version>=3.0.0)
          String bundleVersion = attribs.remove(BUNDLE_VERSION_ATTRIBUTE);
          if (bundleVersion != null && attribs.get(VERSION_ATTRIBUTE) == null) { 
            attribs.put (VERSION_ATTRIBUTE, bundleVersion);
          }
          String filter = ManifestHeaderProcessor.generateFilter(ModellingConstants.OBR_SYMBOLIC_NAME, type, attribs);
          Map<String, String> atts = new HashMap<String, String>(bundle.getValue());
          atts.put(ModellingConstants.OBR_SYMBOLIC_NAME,  bundle.getKey());
          _requiredBundles.add(new ImportedBundleImpl(filter, atts));
        }
      }
    
      String dynamicImports = bundleAttributes.getValue(DYNAMICIMPORT_PACKAGE);
      if (dynamicImports != null) {
        Map<String, NameValueMap<String, String>> dynamicImportPackages = ManifestHeaderProcessor
            .parseImportString(dynamicImports);
        for (Map.Entry<String, NameValueMap<String, String>> dynImportPkg : dynamicImportPackages.entrySet()) {
          if (dynImportPkg.getKey().indexOf("*") == -1) {
            dynImportPkg.getValue().put(RESOLUTION_DIRECTIVE + ":", RESOLUTION_OPTIONAL);
            _importedPackages.add(new ImportedPackageImpl(dynImportPkg.getKey(), dynImportPkg.getValue()));
          }
        }
      }
    }

    logger.debug(LOG_EXIT, "ModelledResourceImpl");
  }


  public String getLocation() {
    logger.debug(LOG_ENTRY, "getLocation");
    logger.debug(LOG_EXIT, "getLocation", _fileURI);
    return _fileURI;
  }


  public ExportedBundle getExportedBundle() {
    logger.debug(LOG_ENTRY, "getExportedBundle");
    logger.debug(LOG_EXIT, "getExportedBundle",  _exportedBundle);
    return _exportedBundle;
  }


  public Collection<ExportedPackage> getExportedPackages() {
    logger.debug(LOG_ENTRY, "getExportedPackages");
    logger.debug(LOG_EXIT, "getExportedPackages",  _exportedPackages);
    return Collections.unmodifiableCollection(_exportedPackages);
  }
  


  public Collection<ImportedPackage> getImportedPackages() {
    logger.debug(LOG_ENTRY, "getImportedPackages");
    logger.debug(LOG_EXIT, "getImportedPackages",  _importedPackages);
    return Collections.unmodifiableCollection(_importedPackages);
  }

  public Collection<ExportedService> getExportedServices() {
    logger.debug(LOG_ENTRY, "getExportedServices");
    logger.debug(LOG_EXIT, "getExportedServices",  _exportedServices);
    return Collections.unmodifiableCollection(_exportedServices);
  }



  public Collection<ImportedService> getImportedServices() {
    logger.debug(LOG_ENTRY, "getImportedServices");
    logger.debug(LOG_EXIT, "getImportedServices",  _exportedServices);
    return Collections.unmodifiableCollection(_importedServices);
  }


  public String getSymbolicName() {
    logger.debug(LOG_ENTRY, "getSymbolicName");
    String result = _exportedBundle.getSymbolicName();
    logger.debug(LOG_EXIT, "getSymbolicName",  result);
    return result;
  }
  

  public String getVersion() {
    logger.debug(LOG_ENTRY, "getVersion");
    String result = _exportedBundle.getVersion();
    logger.debug(LOG_EXIT, "getVersion",  result);
    return result;
  }


  public String toDeploymentString() {
    logger.debug(LOG_ENTRY, "toDeploymentString");
    String result = _exportedBundle.toDeploymentString();
    logger.debug(LOG_EXIT, "toDeploymentString",  result);
    return result;
  }


  public ResourceType getType() {
    logger.debug(LOG_ENTRY, "getType");
    logger.debug(LOG_EXIT, "getType",  ResourceType.BUNDLE);
    return _resourceType;
  }
  
  @Override
  public String toString() {
    return toDeploymentString();
  }


  public Collection<ImportedBundle> getRequiredBundles() {
    logger.debug(LOG_ENTRY, "getRequiredBundles");
    logger.debug(LOG_EXIT, "getRequiredBundles",  _requiredBundles);
    return Collections.unmodifiableCollection(_requiredBundles);
  }


  public ImportedBundle getFragmentHost() {
    logger.debug(LOG_ENTRY, "getFragmentHost");
    ImportedBundle result = _exportedBundle.getFragmentHost();
    logger.debug(LOG_EXIT, "getFragmentHost",  result);
    return result;
  }


  public boolean isFragment() {
    logger.debug(LOG_ENTRY, "isFragment");
    boolean result = _exportedBundle.isFragment();
    logger.debug(LOG_EXIT, "isFragment",  result);
    return result;
  }
}
