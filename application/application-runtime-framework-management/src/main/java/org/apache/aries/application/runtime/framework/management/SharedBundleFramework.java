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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.aries.application.runtime.framework.management;

import java.util.Properties;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.aries.application.management.BundleFramework;
import org.apache.aries.application.management.BundleFrameworkFactory;
import org.apache.aries.application.management.ContextException;

import static org.apache.aries.application.utils.AppConstants.LOG_ENTRY;
import static org.apache.aries.application.utils.AppConstants.LOG_EXIT;
import static org.apache.aries.application.utils.AppConstants.LOG_EXCEPTION;

public class SharedBundleFramework
{  
  private static final Logger LOGGER = LoggerFactory.getLogger(SharedBundleFramework.class);
  
  private static BundleFramework sharedFramework;
  private static final String SHARED_BUNDLE_FRAMEWORK = "shared.bundle.framework";

  /**
   * This is not the right way to make blueprint usable by applications, but it is all
   * we have time to make work. I have locked down the version ranges so that this can
   * be fixed properly in the future. NB The org.osgi.service.blueprint
   * package is deliberately unversioned as it is not part of the osgi compendium.
   */
  private static final String RUNTIME_PACKAGES = "org.osgi.service.blueprint,org.osgi.service.blueprint.container;version=\"[1.0.0,1.0.1]\",org.osgi.service.blueprint.reflect;version=\"[1.0.0,1.0.1]\",org.apache.aries.transaction.exception;version=\"[0.1,1.0.0)\"";

  /**
   * create using any bundle context in EBA App framework
   * as we want to create a child framework under EBA App framework
   * @param bc
   * @throws BundleException
   * @throws InvalidSyntaxException
   */
  private static void createSharedBundleFramework(BundleContext bc,
      BundleFrameworkFactory bundleFrameworkFactory) throws ContextException
  {
    LOGGER.debug(LOG_ENTRY, "createSharedBundleFramework", new Object[] {bc, bundleFrameworkFactory});

    try {
      // Set up the isolated framework
      Properties frameworkConfig = new Properties();
      frameworkConfig.put("osgi.console", "0");

      String osgiFrameworkLocation = bc.getProperty(FrameworkConstants.OSGI_FRAMEWORK);
      if (osgiFrameworkLocation != null) {

        frameworkConfig.put(FrameworkConstants.OSGI_FRAMEWORK, osgiFrameworkLocation);
        if (bc.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA) != null)
        {
          frameworkConfig.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, bc
            .getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA));
        }
      }
      
      Properties compositeManifest = new Properties();
      compositeManifest.put(Constants.BUNDLE_SYMBOLICNAME, SHARED_BUNDLE_FRAMEWORK);

      //Add blueprint so that it is available to applications.
      compositeManifest.put(Constants.IMPORT_PACKAGE, RUNTIME_PACKAGES);

      sharedFramework = bundleFrameworkFactory.createBundleFramework(bc, SHARED_BUNDLE_FRAMEWORK,
          frameworkConfig, compositeManifest);

      sharedFramework.init();
      
    } catch (BundleException e) {
      LOGGER.debug(LOG_EXIT, "createSharedBundleFramework", e);
      throw new ContextException(
          "Unable to create or start the shared framework composite bundle " + sharedFramework, e);
    }

    LOGGER.debug(LOG_EXIT, "createSharedBundleFramework");
  }

  /**
   * pass in the EBA framework bundle context and get the shared bundle
   * framework associated with the bundle context
   * @param bc      any bundle context in EBA framework
   * @return        the composite bundle associated with the shared bundle framework
   * @throws BundleException
   * @throws InvalidSyntaxException
   * @throws SharedFrameworkCreationException
   */
  public synchronized static BundleFramework getSharedBundleFramework(BundleContext bc,
      BundleFrameworkFactory bff) throws ContextException
  {
    LOGGER.debug(LOG_ENTRY, "getSharedBundleFramework", new Object[] {bc, bff});
    
    if (sharedFramework == null) {
      createSharedBundleFramework(bc, bff);
    }
    
    LOGGER.debug(LOG_EXIT, "getSharedBundleFramework", sharedFramework);

    return sharedFramework;
  }
}
