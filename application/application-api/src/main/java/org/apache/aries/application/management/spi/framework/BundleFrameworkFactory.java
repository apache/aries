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

package org.apache.aries.application.management.spi.framework;

import java.util.Properties;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public interface BundleFrameworkFactory
{
  /**
   * Creates a new isolated bundle framework with the properties provided. 
   * @param bc The context in which to install the new framework
   * @param frameworkId The id of the new framework
   * @param frameworkConfig The config properties used to configure the new framework
   * @param frameworkManifest The manifest used to install the new bundle associated with the framework
   * @return
   * @throws BundleException
   */
  public BundleFramework createBundleFramework(BundleContext bc, 
      String frameworkId,
      Properties frameworkConfig,
      Properties frameworkManifest) throws BundleException;
}
