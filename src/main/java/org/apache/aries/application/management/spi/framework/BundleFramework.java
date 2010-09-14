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

import java.util.List;

import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.spi.repository.BundleRepository.BundleSuggestion;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public interface BundleFramework
{
  /**
   * Initialises the framework
   * @throws BundleException
   */
  public void init() throws BundleException;
  
  /**
   * Closes the framework and any associated resource
   * @throws BundleException
   */
  public void close() throws BundleException;
  
  /**
   * Installs a bundle to this framework.
   * @param suggestion The information required to install the bundle
   * @param app The application with which this install is associated
   * @return
   * @throws BundleException
   */
  public Bundle install(BundleSuggestion suggestion, AriesApplication app) throws BundleException;
  
  /**
   * Removes a bundle from this framework
   * @param b The bundle to remove
   * @throws BundleException
   */
  public void uninstall(Bundle b) throws BundleException;
  
  /**
   * Start a previously installed bundle in this framework. 
   * @param b
   * @throws BundleException
   */
  public void start(Bundle b) throws BundleException;

  /**
   * Stop a previously installed bundle in this framework. 
   * @param b
   * @throws BundleException
   */
  public void stop(Bundle b) throws BundleException;

  /**
   * Returns the bundle context for the framework.
   * @return
   */
  public BundleContext getIsolatedBundleContext();

  /**
   * Returns the OSGi bundle representing the framework
   * @return
   */
  public Bundle getFrameworkBundle();
  
  /**
   * Returns a list of bundles currently installed in this framework
   * @return
   */
  public List<Bundle> getBundles();
  
}
