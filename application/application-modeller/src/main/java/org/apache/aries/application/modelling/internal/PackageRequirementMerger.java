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
package org.apache.aries.application.modelling.internal;
import static org.apache.aries.application.utils.AppConstants.LOG_ENTRY;
import static org.apache.aries.application.utils.AppConstants.LOG_EXIT;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.aries.application.modelling.ImportedPackage;
import org.apache.aries.application.modelling.utils.ModellingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A class to merge collections of package requirements, such that multiple requirements
 * for the same package are consolidated to a single requirement with a version constraint
 * that is the intersection of the original version requirements.
 */
public final class PackageRequirementMerger
{
  private final Logger logger = LoggerFactory.getLogger(PackageRequirementMerger.class);
  /** The merged requirements, or null if the merge failed. */
  private final Collection<ImportedPackage> mergedRequirements;
  /** Names of packages for which requirements were incompatible. */
  private final Set<String> invalidRequirements = new HashSet<String>();
  
  /**
   * Constructor.
   * @param requirements the package requirements to be merged.
   * @throws NullPointerException if the parameter is {@code null}.
   */
  public PackageRequirementMerger(Collection<ImportedPackage> requirements)
  {
    logger.debug(LOG_ENTRY, "PackageRequirementMerger", requirements);
    
    if (requirements == null)
    {
      NullPointerException npe = new NullPointerException();
      logger.debug(LOG_EXIT, "PackageRequirementMerger", npe);
      throw npe;
    }

    // Do the merge.
    Map<String, ImportedPackage> reqMap = new HashMap<String, ImportedPackage>();
    for (ImportedPackage req : requirements)
    {
      String pkgName = req.getPackageName();
      ImportedPackage existingReq = reqMap.get(pkgName);
      if (existingReq == null)
      {
        reqMap.put(pkgName, req);
        continue;
      }
      
      ImportedPackage intersectReq = ModellingUtils.intersectPackage(req, existingReq);
      if (intersectReq != null)
      {
        reqMap.put(pkgName, intersectReq);
        continue;
      }

      invalidRequirements.add(pkgName);
    }
    
    mergedRequirements = (invalidRequirements.isEmpty() ? reqMap.values() : null);
    logger.debug(LOG_EXIT,"PackageRequirementMerger");
    }

  /**
   * Check if the requirements could be successfully merged.
   * @return true if the merge was successful; false if the requirements were not compatible.
   */
  public boolean isMergeSuccessful()
  {
    logger.debug(LOG_ENTRY, "isMergeSuccessful");
    boolean result = mergedRequirements != null;
    logger.debug(LOG_EXIT, "isMergeSuccessful", result);
    return result;
  }
  
  /**
   * Get the merged package requirements. The result will mirror the input collection,
   * except that multiple requirements for the same package will be replaced by a single
   * requirement that is the intersection of all the input requirements.
   * <p>
   * The {@code isMergeSuccessful} method should be checked for success prior to calling this method.
   * @param inputRequirements
   * @return A collection of package requirements, or {@code null} if the input contained incompatible requirements.
   * @throws IllegalStateException if the merge was not successful.
   */
  public Collection<ImportedPackage> getMergedRequirements()
  {
    logger.debug(LOG_ENTRY, "getMergedRequirements");
    if (mergedRequirements == null)
    {
      IllegalStateException ise = new IllegalStateException();
      logger.debug(LOG_EXIT, "getMergedRequirements", ise);
      throw ise;
    }
    logger.debug(LOG_EXIT, "getMergedRequirements", mergedRequirements);
    return Collections.unmodifiableCollection(mergedRequirements);
  }
  
  /**
   * Get the names of packages that caused the merge to fail due to their constraints
   * being mutually exclusive.
   * @return an unmodifiable set of package names.
   */
  public Set<String> getInvalidRequirements()
  {
    logger.debug(LOG_ENTRY, "getInvalidRequirements");
    logger.debug(LOG_EXIT, "getInvalidRequirements", invalidRequirements);
    return Collections.unmodifiableSet(invalidRequirements);
  }

}
