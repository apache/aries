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
package org.apache.aries.application.utils;

/**
 * Widely used constants in parsing Aries applications
 */
public interface AppConstants {
  /** The Manifest version */
  String APPLICATION_MANIFEST_VERSION="Manifest-Version";
  
  /** The application content directive for the application manifest */
  String APPLICATION_CONTENT = "Application-Content";
  /** The application version directive for the application manifest */
  String APPLICATION_VERSION = "Application-Version";
  /** The application name directive for the application manifest */
  String APPLICATION_NAME = "Application-Name";
  /** The application symbolic name directive for the application manifest */
  String APPLICATION_SYMBOLIC_NAME = "Application-SymbolicName";
  /** The attribute used to record the deployed version of a bundle */
  String DEPLOYMENT_BUNDLE_VERSION = "deployed-version";

  String MANIFEST_VERSION="1.0";
  /** The application import service directive for the application manifest */
  String APPLICATION_IMPORT_SERVICE = "Application-ImportService";
  /** The application export service directive for the application manifest */
  String APPLICATION_EXPORT_SERVICE = "Application-ExportService";
  /** The use-bundle entry for the application manifest. */
  String APPLICATION_USE_BUNDLE = "Use-Bundle";
  /* The Deployed-Content header in DEPLOYMENT.MF records all the bundles
   * to be deployed for a particular application. 
   */
  String DEPLOYMENT_CONTENT = "Deployed-Content";
  /** deployment.mf entry corresponding to application.mf Use-Bundle. */
  String DEPLOYMENT_USE_BUNDLE = "Deployed-Use-Bundle";
  /** deployment.mf entry 'Import-Package' */
  String DEPLOYMENT_IMPORT_PACKAGES="Import-Package";
  /** Bundle dependencies required by bundles listed in Deployed-Content or Deployed-Use-Bundle. */
  String DEPLOYMENT_PROVISION_BUNDLE = "Provision-Bundle";
  /** Blueprint managed services imported by the isolated bundles */ 
  String DEPLOYMENTSERVICE_IMPORT = "DeployedService-Import";
}
