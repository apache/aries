/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.aries.application.resolver.obr;

import static org.apache.aries.application.utils.AppConstants.LOG_ENTRY;
import static org.apache.aries.application.utils.AppConstants.LOG_EXIT;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.aries.application.ApplicationMetadata;
import org.apache.aries.application.Content;
import org.apache.aries.application.InvalidAttributeException;
import org.apache.aries.application.VersionRange;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.BundleInfo;
import org.apache.aries.application.management.ResolveConstraint;
import org.apache.aries.application.management.ResolverException;
import org.apache.aries.application.management.spi.repository.PlatformRepository;
import org.apache.aries.application.management.spi.resolve.AriesApplicationResolver;
import org.apache.aries.application.modelling.ImportedBundle;
import org.apache.aries.application.modelling.ModelledResource;
import org.apache.aries.application.modelling.ModellingConstants;
import org.apache.aries.application.modelling.ModellingManager;
import org.apache.aries.application.modelling.utils.ModellingHelper;
import org.apache.aries.application.resolver.internal.MessageUtil;
import org.apache.aries.application.resolver.obr.impl.ApplicationResourceImpl;
import org.apache.aries.application.resolver.obr.impl.ModelledBundleResource;
import org.apache.aries.application.resolver.obr.impl.OBRBundleInfo;
import org.apache.aries.application.resolver.obr.impl.RepositoryGeneratorImpl;
import org.apache.aries.application.resolver.obr.impl.ResourceWrapper;
import org.apache.aries.application.utils.filesystem.IOUtils;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor.NameValueMap;
import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.DataModelHelper;
import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Rev$ $Date$
 */
public class OBRAriesResolver implements AriesApplicationResolver
{
  private static Logger log = LoggerFactory.getLogger(OBRAriesResolver.class);

  private final RepositoryAdmin repositoryAdmin;  
  private boolean returnOptionalResources = true;
  private PlatformRepository platformRepository;
  private ModellingManager modellingManager;
  private ModellingHelper modellingHelper;
  
  public void setModellingManager (ModellingManager m) { 
    modellingManager = m;
  }
  
  public void setModellingHelper (ModellingHelper mh) { 
    modellingHelper = mh;
  }
  
  public PlatformRepository getPlatformRepository()
  {
    return platformRepository;
  }

 
  
  public  RepositoryAdmin getRepositoryAdmin() {
    return this.repositoryAdmin;
  }
  public void setPlatformRepository(PlatformRepository platformRepository)
  {
    this.platformRepository = platformRepository;
  }

  public OBRAriesResolver(RepositoryAdmin repositoryAdmin)
  {
    this.repositoryAdmin = repositoryAdmin;
  }

  public void setReturnOptionalResources(boolean optional) 
  {
    this.returnOptionalResources = optional;
  }
  
  public boolean getReturnOptionalResources() 
  {
    return returnOptionalResources;
  }
  
