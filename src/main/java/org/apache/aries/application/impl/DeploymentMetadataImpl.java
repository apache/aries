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

package org.apache.aries.application.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.aries.application.ApplicationMetadata;
import org.apache.aries.application.Content;
import org.apache.aries.application.DeploymentContent;
import org.apache.aries.application.DeploymentMetadata;
import org.apache.aries.application.InvalidAttributeException;
import org.apache.aries.application.VersionRange;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.BundleInfo;
import org.apache.aries.application.management.ResolverException;
import org.apache.aries.application.utils.AppConstants;
import org.apache.aries.application.utils.FilterUtils;
import org.apache.aries.application.utils.manifest.ManifestProcessor;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;

public class DeploymentMetadataImpl implements DeploymentMetadata {
  private ApplicationMetadata _applicationMetadata;
  private List<DeploymentContent> _deploymentContent = new ArrayList<DeploymentContent>();
  private List<DeploymentContent> _provisionSharedContent = new ArrayList<DeploymentContent>();
  private List<DeploymentContent> _deployedUseBundleContent = new ArrayList<DeploymentContent>();
  
  private Set<Content> _deploymentImportPackage = new HashSet<Content>();
  private Map<String, String> _deploymentCustomEntries = new HashMap<String, String>();  
  private Map<String, String> _deploymentEntries = new HashMap<String, String>();
  private Collection<Filter> _deployedImportService = new ArrayList<Filter>();
  
  public DeploymentMetadataImpl (AriesApplication app, Set<BundleInfo> bundlesRequired) throws ResolverException
  {
    _applicationMetadata = app.getApplicationMetadata();
    _deploymentContent = new ArrayList<DeploymentContent>();
    _provisionSharedContent = new ArrayList<DeploymentContent>();
    _deployedUseBundleContent = new ArrayList<DeploymentContent>();
    
    Map<String, VersionRange> appContent = new HashMap<String, VersionRange>();
    
    for (Content c : app.getApplicationMetadata().getApplicationContents()) {
      appContent.put(c.getContentName(), c.getVersion());
    }
    
    Map<String, VersionRange> useBundles = new HashMap<String, VersionRange>();
    for (Content c : app.getApplicationMetadata().getUseBundles()) {
      useBundles.put(c.getContentName(), c.getVersion());
    }
    
    for (BundleInfo info : bundlesRequired) {
      
      VersionRange appContentRange = appContent.get(info.getSymbolicName());
      VersionRange useBundleRange = useBundles.get(info.getSymbolicName());
      DeploymentContent dp = new DeploymentContentImpl(info.getSymbolicName(), info.getVersion());
      
      if ((appContentRange == null) && (useBundleRange == null)){
        _provisionSharedContent.add(dp);
      } else if (appContentRange.matches(info.getVersion())) {
        _deploymentContent.add(dp);
      }  else if (useBundleRange.matches(info.getVersion())) {
        _deployedUseBundleContent.add(dp);
      }
      else {
        throw new ResolverException("Bundle " + info.getSymbolicName() + " at version " + info.getVersion() + " is not in the range " + appContentRange + " or " + useBundleRange);
      }
    }
  }
  
  /**
   * Construct a DeploymentMetadata from Manifest
   * @param src
   * @throws IOException
   */
  public DeploymentMetadataImpl(Manifest mf) throws InvalidAttributeException{ 
    _applicationMetadata = new ApplicationMetadataImpl (mf);

    Attributes attributes = mf.getMainAttributes();
      
    parseDeploymentContent(attributes.getValue(AppConstants.DEPLOYMENT_CONTENT), _deploymentContent);
    parseDeploymentContent(attributes.getValue(AppConstants.DEPLOYMENT_PROVISION_BUNDLE), _provisionSharedContent);
    parseDeploymentContent(attributes.getValue(AppConstants.DEPLOYMENT_USE_BUNDLE), _deployedUseBundleContent);
    parseContent(attributes.getValue(AppConstants.DEPLOYMENT_IMPORT_PACKAGES), _deploymentImportPackage);
    
    _deployedImportService = getFilters(attributes.getValue(AppConstants.DEPLOYMENTSERVICE_IMPORT));
    _deploymentCustomEntries = getCustomEntries(attributes);
    _deploymentEntries = getEntries(attributes);
  }
  
