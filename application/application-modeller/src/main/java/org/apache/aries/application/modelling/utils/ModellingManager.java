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
package org.apache.aries.application.modelling.utils;

import java.util.Collection;
import java.util.Map;
import java.util.jar.Attributes;

import org.apache.aries.application.management.BundleInfo;
import org.apache.aries.application.management.InvalidAttributeException;
import org.apache.aries.application.modelling.ExportedBundle;
import org.apache.aries.application.modelling.ExportedPackage;
import org.apache.aries.application.modelling.ExportedService;
import org.apache.aries.application.modelling.ImportedBundle;
import org.apache.aries.application.modelling.ImportedPackage;
import org.apache.aries.application.modelling.ImportedService;
import org.apache.aries.application.modelling.ModelledResource;
import org.apache.aries.application.modelling.ParsedServiceElements;
import org.apache.aries.application.modelling.impl.ExportedBundleImpl;
import org.apache.aries.application.modelling.impl.ExportedPackageImpl;
import org.apache.aries.application.modelling.impl.ExportedServiceImpl;
import org.apache.aries.application.modelling.impl.ImportedBundleImpl;
import org.apache.aries.application.modelling.impl.ImportedPackageImpl;
import org.apache.aries.application.modelling.impl.ImportedServiceImpl;
import org.apache.aries.application.modelling.impl.ModelledResourceImpl;
import org.apache.aries.application.modelling.impl.ParsedServiceElementsImpl;

public class ModellingManager
{

  public  static ExportedBundle getExportedBundle(Map<String, String> attributes, ImportedBundle fragHost) {

    return new ExportedBundleImpl(attributes, fragHost);
  }
  public  static ExportedPackage getExportedPackage(ModelledResource mr, String pkg, Map<String, Object> attributes)  {

    return new ExportedPackageImpl(mr, pkg, attributes);
  }
  public static  ExportedService getExportedService(String name, int ranking, Collection<String> ifaces, 
      Map<String, Object> serviceProperties ) {
    return new ExportedServiceImpl (name, ranking, ifaces, serviceProperties );
  }
  @SuppressWarnings("deprecation")
  public static ExportedService getExportedService(String ifaceName, Map<String, String> attrs) {
    return new ExportedServiceImpl (ifaceName, attrs );
  }
  
  public static ImportedBundle getImportedBundle(String filterString, Map<String, String> attributes) throws InvalidAttributeException {
    return new ImportedBundleImpl(filterString, attributes);
  }
  
  public static ImportedBundle getImportedBundle(String bundleName, String versionRange) throws InvalidAttributeException {
    return new ImportedBundleImpl(bundleName, versionRange);
  }
  
  public static ImportedPackage getImportedPackage(String pkg, Map<String, String> attributes) throws InvalidAttributeException{
    return new ImportedPackageImpl(pkg, attributes);
  }
  
  public static ImportedService getImportedService(boolean optional, String iface, String componentName, 
      String blueprintFilter, String id, boolean isMultiple) throws InvalidAttributeException{
    return new ImportedServiceImpl(optional, iface, componentName, blueprintFilter, id, isMultiple);
  }
  @SuppressWarnings("deprecation")
  public static ImportedService getImportedService(String ifaceName, Map<String, String> attributes) throws InvalidAttributeException{
    return new ImportedServiceImpl(ifaceName, attributes);
  }
  
  public static ModelledResource getModelledResource(String fileURI, BundleInfo bundleInfo, 
      Collection<ImportedService> importedServices, 
      Collection<ExportedService> exportedServices) throws InvalidAttributeException {
    return new ModelledResourceImpl(fileURI, bundleInfo, importedServices, exportedServices);
    
  }
  
  public static ModelledResource getModelledResource(String fileURI, Attributes bundleAttributes, 
      Collection<ImportedService> importedServices, 
      Collection<ExportedService> exportedServices) throws InvalidAttributeException {
    return new ModelledResourceImpl(fileURI, bundleAttributes, importedServices, exportedServices);
    
  }
  
  public static ParsedServiceElements getParsedServiceElements ( Collection<ExportedService> services, 
      Collection<ImportedService> references) {
    return new ParsedServiceElementsImpl(services, references);
  }
}