  /**
   * Resolve a list of resources from the OBR bundle repositories by OBR
   * resolver.
   * 
   * @param appName - application name
   * @param appVersion - application version
   * @param byValueBundles - by value bundles
   * @param inputs - other constraints
   * @return a collection of modelled resources required by this application
   * @throws ResolverException
   */
  @Override
   public Collection<ModelledResource> resolve(String appName, String appVersion,
      Collection<ModelledResource> byValueBundles, Collection<Content> inputs) throws ResolverException
  {
     log.debug(LOG_ENTRY, "resolve", new Object[]{appName, appVersion,byValueBundles, inputs});
    Collection<ImportedBundle> importedBundles = toImportedBundle(inputs);
    DataModelHelper helper = repositoryAdmin.getHelper();

   
    Collection<ModelledResource> toReturn = new ArrayList<ModelledResource>();
    Repository appRepo;
    try {      
      ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
      RepositoryGeneratorImpl.generateRepository(repositoryAdmin, appName + "_" + appVersion, byValueBundles, bytesOut);
      appRepo = helper.readRepository(new InputStreamReader(new ByteArrayInputStream(bytesOut.toByteArray())));
    } catch (Exception e) {
      throw new ResolverException(e);
    } 
        
    List<Repository> resolveRepos = new ArrayList<Repository>();
    // add system repository
    resolveRepos.add(repositoryAdmin.getSystemRepository());
    // add local repository
    resolveRepos.add(getLocalRepository(repositoryAdmin));
    // add application repository
    resolveRepos.add(appRepo);
    

    
   // Need to refresh the repositories added to repository admin
    
    // add user-defined repositories
    Repository[] repos = repositoryAdmin.listRepositories();
    for (Repository r : repos) {
      resolveRepos.add(r);      
    }     
    Resolver obrResolver = repositoryAdmin.resolver(resolveRepos.toArray(new Repository[resolveRepos.size()]));
    addPlatformRepositories (obrResolver, appName);
    
    // add a resource describing the requirements of the application metadata.
    obrResolver.add(createApplicationResource( appName, appVersion, importedBundles));
    if (obrResolver.resolve()) {
      
      List<Resource> requiredResources = retrieveRequiredResources(obrResolver);

      if (requiredResources == null) {
        log.debug("resolver.getRequiredResources() returned null");
      } else {

        for (Resource r : requiredResources) {
          NameValueMap<String, String> attribs = new NameValueMap<String, String>();
          attribs.put(Constants.VERSION_ATTRIBUTE, "[" + r.getVersion() + ',' + r.getVersion()
              + "]");
          ModelledResource modelledResourceForThisMatch = null; 
          try { 
            modelledResourceForThisMatch = new ModelledBundleResource (r, modellingManager, modellingHelper);
          } catch (InvalidAttributeException iax) { 
            
            ResolverException re = new ResolverException("Internal error occurred: " + iax.toString());
            log.debug(LOG_EXIT, "resolve", re);
            
            throw re;
          }
          toReturn.add(modelledResourceForThisMatch);
        }
      }
      log.debug(LOG_EXIT, "resolve", toReturn); 
      return toReturn;
    } else {
      Reason[] reasons = obrResolver.getUnsatisfiedRequirements();
      // let's refine the list by removing the indirect unsatisfied bundles that are caused by unsatisfied packages or other bundles
      Map<String,Set<String>> refinedReqs = refineUnsatisfiedRequirements(obrResolver, reasons);
      StringBuffer reqList = new StringBuffer();
      List<String> unsatisfiedRequirements = new LinkedList<String>();

      for (Map.Entry<String, Set<String>> filterEntry : refinedReqs.entrySet()) {
        log.debug("unable to satisfied the filter , filter = " + filterEntry.getKey() + "required by "+filterEntry.getValue());
       
        String reason = extractConsumableMessageInfo(filterEntry.getKey(),filterEntry.getValue());

        reqList.append('\n');
        reqList.append(reason);
        unsatisfiedRequirements.add(reason);
      }

      ResolverException re = new ResolverException(MessageUtil.getMessage("RESOLVER_UNABLE_TO_RESOLVE", 
          new Object[] { appName, reqList }));
      re.setUnsatisfiedRequirements(unsatisfiedRequirements);
      log.debug(LOG_EXIT, "resolve", re);
      
      throw re;
    }
    
  }
 
