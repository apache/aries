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
/**
 * Model a bundle resource.
 *
 */
public interface ModelledResource extends DeploymentMFElement
{
  
  /**
   * The resource's symbolic name
   * @return The resource symbolic name
   */
  String getSymbolicName();

  /**
   * The resource version
   * @return The resource version
   */
  String getVersion();

  /** Returns a String which can be turned into a URL to the bundle binary 
   * @return the location of the bundle
   */
  public String getLocation();

  /**
   * Import-Package header modelled to be a collection of the imported packages objects.
   * @return a collection of the imported packages.
   */
  Collection<? extends ImportedPackage> getImportedPackages();  

  /**
   * Import-Service header modelled to be a collection of imported service objects. 
   * This contains the blueprint service referenced by this resource.
   * @return a collection of the imported services.
   */
  Collection<? extends ImportedService> getImportedServices();

  /**
   * Export-Package header modelled to be a collection of exported package objects.
   * @return a collection of exported package objects.
   */
  Collection<? extends ExportedPackage> getExportedPackages();

  /**
   * Export-Service header modelled to be a collection of exported service objects.
   * This includes the blueprint services supported by this resource.
   * @return a collection of exported service objects.
   */
  Collection<? extends ExportedService> getExportedServices();

  /**
   * Return the bundle that represents the resource object.
   * @return
   */
  ExportedBundle getExportedBundle();

  /**
   * The resource type, mainly BUNDLE or other special bundle.
   * @return The resource type.
   */
  ResourceType getType();

  /**
   * The required bundles modelled as a collection of ImportedBundle objects.
   * @return a collection of ImportedBundle objects.
   */
  Collection<? extends ImportedBundle> getRequiredBundles();

  /**
   * Whether the resource is fragment.
   * @return
   */
  boolean isFragment();

  /**
   * The fragment host.
   * @return The fragment host.
   */
  public ImportedBundle getFragmentHost();

}
