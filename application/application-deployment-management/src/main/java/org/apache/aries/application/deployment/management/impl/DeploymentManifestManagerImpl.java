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
package org.apache.aries.application.deployment.management.impl;

import static org.apache.aries.application.utils.AppConstants.LOG_ENTRY;
import static org.apache.aries.application.utils.AppConstants.LOG_EXIT;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.aries.application.ApplicationMetadata;
import org.apache.aries.application.Content;
import org.apache.aries.application.InvalidAttributeException;
import org.apache.aries.application.ServiceDeclaration;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.BundleInfo;
import org.apache.aries.application.management.ResolveConstraint;
import org.apache.aries.application.management.ResolverException;
import org.apache.aries.application.management.spi.resolve.AriesApplicationResolver;
import org.apache.aries.application.management.spi.resolve.DeploymentManifestManager;
import org.apache.aries.application.management.spi.resolve.PostResolveTransformer;
import org.apache.aries.application.management.spi.resolve.PreResolveHook;
import org.apache.aries.application.management.spi.runtime.LocalPlatform;
import org.apache.aries.application.modelling.DeployedBundles;
import org.apache.aries.application.modelling.ExportedPackage;
import org.apache.aries.application.modelling.ExportedService;
import org.apache.aries.application.modelling.ImportedBundle;
import org.apache.aries.application.modelling.ImportedPackage;
import org.apache.aries.application.modelling.ModelledResource;
import org.apache.aries.application.modelling.ModelledResourceManager;
import org.apache.aries.application.modelling.ModellerException;
import org.apache.aries.application.modelling.ModellingManager;
import org.apache.aries.application.modelling.utils.ModellingHelper;
import org.apache.aries.application.utils.AppConstants;
import org.apache.aries.application.utils.manifest.ContentFactory;
import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.io.IOUtils;
import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.service.blueprint.container.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeploymentManifestManagerImpl implements DeploymentManifestManager
{

  private final Logger _logger = LoggerFactory.getLogger(DeploymentManifestManagerImpl.class);
  private AriesApplicationResolver resolver;
  private PostResolveTransformer postResolveTransformer = null;

  private ModelledResourceManager modelledResourceManager;
  private LocalPlatform localPlatform;
  private ModellingManager modellingManager;
  private ModellingHelper modellingHelper;
  private List<PreResolveHook> preResolveHooks;

  public void setModellingManager (ModellingManager m) {
    modellingManager = m; 
  }
  
  public void setModellingHelper (ModellingHelper mh) { 
    modellingHelper = mh;
  }

  public LocalPlatform getLocalPlatform()
  {
    return localPlatform;
  }

  public void setLocalPlatform(LocalPlatform localPlatform)
  {
    this.localPlatform = localPlatform;
  }
  
  public void setPreResolveHooks(List<PreResolveHook> hooks)
  {
    preResolveHooks = hooks;
  }

  public ModelledResourceManager getModelledResourceManager()
  {
    return modelledResourceManager;
  }

  public void setModelledResourceManager(ModelledResourceManager modelledResourceManager)
  {
    this.modelledResourceManager = modelledResourceManager;
  }

  public void setPostResolveTransformer(PostResolveTransformer transformer) {
    postResolveTransformer = transformer;
  }
  
  public void setResolver(AriesApplicationResolver resolver)
  {
    this.resolver = resolver;
  }

  /**
   * Perform provisioning to work out the 'freeze dried list' of the eba
   * @param app - Aries application
   * @param ResolveConstraint - resolver constraint for limiting the resolving results
   * @return manifest the generated deployment manifest
   * @throws ResolverException
   */
  @Override
  public Manifest generateDeploymentManifest(AriesApplication app,  ResolveConstraint... constraints ) throws ResolverException
  {

    _logger.debug(LOG_ENTRY, "generateDeploymentManifest", new Object[]{app, constraints});
    ApplicationMetadata appMetadata = app.getApplicationMetadata();
    Collection<ModelledResource> byValueBundles = null;
    try {
      // find out blueprint information
      byValueBundles = getByValueBundles(app);
      // find out by value bundles and then by reference bundles
    } catch (Exception e) {
      throw new ResolverException (e);
    }

    Collection<Content> bundlesToResolve = new ArrayList<Content>();
    bundlesToResolve.addAll(appMetadata.getApplicationContents());    
    bundlesToResolve.addAll(app.getApplicationMetadata().getUseBundles());

    //If we pass in provision bundles (e.g. import deployment manifest sanity check), we add them into our bundlesToResolve set.
    // This is because we want to make sure all bundles we passed into resolver the same as what we are going to get from resolver. 
    List<Content> restrictedReqs = new ArrayList<Content>();
    for (ResolveConstraint constraint : constraints ) {
      Content content = ContentFactory.parseContent(constraint.getBundleName(), constraint.getVersionRange().toString());
      restrictedReqs.add(content);
    }
    
    DeployedBundles deployedBundles = generateDeployedBundles (appMetadata, 
        byValueBundles, restrictedReqs);
    
    Manifest man = generateDeploymentManifest(appMetadata.getApplicationSymbolicName(),
        appMetadata.getApplicationVersion().toString(), deployedBundles);
    _logger.debug(LOG_EXIT, "generateDeploymentManifest", new Object[] {man});
    return man;
  }

  /**
   * Perform provisioning to work out the 'freeze dried list' of the eba
   * @param appContent - the application content in the application.mf
   * @param useBundleContent - use bundle entry in the application.mf
   * @param providedByValueBundles - bundles contained in the eba  
   * @return
   * @throws ResolverException
   */
    @Override
	public DeployedBundles generateDeployedBundles(ApplicationMetadata appMetadata,
            Collection<ModelledResource> provideByValueBundles, Collection<Content> otherBundles)
            throws ResolverException {
     
    Collection<Content> useBundleSet = appMetadata.getUseBundles();
    Collection<Content> appContent = appMetadata.getApplicationContents();
    
    Collection<Content> bundlesToResolve = new ArrayList<Content>();
    Set<ImportedBundle> appContentIB = toImportedBundle(appContent);
    Set<ImportedBundle> useBundleIB = toImportedBundle(useBundleSet);


    bundlesToResolve.addAll(useBundleSet);

    bundlesToResolve.addAll(appContent);
    bundlesToResolve.addAll(otherBundles);
    Collection<ModelledResource> byValueBundles = new ArrayList<ModelledResource>(provideByValueBundles);
    ModelledResource fakeBundleResource;
    try { 
      fakeBundleResource = createFakeBundle(appMetadata.getApplicationImportServices());
    } catch (InvalidAttributeException iax) { 
      ResolverException rx = new ResolverException (iax);
      _logger.debug(LOG_EXIT, "generateDeploymentManifest", new Object[] {rx});

      throw rx;
    }
    byValueBundles.add(fakeBundleResource);
    
    Collection<ModelledResource> fakeResources = new ArrayList<ModelledResource>();
    for (PreResolveHook hook : preResolveHooks) {
      hook.collectFakeResources(fakeResources);
    }
    
    byValueBundles.addAll(fakeResources);
    
    String appSymbolicName = appMetadata.getApplicationSymbolicName();
    String appVersion = appMetadata.getApplicationVersion().toString();
    String uniqueName = appSymbolicName + "_" + appVersion;
    
    DeployedBundles deployedBundles = modellingHelper.createDeployedBundles(appSymbolicName, appContentIB, useBundleIB, Arrays.asList(fakeBundleResource));
    Collection<ModelledResource> bundlesToBeProvisioned = resolver.resolve(
        appSymbolicName, appVersion, byValueBundles, bundlesToResolve);
    pruneFakeBundlesFromResults (bundlesToBeProvisioned, fakeResources);

    if (bundlesToBeProvisioned.isEmpty()) {
      throw new ResolverException(MessageUtil.getMessage("EMPTY_DEPLOYMENT_CONTENT",uniqueName));
    } 
    for (ModelledResource rbm : bundlesToBeProvisioned)
    {
      deployedBundles.addBundle(rbm);
    }
    Collection<ModelledResource> requiredUseBundle = deployedBundles.getRequiredUseBundle();
    if (requiredUseBundle.size() < useBundleSet.size())
    {
      // Some of the use-bundle entries were redundant so resolve again with just the good ones.
      deployedBundles = modellingHelper.createDeployedBundles(appSymbolicName, appContentIB, useBundleIB, Arrays.asList(fakeBundleResource));
      bundlesToResolve.clear();
      bundlesToResolve.addAll(appContent);
      Collection<ImportedBundle> slimmedDownUseBundle = narrowUseBundles(useBundleIB, requiredUseBundle);
      bundlesToResolve.addAll(toContent(slimmedDownUseBundle));
      bundlesToBeProvisioned = resolver.resolve(appSymbolicName, appVersion,
          byValueBundles, bundlesToResolve);
       pruneFakeBundlesFromResults (bundlesToBeProvisioned, fakeResources);
      for (ModelledResource rbm : bundlesToBeProvisioned)
      {
        deployedBundles.addBundle(rbm);
      }
      
      requiredUseBundle = deployedBundles.getRequiredUseBundle();
    }

    // Check for circular dependencies. No shared bundle can depend on any 
    // isolated bundle. 
    Collection<ModelledResource> sharedBundles = new HashSet<ModelledResource>();
    sharedBundles.addAll (deployedBundles.getDeployedProvisionBundle());
    sharedBundles.addAll (requiredUseBundle); 

    Collection<ModelledResource> appContentBundles = deployedBundles.getDeployedContent();
    Collection<Content> requiredSharedBundles = new ArrayList<Content>();
    for (ModelledResource mr : sharedBundles) { 
      String version = mr.getExportedBundle().getVersion();
      String exactVersion = "[" + version + "," + version + "]";

      Content ib = ContentFactory.parseContent(mr.getExportedBundle().getSymbolicName(), 
          exactVersion);
      requiredSharedBundles.add(ib);

    }
    // This will throw a ResolverException if the shared content does not resolve
    Collection<ModelledResource> resolvedSharedBundles = resolver.resolve(appSymbolicName, appVersion
        , byValueBundles, requiredSharedBundles);

    // we need to find out whether any shared bundles depend on the isolated bundles
    List<String> suspects = findSuspects (resolvedSharedBundles, sharedBundles, appContentBundles);
    // If we have differences, it means that we have shared bundles trying to import packages
    // from isolated bundles. We need to build up the error message and throw a ResolverException
    if (!suspects.isEmpty()) { 
    	
    	
      StringBuilder msgs = new StringBuilder();
      List<String> unsatisfiedRequirements = new ArrayList<String>();

      Map<String, List<String>> isolatedBundles = new HashMap<String, List<String>>();
      // Find the isolated bundles and store all the packages that they export in a map.
      for (ModelledResource mr : resolvedSharedBundles) {
        String mrName = mr.getSymbolicName() + "_" + mr.getExportedBundle().getVersion();
        if (suspects.contains(mrName)) {
          List<String> exportedPackages = new ArrayList<String>();
          isolatedBundles.put(mrName, exportedPackages);
          for (ExportedPackage ep : mr.getExportedPackages()) {
            exportedPackages.add(ep.getPackageName());
          }
        }
      }
      // Now loop through the shared bundles, reading the imported packages, and find which ones 
      // are exported from the isolated bundles.
      for (ModelledResource mr : resolvedSharedBundles) {
        String mrName = mr.getSymbolicName() + "_" + mr.getExportedBundle().getVersion();
        // if current resource isn't an isolated bundle check it's requirements
        if (!!! suspects.contains(mrName)) {
          // Iterate through the imported packages of the current shared bundle.
          for (ImportedPackage ip : mr.getImportedPackages()) {
            String packageName = ip.getPackageName();
            List<String> bundlesExportingPackage = new ArrayList<String>();
            // Loop through each exported package of each isolated bundle, and if we
            // get a match store the info away.
            for (Map.Entry<String, List<String>> currBundle : isolatedBundles.entrySet()) {

              List<String> exportedPackages = currBundle.getValue();
              if (exportedPackages != null && exportedPackages.contains(packageName)) {
                bundlesExportingPackage.add(currBundle.getKey());
              }
            }
            // If we have found at least one matching entry, we construct the sub message for the
            // exception.
            if (!!! bundlesExportingPackage.isEmpty()) {
              String newMsg;
              if (bundlesExportingPackage.size() > 1) {
                newMsg = MessageUtil.getMessage("SHARED_BUNDLE_IMPORTING_FROM_ISOLATED_BUNDLES", 
                    new Object[] {mrName, packageName, bundlesExportingPackage}); 
              } else {
                newMsg = MessageUtil.getMessage("SHARED_BUNDLE_IMPORTING_FROM_ISOLATED_BUNDLE", 
                    new Object[] {mrName, packageName, bundlesExportingPackage});
              }
              msgs.append("\n");
              msgs.append(newMsg);
              unsatisfiedRequirements.add(newMsg);
            }
          }
        }
      }
      // Once we have iterated over all bundles and have got our translated submessages, 
      // throw the exception.
      // Well! if the msgs is empty, no need to throw an exception
      if (msgs.length() !=0) {
        String message = MessageUtil.getMessage(
            "SUSPECTED_CIRCULAR_DEPENDENCIES", new Object[] {appSymbolicName, msgs});
        ResolverException rx = new ResolverException (message);
        rx.setUnsatisfiedRequirements(unsatisfiedRequirements);
        _logger.debug(LOG_EXIT, "generateDeploymentManifest", new Object[] {rx});
        throw (rx);
      }
    }
    
    checkForIsolatedContentInProvisionBundle(appSymbolicName, deployedBundles);
      
    if (postResolveTransformer != null) try {  
      deployedBundles = postResolveTransformer.postResolveProcess (appMetadata, deployedBundles);
    } catch (ServiceUnavailableException e) { 
      _logger.debug(MessageUtil.getMessage("POST_RESOLVE_TRANSFORMER_UNAVAILABLE",e));
    }
    return deployedBundles;
  }
  

  @Override
  public Manifest generateDeploymentManifest(String appSymbolicName,
      String appVersion, DeployedBundles deployedBundles)
      throws ResolverException 
    {
    
    _logger.debug (LOG_ENTRY, "generateDeploymentManifest", 
        new Object[]{appSymbolicName, appVersion, deployedBundles});
    Map<String, String> deploymentManifestMap = generateDeploymentAttributes(appSymbolicName, 
        appVersion, deployedBundles);
    Manifest man = convertMapToManifest(deploymentManifestMap);
    _logger.debug (LOG_EXIT, "generateDeploymentManifest", man);
    return man;
  }
    
  /**
   * Returns a Collection of the {@link ImportedBundle} objects that are
   * satisfied by the contents of the Collection of requiredUseBundles.
   * 
   * @param useBundleSet
   * @param requiredUseBundle
   * @return the collection of ImportedBundle objects
   */
  private Collection<ImportedBundle> narrowUseBundles(
      Collection<ImportedBundle> useBundleSet,
      Collection<ModelledResource> requiredUseBundle) {
    _logger.debug(LOG_ENTRY, "narrowUseBundles", new Object[] {useBundleSet,requiredUseBundle});
    Collection<ImportedBundle> result = new HashSet<ImportedBundle>();

    outer : for(ImportedBundle ib : useBundleSet) {
      for(ModelledResource mb : requiredUseBundle) {
        if(ib.isSatisfied(mb.getExportedBundle())) {
          result.add(ib);
          continue outer;
        }
      }
    }
    _logger.debug(LOG_EXIT, "narrowUseBundles", result);
    return result;
  }



  private Map<String,String> generateDeploymentAttributes(String appSymbolicName, String version, 
      DeployedBundles deployedBundles) throws ResolverException
  {
    _logger.debug(LOG_ENTRY, "generateDeploymentAttributes", new Object[] {appSymbolicName, version});
    Map<String,String> result = new HashMap<String, String>();
    String content = deployedBundles.getContent();
    if (!content.isEmpty()) {
      result.put(AppConstants.DEPLOYMENT_CONTENT, content);
    } else {
      throw new ResolverException(MessageUtil.getMessage("EMPTY_DEPLOYMENT_CONTENT", appSymbolicName));
    }

    String useBundle = deployedBundles.getUseBundle();
    if (!useBundle.isEmpty()) {
      result.put(AppConstants.DEPLOYMENT_USE_BUNDLE, useBundle);
    }

    String provisionBundle = deployedBundles.getProvisionBundle();
    if (!provisionBundle.isEmpty()) {
      result.put(AppConstants.DEPLOYMENT_PROVISION_BUNDLE, provisionBundle);
    }


    String importServices = deployedBundles.getDeployedImportService();
    if (!importServices.isEmpty()) { 
      result.put(AppConstants.DEPLOYMENTSERVICE_IMPORT, importServices);
    }

    String importPackages = deployedBundles.getImportPackage();
    if (!importPackages.isEmpty()) {
      result.put(Constants.IMPORT_PACKAGE, importPackages);
    }

    result.put(AppConstants.APPLICATION_VERSION, version);
    result.put(AppConstants.APPLICATION_SYMBOLIC_NAME, appSymbolicName);
    
    result.putAll(deployedBundles.getExtraHeaders());
    
    _logger.debug(LOG_EXIT, "generateDeploymentAttributes", result);
    return result;
  }

  private Manifest convertMapToManifest(Map<String,String> attributes)
  {
    _logger.debug(LOG_ENTRY, "convertMapToManifest", new Object[]{attributes});
    Manifest man = new Manifest();
    Attributes att = man.getMainAttributes();
    att.putValue(Attributes.Name.MANIFEST_VERSION.toString(), AppConstants.MANIFEST_VERSION);
    for (Map.Entry<String, String> entry : attributes.entrySet()) {
      att.putValue(entry.getKey(),  entry.getValue());
    }
    _logger.debug(LOG_EXIT, "convertMapToManifest", new Object[]{man});
    return man;
  }




  private static final String FAKE_BUNDLE_NAME = "aries.internal.fake.service.bundle";

  // create a 'mock' bundle that does nothing but export services required by 
  // Application-ImportService
  private ModelledResource createFakeBundle (Collection<ServiceDeclaration> appImportServices) throws InvalidAttributeException 
  {
    _logger.debug(LOG_ENTRY, "createFakeBundle", new Object[]{appImportServices});
    Attributes attrs = new Attributes();
    attrs.putValue(Constants.BUNDLE_SYMBOLICNAME, FAKE_BUNDLE_NAME);
    attrs.putValue(Constants.BUNDLE_VERSION_ATTRIBUTE, "1.0");
    attrs.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");

    // Build an ExportedService for every Application-ImportService entry
    Collection<ExportedService> exportedServices = new ArrayList<ExportedService>();
    for (ServiceDeclaration sDec : appImportServices) { 
      Collection<String> ifaces = Arrays.asList(sDec.getInterfaceName());
      Filter filter = sDec.getFilter();
      Map<String, String> serviceProperties;
      if (filter != null) { 
        serviceProperties = ManifestHeaderProcessor.parseFilter(filter.toString());
      } else { 
        serviceProperties = new HashMap<String, String>();
      }
      serviceProperties.put("service.imported", "");
      exportedServices.add (modellingManager.getExportedService("", 0, ifaces, new HashMap<String, Object>(serviceProperties)));
    }
    ModelledResource fakeBundle = modellingManager.getModelledResource(null, attrs, null, exportedServices);

    _logger.debug(LOG_EXIT, "createFakeBundle", new Object[]{fakeBundle});
    return fakeBundle;
  }

  private void pruneFakeBundlesFromResults (Collection<ModelledResource> results, Collection<ModelledResource> fakeResources) { 
    _logger.debug(LOG_ENTRY, "pruneFakeBundleFromResults", new Object[]{results});
    
    List<String> fakeBundles = new ArrayList<String>();
    
    fakeBundles.add(FAKE_BUNDLE_NAME);
    for (ModelledResource resource : fakeResources) {
      fakeBundles.add(resource.getSymbolicName());
    }
    
    Iterator<ModelledResource> it = results.iterator();
    while (it.hasNext()) { 
      ModelledResource mr = it.next();
      if (fakeBundles.contains(mr.getSymbolicName())) { 
        it.remove();
      }
    }
    _logger.debug(LOG_EXIT, "pruneFakeBundleFromResults");

  }

  /**
   * We've done a sanity check resolve on our sharedBundles and received back 
   * resolvedSharedBundles. The resolvedSharedBundles should not contain any bundles listed in the isolated bundle list.
   * If this is not true, we've found a case of shared bundles depending on isolated bundles. 
   * This method extracts the name_versions of those bundles in resolvedSharedBundles
   * that do not appear in sharedBundles. 
   * @param resolvedSharedBundles What we got back from the resolver
   * @param sharedBundles         What we expected to get back from the resolver
   * @param appContentBundles     The isolated bundles
   * @return                      The isolated bundles depended by the shared bundles
   */
  private List<String> findSuspects (Collection<ModelledResource> resolvedSharedBundles, 
      Collection<ModelledResource> sharedBundles, Collection<ModelledResource> appContentBundles){
    _logger.debug(LOG_ENTRY, "findSuspects", new Object[]{resolvedSharedBundles,sharedBundles, appContentBundles });
    Set<String> expectedBundles = new HashSet<String>();
    Set<String> isolatedBundles = new HashSet<String>();
    for (ModelledResource sb : sharedBundles) { 
      expectedBundles.add(sb.getExportedBundle().getSymbolicName() + "_" + 
          sb.getExportedBundle().getVersion());
    }
    for (ModelledResource sb : appContentBundles) { 
    	isolatedBundles.add(sb.getExportedBundle().getSymbolicName() + "_" + 
            sb.getExportedBundle().getVersion());
    }
    List<String> suspects = new ArrayList<String>();
    for (ModelledResource mr : resolvedSharedBundles) {
      String thisBundle = mr.getExportedBundle().getSymbolicName() + "_" + 
      mr.getExportedBundle().getVersion();
      if (!expectedBundles.contains(thisBundle) && (isolatedBundles.contains(thisBundle))) { 
        suspects.add(thisBundle);   
      }
    }
    _logger.debug(LOG_EXIT, "findSuspects", new Object[]{suspects});

    return suspects;
  }
  
  /**
   * Check whether there are isolated bundles deployed into both deployed content and provision bundles. This almost
   * always indicates a resolution problem hence we throw a ResolverException.
   * Note that we check provision bundles rather than provision bundles and deployed use bundles. So in any corner case
   * where the rejected deployment is actually intended, it can still be achieved by introducing a use bundle clause.
   * 
   * @param applicationSymbolicName
   * @param appContentBundles
   * @param provisionBundles
   * @throws ResolverException
   */
  private void checkForIsolatedContentInProvisionBundle(String applicationSymbolicName, DeployedBundles db)
    throws ResolverException
  {
    for (ModelledResource isolatedBundle : db.getDeployedContent()) {
      for (ModelledResource provisionBundle : db.getDeployedProvisionBundle()) {
        if (isolatedBundle.getSymbolicName().equals(provisionBundle.getSymbolicName()) 
            && providesPackage(provisionBundle, db.getImportPackage())) {
          
          throw new ResolverException(
              MessageUtil.getMessage("ISOLATED_CONTENT_PROVISIONED", 
                  applicationSymbolicName,
                  isolatedBundle.getSymbolicName(),
                  isolatedBundle.getVersion(),
                  provisionBundle.getVersion()));
        }
      }
    }
  }
  
  /**
   * Can the modelled resource provide a package against the given import specificiation
   * @param bundle
   * @param importPackages
   * @return
   */
  private boolean providesPackage(ModelledResource bundle, String importPackages)
  {
    Map<String, Map<String, String>> imports = ManifestHeaderProcessor.parseImportString(importPackages);
    
    try {
      for (Map.Entry<String, Map<String,String>> e : imports.entrySet()) {
        ImportedPackage importPackage = modellingManager.getImportedPackage(e.getKey(), e.getValue());
        
        for (ExportedPackage export : bundle.getExportedPackages()) {
          if (importPackage.isSatisfied(export)) return true;
        }
      }
    } catch (InvalidAttributeException iae) {
      _logger.error(MessageUtil.getMessage("UNEXPECTED_EXCEPTION_PARSING_IMPORTS", iae, importPackages), iae);
    }
    
    return false;
  }

  /**
   * Covert a collection of contents to a collection of ImportedBundle objects
   * @param content a collection of content
   * @return a collection of ImportedBundle objects
   * @throws ResolverException
   */
  private Set<ImportedBundle> toImportedBundle(Collection<Content> content) throws ResolverException
  {

    _logger.debug(LOG_ENTRY, "toImportedBundle", new Object[]{content});

    Set<ImportedBundle> result = new HashSet<ImportedBundle>();
    for (Content c : content) {
      try {
        result.add(modellingManager.getImportedBundle(c.getContentName(), c.getVersion().toString()));
      } catch (InvalidAttributeException iax) { 
        ResolverException rx = new ResolverException (iax);
        _logger.debug(LOG_EXIT, "toImportedBundle", new Object[]{rx});
        throw rx;
      }
    }

    _logger.debug(LOG_EXIT, "toImportedBundle", new Object[]{result});

    return result;
  }

  private Collection<Content> toContent(Collection<ImportedBundle> ibs)
  {
    Collection<Content> contents = new ArrayList<Content>();
    for (ImportedBundle ib : ibs) {
      contents.add(ContentFactory.parseContent(ib.getSymbolicName(), ib.getVersionRange()));
    }
    return contents;
  }
  /**
   * Get a list of bundles included by value in this application.
   * @param app The Aries Application
   * @return a list of by value bundles
   * @throws IOException
   * @throws InvalidAttributeException
   * @throws ModellerException
   */
  private Collection<ModelledResource> getByValueBundles(AriesApplication app) throws IOException, InvalidAttributeException, ModellerException {

    _logger.debug(LOG_ENTRY, "getByValueBundles", new Object[]{app});

    Collection<BundleInfo> bundles = app.getBundleInfo();
    Collection<ModelledResource> result = new ArrayList<ModelledResource>();

    for (BundleInfo bundleInfo: bundles) {      
      // find out the eba directory
      String bundleLocation = bundleInfo.getLocation();
      String bundleFileName = bundleLocation.substring(bundleLocation.lastIndexOf('/') + 1);
      // just the portion of root directory excluding !      
      URL jarUrl = new URL(bundleLocation);
      URLConnection jarCon = jarUrl.openConnection();
      jarCon.connect();
      InputStream in = jarCon.getInputStream();
      File dir = getLocalPlatform().getTemporaryDirectory();
      File temp = new File(dir, bundleFileName);
      OutputStream out = new FileOutputStream(temp);
      IOUtils.copy(in, out);
      IOUtils.close(out);
      
      result.add(modelledResourceManager.getModelledResource(null, FileSystem.getFSRoot(temp)));
      // delete the temp file
      temp.delete();
      IOUtils.deleteRecursive(dir);
    }
    _logger.debug(LOG_EXIT, "getByValueBundles", new Object[]{result});
    return result;
  }


}
