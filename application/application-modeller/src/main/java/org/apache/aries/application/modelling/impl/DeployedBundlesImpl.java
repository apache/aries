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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.aries.application.management.ResolverException;
import org.apache.aries.application.modelling.DeployedBundles;
import org.apache.aries.application.modelling.DeploymentMFElement;
import org.apache.aries.application.modelling.ExportedBundle;
import org.apache.aries.application.modelling.ExportedPackage;
import org.apache.aries.application.modelling.ExportedService;
import org.apache.aries.application.modelling.ImportedBundle;
import org.apache.aries.application.modelling.ImportedPackage;
import org.apache.aries.application.modelling.ImportedService;
import org.apache.aries.application.modelling.ModelledResource;
import org.apache.aries.application.modelling.internal.MessageUtil;
import org.apache.aries.application.modelling.internal.PackageRequirementMerger;
import org.apache.aries.application.utils.AppConstants;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Class to generate DEPLOYMENT.MF manifest entries for resolved bundles based on
 * corresponding APPLICATION.MF entries.
 */
public final class DeployedBundlesImpl implements DeployedBundles
{
  private final Logger logger = LoggerFactory.getLogger(DeployedBundlesImpl.class);
  private final String assetName;
  
  private String cachedImportPackage;
  private Collection<ModelledResource> cachedRequiredUseBundle;
  private Collection<ImportedPackage> cachedExternalRequirements;
  private String cachedDeployedImportService;

  /** Content from APPLICATION.MF */
  private final Set<ImportedBundle> appContent = new HashSet<ImportedBundle>();
  /** Use Bundle from APPLICATION.MF */
  private final Set<ImportedBundle> appUseBundle = new HashSet<ImportedBundle>();
  /** Content for deployment.mf deployed-content. */
  private final Set<ModelledResource> deployedContent = new HashSet<ModelledResource>();
  /** Content for deployment.mf use-bundle. */
  private final Set<ModelledResource> deployedUseBundle = new HashSet<ModelledResource>();
  /** Content for deployment.mf provision-bundle. */
  private final Set<ModelledResource> deployedProvisionBundle = new HashSet<ModelledResource>();
  /** Content for deployment.mf DeployedImport-Service. */
  private final Collection<ImportedService> deployedImportService = new HashSet<ImportedService>(); 
  private final Collection<ModelledResource> fakeDeployedBundles = new HashSet<ModelledResource>();
  
  /**
   * Constructor for cases when we have one or more '
   * @param assetName         the name of the asset being deployed.
   * @param appContentNames   the bundle names specified in Deployed-Content.
   * @param appUseBundleNames the bundle names specified in Deployed-Use-Bundle.
   * @param fakeServiceProvidingBundles  bundles that we're pretending are part of the deployed content. Can be null. 
   *                                     These bundles are proxies for bundles provided (for example by SCA) that export
   *                                     services matching Application-ImportService. 
   */
  public DeployedBundlesImpl(String assetName, Collection<ImportedBundle> appContentNames, 
      Collection<ImportedBundle> appUseBundleNames, Collection<ModelledResource> fakeServiceProvidingBundles)
  {
    logger.debug(LOG_ENTRY, "DeployedBundles", new Object[]{appContentNames, appUseBundleNames, fakeServiceProvidingBundles});
    
    this.assetName = assetName;

    appContent.addAll(appContentNames);
    appUseBundle.addAll(appUseBundleNames);
    if (fakeServiceProvidingBundles != null) { 
      fakeDeployedBundles.addAll(fakeServiceProvidingBundles);
    }
    logger.debug(LOG_EXIT, "DeployedBundles");
  }
  
  /**
   * Add provisioned version information for a specific bundle name. This will be added to the
   * appropriate manifest header for the specified bundle.
   * @param resolvedBundle the bundle that has been provisioned.
   * @param resolvedVersion the specific version provisioned.
   */
  public void addBundle(ModelledResource modelledBundle)
  {
    logger.debug(LOG_ENTRY, "addBundle", new Object[]{modelledBundle});
    // Identify the deployment.mf entries the bundle should be added to by matching
    // both the bundle name and resolved version against the name and version range
    // defined in application.mf.
    
    ExportedBundle resolvedBundle = modelledBundle.getExportedBundle();
    
    if (isBundleMatch(appContent, resolvedBundle))
    {
      logger.debug("Added to " + AppConstants.DEPLOYMENT_CONTENT + ": " + resolvedBundle);
     
      deployedContent.add(modelledBundle);
      
      // Add any service dependencies to the list
      deployedImportService.addAll(modelledBundle.getImportedServices());
    }
    else if (isBundleMatch(appUseBundle, resolvedBundle))
    {
      logger.debug("Added to " + AppConstants.DEPLOYMENT_USE_BUNDLE + ": " + resolvedBundle);
      deployedUseBundle.add(modelledBundle);
    } else
    {
      logger.debug("Added to " + AppConstants.DEPLOYMENT_PROVISION_BUNDLE + ": " + resolvedBundle);
      deployedProvisionBundle.add(modelledBundle);
    }
    
    // Invalidate caches
    cachedImportPackage = null;
    cachedRequiredUseBundle = null;
    cachedDeployedImportService = null;
    cachedExternalRequirements = null;
    
   logger.debug(LOG_EXIT, "addBundle");    
  }

