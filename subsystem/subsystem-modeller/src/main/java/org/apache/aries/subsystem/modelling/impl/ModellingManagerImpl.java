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

import java.util.Collection;
import java.util.Map;
import java.util.jar.Attributes;

import org.apache.aries.subsystem.modelling.ExportedService;
import org.apache.aries.subsystem.modelling.ImportedService;
import org.apache.aries.subsystem.modelling.InvalidAttributeException;
import org.apache.aries.subsystem.modelling.ModelledResource;
import org.apache.aries.subsystem.modelling.ModellingManager;
import org.apache.aries.subsystem.modelling.ParsedServiceElements;

public class ModellingManagerImpl implements ModellingManager
{

  public ExportedService getExportedService(String name, int ranking, Collection<String> ifaces,
      Map<String, Object> serviceProperties ) {
    return new ExportedServiceImpl (name, ranking, ifaces, serviceProperties );
  }

  public ImportedService getImportedService(boolean optional, String iface, String componentName,
      String blueprintFilter, String id, boolean isMultiple) throws InvalidAttributeException{
    return new ImportedServiceImpl(optional, iface, componentName, blueprintFilter, id, isMultiple);
  }

  public ModelledResource getModelledResource(String fileURI, Attributes bundleAttributes,
      Collection<ImportedService> importedServices,
      Collection<ExportedService> exportedServices) throws InvalidAttributeException {
    return new ModelledResourceImpl(fileURI, bundleAttributes, importedServices, exportedServices);

  }

  public ParsedServiceElements getParsedServiceElements ( Collection<ExportedService> services,
      Collection<ImportedService> references) {
    return new ParsedServiceElementsImpl(services, references);
  }
}