  @Deprecated
  @Override
  public Set<BundleInfo> resolve(AriesApplication app, ResolveConstraint... constraints) throws ResolverException
  {
    log.trace("resolving {}", app);
    DataModelHelper helper = repositoryAdmin.getHelper();

    
    ApplicationMetadata appMeta = app.getApplicationMetadata();

    String appName = appMeta.getApplicationSymbolicName();
    Version appVersion = appMeta.getApplicationVersion();
    List<Content> appContent = appMeta.getApplicationContents();

    Collection<Content> useBundleContent = appMeta.getUseBundles();
    List<Content> contents = new ArrayList<Content>();
    contents.addAll(appContent);
    contents.addAll(useBundleContent);
    if ((constraints != null ) && (constraints.length > 0 )) {
      for (ResolveConstraint con: constraints) {
        contents.add(ManifestHeaderProcessor.parseContent(con.getBundleName(), con.getVersionRange().toString()));
      }
    }

    Repository appRepo;
    try {
      
      ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
      RepositoryGeneratorImpl.generateRepository(repositoryAdmin, appName + "_" + appVersion, toModelledResource(app.getBundleInfo()), bytesOut);
      
      appRepo = helper.readRepository(new InputStreamReader(new ByteArrayInputStream(bytesOut.toByteArray())));
    } catch (Exception e) {
      throw new ResolverException(e);
    } 
        
    List<Repository> resolveRepos = new ArrayList<Repository>();
    
    // add system repository
    resolveRepos.add(repositoryAdmin.getSystemRepository());
 // add local repository
    resolveRepos.add(getLocalRepository(repositoryAdmin));
    // add application repository
    resolveRepos.add(appRepo);
    
    // add user-defined repositories
    Repository[] repos = repositoryAdmin.listRepositories();
    for (Repository r : repos) {
      resolveRepos.add(r);      
    }    
    Resolver obrResolver = repositoryAdmin.resolver(resolveRepos.toArray(new Repository[resolveRepos.size()]));
    addPlatformRepositories (obrResolver, appName);
    
    // add a resource describing the requirements of the application metadata.
    obrResolver.add(createApplicationResource( appName, appVersion, contents));
    if (obrResolver.resolve()) {
     Set<BundleInfo> result = new HashSet<BundleInfo>();
      List<Resource> requiredResources = retrieveRequiredResources(obrResolver);
      
      for (Resource resource: requiredResources) {
        BundleInfo bundleInfo = toBundleInfo(resource, false);
        result.add(bundleInfo);
      }
      if (returnOptionalResources) {
        for (Resource resource: obrResolver.getOptionalResources()) {
          BundleInfo bundleInfo = toBundleInfo(resource, true);
          result.add(bundleInfo);
        }
      }
            
      return result;
    } else {
      Reason[] reasons = obrResolver.getUnsatisfiedRequirements();
      //refine the list by removing the indirect unsatisfied bundles that are caused by unsatisfied packages or other bundles
      Map<String,Set<String>> refinedReqs = refineUnsatisfiedRequirements(obrResolver, reasons);
      StringBuffer reqList = new StringBuffer();
      List<String> unsatisfiedRequirements = new LinkedList<String>();

      for (Map.Entry<String, Set<String>> filterEntry : refinedReqs.entrySet()) {
        log.debug("unable to satisfied the filter , filter = " + filterEntry.getKey() + "required by "+filterEntry.getValue());
       
        String reason = extractConsumableMessageInfo(filterEntry.getKey(),filterEntry.getValue());

        reqList.append('\n');
        reqList.append(reason);
        unsatisfiedRequirements.add(reason);
      }

      ResolverException re = new ResolverException(MessageUtil.getMessage("RESOLVER_UNABLE_TO_RESOLVE", 
          new Object[] { app.getApplicationMetadata().getApplicationName(), reqList }));
      re.setUnsatisfiedRequirements(unsatisfiedRequirements);
      log.debug(LOG_EXIT, "resolve", re);
      
      throw re;
    }
  }
  
  @Override
  public BundleInfo getBundleInfo(String bundleSymbolicName, Version bundleVersion)
  {
    Map<String, String> attribs = new HashMap<String, String>();
    attribs.put(Resource.VERSION, bundleVersion.toString());
    String filterString = ManifestHeaderProcessor.generateFilter(Resource.SYMBOLIC_NAME, bundleSymbolicName, attribs);
    Resource[] resources;
    try {
      resources = repositoryAdmin.discoverResources(filterString);
      if (resources != null && resources.length > 0) {
        return toBundleInfo(resources[0], false);
      } else {
        return null;
      }
    } catch (InvalidSyntaxException e) {
      log.error("Invalid filter", e);
      return null;
    }
  }

  /* A 'platform repository' describes capabilities of the target runtime environment
   * These should be added to the resolver without being listed as coming from a particular 
   * repository or bundle.  
   */
  private void addPlatformRepositories (Resolver obrResolver, String appName)
  { 
    DataModelHelper helper = repositoryAdmin.getHelper();
    if (platformRepository != null) {
      Collection<URI> uris = platformRepository.getPlatformRepositoryURLs();
      if ((uris != null) && (!uris.isEmpty())) {
        for (URI uri : uris) {
          InputStream is = null;
          try {
            is = uri.toURL().openStream();
            Reader repoReader = new InputStreamReader(is);
            Repository aPlatformRepo = helper.readRepository(repoReader);
            Resource resources[] = aPlatformRepo.getResources();
            for (Resource r : resources) { 
              Capability[] caps = r.getCapabilities();
              for (Capability c : caps) { 
                obrResolver.addGlobalCapability(c);
              }
            }
          } catch (Exception e) {
            // no a big problem
            log.error(MessageUtil.getMessage("RESOLVER_UNABLE_TO_READ_REPOSITORY_EXCEPTION", new Object[]{appName, uri}) );
          } finally { 
            IOUtils.close(is);
          }
        }
      }
    }
  }
  
