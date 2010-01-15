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
package org.apache.aries.application.utils.manifest;

import java.io.File;
import java.util.Map;
import java.util.jar.Manifest;

import org.apache.aries.application.filesystem.IDirectory;
import org.apache.aries.application.filesystem.IFile;
import org.apache.aries.application.utils.AppConstants;
import org.apache.aries.application.utils.filesystem.FileSystem;
import org.osgi.framework.Version;

public class ManifestDefaultsInjector
{
  /**
   * Quick adapter to update a Manifest, using content of a Zip File.
   * <p>
   * This is really a wrapper of updateManifest(Manifest,String,IDirectory), with the
   * IDirectory being being created from the Zip File. This method avoids other Bundles
   * requiring IDirectory solely for calling updateManifest.
   * <p>
   * @param mf Manifest to be updated
   * @param appName The name to use for this app, if the name contains 
   * a '_' char then the portion after the '_' is used, if possible, as 
   * the application version.
   * @param zip Content to use for application.
   * @return true if manifest modified, false otherwise.
   */
  public static boolean updateManifest(Manifest mf, String appName, File zip){
    IDirectory appPathIDir = FileSystem.getFSRoot(zip);
    boolean result = updateManifest(mf, appName, appPathIDir);
    return result;
  }
  
  /**
   * Tests the supplied manifest for the presence of expected 
   * attributes, and where missing, adds them, and defaults 
   * their values appropriately.
   * 
   * @param mf The manifest to test & update if needed.
   * @param appName The name to use for this app, if the name contains 
   * a '_' char then the portion after the '_' is used, if possible, as 
   * the application version.
   * @param appDir The IDirectory to scan to build application content
   * property
   * @return true if manifest modified, false otherwise.
   */
  public static boolean updateManifest(Manifest mf, String appName, IDirectory appDir){ 
    Map<String, String> props = ManifestProcessor.readManifestIntoMap(mf);
    String extracted[] = extractAppNameAndVersionFromNameIfPossible(appName);
    String name = extracted[0];
    String version = extracted[1];

    boolean updated = false;
    updated |= defaultAppSymbolicName(mf, props, name);
    updated |= defaultAppName(mf, props, name);
    updated |= defaultVersion(mf, props, version);
    updated |= defaultAppScope(mf, props, name, version);
    updated |= defaultAppContent(mf, props, appDir);
    
    return updated;
  }
  
  /**
   * Takes a compound name_version string, and returns the Name & Version information. 
   * <p>
   * @param name Contains name data related to this app. Expected format is   name_version  
   * @return Array of String, index 0 is appName, index 1 is Version. 
   * <br> Name will be the appname retrieved from the 'name' argument, Version will be the 
   * version if found and valid, otherwise will be defaulted.
   */
  private static String[] extractAppNameAndVersionFromNameIfPossible(String name){
    String retval[] = new String[2];
    String appName = name;
    String defaultedVersion;

    int index = name.indexOf('_');

    if (index != -1) {
      appName = name.substring(0, index);
      defaultedVersion = name.substring(index + 1);

      try {
        new Version(defaultedVersion);
      } catch (IllegalArgumentException e) {
        // this is not an error condition
        defaultedVersion = AppConstants.DEFAULT_VERSION;
      }
    } else {
      defaultedVersion = AppConstants.DEFAULT_VERSION;
    }

    retval[0] = appName;
    retval[1] = defaultedVersion;
    
    return retval;  
  }
  
  /**
   * Sets the app symbolic name into the manifest, if not already present.
   * 
   * @param mf manifest to update
   * @param props parsed manifest used to test if already present. 
   * @param appName used for name if missing
   * @return true if manifest is modified, false otherwise.
   */
  private static boolean defaultAppSymbolicName(Manifest mf, Map<String, String> props, String appName){
    boolean updated = false;
    if (!props.containsKey(AppConstants.APPLICATION_SYMBOLIC_NAME)) {
      mf.getMainAttributes().putValue(AppConstants.APPLICATION_SYMBOLIC_NAME, appName);
      updated = true;
    }
    return updated;    
  }
  
  /**
   * Sets the app name into the manifest, if not already present.
   * 
   * @param mf manifest to update
   * @param props parsed manifest used to test if already present. 
   * @param appName used for name if missing
   * @return true if manifest is modified, false otherwise.
   */  
  private static boolean defaultAppName(Manifest mf, Map<String, String> props, String appName){
    boolean updated = false;
    if (!props.containsKey(AppConstants.APPLICATION_NAME)) {
      mf.getMainAttributes().putValue(AppConstants.APPLICATION_NAME, appName);
      updated = true;
    }
    return updated;    
  }
    
