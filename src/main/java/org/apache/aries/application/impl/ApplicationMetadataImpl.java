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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.aries.application.ApplicationMetadata;
import org.apache.aries.application.Content;
import org.apache.aries.application.ServiceDeclaration;
import org.apache.aries.application.utils.AppConstants;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor.NameValueMap;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of ApplicationMetadata and DeploymentMetadata
 *
 */
public final class ApplicationMetadataImpl implements ApplicationMetadata
{
  private static final Logger _logger = LoggerFactory.getLogger("org.apache.aries.application.management.impl");
  private String appSymbolicName;
  private Version appVersion;
  private String appName;
  private String appScope;
  private List<Content> appContents;
  private List<ServiceDeclaration> importServices;
  private List<ServiceDeclaration> exportServices;
  private Manifest manifest;
  private List<Content> useBundle;
  /**
   * create the applicationMetadata from appManifest
   * @param appManifest   the Application.mf manifest
   */
  public ApplicationMetadataImpl(Manifest appManifest) {

    this.appContents = new ArrayList<Content>();
    this.useBundle = new ArrayList<Content>();    
    this.importServices = new ArrayList<ServiceDeclaration>();
    this.exportServices = new ArrayList<ServiceDeclaration>();    
    setup(appManifest);
    
    // As of 7 Jan 2010 we have no setter methods. Hence it's currently 
    // fine to keep a copy of appManifest, and to use it in the store()
    // method.
    manifest = appManifest;
  }
  
  /**
   * setup the application metadata from the appManifest
   * @param appManifest     application.mf manifest
   */
  private void setup(Manifest appManifest) 
  {
    Map<String, String> appMap = readManifestIntoMap(appManifest);
    
    // configure the appSymbolicName and appVersion
    this.appSymbolicName = appMap.get(AppConstants.APPLICATION_SYMBOLIC_NAME);
    this.appVersion = new Version(appMap.get(AppConstants.APPLICATION_VERSION));
    this.appName = appMap.get(AppConstants.APPLICATION_NAME);
    this.appScope = this.appSymbolicName + "_" + this.appVersion.toString();
    
    if (this.appSymbolicName == null || this.appVersion == null) {
      throw new IllegalArgumentException("Failed to create ApplicationMetadataImpl object from Manifest " + appManifest);
    }
    
    // configure appContents
 // use parseImportString as we don't allow appContents to be duplicate
    String applicationContents = appMap.get(AppConstants.APPLICATION_CONTENT);
    Map<String, NameValueMap<String, String>> appContentsMap = ManifestHeaderProcessor.parseImportString(applicationContents);
    for (Map.Entry<String, NameValueMap<String, String>> e : appContentsMap.entrySet()) {
      this.appContents.add(new ContentImpl(e.getKey(), e.getValue()));
    }
   
    String useBundleStr = appMap.get(AppConstants.APPLICATION_USE_BUNDLE);
    if (useBundleStr != null) {
      Map<String, NameValueMap<String, String>> useBundleMap = ManifestHeaderProcessor.parseImportString(useBundleStr);
    for (Map.Entry<String, NameValueMap<String, String>> e : useBundleMap.entrySet()) {
        this.useBundle.add(new ContentImpl(e.getKey(), e.getValue()));
      }
    }
    
    String allServiceImports = appMap.get(AppConstants.APPLICATION_IMPORT_SERVICE);
    List<String> serviceImports = ManifestHeaderProcessor.split(allServiceImports, ",");
    for (String s: serviceImports) { 
      try { 
        ServiceDeclaration dec = new ServiceDeclarationImpl(s);
        importServices.add(dec);
      } catch (InvalidSyntaxException ise) { 
        _logger.warn("APPUTILS0013E", new Object[] {s, appSymbolicName});
      }
    }
    
    String allServiceExports = appMap.get(AppConstants.APPLICATION_EXPORT_SERVICE);
    List<String> serviceExports = ManifestHeaderProcessor.split(allServiceExports, ",");
    for (String s: serviceExports) { 
      try { 
        ServiceDeclaration dec = new ServiceDeclarationImpl(s);
        exportServices.add(dec);
      } catch (InvalidSyntaxException ise) { 
        _logger.warn("APPUTILS0014E", new Object[] {s, appSymbolicName});
      }
    }
    
  }
  
  /**
   * Reads a manifest's main attributes into a String->String map.
   * <p>
   * Will always return a map, empty if the manifest had no attributes.
   * 
   * @param mf The manifest to read.
   * @return Map of manifest main attributes.
   */
  private Map<String, String> readManifestIntoMap(Manifest mf){   
    HashMap<String, String> props = new HashMap<String, String>();
    
    Attributes mainAttrs = mf.getMainAttributes();
    if (mainAttrs!=null){
      Set<Entry<Object, Object>> attributeSet =  mainAttrs.entrySet(); 
      if (attributeSet != null){
        // Copy all the manifest headers across. The entry set should be a set of
        // Name to String mappings, by calling String.valueOf we do the conversion
        // to a string and we do not NPE.
        for (Map.Entry<Object, Object> entry : attributeSet) {
          props.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
      }    
    }
       
    return props;
  }  
    
  public List<Content> getApplicationContents()
  {
    return Collections.unmodifiableList(this.appContents);
  }

  public List<ServiceDeclaration> getApplicationExportServices()
  {
    return Collections.unmodifiableList(this.exportServices);
  }

  public List<ServiceDeclaration> getApplicationImportServices()
  {
    return Collections.unmodifiableList(this.importServices);
  }

  public String getApplicationSymbolicName()
  {
    return this.appSymbolicName;
  }

  public Version getApplicationVersion()
  {
    return this.appVersion;
  }

  public String getApplicationName() 
  {
    return this.appName;
  }
  
  public String getApplicationScope() 
  {
    return appScope;
  }
  
  public boolean equals(Object other)
  {
    if (other == this) return true;
    if (other == null) return false;
    if (other instanceof ApplicationMetadataImpl) {
      return appScope.equals(((ApplicationMetadataImpl)other).appScope);
    }
    
    return false;
  }
  
  public int hashCode()
  {
    return appScope.hashCode();
  }

  public void store(File f) throws IOException {
    FileOutputStream fos = new FileOutputStream (f);
    store(fos);
    fos.close();
  }

  public void store(OutputStream out) throws IOException {
    if (manifest != null) {
      Attributes att = manifest.getMainAttributes();
      if ((att.getValue(Attributes.Name.MANIFEST_VERSION.toString())) == null) {
        att.putValue(Attributes.Name.MANIFEST_VERSION.toString(), AppConstants.MANIFEST_VERSION);
      }
      manifest.write(out);
    }
  }

  public Collection<Content> getUseBundles()
  {
    return this.useBundle;
  }
}