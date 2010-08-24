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
package org.apache.aries.application.management.repository;

import java.util.Collections;
import java.util.Set;

import org.apache.aries.application.Content;
import org.apache.aries.application.DeploymentContent;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationResolver;
import org.apache.aries.application.management.BundleInfo;
import org.apache.aries.application.management.BundleRepository;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;

public class ApplicationRepository implements BundleRepository
{
  private static final int REPOSITORY_COST = 0;
  public static final String REPOSITORY_SCOPE = "repositoryScope";
  private AriesApplication app;
  AriesApplicationResolver resolver;

  public ApplicationRepository(AriesApplication app)
  {
    this.app = app;
  }
  
  public int getCost()
  {
    return REPOSITORY_COST;
  }

  public BundleSuggestion suggestBundleToUse(DeploymentContent content)
  {
    return new BundleSuggestionImpl(content);
  }

  private class BundleSuggestionImpl implements BundleSuggestion
  {
    private BundleInfo bundleInfo = null;
    
    BundleSuggestionImpl(DeploymentContent content)
    {
      if ((app.getBundleInfo() != null) && (!app.getBundleInfo().isEmpty())) {
        for (BundleInfo bi : app.getBundleInfo()) {
          if (bi.getSymbolicName().equals(content.getContentName()) && (bi.getVersion().equals(content.getVersion().getExactVersion()))) {
            bundleInfo = bi;
            break;
          }
        }
      }
    }

    public int getCost()
    {
      return REPOSITORY_COST;
    }

    public Set<Content> getExportPackage()
    {
      if (bundleInfo != null) {
      return bundleInfo.getExportPackage();
      } else {
        return null;
      }
    }

    public Set<Content> getImportPackage()
    {
      if (bundleInfo != null) {
        return bundleInfo.getImportPackage();
        } else {
          return null;
        }
      
    }

    public Version getVersion() 
    {
      if (bundleInfo != null) {
        return bundleInfo.getVersion();
        } else {        	
          return null;
        }
      
    }

    public Bundle install(BundleContext ctx, AriesApplication app) throws BundleException
    {
      if (bundleInfo != null ) {
        return ctx.installBundle(bundleInfo.getLocation());
      } else {
        throw new BundleException("Unable to install the bundle, as the BundleInfo is null.");
      }
    }
    
  }
  
}
