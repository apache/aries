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

package org.apache.aries.application.runtime.framework.config;

import java.util.Collection;
import java.util.Properties;

import org.apache.aries.application.Content;
import org.apache.aries.application.DeploymentMetadata;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.spi.framework.BundleFrameworkConfiguration;
import org.apache.aries.application.management.spi.framework.BundleFrameworkConfigurationFactory;
import org.apache.aries.application.runtime.framework.utils.EquinoxFrameworkConstants;
import org.apache.aries.application.runtime.framework.utils.EquinoxFrameworkUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;

public class BundleFrameworkConfigurationFactoryImpl implements BundleFrameworkConfigurationFactory
{

  public BundleFrameworkConfiguration createBundleFrameworkConfig(String frameworkId,
      BundleContext parentCtx, AriesApplication app)
  {

    BundleFrameworkConfiguration config = null;
    DeploymentMetadata metadata = app.getDeploymentMetadata();
    /**
     * Set up framework config properties
     */
    Properties frameworkConfig = new Properties();

    String flowedSystemPackages = EquinoxFrameworkUtils.calculateSystemPackagesToFlow(
        EquinoxFrameworkUtils.getSystemExtraPkgs(parentCtx), metadata.getImportPackage());
    frameworkConfig.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, flowedSystemPackages);

    /**
     * Set up BundleManifest for the framework bundle
     */
    Properties frameworkBundleManifest = new Properties();
    frameworkBundleManifest.put(Constants.BUNDLE_SYMBOLICNAME, metadata
        .getApplicationSymbolicName());
    frameworkBundleManifest.put(Constants.BUNDLE_VERSION, metadata.getApplicationVersion()
        .toString());

    /**
     * Set up Import-Package header for framework manifest
     */
    // Extract the import packages and remove anything we already have available in the current framework
    Collection<Content> imports = EquinoxFrameworkUtils.calculateImports(metadata
        .getImportPackage(), EquinoxFrameworkUtils.getExportPackages(parentCtx));

    if (imports != null && !imports.isEmpty()) {
      StringBuffer buffer = new StringBuffer();
      for (Content i : imports)
        buffer.append(EquinoxFrameworkUtils.contentToString(i) + ",");
      frameworkBundleManifest.put(Constants.IMPORT_PACKAGE, buffer
          .substring(0, buffer.length() - 1));
    }

    /**
     * Set up CompositeServiceFilter-Import header for framework manifest
     */
    StringBuffer serviceImportFilter = new StringBuffer("(" + Constants.OBJECTCLASS + "="
        + EquinoxFrameworkConstants.TRANSACTION_REGISTRY_BUNDLE + ")");

    for (Filter importFilter : metadata.getDeployedServiceImport()) {
      if (serviceImportFilter.length() > 0) {
        serviceImportFilter.append(",");
      }
      serviceImportFilter.append(importFilter.toString());
    }

    frameworkBundleManifest.put(EquinoxFrameworkConstants.COMPOSITE_SERVICE_FILTER_IMPORT,
        serviceImportFilter.toString());

    config = new BundleFrameworkConfigurationImpl(frameworkId, frameworkConfig,
        frameworkBundleManifest);

    return config;
  }

  public BundleFrameworkConfiguration createBundleFrameworkConfig(String frameworkId,
      BundleContext parentCtx)
  {
    BundleFrameworkConfiguration config = null;

    /**
     * Set up framework config properties
     */
    Properties frameworkConfig = new Properties();

    if (parentCtx.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA) != null)
      frameworkConfig.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, parentCtx
          .getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA));

    /**
     * Set up BundleManifest for the framework bundle
     */
    Properties frameworkBundleManifest = new Properties();

    /**
     * Set up CompositeServiceFilter-Import header for framework manifest
     */
    StringBuffer serviceImportFilter = new StringBuffer("(" + Constants.OBJECTCLASS + "="
        + EquinoxFrameworkConstants.TRANSACTION_REGISTRY_BUNDLE + ")");

    frameworkBundleManifest.put(EquinoxFrameworkConstants.COMPOSITE_SERVICE_FILTER_IMPORT,
        serviceImportFilter.toString());

    config = new BundleFrameworkConfigurationImpl(frameworkId, frameworkConfig,
        frameworkBundleManifest);

    return config;
  }
}