  /**
   * Check if a match is found between the supplied map of application bundle name/version information,
   * and the supplied bundle name and version.
   * @param imports Imported bundles
   * @param potentialMatch the exported bundle or composite we're interested in
   * @return true if a match is found; otherwise false.
   */
  private boolean isBundleMatch(Set<ImportedBundle> imports, ExportedBundle potentialMatch)
  {
    boolean result = false;
    
    for (ImportedBundle ib : imports)
    {
      if (ib.isSatisfied(potentialMatch))
      {
        result = true;
        break;
      }
    }
  
    return result;
  }
  
  /**
   * Get the value corresponding to the Deployed-Content header in the deployment.mf.
   * @return a manifest entry, or an empty string if there is no content.
   */
  public String getContent()
  {
    return createManifestString(deployedContent);
  }
  
  /**
   * Get the value corresponding to the Deployed-Use-Bundle header in the deployment.mf.
   * @return a manifest entry, or an empty string if there is no content.
   */
  public String getUseBundle()
  {
    return createManifestString(deployedUseBundle);
  }
  
  /**
   * Get the value corresponding to the Provision-Bundle header in the deployment.mf.
   * @return a manifest entry, or an empty string if there is no content.
   */
  public String getProvisionBundle()
  {
    return createManifestString(deployedProvisionBundle);
  }
  
  /**
   * Get the value corresponding to the Import-Package header in the deployment.mf. 
   * @return a manifest entry, or an empty string if there is no content.
   * @throws ResolverException if the requirements could not be resolved.
   */
  public String getImportPackage() throws ResolverException
  {
    logger.debug(LOG_ENTRY, "getImportPackage");
    
    String result = cachedImportPackage; 
    if (result == null)
    {
      
      Collection<ImportedPackage> externalReqs = new ArrayList<ImportedPackage>(getExternalPackageRequirements());
  
      //Validate that we don't have attributes that will break until RFC138 is used
      validateOtherImports(externalReqs);
      
      // Find the matching capabilities from bundles in use bundle, and prune
      // matched requirements out of the external requirements collection.
      Map<ImportedPackage,ExportedPackage> useBundlePkgs = new HashMap<ImportedPackage,ExportedPackage>();
      for (Iterator<ImportedPackage> iter = externalReqs.iterator(); iter.hasNext(); )
      {
        ImportedPackage req = iter.next();
        ExportedPackage match = getPackageMatch(req, deployedUseBundle);
        if (match != null)
        {
          useBundlePkgs.put(req, match);
          iter.remove();
        }
      }
      
      StringBuilder useBundleImports = new StringBuilder();
      for(Map.Entry<ImportedPackage, ExportedPackage> entry : useBundlePkgs.entrySet()) {
        useBundleImports.append(entry.getValue().toDeploymentString());
        ImportedPackage key = entry.getKey();
        if(key.isOptional())
          useBundleImports.append(";" + Constants.RESOLUTION_DIRECTIVE +":=" + Constants.RESOLUTION_OPTIONAL);
        useBundleImports.append(",");
      }
      
      result = useBundleImports.toString() + createManifestString(externalReqs);
      
      if(result.endsWith(","))
        result = result.substring(0, result.length() - 1);
      
      cachedImportPackage = result;
    }
    
    logger.debug(LOG_EXIT, "getImportPackage", result);
    return result;
  }
  