  public DeploymentMetadataImpl(Map<String, String> map) throws InvalidAttributeException{ 

    Attributes attributes = new Attributes();
    if (map != null) {
    for (Map.Entry<String, String> entry : map.entrySet()) {
      attributes.putValue(entry.getKey(), entry.getValue());
    }
    }
    parseDeploymentContent(map.get(AppConstants.DEPLOYMENT_CONTENT), _deploymentContent);
    parseDeploymentContent(map.get(AppConstants.DEPLOYMENT_PROVISION_BUNDLE), _provisionSharedContent);
    parseDeploymentContent(map.get(AppConstants.DEPLOYMENT_USE_BUNDLE), _deployedUseBundleContent);
    parseContent(attributes.getValue(AppConstants.DEPLOYMENT_IMPORT_PACKAGES), _deploymentImportPackage);
    _deployedImportService = getFilters(attributes.getValue(AppConstants.DEPLOYMENTSERVICE_IMPORT));
    _deploymentCustomEntries = getCustomEntries(attributes);
    _deploymentEntries = getEntries(attributes);
    
  }

  private Collection<Attributes.Name> getDeploymentStandardHeaders() {
    Collection<Attributes.Name> standardKeys = new HashSet<Attributes.Name> ();
    standardKeys.add(new Attributes.Name(AppConstants.APPLICATION_MANIFEST_VERSION));
    standardKeys.add(new Attributes.Name(AppConstants.DEPLOYMENT_CONTENT));
    standardKeys.add(new Attributes.Name(AppConstants.DEPLOYMENT_PROVISION_BUNDLE));
    standardKeys.add(new Attributes.Name(AppConstants.DEPLOYMENT_USE_BUNDLE));
    standardKeys.add(new Attributes.Name(AppConstants.DEPLOYMENT_IMPORT_PACKAGES));
    standardKeys.add(new Attributes.Name(AppConstants.DEPLOYMENTSERVICE_IMPORT));
    standardKeys.add(new Attributes.Name(AppConstants.APPLICATION_SYMBOLIC_NAME));
    standardKeys.add(new Attributes.Name(AppConstants.APPLICATION_VERSION));
    return standardKeys;
  }

  private Collection<String> getCustomHeaders(Attributes attrs) {
    
    Collection<String> customKeys = new HashSet<String>();
    Collection<Attributes.Name> standardKeys = getDeploymentStandardHeaders();
    if ((attrs != null) && (!!!attrs.isEmpty())) {
     Set<Object> keys = attrs.keySet();
     
     if ((keys != null) && (!!!keys.isEmpty())) {
       for (Object eachKey : keys) {
         String key = eachKey.toString();
         customKeys.add(key);
       }
       
         customKeys.removeAll(standardKeys);
       
     }
    }
    return customKeys;
  }
  
  private String getContentsAsString (Collection<Content> contents) {
    StringBuilder builder = new StringBuilder();
    boolean beginning = true;
    for (Content c : contents) {
      if (!!!beginning) {
        builder.append(",");
      }
      builder.append(c);
      beginning = false;
      
    }
    return builder.toString();
  }
  
  public List<DeploymentContent> getApplicationDeploymentContents() {
    return Collections.unmodifiableList(_deploymentContent);
  }
  
  public List<DeploymentContent> getApplicationProvisionBundles() {
    return Collections.unmodifiableList(_provisionSharedContent);
  }

  public ApplicationMetadata getApplicationMetadata() {
    return _applicationMetadata;
  }

  public String getApplicationSymbolicName() {
    return _applicationMetadata.getApplicationSymbolicName();
  }

  public Version getApplicationVersion() {
    return _applicationMetadata.getApplicationVersion();
  }


  public void store(File f) throws FileNotFoundException, IOException{
    FileOutputStream fos = new FileOutputStream (f);
    store(fos);
    fos.close();
  }

