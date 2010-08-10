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
package org.apache.aries.application.utils.runtime;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.aries.application.Content;
import org.apache.aries.application.impl.ContentImpl;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor.NameValueMap;
import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor.NameValuePair;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

public class InstallUtils
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
          exports.add(new ContentImpl(nvp.getName(), nvp.getValue()));
      }
    }
    return Collections.unmodifiableSet(exports);
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

}
