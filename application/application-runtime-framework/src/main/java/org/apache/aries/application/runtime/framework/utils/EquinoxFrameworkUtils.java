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
package org.apache.aries.application.runtime.framework.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.aries.application.Content;
import org.apache.aries.application.utils.manifest.ContentFactory;
import org.apache.aries.util.VersionRange;
import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.NameValueMap;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.NameValuePair;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class EquinoxFrameworkUtils
{

  public static Collection<Content> getExportPackages(BundleContext isolatedBundleContext)
  {
    Set<Content> exports = new HashSet<Content>();

    Bundle sysBundle = isolatedBundleContext.getBundle(0);
    if (sysBundle != null && sysBundle.getHeaders() != null) {
      String exportString = (String) sysBundle.getHeaders().get(Constants.EXPORT_PACKAGE);
      if (exportString != null) {
        for (NameValuePair<String, NameValueMap<String, String>> nvp : ManifestHeaderProcessor
            .parseExportString(exportString))
          exports.add(ContentFactory.parseContent(nvp.getName(), nvp.getValue()));
      }
    }
    return Collections.unmodifiableSet(exports);
  }
  
  public static Collection<Content> getSystemExtraPkgs(BundleContext context)
  {
    Set<Content> extraPkgs = new HashSet<Content>();
    
      String exportString = context.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA);
      if (exportString != null) {
        for (NameValuePair<String, NameValueMap<String, String>> nvp : ManifestHeaderProcessor
            .parseExportString(exportString))
          extraPkgs.add(ContentFactory.parseContent(nvp.getName(), nvp.getValue()));
      }
    
    return Collections.unmodifiableSet(extraPkgs);
  }

  public static Collection<Content> calculateImports(final Collection<Content> importPackage,
      final Collection<Content> exportPackages)
  {
    Set<Content> results = new HashSet<Content>();
    if (importPackage != null && !importPackage.isEmpty()) {
      for (Content exportPkg : exportPackages) {
        for (Content importPkg : importPackage) {
          if (!(importPkg.getContentName().equals(exportPkg.getContentName())
              && importPkg.getVersion().equals(exportPkg.getVersion()))) {
            results.add(importPkg);
          }
        }
      }
    }
    return Collections.unmodifiableSet(results);
  }

  public static String contentToString(Content content)
  {
    StringBuffer value = new StringBuffer();
    value.append(content.getContentName());

    Map<String, String> nvm = content.getNameValueMap();

    for (Map.Entry<String, String> entry : nvm.entrySet()) {
      if (entry.getKey().equalsIgnoreCase(Constants.VERSION_ATTRIBUTE) || entry.getKey().equalsIgnoreCase(Constants.BUNDLE_VERSION_ATTRIBUTE)) {
        value.append(";" + entry.getKey() + "=\"" + entry.getValue() + "\"");
      } else {
        value.append(";" + entry.getKey() + "=" + entry.getValue());
      }
    }

    return value.toString();
  }
  
  /**
   * Calculates which system packages should be flowed 
   * to a child framework based on what packages the 
   * child framework imports. Equinox will require anything imported by the 
   * child framework which is available from the system bundle 
   * in the parent framework to come from the system bundle 
   * in the child framework. However, we don't want to flow 
   * all the extra system packages by default since we want CBAs 
   * which use them to explicitly import them.
   * @param importPackage
   * @return
   * @throws CompositeBundleCalculateException 
   */
  public static String calculateSystemPackagesToFlow(final Collection<Content> systemExports, 
      final Collection<Content> imports)
  {

    // Let's always set javax.transaction as system extra packages because of the split package. 
    // It is reasonable to do so because we always flow userTransaction service into child framework anyway.
    NameValueMap<String, String> map = new NameValueMap<String, String>();
    map.put(EquinoxFrameworkConstants.TRANSACTION_BUNDLE, EquinoxFrameworkConstants.TRANSACTION_BUNDLE_VERSION);
    Map<String, Map<String, String>> resultMap = new HashMap<String, Map<String, String>>();
    resultMap.put(EquinoxFrameworkConstants.TRANSACTION_BUNDLE, map);

    // let's go through the import list to build the resultMap
    for (Content nvp : imports) {
      String name = nvp.getContentName().trim();
      // if it exist in the list of packages exported by the system, we need to add it to the result
      if (existInExports(name, nvp.getNameValueMap(), systemExports)) {
        /* We've now ensured the versions match, but not worried too 
        * much about other constraints like company or any of the 
        * other things which could be added to a version statement. 
        * We don't want to flow system packages we don't need to, 
        * but we're not in the business of provisioning, so we'll
        * let OSGi decide whether every constraint is satisfied
        * and resolve the bundle or not, as appropriate.
        */

        for (Content nvpp : systemExports) {
          if (nvpp.getContentName().trim().equals(name)) {
            Map<String, String> frameworkVersion = nvpp.getNameValueMap();
            resultMap.put(name, frameworkVersion);
            // We don't break here since we're too lazy to check the version
            // again and so we might end up flowing multiple statements for the 
            // same package (but with different versions). Better this than 
            // accidentally flowing the wrong version if we hit it first.
          }
        }
      }
    }

    StringBuffer result = new StringBuffer();
    for (String key : resultMap.keySet()) {
      result.append(getString(key, resultMap) + ",");
    }
    String toReturn = trimEndString(result.toString().trim(), ",");

    return toReturn;

  }

  /**
   * check if the value in nvm already exist in the exports
   * @param key
   * @param nvm
   * @param exports
   * @return boolean whether the value in nvm already exist in the exports
   */
  private static boolean existInExports(String key, Map<String, String> nvm,
      final Collection<Content> exports)
  {
    boolean value = false;
    for (Content nvp : exports) {
      if (nvp.getContentName().trim().equals(key.trim())) {
        // ok key equal.  let's check the version
        // if version is higher, we still want to import, for example javax.transaction;version=1.1
        String vi = nvm.get(Constants.VERSION_ATTRIBUTE);
        String ve = nvp.getNameValueMap().get(Constants.VERSION_ATTRIBUTE);
        if (vi == null || vi.length() == 0) {
          vi = "0.0.0";
        }

        if (ve == null || ve.length() == 0) {
          ve = "0.0.0";
        }

        if (vi.indexOf(",") == -1) {

          if (new Version(vi).compareTo(new Version(ve)) <= 0) {
            // we got it covered in our exports
            value = true;
          }
        } else {
          // parse vi into version range.
          VersionRange vri = ManifestHeaderProcessor.parseVersionRange(vi);
          Version minV = vri.getMinimumVersion();
          Version maxV = vri.getMaximumVersion();
          if (minV.compareTo(new Version(ve)) < 0 && maxV.compareTo(new Version(ve)) > 0) {
            value = true;
          } else if (minV.compareTo(new Version(ve)) == 0 && !!!vri.isMinimumExclusive()) {
            value = true;
          } else if (maxV.compareTo(new Version(ve)) == 0 && !!!vri.isMaximumExclusive()) {
            value = true;
          }
        }

      }
    }
    
    return value;
  }
 
  private static String trimEndString(String s, String trim)
  {
    if (s.startsWith(trim)) {
      s = s.substring(trim.length());
    }
    if (s.endsWith(trim)) {
      s = s.substring(0, s.length() - trim.length());
    }
    return s;
  }

  private static String getString(String key, Map<String, Map<String, String>> imports)
  {
    StringBuffer value = new StringBuffer();
    value.append(key);

    Map<String, String> nvm = imports.get(key);

    for (Map.Entry<String, String> entry : nvm.entrySet()) {
      if (entry.getKey().equalsIgnoreCase(Constants.VERSION_ATTRIBUTE) || entry.getKey().equalsIgnoreCase(Constants.BUNDLE_VERSION_ATTRIBUTE)) {
        value.append(";" + entry.getKey() + "=\"" + entry.getValue() + "\"");
      } else {
        value.append(";" + entry.getKey() + "=" + entry.getValue());
      }
    }

    return value.toString();
  }
}
