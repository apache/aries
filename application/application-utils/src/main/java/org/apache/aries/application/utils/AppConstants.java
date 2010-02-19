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
public interface AppConstants
{
  /** Trace group for this bundle */
  public String TRACE_GROUP = "Aries.app.utils";

  /** The Provision-Content header for the deployment.mf */
  public static final String PROVISION_CONTENT = "Provision-Bundle";
  
  /** The application scope (used to find the applications bundle repository */
  public static final String APPLICATION_SCOPE = "Application-Scope";
  /** The application content directive for the application manifest */
  public static final String APPLICATION_CONTENT = "Application-Content";
  /** The application version directive for the application manifest */
  public static final String APPLICATION_VERSION = "Application-Version";
  /** The application name directive for the application manifest */
  public static final String APPLICATION_NAME = "Application-Name";
  /** The application symbolic name directive for the application manifest */
  public static final String APPLICATION_SYMBOLIC_NAME = "Application-SymbolicName";
  /** The default version for applications that do not have one */
  public static final String DEFAULT_VERSION = "0.0.0";
  /** The name of the application manifest in the application */
  public static final String APPLICATION_MF = "META-INF/APPLICATION.MF";
  /** The name of the deployment manifest in the application */
  public static final String DEPLOYMENT_MF = "META-INF/DEPLOYMENT.MF";
  /** The name of the META-INF directory   */
  public static final String META_INF = "META-INF";
  /** The name of an application.xml file which will be used in processing legacy .war files */
  public static final String APPLICATION_XML = "META-INF/application.xml";
  /** The expected lower case suffix of a jar file */
  public static final String LOWER_CASE_JAR_SUFFIX = ".jar";
  /** The expected lower case suffix of a war file */
  public static final String LOWER_CASE_WAR_SUFFIX = ".war";
  /** The attribute used to record the deployed version of a bundle */
  public static final String DEPLOYMENT_BUNDLE_VERSION = "deployed-version";
  /** The name of the bundle manifest */
  public static final String MANIFEST_MF = "META-INF/MANIFEST.MF";
  
  public static final String MANIFEST_VERSION="1.0";
  
  /* The Deployed-Content header in DEPLOYMENT.MF records all the bundles
   * to be deployed for a particular application. 
   */
  public static final String DEPLOYMENT_CONTENT = "Deployed-Content";
}
