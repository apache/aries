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

package org.apache.aries.application.management.spi.resolve;

import java.util.Collection;
import java.util.Set;

import org.apache.aries.application.ApplicationMetadata;
import org.apache.aries.application.Content;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationManager;
import org.apache.aries.application.management.BundleInfo;
import org.apache.aries.application.management.ResolveConstraint;
import org.apache.aries.application.management.ResolverException;
import org.apache.aries.application.modelling.ModelledResource;
import org.osgi.framework.Version;

/**
 * An {@code AriesApplicationResolver} is a service used by the {@link AriesApplicationManager} when one of the
 * {@code createApplication} methods are called. It is used to "deploy" the application. The "deploy" process
 * generates an Aries Deployment manifest <a href="http://incubator.apache.org/aries/applications.html"/>See
 * the design documentation</a>.
 * 
 * <p>The {@code AriesApplicationManager} calls the resolve method in order to determine which bundles are required.
 * </p>
 */
public interface AriesApplicationResolver {

  /** 
   * Deprecated. Use the method resolve(String appName, String appVersion, Collection<ModelledResource> byValueBundles, Collection<Content> inputs) throws ResolverException;
   * Resolve an AriesApplication. The implementation of this method is expected to do the following:
   * 
   * <ol>
   *   <li>Extract from the {@link AriesApplication}'s the application's content. This is performed
   *     using the {@link AriesApplication#getApplicationMetadata()} method following by calling 
   *     {@link ApplicationMetadata#getApplicationContents()}.
   *   </li>
   *   <li>Resolve the application content using any configured repositories for bundles, and the
   *     bundles that are contained by value inside the application. These bundles can be obtained
   *     by calling {@link AriesApplication#getBundleInfo()}.
   *   </li>
   * </ol>
   * 
   * The method returns the set of bundle info objects that should be used.
   * 
   * @param app The application to resolve
   * @return The additional bundles required to ensure that the application resolves. This
   *         set will not include those provided by value within the application.
   * @throws ResolverException if the application cannot be resolved.  
   */
  @Deprecated
  Set<BundleInfo> resolve (AriesApplication app, ResolveConstraint... constraints) throws ResolverException ;

  /** 
   * Return the info for the requested bundle. This method is called when installing
   * an application to determine where the bundle is located in order to install it.
   * 
   * <p>If no matching bundle exists in the resolver runtime then null is returned.</p>
   * 
   * @param bundleSymbolicName the bundle symbolic name.
   * @param bundleVersion      the version of the bundle
   * @return the BundleInfo for the requested bundle, or null if none could be found.
   */
  BundleInfo getBundleInfo(String bundleSymbolicName, Version bundleVersion);
  /**
   * Resolve an AriesApplication. The resolving process will build a repository from by-value bundles. 
   * It then scans all the required bundles and pull the dependencies required to resolve the bundles.
   * 
   * 
   * Return a collect of modelled resources. This method is called when installing an application
   * @param appName Application name
   * @param appVersion application version
   * @param byValueBundles by value bundles
   * @param inputs bundle requirement
   * @return a collection of modelled resource required by this application.
   * @throws ResolverException
   */
  Collection<ModelledResource> resolve(String appName, String appVersion, Collection<ModelledResource> byValueBundles, Collection<Content> inputs) throws ResolverException;

}