  private Resource createApplicationResource( String appName, Version appVersion,
      List<Content> appContent)
  {
    return new ApplicationResourceImpl(appName, appVersion, appContent);
  }
  
  private Resource createApplicationResource( String appName, String appVersion,
      Collection<ImportedBundle> inputs)
  {
    return new ApplicationResourceImpl(appName, Version.parseVersion(appVersion), inputs);
  }
  
  private BundleInfo toBundleInfo(Resource resource, boolean optional) 
  {
    Map<String, String> directives = null;
    if (optional) {
      directives = new HashMap<String, String>();
      directives.put(Constants.RESOLUTION_DIRECTIVE, Constants.RESOLUTION_OPTIONAL);
    }
    

    return new OBRBundleInfo(resource.getSymbolicName(),
                             resource.getVersion(),
                             resource.getURI(),
                             null,
                             null,
                             null,
                             null,
                             null, 
                             null,
                             directives,
                             null);
    
  }
  
  /**
   * Get the list of resources returned by the resolver
   * @param resolver OBR resolver
   * @return a list of required resources
   */
  protected List<Resource> retrieveRequiredResources(Resolver resolver)
  {
    log.debug(LOG_ENTRY,"retrieveRequiredResources", resolver);
    Map<String, List<Resource>> resourcesByName = new HashMap<String, List<Resource>>();

    for (Resource r : resolver.getRequiredResources()) {
      resourcesByName.put(r.getSymbolicName(), mergeResource(resolver, r, resourcesByName.get(r
          .getSymbolicName())));
    }

    List<Resource> result = new ArrayList<Resource>();
    for (List<Resource> res : resourcesByName.values()) {
      result.addAll(res);
    }

    log.debug(LOG_EXIT,  "retrieveRequiredResources", result);
    return result;
  }

  
/**
 * Get rid of the redundant resources
 * @param resolver OBR resolver
 * @param r a resource
 * @param list similar resources
 * @return the list of minimum resources
 */
  protected List<Resource> mergeResource(Resolver resolver, Resource r,
      List<Resource> list)
  {
    log.debug(LOG_ENTRY, "mergeResource", new Object[]{resolver, r, list});
    
    if (list == null) {
      log.debug(LOG_EXIT, "mergeResource", Arrays.asList(r));
      return Arrays.asList(r);
    } else {
      List<Resource> result = new ArrayList<Resource>();

      for (Resource old : list) {
        boolean oldRedundant = satisfiesAll(r, resolver.getReason(old));
        boolean newRedundant = satisfiesAll(old, resolver.getReason(r));
        if (oldRedundant && newRedundant) {
          int comp = old.getVersion().compareTo(r.getVersion());
          oldRedundant = comp < 0;
          newRedundant = comp >= 0;
        }
        
        if (newRedundant) {
          log.debug(LOG_EXIT, "mergeResource", list);
          return list;
        } else if (oldRedundant) {
          // do nothing -> so don't add the old resource to the new list
        } else {
          result.add(old);
        }
      }

      result.add(r);

      log.debug(LOG_EXIT, "mergeResource", result);
      
      return result;
    }
  }
  protected boolean satisfiesAll(Resource res, Reason[] reasons)
  {
    log.debug(LOG_ENTRY,"satisfiesAll", new Object[] {res, Arrays.toString(reasons)});
    //Let's convert the reason to requirement
    List<Requirement> reqs = new ArrayList<Requirement>();
    for (Reason reason : reasons) {
      reqs.add(reason.getRequirement());
    }
    boolean result = true;
    
    outer: for (Requirement r : reqs) {
      boolean found = false;
      inner: for (Capability c : res.getCapabilities()) {
        if (r.isSatisfied(c)) {
          found = true;
          break inner;
        }
      }
      
      if (!!!found && !!!r.isOptional()) {
        result = false;
        break outer;
      }
    }
    log.debug(LOG_EXIT, "satisfiesAll", result);
    return result;
  }