  /**
   * Sets the app version into the manifest, if not already present.
   * 
   * @param mf manifest to update
   * @param props parsed manifest used to test if already present. 
   * @param appVersion used for version if missing
   * @return true if manifest is modified, false otherwise.
   */  
  private static boolean defaultVersion(Manifest mf, Map<String, String> props, String appVersion){
    boolean updated = false;
    if (!props.containsKey(AppConstants.APPLICATION_VERSION)) {
      mf.getMainAttributes().putValue(AppConstants.APPLICATION_VERSION, appVersion);
      updated = true;
    }
    return updated;
  }
  
  /**
   * Sets the app scope into the manifest, if not already present.
   * 
   * @param mf manifest to update
   * @param props parsed manifest used to test if already present. 
   * @param name used to build appScope if app symbolic name not set.
   * @param version used to build appScope if app version missing.
   * @return true if manifest is modified, false otherwise.
   */   
  private static boolean defaultAppScope(Manifest mf, Map<String, String> props, String name, String version){
    boolean updated = false;
    if (!props.containsKey(AppConstants.APPLICATION_SCOPE)) {

      String appSymbolicName;
      if (props.containsKey(AppConstants.APPLICATION_SYMBOLIC_NAME)) {
        appSymbolicName = props.get(AppConstants.APPLICATION_SYMBOLIC_NAME);
      } else {
        appSymbolicName = name;
      }

      String appVersion;
      if (props.containsKey(AppConstants.APPLICATION_VERSION)) {
        appVersion = props.get(AppConstants.APPLICATION_VERSION);
      } else {
        appVersion = version;
      }

      String appScope = appSymbolicName + '_' + appVersion;
      mf.getMainAttributes().putValue(AppConstants.APPLICATION_SCOPE, appScope);
      updated = true;
    }
    return updated;
  }
  
  /**
   * Sets the app content into the manifest, if not already present.
   * <p>
   * This method will NOT set the appcontent if it is unable to build it.
   * This is important, as the absence of appcontent is used by some callers
   * to test if a manifest contains all required content.
   * 
   * @param mf manifest to update
   * @param props parsed manifest used to test if already present. 
   * @param appDir used to build app content if missing.
   * @return true if manifest is modified, false otherwise.
   */    
  private static boolean defaultAppContent(Manifest mf, Map<String, String> props, IDirectory appDir){
    boolean updated = false;
    if (!props.containsKey(AppConstants.APPLICATION_CONTENT)) {
      String appContent = calculateAppContent(appDir);
      if (appContent != null) {
        mf.getMainAttributes().putValue(AppConstants.APPLICATION_CONTENT, appContent);
        updated = true;
      }
    }
    return updated;    
  }
  
  /**
   * Processes an IDirectory to find targets that require adding to the application content attrib.
   * 
   * @param appDir The IDirectory to scan
   * @return AppContent string, or null if no content was found.
   */
  private static String calculateAppContent(IDirectory appDir){
    StringBuilder builder = new StringBuilder();
    for (IFile file : appDir) {
      processPossibleBundle(file, builder);
    }
    String returnVal = null;
    if (builder.length() > 0) {
      builder.deleteCharAt(builder.length() - 1);
      returnVal = builder.toString();
    }
    return returnVal;
  }
  
  /**
   * This method works out if the given IFile represents an OSGi bundle and if
   * it is then we append a matching rule to the String builder.
   * 
   * @param file    to file to check.
   * @param builder the builder to append to.
   */
  private static void processPossibleBundle(IFile file, StringBuilder builder)
  {
    if (file.isDirectory() || (file.isFile() && (file.getName().endsWith(".jar") || file.getName().endsWith(".war")))) {
      BundleManifest bundleMf = BundleManifest.fromBundle(file);
      if (bundleMf != null) {
        String manifestVersion = bundleMf.getManifestVersion();
        String name = bundleMf.getSymbolicName();
        String version = bundleMf.getVersion().toString();

        // if the bundle manifest version is 2 AND a symbolic name is specified we have a valid bundle
        if ("2".equals(manifestVersion) && name != null) {

          builder.append(name);

          // bundle version is not a required manifest header
          if (version != null) {
            builder.append(";version=\"[");
            builder.append(version);
            builder.append(',');
            builder.append(version);
            builder.append("]\"");
          }

          // the last comma will be removed once all content has been added
          builder.append(",");
        }
      }
    }
  }
}
