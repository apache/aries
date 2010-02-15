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

import java.util.Set;

import org.osgi.framework.Version;

public interface AriesApplicationResolver {

  /** Resolve an AriesApplication 
   * 
   * @param app The application to resolve
   * @return The additional bundles required to ensure that the application resolves. This
   *         set will not include those provided by value within the application.
   * @throws ResolverException if the application cannot be resolved.  
   */
  Set<BundleInfo> resolve (AriesApplication app) throws ResolverException ;

  /** 
   * Return the info for the requested bundle. If no matching bundle exists in the
   * resolver runtime then null is returned.
   * 
   * @param bundleSymbolicName the bundle symbolic name.
   * @param bundleVersion      the version of the bundle
   * @return the BundleInfo for the requested bundle, or null if none could be found.
   */
  BundleInfo getBundleInfo(String bundleSymbolicName, Version bundleVersion);
}
