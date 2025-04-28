/**
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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.application.modelling.utils;

import java.util.Collection;
import java.util.Map;

import org.apache.aries.application.InvalidAttributeException;
import org.apache.aries.application.modelling.DeployedBundles;
import org.apache.aries.application.modelling.ImportedBundle;
import org.apache.aries.application.modelling.ImportedPackage;
import org.apache.aries.application.modelling.ModelledResource;
import org.apache.aries.application.modelling.Provider;

/**
 * Useful functions associated with application modelling 
 *
 */
public interface ModellingHelper {

  /**
   * Check that all mandatory attributes from a Provider are specified by the consumer's attributes
   * @param consumerAttributes
   * @param p
   * @return true if all mandatory attributes are present, or no attributes are mandatory
   */
  boolean areMandatoryAttributesPresent(Map<String,String> consumerAttributes, Provider p);

  /**
   * Create an ImportedBundle from a Fragment-Host string
   * @param fragmentHostHeader
   * @return the imported bundle
   * @throws InvalidAttributeException
   */
  ImportedBundle buildFragmentHost(String fragmentHostHeader) throws InvalidAttributeException;
  
  /**
   * Create a new ImnportedPackage that is the intersection of the two supplied imports.
   * @param p1
   * @param p2
   * @return ImportedPackageImpl representing the intersection, or null. All attributes must match exactly.
   */
  ImportedPackage intersectPackage (ImportedPackage p1, ImportedPackage p2); 
    
  
  /**
   * Factory method for objects implementing the DeployedBundles interface
   *  
   */
  DeployedBundles createDeployedBundles (String assetName, Collection<ImportedBundle> appContentNames, 
      Collection<ImportedBundle> appUseBundleNames, Collection<ModelledResource> fakeServiceProvidingBundles);
}