  private static final Set<String> SPECIAL_FILTER_ATTRS = Collections
  .unmodifiableSet(new HashSet<String>(Arrays.asList(ModellingConstants.OBR_PACKAGE,
      ModellingConstants.OBR_SYMBOLIC_NAME, ModellingConstants.OBR_SERVICE, Constants.VERSION_ATTRIBUTE)));

  /**
   * Turn a requirement into a human readable String for debug.
   * @param filter The filter that is failing
   * @param bundlesFailing For problems with a bundle, the set of bundles that have a problem
   * @return human readable form
   */
  private String extractConsumableMessageInfo(String filter, Set<String> bundlesFailing)
  {
    log.debug(LOG_ENTRY, "extractConsumableMessageInfo", new Object[] {filter, bundlesFailing});
    
    Map<String, String> attrs = ManifestHeaderProcessor.parseFilter(filter);
    Map<String, String> customAttrs = new HashMap<String, String>();
    for (Map.Entry<String, String> e : attrs.entrySet()) {
      if (!SPECIAL_FILTER_ATTRS.contains(e.getKey())) {
        customAttrs.put(e.getKey(), e.getValue());
      }
    }

    StringBuilder msgKey = new StringBuilder();
    List<Object> inserts = new ArrayList<Object>();

    boolean unknownType = false;
    if (attrs.containsKey(ModellingConstants.OBR_PACKAGE)) {
      msgKey.append("RESOLVER_UNABLE_TO_RESOLVE_PACKAGE");
      inserts.add(attrs.get(ModellingConstants.OBR_PACKAGE));
    } else if (attrs.containsKey(ModellingConstants.OBR_SYMBOLIC_NAME)) {
      msgKey.append("RESOLVER_UNABLE_TO_RESOLVE_BUNDLE");
      inserts.add(attrs.get(ModellingConstants.OBR_SYMBOLIC_NAME));
    } else if (attrs.containsKey(ModellingConstants.OBR_SERVICE)) {
      msgKey.append("RESOLVER_UNABLE_TO_RESOLVE_SERVICE");
      //No insert for service name as the name must be "*" to match any Service capability
    } else {
      unknownType = true;
      msgKey.append("RESOLVER_UNABLE_TO_RESOLVE_FILTER");
      inserts.add(filter);
    }

    if (!unknownType && !customAttrs.isEmpty()) {
      inserts.add(customAttrs);    
    }
    if (bundlesFailing != null && bundlesFailing.size() != 0) {
      msgKey.append("_REQUIRED_BY_BUNDLE");
      if (bundlesFailing.size() == 1)
        inserts.add(bundlesFailing.iterator().next()); // Just take the string if there's only one of them
      else
        inserts.add(bundlesFailing.toString()); // Add the whole set if there isn't exactly one
    }
    if (!unknownType && !customAttrs.isEmpty()) {
      msgKey.append("_WITH_ATTRS");
    }

    if (!unknownType && attrs.containsKey(Constants.VERSION_ATTRIBUTE)) {
      msgKey.append("_WITH_VERSION");
      VersionRange vr = ManifestHeaderProcessor.parseVersionRange(attrs
          .get(Constants.VERSION_ATTRIBUTE));
      inserts.add(vr.getMinimumVersion());

      if (!!!vr.isExactVersion()) {
        msgKey.append(vr.isMinimumExclusive() ? "_LOWEX" : "_LOW");
        if (vr.getMaximumVersion() != null) {
          msgKey.append(vr.isMaximumExclusive() ? "_UPEX" : "_UP");
          inserts.add(vr.getMaximumVersion());
        }
      }
    }

    String msgKeyStr = msgKey.toString();
    
    String msg = MessageUtil.getMessage(msgKeyStr, inserts.toArray());

    log.debug(LOG_EXIT, "extractConsumableMessageInfo", msg);
    
    return msg;
  }
  