  /**
   * Get the Deployed-ImportService header. 
   * this.deployedImportService contains all the service import filters for every 
   * blueprint component within the application. We will only write an entry
   * to Deployed-ImportService if
   *   a) the reference isMultiple(), or
   *   b) the service was not available internally when the app was first deployed
   *   
   */
  public String getDeployedImportService() { 
    logger.debug(LOG_ENTRY,"getDeployedImportService");
    
    String result = cachedDeployedImportService;
    if (result == null)
    {
      Collection<ImportedService> deployedBundleServiceImports = new ArrayList<ImportedService>();
      Collection<ExportedService> servicesExportedWithinIsolatedContent = new ArrayList<ExportedService>();
      for (ModelledResource mRes : getDeployedContent()) { 
        servicesExportedWithinIsolatedContent.addAll(mRes.getExportedServices());
      }
      for (ModelledResource mRes : fakeDeployedBundles) { 
        servicesExportedWithinIsolatedContent.addAll(mRes.getExportedServices());
      }
      for (ImportedService impService : deployedImportService) { 
        if (impService.isMultiple()) { 
          deployedBundleServiceImports.add(impService);
        } else { 
          boolean serviceProvidedWithinIsolatedContent = false;
          Iterator<ExportedService> it = servicesExportedWithinIsolatedContent.iterator();
          while (!serviceProvidedWithinIsolatedContent && it.hasNext()) { 
            ExportedService svc = it.next(); 
            serviceProvidedWithinIsolatedContent |= impService.isSatisfied(svc);
          }
          if (!serviceProvidedWithinIsolatedContent) { 
            deployedBundleServiceImports.add(impService);
          }
        }
      }
      
      result = createManifestString(deployedBundleServiceImports);
      cachedDeployedImportService = result;
    }
    logger.debug(LOG_EXIT,"getDeployedImportService", result);
    
    return result;
  }
  /**
   * Get all the requirements of bundles in deployed content that are not satisfied
   * by other bundles in deployed content.
   * @return a collection of package requirements.
   * @throws ResolverException if the requirements could not be resolved.
   */
  private Collection<ImportedPackage> getExternalPackageRequirements()
    throws ResolverException
  {
    logger.debug(LOG_ENTRY,"getExternalPackageRequirements");
    
    Collection<ImportedPackage> result = cachedExternalRequirements;
    if (result == null)
    {
      // Get all the internal requirements.
      Collection<ImportedPackage> requirements = new ArrayList<ImportedPackage>();
      Collection<ExportedPackage> internalExports = new ArrayList<ExportedPackage>();
      for (ModelledResource bundle : deployedContent)
      {
        requirements.addAll(bundle.getImportedPackages());
        internalExports.addAll(bundle.getExportedPackages());
      }
          
      // Filter out requirements satisfied by internal capabilities.
      result = new ArrayList<ImportedPackage>();
      for (ImportedPackage req : requirements)
      {
        boolean satisfied = false;
        for (ExportedPackage export : internalExports)
        {
          if (req.isSatisfied(export))
          {
            satisfied = true;
            break;
          }
        }
        //If we didn't find a match then it must come from outside
        if (!satisfied)
          result.add(req);
      }
      
      PackageRequirementMerger merger = new PackageRequirementMerger(result);
      if (!merger.isMergeSuccessful())
      {
        List<String> pkgNames = new ArrayList<String>(merger.getInvalidRequirements());
        
        StringBuilder buff = new StringBuilder();
        for (String pkgName : merger.getInvalidRequirements())
        {
          buff.append(pkgName).append(", ");
        }
  
        int buffLen = buff.length();
        String pkgString = (buffLen > 0 ? buff.substring(0, buffLen - 2) : "");
  
        ResolverException re = new ResolverException(MessageUtil.getMessage(
            "INCOMPATIBLE_PACKAGE_VERSION_REQUIREMENTS", new Object[] { assetName, pkgString }));
        re.setUnsatisfiedRequirements(pkgNames);
        logger.debug(LOG_EXIT,"getExternalPackageRequirements", re);
        
        throw re;
      }
      
      result = merger.getMergedRequirements();
      cachedExternalRequirements = result;
    }
    logger.debug(LOG_EXIT,"getExternalPackageRequirements", result);
    
    return result;
  }
  
  /**
   * Create entries for the Import-Package header corresponding to the supplied
   * packages, referring to bundles not in Use-Bundle.
   * @param requirements packages for which entries should be created.
   * @return manifest header entries.
   * @throws ResolverException if the imports are invalid.
   */
  private void validateOtherImports(Collection<ImportedPackage> requirements)
    throws ResolverException
  {
    logger.debug(LOG_ENTRY, "validateOtherImports", requirements);
    for (ImportedPackage req : requirements)
    {
      String pkgName = req.getPackageName();

      for (String name : req.getAttributes().keySet())
      {
        if (Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE.equals(name)
            || Constants.BUNDLE_VERSION_ATTRIBUTE.equals(name))
        {
          ResolverException re = new ResolverException(MessageUtil.getMessage(
              "INVALID_PACKAGE_REQUIREMENT_ATTRIBUTES", new Object[] { assetName, name, pkgName }));
          re.setUnsatisfiedRequirements(Arrays.asList(pkgName));
          logger.debug(LOG_EXIT, "validateOtherImports", re);
          throw re;
        }
      }
    }
    logger.debug(LOG_EXIT, "validateOtherImports");
  }

