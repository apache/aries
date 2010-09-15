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

import java.util.Collection;
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
import org.apache.aries.application.modelling.ModellingManager;
import org.apache.aries.application.modelling.ParsedServiceElements;

public class ModellingManagerImpl implements ModellingManager
{

  /* (non-Javadoc)
   * @see org.apache.aries.application.modelling.ModellingManager#getExportedBundle(java.util.Map, org.apache.aries.application.modelling.ImportedBundle)
   */
  public ExportedBundle getExportedBundle(Map<String, String> attributes, ImportedBundle fragHost) {

    return new ExportedBundleImpl(attributes, fragHost);
  }
  /* (non-Javadoc)
   * @see org.apache.aries.application.modelling.ModellingManager#getExportedPackage(org.apache.aries.application.modelling.ModelledResource, java.lang.String, java.util.Map)
   */
  public ExportedPackage getExportedPackage(ModelledResource mr, String pkg, Map<String, Object> attributes)  {

    return new ExportedPackageImpl(mr, pkg, attributes);
  }
  /* (non-Javadoc)
   * @see org.apache.aries.application.modelling.ModellingManager#getExportedService(java.lang.String, int, java.util.Collection, java.util.Map)
   */
  public ExportedService getExportedService(String name, int ranking, Collection<String> ifaces, 
      Map<String, Object> serviceProperties ) {
    return new ExportedServiceImpl (name, ranking, ifaces, serviceProperties );
  }
  /* (non-Javadoc)
   * @see org.apache.aries.application.modelling.ModellingManager#getExportedService(java.lang.String, java.util.Map)
   */
  @SuppressWarnings("deprecation")
  public ExportedService getExportedService(String ifaceName, Map<String, String> attrs) {
    return new ExportedServiceImpl (ifaceName, attrs );
  }
  
  /* (non-Javadoc)
   * @see org.apache.aries.application.modelling.ModellingManager#getImportedBundle(java.lang.String, java.util.Map)
   */
  public ImportedBundle getImportedBundle(String filterString, Map<String, String> attributes) throws InvalidAttributeException {
    return new ImportedBundleImpl(filterString, attributes);
  }
  
  /* (non-Javadoc)
   * @see org.apache.aries.application.modelling.ModellingManager#getImportedBundle(java.lang.String, java.lang.String)
   */
  public ImportedBundle getImportedBundle(String bundleName, String versionRange) throws InvalidAttributeException {
    return new ImportedBundleImpl(bundleName, versionRange);
  }
  
  /* (non-Javadoc)
   * @see org.apache.aries.application.modelling.ModellingManager#getImportedPackage(java.lang.String, java.util.Map)
   */
  public ImportedPackage getImportedPackage(String pkg, Map<String, String> attributes) throws InvalidAttributeException{
    return new ImportedPackageImpl(pkg, attributes);
  }
  
  /* (non-Javadoc)
   * @see org.apache.aries.application.modelling.ModellingManager#getImportedService(boolean, java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean)
   */
  public ImportedService getImportedService(boolean optional, String iface, String componentName, 
      String blueprintFilter, String id, boolean isMultiple) throws InvalidAttributeException{
    return new ImportedServiceImpl(optional, iface, componentName, blueprintFilter, id, isMultiple);
  }
  /* (non-Javadoc)
   * @see org.apache.aries.application.modelling.ModellingManager#getImportedService(java.lang.String, java.util.Map)
   */
  @SuppressWarnings("deprecation")
  public ImportedService getImportedService(String ifaceName, Map<String, String> attributes) throws InvalidAttributeException{
    return new ImportedServiceImpl(ifaceName, attributes);
  }
  
  /* (non-Javadoc)
   * @see org.apache.aries.application.modelling.ModellingManager#getModelledResource(java.lang.String, org.apache.aries.application.management.BundleInfo, java.util.Collection, java.util.Collection)
   */
  public ModelledResource getModelledResource(String fileURI, BundleInfo bundleInfo, 
      Collection<ImportedService> importedServices, 
      Collection<ExportedService> exportedServices) throws InvalidAttributeException {
    return new ModelledResourceImpl(fileURI, bundleInfo, importedServices, exportedServices);
    
  }
  
  /* (non-Javadoc)
   * @see org.apache.aries.application.modelling.ModellingManager#getModelledResource(java.lang.String, java.util.jar.Attributes, java.util.Collection, java.util.Collection)
   */
  public ModelledResource getModelledResource(String fileURI, Attributes bundleAttributes, 
      Collection<ImportedService> importedServices, 
      Collection<ExportedService> exportedServices) throws InvalidAttributeException {
    return new ModelledResourceImpl(fileURI, bundleAttributes, importedServices, exportedServices);
    
  }
  
  /* (non-Javadoc)
   * @see org.apache.aries.application.modelling.ModellingManager#getParsedServiceElements(java.util.Collection, java.util.Collection)
   */
  public ParsedServiceElements getParsedServiceElements ( Collection<ExportedService> services, 
      Collection<ImportedService> references) {
    return new ParsedServiceElementsImpl(services, references);
  }
}
