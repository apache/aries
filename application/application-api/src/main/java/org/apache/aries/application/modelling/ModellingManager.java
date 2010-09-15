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

package org.apache.aries.application.modelling;

import java.util.Collection;
import java.util.Map;
import java.util.jar.Attributes;

import org.apache.aries.application.InvalidAttributeException;
import org.apache.aries.application.management.BundleInfo;

public interface ModellingManager {

  ExportedBundle getExportedBundle(Map<String, String> attributes,
      ImportedBundle fragHost);

  ExportedPackage getExportedPackage(ModelledResource mr, String pkg,
      Map<String, Object> attributes);

  ExportedService getExportedService(String name, int ranking,
      Collection<String> ifaces, Map<String, Object> serviceProperties);

  ExportedService getExportedService(String ifaceName, Map<String, String> attrs);

  ImportedBundle getImportedBundle(String filterString,
      Map<String, String> attributes) throws InvalidAttributeException;

  ImportedBundle getImportedBundle(String bundleName, String versionRange)
      throws InvalidAttributeException;

  ImportedPackage getImportedPackage(String pkg, Map<String, String> attributes)
      throws InvalidAttributeException;

  ImportedService getImportedService(boolean optional, String iface,
      String componentName, String blueprintFilter, String id,
      boolean isMultiple) throws InvalidAttributeException;

  ImportedService getImportedService(String ifaceName,
      Map<String, String> attributes) throws InvalidAttributeException;

  ModelledResource getModelledResource(String fileURI, BundleInfo bundleInfo,
      Collection<ImportedService> importedServices,
      Collection<ExportedService> exportedServices)
      throws InvalidAttributeException;

  ModelledResource getModelledResource(String fileURI,
      Attributes bundleAttributes,
      Collection<ImportedService> importedServices,
      Collection<ExportedService> exportedServices)
      throws InvalidAttributeException;

  ParsedServiceElements getParsedServiceElements(
      Collection<ExportedService> services,
      Collection<ImportedService> references);

}