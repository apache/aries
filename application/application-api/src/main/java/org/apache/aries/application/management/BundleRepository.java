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

import org.apache.aries.application.Content;
import org.apache.aries.application.DeploymentContent;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;

public interface BundleRepository {
  /**
   * Service property denoting the scope of the bundle repository. This can 
   * <ul>
   *  <li>global</li>
   *  <li>&lt;app symbolic name&gt;_&lt;app version&gt;</li>
   * </ul>
   */
  public static final String REPOSITORY_SCOPE = "repositoryScope";
  public static final String GLOBAL_SCOPE = "global";
  
	/**
	   * A suggested bundle to use.
	   */
	  public interface BundleSuggestion
	  {
	    /**
	     * Install the bundle represented by this suggestion via the given context
	     * 
	     * @param ctx The context of the framework where the bundle is to be install
	     * @param app The AriesApplication being installed
	     * @return the installed bundle
	     * @throws BundleException
	     */
	    public Bundle install(BundleContext ctx, 
	                          AriesApplication app) throws BundleException;
	    
	    /**
	     * Get the imports of the bundle 
	     * @return 
	     */
	    public Set<Content> getImportPackage();
	    
	    /**
	     * Get the exports of the bundle
	     * @return
	     */
	    public Set<Content> getExportPackage();
	    
	    /**
	     * @return the version of the bundle.
	     */
	    public Version getVersion();
	    /**
	     * This method can be queried to discover the cost of using this bundle 
	     * repository. If two repositories define the same bundle with the same version
	     * then the cheaper one will be used.
	     * 
	     * @return the cost of using this repository.
	     */
	    public int getCost();
	  }

	  /**
	   * This method attempts to find a bundle in the repository that matches the
	   * specified DeploymentContent specification. If none can be found then null must
	   * be returned.
	   * 
	   * @param content the content used to locate a bundle.
	   * @return      the bundle suggestion, or null.
	   */
	  public BundleSuggestion suggestBundleToUse(DeploymentContent content);
	  
	  /**
	   * This method can be queried to discover the cost of using this bundle 
	   * repository. If two repositories define the same bundle with the same version
	   * then the cheaper one will be used.
	   * 
	   * @return the cost of using this repository.
	   */
	  public int getCost();
}