  public void store(OutputStream out) throws IOException {
    // We weren't built from a Manifest, so construct one. 
    Manifest mf = new Manifest();
    Attributes attributes = mf.getMainAttributes();
    attributes.putValue(Attributes.Name.MANIFEST_VERSION.toString(), AppConstants.MANIFEST_VERSION);
    attributes.putValue(AppConstants.APPLICATION_VERSION, getApplicationVersion().toString());
    attributes.putValue(AppConstants.APPLICATION_SYMBOLIC_NAME, getApplicationSymbolicName());
    if ((_deploymentContent != null) && (!_deploymentContent.isEmpty())) {
      attributes.putValue(AppConstants.DEPLOYMENT_CONTENT, getDeploymentContentsAsString(_deploymentContent));
    }
    if ((_provisionSharedContent != null) && (!_provisionSharedContent.isEmpty())) {
      attributes.putValue(AppConstants.DEPLOYMENT_PROVISION_BUNDLE, getDeploymentContentsAsString(_provisionSharedContent));
    }
    if ((_deployedUseBundleContent != null) && (!_deployedUseBundleContent.isEmpty())) {
      attributes.putValue(AppConstants.DEPLOYMENT_USE_BUNDLE, getDeploymentContentsAsString(_deployedUseBundleContent));
    }
    if ((_deploymentImportPackage != null) && (!_deploymentImportPackage.isEmpty())) {
      attributes.putValue(AppConstants.DEPLOYMENT_IMPORT_PACKAGES, getContentsAsString(_deploymentImportPackage));
    }
    if ((_deployedImportService != null) && (!!!_deployedImportService.isEmpty())) {
      attributes.putValue(AppConstants.DEPLOYMENTSERVICE_IMPORT, convertFiltersToString(_deployedImportService, ",") );
    }
    // let's write out the custom headers
    if ((_deploymentCustomEntries != null) && (_deploymentCustomEntries.isEmpty())) {
      for (Map.Entry<String, String> customEntry : _deploymentCustomEntries.entrySet()) {
        attributes.putValue(customEntry.getKey(), customEntry.getValue());
      }
    }
    mf.write(out);
  }
  
  
  
  private String getDeploymentContentsAsString (List<DeploymentContent> content) { 
    StringBuilder builder = new StringBuilder();
    for (DeploymentContent dc : content) {
      builder.append(dc.getContentName());
      builder.append(';' + AppConstants.DEPLOYMENT_BUNDLE_VERSION + "=");
      builder.append(dc.getExactVersion());
      builder.append(",");
    }
    if (builder.length() > 0) { 
      builder.deleteCharAt(builder.length() - 1);
    }
    return builder.toString();
  }

  private void parseDeploymentContent(String content, List<DeploymentContent> contents)
  {
    List<String> pcList = ManifestProcessor.split(content, ",");
    for (String s : pcList) {
      contents.add(new DeploymentContentImpl(s));
    }
  }

  private void parseContent(String content, Collection<Content> contents)
  {
    List<String> pcList = ManifestProcessor.split(content, ",");
    for (String s : pcList) {
      contents.add(new ContentImpl(s));
    }
  }
  

  public List<DeploymentContent> getDeployedUseBundle()
  {
    return Collections.unmodifiableList(_deployedUseBundleContent);
  }

  public Set<Content> getImportPackage()
  {
    return Collections.unmodifiableSet(_deploymentImportPackage);
  }

  public Collection<Filter> getDeployedServiceImport() throws InvalidAttributeException
  {
    return Collections.unmodifiableCollection(_deployedImportService);
  }

  public Map<String, String> getHeaders()
  {
    return Collections.unmodifiableMap(_deploymentEntries);
  }
  
  private Map<String, String> getEntries(Attributes attrs) {
    Map<String, String> entries = new HashMap<String, String>();
    if ((attrs != null) && (!attrs.isEmpty())) {
      Set<Object> keys = attrs.keySet();
      for (Object key : keys) {
        entries.put(key.toString(),  attrs.getValue((Attributes.Name)key));
      }
    }
    return entries;
  }
 

  private Map<String, String> getCustomEntries(Attributes attrs) {
    Map<String, String> customEntry = new HashMap<String, String> ();
    Collection<String> customHeaders = getCustomHeaders(attrs);
    if ((customHeaders != null) && (customHeaders.isEmpty())) {
      for (String customHeader : customHeaders)
        customEntry.put(customHeader, attrs.getValue(customHeader));
      
    }
    return customEntry;
    
  }
  
   private Collection<Filter> getFilters(String filterString) throws InvalidAttributeException{
     Collection<Filter> filters = new ArrayList<Filter>();
     List<String> fs = ManifestProcessor.split(filterString, ",");
     if ((fs != null) && (!!!fs.isEmpty())) {
       for (String filter : fs) {
         try {
         filters.add(FrameworkUtil.createFilter(FilterUtils.removeMandatoryFilterToken(filter)));
         } catch (InvalidSyntaxException ise) {
           InvalidAttributeException iae = new InvalidAttributeException(ise);
           throw iae;
         }
       }
     }
     return filters;
   }
  
   private  String convertFiltersToString(Collection<Filter> contents, String separator) {
     StringBuilder newContent = new StringBuilder();
     if ((contents != null) && (!!!contents.isEmpty())) {
       boolean beginning = true;
       for (Filter content: contents) {
         if (beginning)
           newContent.append(separator);
         newContent.append(content.toString());
         beginning = false;
       }
     }
     return newContent.toString();
   }
   
   
}