  /**
   * Refine the unsatisfied requirements ready for later human comsumption
   * 
   * @param resolver The resolver to be used to refine the requirements
   * @param reasons The reasons
   * @return A map of the unsatifiedRequirement to the set of bundles that have that requirement unsatisfied (values associated with the keys can be null) 
   */
  private Map<String,Set<String>> refineUnsatisfiedRequirements(Resolver resolver, Reason[] reasons) {
    log.debug(LOG_ENTRY, "refineUnsatisfiedRequirements", new Object[]{resolver, Arrays.toString(reasons)});
    
    Map<Requirement,Set<String>> req_resources = new HashMap<Requirement,Set<String>>();
    // add the reasons to the map, use the requirement as the key, the resources required the requirement as the values
    Set<Resource> resources = new HashSet<Resource>();
    for (Reason reason: reasons) {
      resources.add(reason.getResource());
      Requirement key = reason.getRequirement();
      String value = reason.getResource().getSymbolicName()+"_" + reason.getResource().getVersion().toString();
      Set<String> values = req_resources.get(key);
      if (values == null) {
        values = new HashSet<String>();
      }
      values.add(value);
      req_resources.put(key, values);
    }
    
    // remove the requirements that can be satisifed by the resources. It is listed because the resources are not satisfied by other requirements.
    // For an instance, the unsatisfied reasons are [package a, required by bundle aa], [package b, required by bundle bb] and [package c, required by bundle cc], 
    // If the bundle aa exports the package a and c. In our error message, we only want to display package a is needed by bundle aa.
    // Go through each requirement and find out whether the requirement can be satisfied by the reasons.
    Set<Capability> caps = new HashSet<Capability>();
    for (Resource res : resources) {
      if ((res !=null) && (res.getCapabilities() != null)) {
        
      List<Capability> capList = Arrays.asList(res.getCapabilities());
      if (capList != null) {
      caps.addAll(capList);
      }
      }
    }
    Iterator<Map.Entry<Requirement, Set<String>>> iterator = req_resources.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<Requirement, Set<String>> entry = iterator.next();
      Requirement req = entry.getKey();
      for (Capability cap :caps) {
        if (req.isSatisfied(cap)){
          // remove the key from the map
          iterator.remove();
        }
      }
    }
    //Now the map only contains the necessary missing requirements
    
      Map<String,Set<String>> result = new HashMap<String, Set<String>>();
      for (Map.Entry<Requirement, Set<String>> req_res : req_resources.entrySet()) {
        result.put(req_res.getKey().getFilter(), req_res.getValue());
      }
      log.debug(LOG_EXIT, "refineUnsatisfiedRequirements", new Object[]{result});
      
    return result;
    }
  
 
   
   private Collection<ImportedBundle> toImportedBundle(Collection<Content> content) throws ResolverException
   {
     log.debug(LOG_ENTRY, "toImportedBundle", content);

     List<ImportedBundle> result = new ArrayList<ImportedBundle>();
     for (Content c : content) {
       try {
       result.add(modellingManager.getImportedBundle(c.getContentName(), c.getVersion().toString()));
       } catch (InvalidAttributeException iae) {
         throw new ResolverException(iae);
       }
     }
     log.debug(LOG_EXIT, "toImportedBundle", result);
     return result;
   }
   
   private Collection<ModelledResource> toModelledResource(Collection<BundleInfo> bundleInfos) throws ResolverException{

     Collection<ModelledResource> result = new ArrayList<ModelledResource>();
    
     if ((bundleInfos != null) && (!!!bundleInfos.isEmpty())) {
       for (BundleInfo bi : bundleInfos) {
         try {
         result.add(modellingManager.getModelledResource(null, bi, null, null));
         } catch (InvalidAttributeException iae) {
           throw new ResolverException(iae);
         }
       }
     }
     return result;
   }
  
   private Repository getLocalRepository(RepositoryAdmin admin) 
   {
       Repository localRepository = repositoryAdmin.getLocalRepository();
       
       Resource[] resources = localRepository.getResources();

       Resource[] newResources = new Resource[resources.length];
       for (int i = 0; i < resources.length; i++) {
           newResources[i] = new ResourceWrapper(resources[i]); 
       }
       
       return repositoryAdmin.getHelper().repository(newResources);
   }
   


}