  /**
   * Get a package match between the specified requirement and a capability of the supplied
   * bundles. The resulting match object might not refer to any matching capability.
   * @param requirement the {@link ImportedPackageImpl} to be matched.
   * @param bundles the bundles to be searched for matching capabilities.
   * @return an ExportedPackageImpl or null if no match is found.
   */
  private ExportedPackage getPackageMatch(ImportedPackage requirement, Collection<ModelledResource> bundles)
  {
    logger.debug(LOG_ENTRY, "getPackageMatch", new Object[]{requirement, bundles});
    ExportedPackage result = null;
    
    outer: for (ModelledResource bundle : bundles)
    {
      for (ExportedPackage pkg : bundle.getExportedPackages())
      {
        if(requirement.isSatisfied(pkg)) {
          result = pkg;
          break outer;
        }
      }
    }
    logger.debug(LOG_EXIT, "getPackageMatch", new Object[]{result});
    return result;
  }
  
  private String createManifestString(Collection<? extends DeploymentMFElement> values)
  {
    logger.debug(LOG_ENTRY, "createManifestString", new Object[]{values});
    StringBuilder builder = new StringBuilder();
    for (DeploymentMFElement value : values)
    {
      builder.append(value.toDeploymentString()).append(",");
    }
    
    int length = builder.length();
    String result = (length > 0 ? builder.substring(0, length - 1) : "");
    logger.debug(LOG_EXIT, "createManifestString", new Object[]{result});
    return result;
  }
  

  @Override
  public String toString()
  {
    return AppConstants.DEPLOYMENT_CONTENT + '=' + deployedContent + ' ' +
        AppConstants.DEPLOYMENT_USE_BUNDLE + '=' + deployedUseBundle + ' ' +
        AppConstants.DEPLOYMENT_PROVISION_BUNDLE + '=' + deployedProvisionBundle;
  }
  
  /**
   * Get the set of bundles that are going to be deployed into an isolated framework
   * @return a set of bundle metadata
   */
  public Collection<ModelledResource> getDeployedContent()
  {
    logger.debug(LOG_ENTRY, "getDeployedContent");
    logger.debug(LOG_EXIT,"getDeployedContent", deployedContent);
    return Collections.unmodifiableCollection(deployedContent);
  }
  
  /**
   * Get the set of bundles that map to Provision-Bundle: these plus 
   * getRequiredUseBundle combined give the bundles that will be provisioned
   * into the shared bundle space
   * 'getProvisionBundle' returns the manifest header string, so this method 
   * needs to be called something else. 
   *
   */
  public Collection<ModelledResource> getDeployedProvisionBundle () 
  { 
    logger.debug(LOG_ENTRY,"getDeployedProvisionBundle");
    logger.debug(LOG_EXIT, "getDeployedProvisionBundle", deployedContent);
    return Collections.unmodifiableCollection(deployedProvisionBundle);
  }
  
  /**
   * Get the subset of bundles specified in use-bundle that are actually required to
   * satisfy direct requirements of deployed content.
   * @return a set of bundle metadata.
   * @throws ResolverException if the requirements could not be resolved.
   */
  public Collection<ModelledResource> getRequiredUseBundle() throws ResolverException
  {
    logger.debug(LOG_ENTRY, "getRequiredUseBundle");
    
    Collection<ModelledResource> usedUseBundles =  cachedRequiredUseBundle;
    if (usedUseBundles == null)
    {
      Collection<ImportedPackage> externalReqs = getExternalPackageRequirements();
      usedUseBundles = new HashSet<ModelledResource>();
      for (ImportedPackage req : externalReqs)
      {
        // Find a match from the supplied bundle capabilities.
        ExportedPackage match = getPackageMatch(req, deployedUseBundle);
        if (match != null)
        {
            usedUseBundles.add(match.getBundle());
        }
      }
      cachedRequiredUseBundle = usedUseBundles;
    }
    
    logger.debug(LOG_EXIT, "getRequiredUseBundle", usedUseBundles);
    return usedUseBundles;
  }

  /** This method will be overridden by a PostResolveTransformer returning an extended version of
   * DeployedBundles 
   */
  public Map<String, String> getExtraHeaders() {
    logger.debug (LOG_ENTRY, "getExtraHeaders");
    Map<String, String> result = Collections.emptyMap();
    logger.debug (LOG_EXIT, "getExtraHeaders", result);
    return result;
  }
}
