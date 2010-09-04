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


package org.apache.aries.application.management;

import java.net.URL;

import org.apache.aries.application.DeploymentMetadata;
import org.apache.aries.application.filesystem.IDirectory;
import org.osgi.framework.BundleException;

/**
 * An AriesApplicationManager service is used to create, install and uninstall Aries
 * applications. 
 */
public interface AriesApplicationManager
{
  /**
   * Create an AriesApplication from a local resource.
   * The application won't be automatically resolved if the
   * archive does not contain a deployment manifest.
   *
   * @param source .eba file, or exploded directory
   * @return AriesApplication
   * @throws ManagementException
   */
  public AriesApplication createApplication(IDirectory source) throws ManagementException;
  
  /**
   * Create an AriesApplication from a remote resource.
   * The application won't be automatically resolved if the
   * archive does not contain a deployment manifest.
   *
   * @param url
   * @return
   * @throws ManagementException
   */
  public AriesApplication createApplication(URL url) throws ManagementException;
  
  /**
   * Install an AriesApplication - i.e. load its bundles into the runtime, but do 
   * not start them.
   * If the application is not resolved, a call to {@link #resolve(AriesApplication, ResolveConstraint...)}
   * will be performed and the resolved application will be installed.  In such a case the resolved
   * application can be obtained by calling {@link org.apache.aries.application.management.AriesApplicationContext#getApplication()}
   * on the returned ApplicationContext.
   *
   * @param app Application to install 
   * @return AriesApplicationContext, a handle to an application in the runtime
   * @throws BundleException
   * @throws ManagementException 
   */
  public AriesApplicationContext install(AriesApplication app) throws BundleException, ManagementException, ResolverException;
  
  /**
   * Uninstall an AriesApplication - i.e. unload its bundles from the runtime. 
   * @param app The installed application to uninstall
   * @throws BundleException
   */
  public void uninstall(AriesApplicationContext app) throws BundleException;
  
  /**
   * Add an AriesApplicationListener
   * @param l
   */
  public void addApplicationListener(AriesApplicationListener l);
  
  /**
   * Remove an AriesApplicationListener
   * @param l
   */
  public void removeApplicationListener(AriesApplicationListener l);
  
  /**
   * Update an application's deployment and apply the changes to the runtime if the application is deployed
   * @param app The application to change
   * @param depMf The new deployment metadata
   * @return {@link AriesApplicationContext} Returns a new application context if the app
   * is currently deployed, or null if the app is not currently installed
   * 
   * @throws UpdateException if the deployment changes could not be effected in the runtime
   * @throws IllegalArgumentException if the deployment metadata does not correspond to the same application
   * (same symbolic name and same version)
   */
  public AriesApplicationContext update(AriesApplication app, DeploymentMetadata depMf) throws UpdateException;
  
  /**
   * Resolve an AriesApplication against a set of constraints. Each ResolveConstraint
   * represents a single proposed change to the content of an application.
   * If no constraints are given, a default resolution will be performed.
   *
   * @param originalApp Original application
   * @param constraints Constraints
   * @throws ResolverException
   * @return New AriesApplication
   */
  AriesApplication resolve (AriesApplication originalApp, ResolveConstraint ... constraints)
    throws ResolverException;
}
